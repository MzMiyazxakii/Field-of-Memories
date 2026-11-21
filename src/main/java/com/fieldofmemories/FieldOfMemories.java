package com.fieldofmemories;

import com.fieldofmemories.event.HorrorEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(FieldOfMemories.MOD_ID)
public class FieldOfMemories {
    public static final String MOD_ID = "fieldofmemories";

    public FieldOfMemories() {
        MinecraftForge.EVENT_BUS.register(HorrorEvents.class);
    }
}
