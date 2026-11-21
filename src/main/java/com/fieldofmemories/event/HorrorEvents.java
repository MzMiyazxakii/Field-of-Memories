package com.fieldofmemories.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Early-gameplay foundation: rare, server-authoritative ambient disturbances.
 * The events are intentionally subtle and only occur in dark outdoor places at night.
 */
public final class HorrorEvents {
    private static final Map<UUID, Integer> PLAYER_COOLDOWNS = new ConcurrentHashMap<>();
    private static final int CHECK_INTERVAL = 100;

    private HorrorEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        UUID id = player.getUUID();
        int cooldown = PLAYER_COOLDOWNS.getOrDefault(id, 0);
        if (cooldown > 0) {
            PLAYER_COOLDOWNS.put(id, cooldown - 1);
            return;
        }

        Level level = player.level();
        if (level.isClientSide || !level.isNight() || level.getMaxLocalRawBrightness(player.blockPosition()) > 7) return;
        if (level.canSeeSky(player.blockPosition()) && level.random.nextInt(CHECK_INTERVAL) == 0) {
            triggerWhisper(player);
            PLAYER_COOLDOWNS.put(id, 20 * (45 + level.random.nextInt(90)));
        }
    }

    private static void triggerWhisper(ServerPlayer player) {
        RandomSource random = player.getRandom();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = 7.0 + random.nextDouble() * 5.0;
        double x = player.getX() + Math.cos(angle) * distance;
        double z = player.getZ() + Math.sin(angle) * distance;
        BlockPos soundPos = BlockPos.containing(x, player.getY(), z);

        // A barely audible environmental cue, positioned away from the player.
        player.level().playSound(null, soundPos, SoundEvents.AMBIENT_CAVE.value(),
                SoundSource.AMBIENT, 0.22F, 0.65F + random.nextFloat() * 0.25F);
    }
}
