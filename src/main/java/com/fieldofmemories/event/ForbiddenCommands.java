package com.fieldofmemories.event;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import com.fieldofmemories.FieldOfMemories;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = "fieldofmemories", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForbiddenCommands {
    private ForbiddenCommands() {}

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        String command = event.getParseResults().getReader().getString().trim();
        if (command.startsWith("/")) command = command.substring(1);
        String[] parts = command.toLowerCase(Locale.ROOT).split("\\s+");
        if (parts.length == 0) return;

        String root = parts[0];
        int namespace = root.indexOf(':');
        if (namespace >= 0) root = root.substring(namespace + 1);

        boolean forbidden = root.equals("tp") || root.equals("teleport") || root.equals("kill") || root.equals("keepinventory")
                || (root.equals("gamerule") && parts.length >= 2
                && parts[1].replace("minecraft:", "").equals("keepinventory"));
        if (!forbidden) return;

        event.setCanceled(true);
        var source = event.getParseResults().getContext().getSource();
        if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            player.sendSystemMessage(Component.literal("Но...ничего не произошло").withStyle(ChatFormatting.RED));
            player.playNotifySound(FieldOfMemories.SPIRIT_PRESENCE_SOUND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
