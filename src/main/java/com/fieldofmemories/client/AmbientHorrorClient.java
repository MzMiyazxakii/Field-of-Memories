package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
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

    private static int darkTicks;
    private static int blackoutTicks;
    private static int silenceTicks;
    private static BackgroundMusicSound backgroundMusic2;
    private static BackgroundMusicSound backgroundMusic4;

    private AmbientHorrorClient() {}

    public static void startEffect(int effect, int duration) {
        Minecraft mc = Minecraft.getInstance();
        int safeDuration = Math.max(1, Math.min(24000, duration));
        if (effect == EFFECT_DARK_PERIOD) {
            darkTicks = Math.max(darkTicks, safeDuration);
        } else if (effect == EFFECT_BLACKOUT) {
            blackoutTicks = Math.max(blackoutTicks, safeDuration);
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

        if (mc.level == null || mc.player == null
                || !mc.level.dimension().equals(Level.OVERWORLD) || silenceTicks > 0) {
            stopBackgroundMusic(mc);
            return;
        }

        // Both ambient layers begin as soon as the player enters the Overworld.
        if (backgroundMusic2 == null || backgroundMusic2.isStopped()) {
            backgroundMusic2 = new BackgroundMusicSound(FieldOfMemories.NIGHT_AMBIENT_2.get(), 0.22F);
            mc.getSoundManager().play(backgroundMusic2);
        }
        if (backgroundMusic4 == null || backgroundMusic4.isStopped()) {
            // This recording is mastered much louder, so keep its in-game level low.
            backgroundMusic4 = new BackgroundMusicSound(FieldOfMemories.NIGHT_AMBIENT_4.get(), 0.065F);
            mc.getSoundManager().play(backgroundMusic4);
        }
    }

    @SubscribeEvent
    public static void hideChatDuringBlackout(RenderGuiOverlayEvent.Pre event) {
        if (blackoutTicks > 0 && event.getOverlay().id().getPath().equals("chat")) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void renderEffectsWhileHudHidden(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.hideGui || mc.screen != null || mc.player == null || event.getOverlay() != VanillaGuiOverlay.VIGNETTE.type()) return;
        int width = event.getGuiGraphics().guiWidth(), height = event.getGuiGraphics().guiHeight();
        if (blackoutTicks > 0) event.getGuiGraphics().fill(0, 0, width, height, 0xF5000000);
        else if (darkTicks > 0) { int alpha = darkTicks < 100 ? Math.max(0, 34 * darkTicks / 100) : 34; event.getGuiGraphics().fill(0, 0, width, height, alpha << 24); }
    }

    @SubscribeEvent
    public static void onSound(PlaySoundEvent event) {
        if (silenceTicks > 0) event.setSound(null);
    }

    @SubscribeEvent
    public static void onAmbientOverlay(RenderGuiEvent.Post event) {
        if (Minecraft.getInstance().screen != null || (darkTicks <= 0 && blackoutTicks <= 0)) return;
        int width = event.getGuiGraphics().guiWidth();
        int height = event.getGuiGraphics().guiHeight();
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
            super(sound, SoundSource.MUSIC, RandomSource.create());
            this.looping = true;
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
