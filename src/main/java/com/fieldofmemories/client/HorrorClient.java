package com.fieldofmemories.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.fieldofmemories.FieldOfMemories;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import com.fieldofmemories.entity.SpiritEntity;
import java.util.Random;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "fieldofmemories", value = Dist.CLIENT)
public final class HorrorClient {
    private static final ResourceLocation SCREAM_TEXTURE =
            new ResourceLocation("fieldofmemories", "textures/entity/smile_scary.png");
    private static int screamTicks;
    private static float spiritInterference;
    private static int interferenceTicks;

    private HorrorClient() {}

    public static void startScream() {
        screamTicks = 50;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            var sound = mc.level != null && mc.level.random.nextBoolean()
                    ? FieldOfMemories.SMILE_FACE_SCREAM_1.get()
                    : FieldOfMemories.SMILE_FACE_SCREAM_2.get();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, 1.0F));
        }
        MovementClient.beginForcedFallFromServer();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (screamTicks > 0) screamTicks--;
        float target = 0.0F;
        if (mc.level != null && mc.player != null) {
            SpiritEntity nearest = mc.level.getEntitiesOfClass(SpiritEntity.class,
                    mc.player.getBoundingBox().inflate(28.0D)).stream()
                    .filter(spirit -> spirit.isAlive() && !spirit.isRemoved())
                    .min((a, b) -> Double.compare(mc.player.distanceToSqr(a), mc.player.distanceToSqr(b)))
                    .orElse(null);
            if (nearest != null) {
                float distance = mc.player.distanceTo(nearest);
                target = Math.max(0.0F, Math.min(1.0F, 1.0F - distance / 28.0F));
            }
        }
        spiritInterference += (target - spiritInterference) * (target > spiritInterference ? 0.22F : 0.055F);
        if (spiritInterference > 0.015F) interferenceTicks++; else interferenceTicks = 0;
    }

    @SubscribeEvent
    public static void onSpiritInterference(RenderGuiOverlayEvent.Post event) {
        // Render once per frame, not once for every HUD layer.
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;
        if (spiritInterference < 0.015F || Minecraft.getInstance().player == null) return;
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        float strength = Math.min(1.0F, spiritInterference);
        var gui = event.getGuiGraphics();

        // A warm yellow screen tint grows as the spirit approaches.
        // Keep it subtle at long range and clearly visible nearby.
        int yellowAlpha = (int)(5 + strength * 42);
        gui.fill(0, 0, width, height, (yellowAlpha << 24) | 0x00E6C94F);

        int alpha = (int)(3 + strength * 9);
        gui.fill(0, 0, width, height, (alpha << 24) | 0x00303030);

        // A short warning appears just above the hotbar while the spirit's presence is felt.
        if (strength > 0.12F) {
            String warning = ((interferenceTicks / 100) & 1) == 0
                    ? "вам тяжело дышать" : "вы чувствуете тяжесть на душе";
            int textAlpha = Math.min(255, (int)(90 + strength * 165));
            int color = (textAlpha << 24) | 0x00F2E6A0;
            int textWidth = Minecraft.getInstance().font.width(warning);
            gui.drawString(Minecraft.getInstance().font, Component.literal(warning),
                    (width - textWidth) / 2, height - 58, color, true);
        }
        Random random = new Random(interferenceTicks * 31L + 7L);
        int lines = 1 + (int)(strength * 5.0F);
        for (int i = 0; i < lines; i++) {
            int y = random.nextInt(Math.max(1, height));
            int lineHeight = random.nextFloat() < strength * 0.12F ? 2 : 1;
            int lineWidth = Math.max(12, (int)(width * (0.08F + random.nextFloat() * 0.32F)));
            int x = random.nextInt(Math.max(1, width - lineWidth + 1));
            int a = (int)(12 + strength * 32);
            int color = (a << 24) | (random.nextBoolean() ? 0x00DADADA : 0x006B7777);
            gui.fill(x, y, Math.min(width, x + lineWidth), Math.min(height, y + lineHeight), color);
        }
        if (strength > 0.72F && interferenceTicks % 11 == 0) {
            int bandY = random.nextInt(Math.max(1, height));
            gui.fill(0, bandY, width, Math.min(height, bandY + 2), 0x18777777);
        }
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Post event) {
        if (screamTicks <= 0 || Minecraft.getInstance().player == null) return;
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        float alpha = Math.min(1.0F, screamTicks / 12.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        event.getGuiGraphics().fill(0, 0, width, height, ((int) (150.0F * alpha) << 24));
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        event.getGuiGraphics().blit(SCREAM_TEXTURE, 0, 0, 0, 0, width, height, 64, 64);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
