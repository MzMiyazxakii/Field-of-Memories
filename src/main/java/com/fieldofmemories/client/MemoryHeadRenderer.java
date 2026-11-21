package com.fieldofmemories.client;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MemoryHeadRenderer {
    private static final Set<UUID> VISIBLE_PLAYERS = ConcurrentHashMap.newKeySet();

    private MemoryHeadRenderer() {}

    public static void setVisible(UUID playerId, boolean visible) {
        if (visible) VISIBLE_PLAYERS.add(playerId);
        else VISIBLE_PLAYERS.remove(playerId);
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (!VISIBLE_PLAYERS.contains(player.getUUID())) return;

        var minecraft = Minecraft.getInstance();
        ItemStack headModel = new ItemStack(FieldOfMemories.MEMORY_HEAD.get());
        var poseStack = event.getPoseStack();
        poseStack.pushPose();
        event.getRenderer().getModel().head.translateAndRotate(poseStack);
        minecraft.getItemRenderer().renderStatic(
                player,
                headModel,
                ItemDisplayContext.HEAD,
                false,
                poseStack,
                event.getMultiBufferSource(),
                player.level(),
                event.getPackedLight(),
                OverlayTexture.NO_OVERLAY,
                player.getId()
        );
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        VISIBLE_PLAYERS.clear();
    }
}
