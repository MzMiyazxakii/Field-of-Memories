package com.fieldofmemories.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class RunningShadowEntity extends PathfinderMob {
    private Vec3 runDirection = Vec3.ZERO;
    private UUID targetPlayer;
    private boolean hasBeenSeen;

    public RunningShadowEntity(EntityType<? extends RunningShadowEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setNoGravity(true);
        setSilent(true);
    }

    public void setRunDirection(Vec3 direction) {
        this.runDirection = direction.normalize();
    }

    public void setTargetPlayer(UUID player) {
        this.targetPlayer = player;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        move(MoverType.SELF, runDirection);
        ServerPlayer target = targetPlayer == null ? null
                : ((net.minecraft.server.level.ServerLevel) level()).getServer()
                    .getPlayerList().getPlayer(targetPlayer);
        if (target == null || !target.isAlive() || target.isSpectator()) {
            discard();
            return;
        }

        boolean visible = false;
        if (target.distanceToSqr(this) <= 48.0D * 48.0D && target.hasLineOfSight(this)) {
            Vec3 toEntity = position().add(0.0D, getBbHeight() * 0.5D, 0.0D)
                    .subtract(target.getEyePosition()).normalize();
            visible = toEntity.dot(target.getLookAngle()) > 0.58D;
        }
        if (visible) hasBeenSeen = true;
        else if (hasBeenSeen || tickCount > 45) {
            discard();
        }
    }
}
