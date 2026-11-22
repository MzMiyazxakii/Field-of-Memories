package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.item.OldCameraItem;
import com.fieldofmemories.network.HorrorNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, value = Dist.CLIENT)
public final class OldCameraClientEvents {
    private static int holdTicks;

    private OldCameraClientEvents() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !mc.options.keyUse.isDown()) {
            holdTicks = 0;
            return;
        }
        var main = mc.player.getMainHandItem();
        var off = mc.player.getOffhandItem();
        var camera = main.getItem() instanceof OldCameraItem ? main
                : off.getItem() instanceof OldCameraItem ? off : net.minecraft.world.item.ItemStack.EMPTY;
        if (camera.isEmpty() || OldCameraItem.getCharge(camera) < 10) {
            holdTicks = 0;
            return;
        }
        if (holdTicks == 0 || ++holdTicks >= 20) {
            HorrorNetwork.requestOldCameraFlash();
            holdTicks = 1;
        }
    }
}
