package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.item.VideoCameraItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class VideoCameraClient {
    private static boolean viewing;

    private VideoCameraClient() {}

    public static void toggleView() {
        Minecraft mc = Minecraft.getInstance();
        if (viewing) {
            viewing = false;
            return;
        }
        if (mc.player == null) return;
        var main = mc.player.getMainHandItem();
        var off = mc.player.getOffhandItem();
        if ((main.getItem() instanceof VideoCameraItem && VideoCameraItem.isOpen(main))
                || (off.getItem() instanceof VideoCameraItem && VideoCameraItem.isOpen(off))) {
            viewing = true;
        }
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
            if (mc.player == null) {
                viewing = false;
                return;
            }
            if (viewing) {
                var main = mc.player.getMainHandItem();
                var off = mc.player.getOffhandItem();
                boolean hasOpenCamera = (main.getItem() instanceof VideoCameraItem && VideoCameraItem.isOpen(main))
                        || (off.getItem() instanceof VideoCameraItem && VideoCameraItem.isOpen(off));
                if (!hasOpenCamera) viewing = false;
            }
        }

        @SubscribeEvent
        public static void fov(ComputeFovModifierEvent event) {
            if (viewing) event.setNewFovModifier(event.getNewFovModifier() * 0.68F);
        }

        @SubscribeEvent
        public static void tintWorld(RenderGuiEvent.Pre event) {
            if (!viewing || Minecraft.getInstance().player == null) return;
            var gui = event.getGuiGraphics();
            gui.fill(0, 0, gui.guiWidth(), gui.guiHeight(), 0xA0808080);
        }

        @SubscribeEvent
        public static void render(RenderGuiEvent.Post event) {
            if (!viewing || Minecraft.getInstance().player == null) return;
            var gui = event.getGuiGraphics();
            int w = gui.guiWidth();
            int h = gui.guiHeight();
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
