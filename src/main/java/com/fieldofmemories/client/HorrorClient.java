package com.fieldofmemories.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "fieldofmemories", value = Dist.CLIENT)
public final class HorrorClient {
    private static final ResourceLocation SCREAM_TEXTURE =
            new ResourceLocation("fieldofmemories", "textures/entity/smile_scary.png");
    private static int screamTicks;

    private HorrorClient() {}

    public static void startScream() {
        screamTicks = 80;
        MovementClient.beginForcedFallFromServer();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && screamTicks > 0) screamTicks--;
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
