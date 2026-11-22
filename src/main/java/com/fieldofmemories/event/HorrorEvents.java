package com.fieldofmemories.event;

import com.fieldofmemories.data.HorrorWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "fieldofmemories")
public final class HorrorEvents {
    private HorrorEvents() {}

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) return;

        long now = level.getGameTime();
        if (now % 100L != 0L || now < HorrorWorldData.getNextAmbientEvent(level)) return;

        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.serverLevel() == level && !player.isCreative() && !player.isSpectator()
                    && level.isNight() && level.getMaxLocalRawBrightness(player.blockPosition()) <= 7
                    && level.canSeeSky(player.blockPosition())) {
                candidates.add(player);
            }
        }

        if (candidates.isEmpty()) return;

        int danger = HorrorWorldData.getDangerLevel(level);
        int activity = HorrorWorldData.getActivityMultiplier(level);
        RandomSource random = level.getRandom();
        int eventChance = Math.min(85, 8 + danger / 2);
        eventChance = Math.min(100, eventChance * activity / 100);

        if (random.nextInt(100) < eventChance) {
            ServerPlayer target = candidates.get(random.nextInt(candidates.size()));
            triggerWorldWhisper(level, target, random);
        }

        // One shared world cooldown: higher danger and activity shorten the interval for everyone.
        double dangerFactor = 1.0D - danger * 0.006D;
        long interval = Math.max(600L, (long) (6000.0D * dangerFactor * 100.0D / activity));
        HorrorWorldData.setNextAmbientEvent(level, now + interval);
    }

    private static void triggerWorldWhisper(ServerLevel level, ServerPlayer player, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = 7.0D + random.nextDouble() * 5.0D;
        double x = player.getX() + Math.cos(angle) * distance;
        double z = player.getZ() + Math.sin(angle) * distance;
        BlockPos soundPos = BlockPos.containing(x, player.getY(), z);

        // The sound is emitted by the world, so nearby players can hear the same event.
        level.playSound(null, soundPos, SoundEvents.AMBIENT_CAVE.value(),
                SoundSource.AMBIENT, 0.22F, 0.65F + random.nextFloat() * 0.25F);
    }
}
