package com.fieldofmemories.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class AdminToolItemClient {
    private AdminToolItemClient() {}
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        // Item.use may be invoked on the integrated server thread. All GUI
        // operations must be queued onto the render/client thread.
        mc.execute(() -> {
            if (mc.player != null && mc.player.hasPermissions(2)) {
                mc.setScreen(new AdminToolScreen());
            }
        });
    }
}