package com.fieldofmemories.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.VertexSorting;

/**
 * Screen-space horror effects rendered after the 3D world, independently of
 * Minecraft's HUD visibility. F1 hides the HUD, not the mod's view effects.
 */
@Mod.EventBusSubscriber(modid = "fieldofmemories", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScreenEffectsRenderer {
    private static final ResourceLocation SCREAM_TEXTURE =
            new ResourceLocation("fieldofmemories", "textures/entity/smile_scary.png");

    private ScreenEffectsRenderer() {}

    @SubscribeEvent
    public static void renderAfterWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;

        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.hideGui || mc.player == null || mc.level == null || mc.screen != null) return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        if (width <= 0 || height <= 0) return;

        // RenderLevelStageEvent still fires when F1 suppresses Gui.render().
        // Temporarily install the same orthographic projection used by the GUI.
        RenderSystem.backupProjectionMatrix();
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        try {
            RenderSystem.setProjectionMatrix(
                    new Matrix4f().setOrtho(0.0F, width, height, 0.0F, 1000.0F, 3000.0F),
                    VertexSorting.ORTHOGRAPHIC_Z);
            modelView.setIdentity();
            modelView.translate(0.0D, 0.0D, -2000.0D);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            GuiGraphics gui = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            drawEffects(mc, gui, width, height);
            gui.flush();
        } finally {
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
        }
    }

    private static void drawEffects(Minecraft mc, GuiGraphics gui, int w, int h) {
        int darkTicks = AmbientHorrorClient.getDarkTicksForScreenEffects();
        int blackoutTicks = AmbientHorrorClient.getBlackoutTicksForScreenEffects();
        if (blackoutTicks > 0) {
            gui.fill(0, 0, w, h, 0xF5000000);
        } else if (darkTicks > 0) {
            int alpha = darkTicks < 100 ? Math.max(0, 34 * darkTicks / 100) : 34;
            gui.fill(0, 0, w, h, alpha << 24);
        }

        if (MemoryDiscClient.shouldDarkenScreen()) {
            gui.fill(0, 0, w, h, 0xEA000000);
        } else if (MemoryDiscClient.isReading()) {
            gui.fill(0, 0, w, h, 0x16A00020);
        }

        float spirit = Math.min(1.0F, HorrorClient.getSpiritInterferenceForScreenEffects());
        int interferenceTicks = HorrorClient.getInterferenceTicksForScreenEffects();
        if (spirit >= 0.015F) {
            int yellowAlpha = (int)(5 + spirit * 42);
            gui.fill(0, 0, w, h, (yellowAlpha << 24) | 0x00E6C94F);
            int shadeAlpha = (int)(3 + spirit * 9);
            gui.fill(0, 0, w, h, (shadeAlpha << 24) | 0x00303030);

            RandomSource random = RandomSource.create((long) interferenceTicks * 31L + 7L);
            int lines = 1 + (int)(spirit * 5.0F);
            for (int i = 0; i < lines; i++) {
                int y = random.nextInt(h);
                int lineHeight = random.nextFloat() < spirit * 0.12F ? 2 : 1;
                int lineWidth = Math.max(12, (int)(w * (0.08F + random.nextFloat() * 0.32F)));
                int x = random.nextInt(Math.max(1, w - lineWidth + 1));
                int alpha = (int)(12 + spirit * 32);
                int color = (alpha << 24) | (random.nextBoolean() ? 0x00DADADA : 0x006B7777);
                gui.fill(x, y, Math.min(w, x + lineWidth), Math.min(h, y + lineHeight), color);
            }
            if (spirit > 0.72F && interferenceTicks % 11 == 0) {
                int bandY = RandomSource.create((long) interferenceTicks * 17L).nextInt(h);
                gui.fill(0, bandY, w, Math.min(h, bandY + 2), 0x18777777);
            }
        }

        boolean prone = MovementClient.isProneForScreenEffects();
        boolean waitingForSpace = MovementClient.isWaitingForSpaceForScreenEffects();
        if (!waitingForSpace) {
            gui.fill(0, 0, w, h, 0x12000000);
            if (prone) gui.fill(0, 0, w, h, 0x40000000);
            for (int y = 0; y < h; y += 2) {
                gui.fill(0, y, w, Math.min(h, y + 1), 0x1B000000);
            }

            int band = (mc.player.tickCount * 3) % Math.max(1, h);
            gui.fill(0, band, w, Math.min(h, band + 3), 0x18D5E0E5);
            int phase = mc.player.tickCount / 2;
            for (int i = 0; i < 13; i++) {
                int seed = phase * 1664525 + i * 1013904223;
                int x = Math.floorMod(seed ^ (seed >>> 15), Math.max(1, w));
                int y = Math.floorMod(seed * 17 + i * 7919, Math.max(1, h));
                int sw = 2 + Math.floorMod(seed >>> 5, 9);
                gui.fill(x, y, Math.min(w, x + sw), Math.min(h, y + 1), 0x24DDE5E8);
            }
        }

        int recovery = MovementClient.getFallRecoveryTicksForScreenEffects();
        if (prone && !waitingForSpace) {
            int alpha = Math.min(72, Math.max(0, Math.round(72.0F * recovery / 40.0F)));
            if (alpha > 0) gui.fill(0, 0, w, h, alpha << 24);
        }

        if (!waitingForSpace) {
            gui.fill(0, 0, 12, h, 0x28000000);
            gui.fill(Math.max(0, w - 12), 0, w, h, 0x28000000);
            gui.fill(0, 0, w, 3, 0x28000000);
            gui.fill(0, Math.max(0, h - 3), w, h, 0x28000000);
        }

        int photoFlashTicks = OldCameraClientEvents.getPhotoFlashTicksForScreenEffects();
        if (photoFlashTicks > 0) {
            int flashAlpha = Math.min(220, photoFlashTicks * 34);
            gui.fill(0, 0, w, h, (flashAlpha << 24) | 0x00FFFFFF);
        }

        int screamTicks = HorrorClient.getScreamTicksForScreenEffects();
        if (screamTicks > 0) {
            float alpha = Math.min(1.0F, screamTicks / 12.0F);
            gui.fill(0, 0, w, h, ((int)(150.0F * alpha) << 24));
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            gui.blit(SCREAM_TEXTURE, 0, 0, 0, 0, w, h, 64, 64);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
