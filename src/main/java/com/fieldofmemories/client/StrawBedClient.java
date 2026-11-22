package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.network.StrawBedNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, value = Dist.CLIENT)
public final class StrawBedClient {
    private static boolean resting;

    private StrawBedClient() {}

    public static void setResting(boolean value) {
        resting = value;
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (!resting || event.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (event.getKey() == GLFW.GLFW_KEY_I) {
            if (mc.level != null && mc.level.isNight()) StrawBedNetwork.requestSleep();
        } else if (event.getKey() == GLFW.GLFW_KEY_SPACE || event.getKey() == GLFW.GLFW_KEY_ESCAPE) {
            StrawBedNetwork.requestWake();
        }
    }

    @SubscribeEvent
    public static void renderHint(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!resting || mc.player == null || mc.level == null || !mc.level.isNight()) return;
        var gui = event.getGuiGraphics();
        gui.drawCenteredString(mc.font, Component.literal("Нажмите I чтоб уснуть"),
                gui.guiWidth() / 2, gui.guiHeight() - 42, 0xFFFFFFFF);
    }
}
