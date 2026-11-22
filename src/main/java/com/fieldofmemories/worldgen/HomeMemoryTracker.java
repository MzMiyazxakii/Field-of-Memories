package com.fieldofmemories.worldgen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "fieldofmemories")
public final class HomeMemoryTracker {
    private static final String DATA_KEY = "FieldOfMemoriesHomeMemory";
    private static final int REQUIRED_ACTIVE_TICKS = 18_000; // 15 minutes of active time at a new home
    private static final double HOME_RADIUS_SQUARED = 32.0D * 32.0D;

    private HomeMemoryTracker() {}

    public static boolean hasRecognizedHome(ServerPlayer player) {
        return player.getPersistentData().getCompound(DATA_KEY).getBoolean("HomeRecognized");
    }

    public static BlockPos getRecognizedHome(ServerPlayer player) {
        CompoundTag data = player.getPersistentData().getCompound(DATA_KEY);
        if (!data.getBoolean("HomeRecognized")) return null;
        return new BlockPos(data.getInt("HomeX"), data.getInt("HomeY"), data.getInt("HomeZ"));
    }

    public static String getRecognizedHomeDimension(ServerPlayer player) {
        return player.getPersistentData().getCompound(DATA_KEY).getString("HomeDimension");
    }

    public static int getActiveHomeTicks(ServerPlayer player) {
        return player.getPersistentData().getCompound(DATA_KEY).getInt("ActiveTicks");
    }

    public static BlockPos getHorrorAnchor(ServerPlayer player) {
        if (!hasRecognizedHome(player)) return null;
        if (!player.serverLevel().dimension().location().toString().equals(getRecognizedHomeDimension(player))) return null;
        BlockPos home = getRecognizedHome(player);
        return player.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D) <= 96.0D * 96.0D ? home : null;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        BlockPos respawn = player.getRespawnPosition();
        if (respawn == null || !player.serverLevel().dimension().equals(player.getRespawnDimension())) {
            // Custom straw beds also count as a home clue even when they do not set vanilla respawn.
            respawn = BlockPos.findClosestMatch(player.blockPosition(), 24, 8,
                    pos -> player.serverLevel().getBlockState(pos).getBlock() instanceof BedBlock).orElse(null);
        }
        if (respawn == null) return;

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
            data.putInt("CandidateActiveTicks", 0);
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
            int ticks = data.getInt("CandidateActiveTicks") + 1;
            data.putInt("CandidateActiveTicks", ticks);
            boolean sameAsRemembered = data.getBoolean("HomeRecognized")
                    && dimension.equals(data.getString("HomeDimension"))
                    && respawn.getX() == data.getInt("HomeX")
                    && respawn.getY() == data.getInt("HomeY")
                    && respawn.getZ() == data.getInt("HomeZ");
            // Keep the old home until the player has genuinely settled at the new bed.
            if (ticks >= REQUIRED_ACTIVE_TICKS && !sameAsRemembered) {
                data.putBoolean("HomeRecognized", true);
                data.putString("HomeDimension", dimension);
                data.putInt("HomeX", respawn.getX());
                data.putInt("HomeY", respawn.getY());
                data.putInt("HomeZ", respawn.getZ());
                data.putLong("HomeRecognizedAt", player.serverLevel().getGameTime());
                data.putInt("CandidateActiveTicks", 0);
            }
        } else if (!nearHome) {
            data.putInt("CandidateActiveTicks", Math.max(0, data.getInt("CandidateActiveTicks") - 2));
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
