package com.fieldofmemories.event;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForbiddenCommandsEvent {
    private ForbiddenCommandsEvent() {}

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        String input = event.getParseResults().getReader().getString().stripLeading();
        if (input.startsWith("/")) input = input.substring(1);

        String[] parts = input.split("\\s+", 3);
        if (parts.length == 0) return;

        String root = parts[0];
        int namespace = root.indexOf(':');
        if (namespace >= 0) root = root.substring(namespace + 1);

        boolean blocked = root.equalsIgnoreCase("tp")
                || root.equalsIgnoreCase("teleport")
                || root.equalsIgnoreCase("kill");

        if (!blocked && root.equalsIgnoreCase("gamerule") && parts.length > 1) {
            String rule = parts[1];
            int ruleNamespace = rule.indexOf(':');
            if (ruleNamespace >= 0) rule = rule.substring(ruleNamespace + 1);
            blocked = rule.equalsIgnoreCase("keepInventory");
        }

        if (!blocked) return;

        event.setCanceled(true);
        if (event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.literal("Но...ничего не произошло")
                    .withStyle(ChatFormatting.RED));
            player.playNotifySound(FieldOfMemories.NOT_TRY_COMMAND_SOUND.get(),
                    SoundSource.MASTER, 1.0F, 1.0F);
        }
    }
}
