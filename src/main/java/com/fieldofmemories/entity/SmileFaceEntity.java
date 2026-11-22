package com.fieldofmemories.entity;

import com.fieldofmemories.network.HorrorNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
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

            Vec3 faceCenter = position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
            Vec3 eyePosition = player.getEyePosition();
            Vec3 toFace = faceCenter.subtract(eyePosition).normalize();
            boolean watching = distance < 40.0D * 40.0D
                    && hasClearHorrorSight(serverLevel, eyePosition, faceCenter)
                    && toFace.dot(player.getLookAngle()) > 0.992D;
            if (watching) {
                int ticks = gazeTicks.getOrDefault(player.getUUID(), 0) + 1;
                gazeTicks.put(player.getUUID(), ticks);
                if (ticks >= 12) {
                    triggered = true;
                    if (!com.fieldofmemories.item.CrossItem.tryProtect(player, false)) HorrorNetwork.triggerScream(player);
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

    // Transparent blocks and non-occluding vegetation must not hide the face.
    private static boolean hasClearHorrorSight(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 ray = to.subtract(from);
        double distance = ray.length();
        int steps = Math.max(1, (int) Math.ceil(distance * 12.0D));
        for (int i = 1; i < steps; i++) {
            Vec3 sample = from.add(ray.scale((double) i / steps));
            BlockPos pos = BlockPos.containing(sample);
            var state = level.getBlockState(pos);
            if (!state.canOcclude()) continue;

            VoxelShape shape = state.getOcclusionShape(level, pos);
            if (shape.isEmpty()) continue;
            double localX = sample.x - pos.getX();
            double localY = sample.y - pos.getY();
            double localZ = sample.z - pos.getZ();
            for (AABB box : shape.toAabbs()) {
                if (box.contains(localX, localY, localZ)) return false;
            }
        }
        return true;
    }

    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
