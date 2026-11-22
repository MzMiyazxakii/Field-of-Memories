package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, value = Dist.CLIENT)
public final class FogClient {
    private static int distance = 88;

    private FogClient() {}

    public static int getDistance() {
        return distance;
    }

    public static void setDistance(int value) {
        distance = Math.max(48, Math.min(160, value));
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        float far = distance;
        event.setNearPlaneDistance(Math.max(4.0F, far * 0.12F));
        event.setFarPlaneDistance(far);
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        // Slightly lower the atmospheric brightness without washing out the scene.
        event.setRed(event.getRed() * 0.92F);
        event.setGreen(event.getGreen() * 0.92F);
        event.setBlue(event.getBlue() * 0.92F);
    }
}
