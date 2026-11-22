package com.fieldofmemories.entity;

import com.fieldofmemories.network.HorrorNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SmileFaceEntity extends Entity {
    private final Map<UUID, Integer> gazeTicks = new HashMap<>();
    private boolean triggered;

    public SmileFaceEntity(EntityType<? extends SmileFaceEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        this.noPhysics = true;
        if (level().isClientSide || triggered) return;

        ServerLevel serverLevel = (ServerLevel) level();
        var nearby = serverLevel.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(48.0D));
        if (nearby.isEmpty() || tickCount > 2400) {
            discard();
            return;
        }

        ServerPlayer closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : nearby) {
            double distance = distanceToSqr(player);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = player;
            }

            Vec3 toFace = position().subtract(player.getEyePosition()).normalize();
            boolean watching = distance < 40.0D * 40.0D
                    && player.hasLineOfSight(this)
                    && toFace.dot(player.getLookAngle()) > 0.992D;
            if (watching) {
                int ticks = gazeTicks.getOrDefault(player.getUUID(), 0) + 1;
                gazeTicks.put(player.getUUID(), ticks);
                if (ticks >= 45) {
                    triggered = true;
                    HorrorNetwork.triggerScream(player);
                    discard();
                    return;
                }
            } else {
                gazeTicks.remove(player.getUUID());
            }
        }

        if (closest != null) {
            double dx = closest.getX() - getX();
            double dz = closest.getZ() - getZ();
            setYRot((float) (Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F);
        }
        setPos(getX(), getY() + Math.sin(tickCount * 0.08D) * 0.003D, getZ());
    }

    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
