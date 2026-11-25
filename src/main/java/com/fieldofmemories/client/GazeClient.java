package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Short full-screen eyes apparition, shown only to the player selected by the server. */
@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GazeClient {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(FieldOfMemories.MOD_ID, "textures/gui/gaze.png");
    private static int ticks;

    private GazeClient() {}

    public static void start() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ticks = 36;
        var sound = mc.level.random.nextBoolean()
                ? FieldOfMemories.SMILE_FACE_SCREAM_1.get()
                : FieldOfMemories.SMILE_FACE_SCREAM_2.get();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, 0.88F));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ticks > 0) ticks--;
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (ticks <= 0 || mc.player == null || mc.screen != null) return;
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        float alpha = ticks > 30 ? (36 - ticks) / 6.0F : (ticks < 6 ? ticks / 6.0F : 1.0F);
        alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        event.getGuiGraphics().blit(TEXTURE, 0, 0, 0, 0, width, height, 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
