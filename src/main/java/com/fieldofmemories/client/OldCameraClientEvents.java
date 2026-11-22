package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.item.OldCameraItem;
import com.fieldofmemories.network.HorrorNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, value = Dist.CLIENT)
public final class OldCameraClientEvents {
    private static boolean aiming;
    private static int photoFlashTicks;

    private OldCameraClientEvents() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (photoFlashTicks > 0) photoFlashTicks--;

        boolean cameraInHand = mc.player != null
                && (mc.player.getMainHandItem().getItem() instanceof OldCameraItem
                || mc.player.getOffhandItem().getItem() instanceof OldCameraItem);
        boolean canAim = mc.player != null && mc.screen == null && cameraInHand
                && getHeldCameraCharge(mc) >= 10;
        boolean useDown = canAim && mc.options.keyUse.isDown();

        if (useDown) {
            aiming = true;
        } else if (aiming) {
            aiming = false;
            if (canAim) {
                HorrorNetwork.requestOldCameraFlash();
                photoFlashTicks = 7;
            }
        }
        if (!canAim && !useDown) aiming = false;
    }

    private static int getHeldCameraCharge(Minecraft mc) {
        if (mc.player.getMainHandItem().getItem() instanceof OldCameraItem)
            return OldCameraItem.getCharge(mc.player.getMainHandItem());
        if (mc.player.getOffhandItem().getItem() instanceof OldCameraItem)
            return OldCameraItem.getCharge(mc.player.getOffhandItem());
        return 0;
    }

    @SubscribeEvent
    public static void fov(ComputeFovModifierEvent event) {
        if (aiming) event.setNewFovModifier(event.getNewFovModifier() * 0.58F);
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var gui = event.getGuiGraphics();
        int w = gui.guiWidth();
        int h = gui.guiHeight();

        if (aiming) {
            int cx = w / 2;
            int cy = h / 2;
            int frameW = Math.min(190, w / 3);
            int frameH = Math.min(130, h / 3);
            int left = cx - frameW / 2;
            int right = cx + frameW / 2;
            int top = cy - frameH / 2;
            int bottom = cy + frameH / 2;
            int shade = 0xD9000000;
            gui.fill(0, 0, w, top, shade);
            gui.fill(0, bottom, w, h, shade);
            gui.fill(0, top, left, bottom, shade);
            gui.fill(right, top, w, bottom, shade);
            int c = 0xDDE8E0C8;
            int len = 12;
            gui.fill(left, top, left + len, top + 1, c);
            gui.fill(left, top, left + 1, top + len, c);
            gui.fill(right - len, top, right, top + 1, c);
            gui.fill(right - 1, top, right, top + len, c);
            gui.fill(left, bottom - 1, left + len, bottom, c);
            gui.fill(left, bottom - len, left + 1, bottom, c);
            gui.fill(right - len, bottom - 1, right, bottom, c);
            gui.fill(right - 1, bottom - len, right, bottom, c);
            gui.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
            String label = "PHOTO";
            gui.drawString(mc.font, label, left + 5, top + 5, 0xFFE8E0C8, false);
        }

        if (photoFlashTicks > 0) {
            int alpha = Math.min(220, photoFlashTicks * 34);
            gui.fill(0, 0, w, h, (alpha << 24) | 0x00FFFFFF);
        }
    }
}
