package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.item.VideoCameraItem;
import com.fieldofmemories.network.HorrorNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;

public final class VideoCameraClient {
    private static boolean viewing;
    private static boolean effectLoaded;
    private static float viewBlend;
    private static int exitCooldown;
    private static int exitKickTicks;
    private static int emptyBatteryShakeTicks;
    private static int emptyBatteryShakePhase;

    private VideoCameraClient() {}

    public static boolean isViewing() {
        return viewing;
    }

    public static void toggleView() {
        Minecraft mc = Minecraft.getInstance();
        if (viewing) {
            setViewing(false);
            return;
        }
        if (mc.player == null) return;
        var main = mc.player.getMainHandItem();
        var off = mc.player.getOffhandItem();
        var camera = main.getItem() instanceof VideoCameraItem ? main
                : off.getItem() instanceof VideoCameraItem ? off : net.minecraft.world.item.ItemStack.EMPTY;
        if (!camera.isEmpty() && VideoCameraItem.isOpen(camera)
                && VideoCameraItem.getBattery(camera) > 0 && exitCooldown <= 0) {
            setViewing(true);
        }
    }

    private static void setViewing(boolean active) {
        if (viewing == active) return;
        viewing = active;
        if (active) {
            exitCooldown = 0;
            loadCameraEffect();
        } else {
            exitCooldown = 12;
            exitKickTicks = 7;
        }
        HorrorNetwork.requestCameraViewing(active);
    }

    private static void loadCameraEffect() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer != null && !effectLoaded) {
            mc.gameRenderer.loadEffect(new ResourceLocation(FieldOfMemories.MOD_ID, "shaders/post/video_camera.json"));
            effectLoaded = true;
        }
    }

    public static void forceStopFromServer() {
        viewing = false;
        exitCooldown = 12;
    }

    @Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void setup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ItemProperties.register(FieldOfMemories.VIDEO_CAMERA.get(),
                    new ResourceLocation(FieldOfMemories.MOD_ID, "open"),
                    (stack, level, entity, seed) -> VideoCameraItem.isOpen(stack) ? 1.0F : 0.0F));
        }
    }

    @Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (exitCooldown > 0) exitCooldown--;
            if (exitKickTicks > 0) exitKickTicks--;
            if (emptyBatteryShakeTicks > 0) emptyBatteryShakeTicks--;
            emptyBatteryShakePhase++;
            viewBlend = viewing ? Math.min(1.0F, viewBlend + 0.125F)
                    : Math.max(0.0F, viewBlend - 0.125F);
            if (viewBlend <= 0.0F && !viewing && effectLoaded) {
                mc.gameRenderer.shutdownEffect();
                effectLoaded = false;
            }
            if (mc.player == null) {
                if (viewing) setViewing(false);
                return;
            }
            if (viewing) {
                var main = mc.player.getMainHandItem();
                var off = mc.player.getOffhandItem();
                boolean hasOpenCamera = (main.getItem() instanceof VideoCameraItem
                        && VideoCameraItem.isOpen(main) && VideoCameraItem.getBattery(main) > 0)
                        || (off.getItem() instanceof VideoCameraItem
                        && VideoCameraItem.isOpen(off) && VideoCameraItem.getBattery(off) > 0);
                if (!hasOpenCamera) setViewing(false);
            }
        }

        @SubscribeEvent
        public static void emptyCameraUse(PlayerInteractEvent.RightClickItem event) {
            if (!event.getLevel().isClientSide || !(event.getItemStack().getItem() instanceof VideoCameraItem)) return;
            if (VideoCameraItem.getBattery(event.getItemStack()) <= 0) {
                emptyBatteryShakeTicks = 14;
                emptyBatteryShakePhase = 0;
            }
        }

        @SubscribeEvent
        public static void fov(ComputeFovModifierEvent event) {
            float fov = event.getNewFovModifier();
            if (viewBlend > 0.0F) fov *= (1.0F - 0.32F * viewBlend);
            if (exitKickTicks > 0) {
                float progress = (7.0F - exitKickTicks) / 7.0F;
                fov *= 1.0F + (float) Math.sin(progress * Math.PI) * 0.035F;
            }
            event.setNewFovModifier(fov);
        }

        @SubscribeEvent
        public static void hideCameraHand(RenderHandEvent event) {
            if (!viewing) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            var stack = mc.player.getItemInHand(event.getHand());
            if (stack.getItem() instanceof VideoCameraItem) event.setCanceled(true);
        }

        @SubscribeEvent
        public static void render(RenderGuiEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            var gui = event.getGuiGraphics();
            int w = gui.guiWidth();
            int h = gui.guiHeight();
            var main = mc.player.getMainHandItem();
            var off = mc.player.getOffhandItem();
            boolean hasCamera = main.getItem() instanceof VideoCameraItem || off.getItem() instanceof VideoCameraItem;
            boolean cameraOpen = (main.getItem() instanceof VideoCameraItem && VideoCameraItem.isOpen(main))
                    || (off.getItem() instanceof VideoCameraItem && VideoCameraItem.isOpen(off));
            if (cameraOpen && !viewing) {
                String prompt = "Нажмите U чтоб посмотреть";
                gui.drawString(mc.font, prompt, w - mc.font.width(prompt) - 8, h - 30, 0xFFFFFFFF, true);
            }
            if (hasCamera) {
                var stack = main.getItem() instanceof VideoCameraItem ? main : off;
                int charge = VideoCameraItem.getBattery(stack);
                int barWidth = 64;
                int x = w - barWidth - 10;
                int y = h / 2 - 4;
                if (emptyBatteryShakeTicks > 0 && charge <= 0) {
                    x += Math.round((float) Math.sin(emptyBatteryShakePhase * 2.7F) * 3.0F
                            * (emptyBatteryShakeTicks / 14.0F));
                }
                gui.fill(x - 1, y - 1, x + barWidth + 1, y + 7, 0xCC080808);
                gui.fill(x, y, x + barWidth, y + 5, 0xFF282828);
                int filled = Math.round(barWidth * charge / (float) VideoCameraItem.MAX_BATTERY);
                int color = charge > 360 ? 0xFFB7D48A : charge > 120 ? 0xFFE0B34F : 0xFFCC5148;
                gui.fill(x, y, x + filled, y + 5, color);
                String percent = (charge * 100 / VideoCameraItem.MAX_BATTERY) + "%";
                gui.drawString(mc.font, percent, x + barWidth - mc.font.width(percent), y - 12, 0xFFFFFFFF, true);
            }
            if (!viewing) return;
            gui.fill(0, 0, w, h, 0x26000000);
            int cx = w / 2;
            int cy = h / 2;
            int color = 0xBFFFFFFF;
            gui.fill(cx - 1, cy - 8, cx + 1, cy + 8, color);
            gui.fill(cx - 8, cy - 1, cx + 8, cy + 1, color);
            int inset = Math.max(12, Math.min(w, h) / 14);
            int len = Math.max(8, inset / 2);
            gui.fill(inset, inset, inset + len, inset + 1, color);
            gui.fill(inset, inset, inset + 1, inset + len, color);
            gui.fill(w - inset - len, inset, w - inset, inset + 1, color);
            gui.fill(w - inset - 1, inset, w - inset, inset + len, color);
            gui.fill(inset, h - inset - 1, inset + len, h - inset, color);
            gui.fill(inset, h - inset - len, inset + 1, h - inset, color);
            gui.fill(w - inset - len, h - inset - 1, w - inset, h - inset, color);
            gui.fill(w - inset - 1, h - inset - len, w - inset, h - inset, color);
        }
    }
}
