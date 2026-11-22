package com.fieldofmemories.worldgen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "fieldofmemories")
public final class HomeMemoryTracker {
    private static final String DATA_KEY = "FieldOfMemoriesHomeMemory";
    private static final int REQUIRED_ACTIVE_TICKS = 12_000; // 10 minutes of activity near the respawn point
    private static final double HOME_RADIUS_SQUARED = 32.0D * 32.0D;

    private HomeMemoryTracker() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        BlockPos respawn = player.getRespawnPosition();
        if (respawn == null || !player.serverLevel().dimension().equals(player.getRespawnDimension())) return;

        CompoundTag data = player.getPersistentData().getCompound(DATA_KEY);
        String dimension = player.serverLevel().dimension().location().toString();
        String candidateDimension = data.getString("CandidateDimension");
        int candidateX = data.getInt("CandidateX");
        int candidateY = data.getInt("CandidateY");
        int candidateZ = data.getInt("CandidateZ");

        if (!dimension.equals(candidateDimension) || candidateX != respawn.getX()
                || candidateY != respawn.getY() || candidateZ != respawn.getZ()) {
            data.putString("CandidateDimension", dimension);
            data.putInt("CandidateX", respawn.getX());
            data.putInt("CandidateY", respawn.getY());
            data.putInt("CandidateZ", respawn.getZ());
            data.putInt("ActiveTicks", 0);
            data.putBoolean("HomeRecognized", false);
            data.putDouble("LastX", player.getX());
            data.putDouble("LastY", player.getY());
            data.putDouble("LastZ", player.getZ());
        }

        double dx = player.getX() - respawn.getX() - 0.5D;
        double dy = player.getY() - respawn.getY();
        double dz = player.getZ() - respawn.getZ() - 0.5D;
        double lastDx = player.getX() - data.getDouble("LastX");
        double lastDy = player.getY() - data.getDouble("LastY");
        double lastDz = player.getZ() - data.getDouble("LastZ");
        boolean nearHome = dx * dx + dy * dy + dz * dz <= HOME_RADIUS_SQUARED;
        boolean moving = lastDx * lastDx + lastDy * lastDy + lastDz * lastDz > 0.0004D;

        if (nearHome && moving) {
            int ticks = data.getInt("ActiveTicks") + 1;
            data.putInt("ActiveTicks", ticks);
            if (ticks >= REQUIRED_ACTIVE_TICKS && !data.getBoolean("HomeRecognized")) {
                data.putBoolean("HomeRecognized", true);
                data.putString("HomeDimension", dimension);
                data.putInt("HomeX", respawn.getX());
                data.putInt("HomeY", respawn.getY());
                data.putInt("HomeZ", respawn.getZ());
                data.putLong("HomeRecognizedAt", player.serverLevel().getGameTime());
            }
        }

        data.putDouble("LastX", player.getX());
        data.putDouble("LastY", player.getY());
        data.putDouble("LastZ", player.getZ());
        player.getPersistentData().put(DATA_KEY, data);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag oldData = event.getOriginal().getPersistentData();
        if (oldData.contains(DATA_KEY)) {
            event.getEntity().getPersistentData().put(DATA_KEY, oldData.getCompound(DATA_KEY).copy());
        }
    }
}
