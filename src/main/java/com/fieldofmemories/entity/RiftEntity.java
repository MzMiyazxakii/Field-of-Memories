package com.fieldofmemories.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class RiftEntity extends Entity {
    private static final EntityDataAccessor<Boolean> BLACK =
            SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);
    private int lifetime = 400;

    public RiftEntity(EntityType<? extends RiftEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override protected void defineSynchedData() { entityData.define(BLACK, false); }
    public void setBlackRift(boolean value) { entityData.set(BLACK, value); }
    public boolean isBlackRift() { return entityData.get(BLACK); }
    public void setLifetime(int ticks) { lifetime = Math.max(40, ticks); }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            for (int i = 0; i < 8; i++) {
                double ox = (random.nextDouble() - 0.5D) * 2.2D;
                double oy = (random.nextDouble() - 0.5D) * 2.2D;
                double oz = (random.nextDouble() - 0.5D) * 2.2D;
                level().addParticle(new DustParticleOptions(new Vector3f(0.005F, 0.005F, 0.005F), 1.35F),
                        getX() + ox, getY() + oy, getZ() + oz, 0.0D, 0.015D, 0.0D);
            }
            return;
        }
        if (--lifetime <= 0) { discard(); return; }
        for (ServerPlayer player : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(8.0D))) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) continue;
            Vec3 pull = position().subtract(player.position());
            double distance = Math.max(0.2D, pull.length());
            if (distance < 8.0D) {
                Vec3 force = pull.normalize().scale((8.0D - distance) * 0.012D);
                player.setDeltaMovement(player.getDeltaMovement().add(force.x, force.y * 0.25D, force.z));
                player.hurtMarked = true;
            }
            if (distance < 1.35D) {
                if (com.fieldofmemories.item.CrossItem.tryProtect(player, true)) { discard(); return; }
                if (isBlackRift()) {
                    player.kill();
                } else {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double x = getX() + Math.cos(angle) * (30.0D + random.nextDouble() * 18.0D);
                    double z = getZ() + Math.sin(angle) * (30.0D + random.nextDouble() * 18.0D);
                    int y = player.serverLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            (int) Math.floor(x), (int) Math.floor(z));
                    player.teleportTo(x, y, z);
                }
                discard();
                return;
            }
        }
    }

    @Override protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        lifetime = Math.max(1, tag.getInt("Lifetime"));
        setBlackRift(tag.getBoolean("Black"));
    }
    @Override protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        tag.putInt("Lifetime", lifetime);
        tag.putBoolean("Black", isBlackRift());
    }
    @Override public boolean isPickable() { return false; }
}