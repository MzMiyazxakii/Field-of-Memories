package com.fieldofmemories.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class ScarecrowEntity extends Entity {
    private static final EntityDataAccessor<Integer> POSE_VARIANT =
            SynchedEntityData.defineId(ScarecrowEntity.class, EntityDataSerializers.INT);
    public ScarecrowEntity(EntityType<? extends ScarecrowEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        if (!level.isClientSide) this.entityData.set(POSE_VARIANT, this.random.nextInt(3));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > 1800) {
            discard();
            return;
        }
        setDeltaMovement(Vec3.ZERO);
        this.noPhysics = true;
        // Static world prop: never turn toward players.
        setYRot(0.0F);
        setXRot(0.0F);
        yRotO = 0.0F;
        xRotO = 0.0F;
    }

    public int getPoseVariant() {
        return this.entityData.get(POSE_VARIANT);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(POSE_VARIANT, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("PoseVariant")) {
            this.entityData.set(POSE_VARIANT, Math.max(0, Math.min(2, tag.getInt("PoseVariant"))));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("PoseVariant", getPoseVariant());
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
