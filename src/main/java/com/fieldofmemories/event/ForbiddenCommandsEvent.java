package com.fieldofmemories.event;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import com.mojang.brigadier.tree.CommandNode;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForbiddenCommandsEvent {
    private ForbiddenCommandsEvent() {}

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        var context = event.getParseResults().getContext();
        var nodes = context.getNodes();
        if (nodes.isEmpty()) return;

        String root = nodes.get(0).getNode().getName();
        int namespace = root.indexOf(':');
        if (namespace >= 0) root = root.substring(namespace + 1);

        boolean blocked = root.equalsIgnoreCase("tp")
                || root.equalsIgnoreCase("teleport")
                || root.equalsIgnoreCase("kill");

        if (!blocked && root.equalsIgnoreCase("gamerule")) {
            blocked = nodes.stream().skip(1)
                    .map(node -> node.getNode().getName())
                    .anyMatch(name -> name.equalsIgnoreCase("keepInventory"));
        }

        if (!blocked) return;

        event.setCanceled(true);
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.literal("Но...ничего не произошло")
                    .withStyle(ChatFormatting.RED));
            player.playNotifySound(FieldOfMemories.NOT_TRY_COMMAND_SOUND.get(),
                    SoundSource.MASTER, 1.0F, 1.0F);
        }
    }
}
