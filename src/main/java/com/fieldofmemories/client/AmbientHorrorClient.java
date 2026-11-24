package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AmbientHorrorClient {
    public static final int EFFECT_DARK_PERIOD = 0;
    public static final int EFFECT_BLACKOUT = 1;
    public static final int EFFECT_SILENCE = 2;
    public static final int EFFECT_BLOOD_RAIN = 3;

    private static int darkTicks;
    private static int blackoutTicks;
    private static int silenceTicks;
    private static int bloodRainTicks;
    private static BackgroundMusicSound backgroundMusic2;
    private static BackgroundMusicSound backgroundMusic4;
    private static boolean playAmbient4Next;

    private AmbientHorrorClient() {}

    public static int getDarkTicksForScreenEffects() { return darkTicks; }
    public static int getBlackoutTicksForScreenEffects() { return blackoutTicks; }

    public static void startEffect(int effect, int duration) {
        Minecraft mc = Minecraft.getInstance();
        int safeDuration = Math.max(1, Math.min(24000, duration));
        if (effect == EFFECT_DARK_PERIOD) {
            darkTicks = Math.max(darkTicks, safeDuration);
        } else if (effect == EFFECT_BLACKOUT) {
            blackoutTicks = Math.max(blackoutTicks, safeDuration);
        } else if (effect == EFFECT_BLOOD_RAIN) {
            bloodRainTicks = Math.max(bloodRainTicks, safeDuration);
        } else if (effect == EFFECT_SILENCE) {
            silenceTicks = Math.max(silenceTicks, safeDuration);
            stopBackgroundMusic(mc);
            mc.getSoundManager().stop();
        }
    }

    public static boolean isSilent() {
        return silenceTicks > 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (darkTicks > 0) darkTicks--;
        if (blackoutTicks > 0) blackoutTicks--;
        if (silenceTicks > 0) silenceTicks--;
        if (bloodRainTicks > 0) bloodRainTicks--;
        if (mc.level == null) bloodRainTicks = 0;

        if (mc.level == null || mc.player == null
                || !mc.level.dimension().equals(Level.OVERWORLD) || silenceTicks > 0) {
            stopBackgroundMusic(mc);
            return;
        }

        // A sound can finish naturally without setting AbstractSoundInstance.isStopped().
        // Check the sound manager and release references to tracks that have ended.
        boolean track2Playing = backgroundMusic2 != null && mc.getSoundManager().isActive(backgroundMusic2);
        boolean track4Playing = backgroundMusic4 != null && mc.getSoundManager().isActive(backgroundMusic4);
        if (!track2Playing) backgroundMusic2 = null;
        if (!track4Playing) backgroundMusic4 = null;

        // Play exactly one ambient track at a time, switching when the previous one ends.
        if (!track2Playing && !track4Playing) {
            startNextAmbientTrack(mc);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft mc = Minecraft.getInstance();
        stopBackgroundMusic(mc);
        playAmbient4Next = false;
        startNextAmbientTrack(mc);
    }

    private static void startNextAmbientTrack(Minecraft mc) {
        if (mc.level == null || mc.player == null
                || !mc.level.dimension().equals(Level.OVERWORLD) || silenceTicks > 0) {
            return;
        }

        if (playAmbient4Next) {
            backgroundMusic4 = new BackgroundMusicSound(FieldOfMemories.NIGHT_AMBIENT_4.get(), 0.01F);
            mc.getSoundManager().play(backgroundMusic4);
        } else {
            backgroundMusic2 = new BackgroundMusicSound(FieldOfMemories.NIGHT_AMBIENT_2.get(), 0.015F);
            mc.getSoundManager().play(backgroundMusic2);
        }
        playAmbient4Next = !playAmbient4Next;
    }

    @SubscribeEvent
    public static void hideChatDuringBlackout(RenderGuiOverlayEvent.Pre event) {
        if (blackoutTicks > 0 && event.getOverlay().id().getPath().equals("chat")) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onSound(PlaySoundEvent event) {
        if (silenceTicks > 0) event.setSound(null);
    }

    @SubscribeEvent
    public static void onAmbientOverlay(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || (darkTicks <= 0 && blackoutTicks <= 0
                && (bloodRainTicks <= 0 || mc.level == null || !mc.level.isRaining()))) return;
        int width = event.getGuiGraphics().guiWidth();
        int height = event.getGuiGraphics().guiHeight();
        if (bloodRainTicks > 0 && mc.level != null && mc.level.isRaining()) {
            // A restrained red cast plus fast, narrow streaks makes the normal rain read as blood.
            event.getGuiGraphics().fill(0, 0, width, height, 0x0D8B0000);
            java.util.Random rainRandom = new java.util.Random(mc.level.getGameTime() * 31L
                    + (mc.player == null ? 0L : mc.player.tickCount / 2L));
            int streaks = Math.max(42, width / 12);
            for (int i = 0; i < streaks; i++) {
                int x = rainRandom.nextInt(Math.max(1, width));
                int y = rainRandom.nextInt(Math.max(1, height));
                int length = 7 + rainRandom.nextInt(11);
                int alpha = 105 + rainRandom.nextInt(95);
                int color = (alpha << 24) | 0x00A60008;
                event.getGuiGraphics().fill(x, y, Math.min(width, x + 1), Math.min(height, y + length), color);
                if (x + 1 < width && y + length < height) {
                    event.getGuiGraphics().fill(x + 1, y + length, x + 2,
                            Math.min(height, y + length + 3), color);
                }
            }
        }
        if (blackoutTicks > 0) {
            event.getGuiGraphics().fill(0, 0, width, height, 0xF5000000);
        } else {
            int alpha = 34;
            if (darkTicks < 100) alpha = Math.max(0, alpha * darkTicks / 100);
            event.getGuiGraphics().fill(0, 0, width, height, (alpha << 24));
        }
    }

    private static void stopBackgroundMusic(Minecraft mc) {
        if (backgroundMusic2 != null) {
            mc.getSoundManager().stop(backgroundMusic2);
            backgroundMusic2 = null;
        }
        if (backgroundMusic4 != null) {
            mc.getSoundManager().stop(backgroundMusic4);
            backgroundMusic4 = null;
        }
    }

    private static final class BackgroundMusicSound extends AbstractTickableSoundInstance {
        private BackgroundMusicSound(SoundEvent sound, float volume) {
            super(sound, SoundSource.MASTER, RandomSource.create());
            this.looping = false;
            this.delay = 0;
            this.volume = volume;
            this.pitch = 1.0F;
            this.attenuation = Attenuation.NONE;
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null
                    || !mc.level.dimension().equals(Level.OVERWORLD)
                    || AmbientHorrorClient.isSilent()) {
                stop();
                return;
            }
            this.x = mc.player.getX();
            this.y = mc.player.getY();
            this.z = mc.player.getZ();
        }
    }
}
