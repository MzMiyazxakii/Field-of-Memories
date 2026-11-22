package com.fieldofmemories.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;

public class SpiritEntity extends PathfinderMob {
    private static final String CIRCLING_KEY = "SpiritCircling";
    private static final String CIRCLE_TICKS_KEY = "SpiritCircleTicks";
    private static final int CIRCLE_DURATION = 300;
    private static final double APPROACH_DISTANCE = 8.0D;
    private static final double ORBIT_RADIUS = 5.5D;

    private boolean circling;
    private int circleTicks;
    private int orbitRefresh;

    public SpiritEntity(EntityType<? extends SpiritEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        // The spirit uses its own approach-and-circle behavior in tick().
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // Footsteps are intentionally silent; the presence sound is distance-based.
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        if (tickCount > 2400) {
            discard();
            return;
        }

        Player target = level().getEntitiesOfClass(Player.class,
                        getBoundingBox().inflate(48.0D, 16.0D, 48.0D))
                .stream()
                .filter(player -> player.isAlive() && !player.isSpectator() && !player.isCreative())
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);

        if (target == null) {
            getNavigation().stop();
            return;
        }

        getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distanceSqr = distanceToSqr(target);

        if (!circling) {
            if (distanceSqr > APPROACH_DISTANCE * APPROACH_DISTANCE) {
                getNavigation().moveTo(target, 0.46D);
            } else {
                circling = true;
                circleTicks = 0;
                orbitRefresh = 0;
                getNavigation().stop();
            }
            return;
        }

        circleTicks++;
        orbitRefresh++;
        if (orbitRefresh >= 35 || getNavigation().isDone()) {
            orbitRefresh = 0;
            double angle = level().random.nextDouble() * Math.PI * 2.0D;
            double x = target.getX() + Math.cos(angle) * ORBIT_RADIUS;
            double z = target.getZ() + Math.sin(angle) * ORBIT_RADIUS;
            getNavigation().moveTo(x, target.getY(), z, 0.38D);
        }

        if (circleTicks >= CIRCLE_DURATION) {
            for (ServerPlayer player : ((net.minecraft.server.level.ServerLevel) level())
                    .getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(16.0D))) {
                if (!player.isAlive() || player.isCreative() || player.isSpectator()
                        || distanceToSqr(player) > 16.0D * 16.0D) continue;
                if (player.getHealth() > 1.0F) player.setHealth(1.0F);
                com.fieldofmemories.network.NetworkHandler.forceFall(player);
            }
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(CIRCLING_KEY, circling);
        tag.putInt(CIRCLE_TICKS_KEY, circleTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        circling = tag.getBoolean(CIRCLING_KEY);
        circleTicks = Math.max(0, Math.min(CIRCLE_DURATION, tag.getInt(CIRCLE_TICKS_KEY)));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
