package com.fieldofmemories.event;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.network.RevolverNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RevolverEvents {
    private RevolverEvents() {}
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer)
            RevolverNetwork.tickReload(event.player);
    }
}