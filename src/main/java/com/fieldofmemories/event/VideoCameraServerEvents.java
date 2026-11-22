package com.fieldofmemories.event;

import com.fieldofmemories.item.VideoCameraItem;
import com.fieldofmemories.network.HorrorNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "fieldofmemories")
public final class VideoCameraServerEvents {
    private VideoCameraServerEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || !player.getPersistentData().getBoolean("FOMCameraViewing")) return;

        net.minecraft.world.item.ItemStack active = net.minecraft.world.item.ItemStack.EMPTY;
        for (var hand : net.minecraft.world.InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof VideoCameraItem && VideoCameraItem.isOpen(stack)) {
                active = stack;
                break;
            }
        }
        if (active.isEmpty()) {
            player.getPersistentData().remove("FOMCameraViewing");
            HorrorNetwork.stopCameraViewing(player);
            return;
        }

        int charge = VideoCameraItem.getBattery(active);
        if (charge <= 0) {
            player.getPersistentData().remove("FOMCameraViewing");
            HorrorNetwork.stopCameraViewing(player);
            return;
        }
        VideoCameraItem.setBattery(active, charge - 1);
        if (charge == 1) {
            player.getPersistentData().remove("FOMCameraViewing");
            HorrorNetwork.stopCameraViewing(player);
        }
    }
}
