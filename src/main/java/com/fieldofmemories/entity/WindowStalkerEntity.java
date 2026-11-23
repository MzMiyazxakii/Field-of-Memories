package com.fieldofmemories.entity;

import com.fieldofmemories.data.BookProgress;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

public class WindowStalkerEntity extends PathfinderMob {
    private int noticedTicks;
    private int fleeingTicks;
    private boolean fleeing;
    private UUID fleeingFrom;

    public WindowStalkerEntity(EntityType<? extends WindowStalkerEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override protected void registerGoals() {}

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (tickCount > 900) { discard(); return; }

        ServerPlayer closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : ((ServerLevel) level()).getEntitiesOfClass(
                ServerPlayer.class, getBoundingBox().inflate(36.0D, 18.0D, 36.0D))) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) continue;
            double distance = distanceToSqr(player);
            if (distance < closestDistance) { closestDistance = distance; closest = player; }
            if (distance < 16.0D) {
                Vec3 away = position().subtract(player.position());
                if (away.lengthSqr() < 0.01D) away = new Vec3(1, 0, 0);
                Vec3 destination = position().add(away.normalize().scale(18.0D));
                int x = (int) Math.floor(destination.x);
                int z = (int) Math.floor(destination.z);
                int y = ((ServerLevel) level()).getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                teleportTo(x + 0.5D, y, z + 0.5D);
                fleeing = true;
                fleeingFrom = player.getUUID();
                fleeingTicks = Math.max(fleeingTicks, 1);
                break;
            }
        }
        if (closest == null) {
            if (fleeing && ++fleeingTicks > 100) discard();
            return;
        }

        if (!fleeing) {
            Vec3 toEntity = position().subtract(closest.getEyePosition()).normalize();
            boolean noticed = closest.hasLineOfSight(this) && toEntity.dot(closest.getLookAngle()) > 0.982D;
            if (noticed) BookProgress.discover(closest, BookProgress.WINDOW_STALKER);
            noticedTicks = noticed ? noticedTicks + 1 : Math.max(0, noticedTicks - 2);
            if (noticedTicks >= 12) {
                fleeing = true;
                fleeingFrom = closest.getUUID();
                fleeingTicks = 0;
            }
            getLookControl().setLookAt(closest, 20.0F, 20.0F);
        }

        if (fleeing) {
            ServerPlayer observer = fleeingFrom == null ? null
                    : ((ServerLevel) level()).getServer().getPlayerList().getPlayer(fleeingFrom);
            // Once it has escaped the observer's view, remove it instead of letting it run forever.
            if (observer == null || !observer.isAlive() || observer.serverLevel() != level()
                    || distanceToSqr(observer) > 48.0D * 48.0D) {
                discard();
                return;
            }
            fleeingTicks++;
            Vec3 away = position().subtract(observer.position());
            if (away.lengthSqr() < 0.01D) away = new Vec3(1, 0, 0);
            Vec3 destination = position().add(away.normalize().scale(22.0D));
            getNavigation().moveTo(destination.x, destination.y, destination.z, 2.0D);
            getLookControl().setLookAt(destination.x, destination.y + 1.0D, destination.z);
            if (fleeingTicks > 100) discard();
        }
    }

    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override public boolean removeWhenFarAway(double distance) { return false; }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("NoticedTicks", noticedTicks);
        tag.putInt("FleeingTicks", fleeingTicks);
        tag.putBoolean("Fleeing", fleeing);
        if (fleeingFrom != null) tag.putUUID("FleeingFrom", fleeingFrom);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        noticedTicks = tag.getInt("NoticedTicks");
        fleeingTicks = tag.getInt("FleeingTicks");
        fleeing = tag.getBoolean("Fleeing");
        fleeingFrom = tag.hasUUID("FleeingFrom") ? tag.getUUID("FleeingFrom") : null;
    }
}
