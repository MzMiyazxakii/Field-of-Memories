package com.fieldofmemories.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
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
    private static final double ORBIT_RADIUS = 4.5D;

    private boolean circling;
    private int circleTicks;
    private int orbitRefresh;
    private double orbitAngle;

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
    public boolean hurt(DamageSource source, float amount) {
        // Ordinary weapons and damage cannot hurt the spirit. The photo flash removes it separately.
        return false;
    }

    private void attackPlayer(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isCreative() || player.isSpectator()) return;
        com.fieldofmemories.item.CrossItem.tryDepleteOnAttack(player);
        level().playSound(null, player.blockPosition(), com.fieldofmemories.FieldOfMemories.SCREAMSOUL_SOUND.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.82F);
        if (player.getHealth() > 1.0F) player.setHealth(1.0F);
        com.fieldofmemories.network.NetworkHandler.forceFall(player);
        discard();
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

        // Touching the spirit is an immediate attack; no circling timer is required.
        for (Player nearby : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(0.15D))) {
            if (nearby.isAlive() && !nearby.isCreative() && !nearby.isSpectator()
                    && getBoundingBox().intersects(nearby.getBoundingBox())
                    && nearby instanceof ServerPlayer serverPlayer) {
                attackPlayer(serverPlayer);
                return;
            }
        }

        double distanceSqr = distanceToSqr(target);
        if (!circling) {
            if (distanceSqr > APPROACH_DISTANCE * APPROACH_DISTANCE) {
                getNavigation().moveTo(target, 0.46D);
            } else {
                circling = true;
                circleTicks = 0;
                orbitRefresh = 0;
                orbitAngle = Math.atan2(getZ() - target.getZ(), getX() - target.getX());
                getNavigation().stop();
            }
            return;
        }

        if (distanceSqr > 12.0D * 12.0D) {
            circling = false;
            getNavigation().moveTo(target, 0.46D);
            return;
        }

        circleTicks++;
        orbitAngle += 0.06D;
        orbitRefresh++;
        if (orbitRefresh >= 10 || getNavigation().isDone()) {
            orbitRefresh = 0;
            double x = target.getX() + Math.cos(orbitAngle) * ORBIT_RADIUS;
            double z = target.getZ() + Math.sin(orbitAngle) * ORBIT_RADIUS;
            getNavigation().moveTo(x, target.getY(), z, 0.42D);
        }

        if (circleTicks >= CIRCLE_DURATION) {
            for (ServerPlayer player : ((net.minecraft.server.level.ServerLevel) level())
                    .getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(16.0D))) {
                if (!player.isAlive() || player.isCreative() || player.isSpectator()
                        || distanceToSqr(player) > 16.0D * 16.0D) continue;
                attackPlayer(player);
                return;
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
