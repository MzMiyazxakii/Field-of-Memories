package com.fieldofmemories.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.network.NetworkHooks;

import java.util.Optional;
import java.util.UUID;

public class PlayerEchoEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> SKIN_OWNER =
            SynchedEntityData.defineId(PlayerEchoEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public PlayerEchoEntity(EntityType<? extends PlayerEchoEntity> type, Level level) {
        super(type, level);
        this.setNoAi(false);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 24.0F, 0.025F));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.15D));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SKIN_OWNER, Optional.empty());
    }

    public void setSkinOwner(UUID uuid) {
        this.entityData.set(SKIN_OWNER, Optional.ofNullable(uuid));
    }

    public UUID getSkinOwner() {
        return this.entityData.get(SKIN_OWNER).orElse(null);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) return false;
        if (level() instanceof ServerLevel serverLevel) {
            if (source.getEntity() instanceof LivingEntity attacker && attacker != this) {
                if (attacker instanceof net.minecraft.server.level.ServerPlayer player) {
                    com.fieldofmemories.item.CrossItem.tryDepleteOnAttack(player);
                }
                attacker.hurt(serverLevel.damageSources().mobAttack(this), 16.0F);
            }
            serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                    getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    36, 0.45D, 0.75D, 0.45D, 0.025D);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    18, 0.35D, 0.65D, 0.35D, 0.015D);
        }
        discard();
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (tickCount % 45 == 0 && getNavigation().isDone() && random.nextFloat() < 0.75F) {
                getNavigation().moveTo(getX() + (random.nextDouble() - 0.5D) * 12.0D,
                        getY(), getZ() + (random.nextDouble() - 0.5D) * 12.0D, 1.2D);
            }
            if (onGround() && tickCount % 37 == 0 && random.nextFloat() < 0.28F) getJumpControl().jump();
            if (getNavigation().isInProgress()) {
                getLookControl().setLookAt(getX() + getDeltaMovement().x * 8.0D,
                        getEyeY(), getZ() + getDeltaMovement().z * 8.0D);
            }
            if (tickCount > 2400) discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = getSkinOwner();
        if (owner != null) tag.putUUID("SkinOwner", owner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSkinOwner(tag.hasUUID("SkinOwner") ? tag.getUUID("SkinOwner") : null);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
