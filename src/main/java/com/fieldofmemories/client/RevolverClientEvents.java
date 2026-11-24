package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.item.RevolverItem;
import com.fieldofmemories.network.RevolverNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RevolverClientEvents {
    private static final KeyMapping RELOAD = new KeyMapping(
            "key.fieldofmemories.revolver_reload", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, "key.categories.fieldofmemories");
    private static boolean aiming;
    private RevolverClientEvents() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) { event.register(RELOAD); }

    @Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) { aiming = false; return; }
            if (aiming && !mc.player.getMainHandItem().is(FieldOfMemories.REVOLVER.get())) aiming = false;
            while (RELOAD.consumeClick()) {
                if (mc.screen == null && mc.player.getMainHandItem().is(FieldOfMemories.REVOLVER.get()))
                    RevolverNetwork.requestReload();
            }
        }

        @SubscribeEvent
        public static void onMouse(InputEvent.MouseButton.Pre event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
            if (event.getAction() == GLFW.GLFW_PRESS && mc.player.getMainHandItem().is(FieldOfMemories.REVOLVER.get())) {
                event.setCanceled(true);
                aiming = true;
            } else if (event.getAction() == GLFW.GLFW_RELEASE && aiming) {
                event.setCanceled(true);
                aiming = false;
                RevolverNetwork.requestFire();
            }
        }

        @SubscribeEvent
        public static void onFov(ComputeFovModifierEvent event) {
            if (aiming) event.setNewFovModifier(event.getFovModifier() * 0.88F);
        }

        @SubscribeEvent
        public static void onGui(RenderGuiEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            var stack = mc.player.getMainHandItem();
            if (!stack.is(FieldOfMemories.REVOLVER.get())) return;
            String count = RevolverItem.getAmmoCount(stack) + "/4";
            int color = RevolverItem.hasHolyAmmo(stack) ? 0xFF64C8FF : 0xFFFFFFFF;
            int x = event.getGuiGraphics().guiWidth() - mc.font.width(count) - 10;
            event.getGuiGraphics().drawString(mc.font, count, x, 10, color, true);
        }
    }
}