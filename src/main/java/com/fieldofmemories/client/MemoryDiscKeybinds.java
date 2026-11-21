package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class MemoryDiscKeybinds {
    public static final KeyMapping READ_MEMORY = new KeyMapping(
            "key.fieldofmemories.read_memory_disc",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            "key.categories.fieldofmemories"
    );

    private MemoryDiscKeybinds() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(READ_MEMORY);
    }
}
