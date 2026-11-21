package com.fieldofmemories;

import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID)
public final class AdminToolCommands {
    private AdminToolCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("admintool")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ItemStack tool = new ItemStack(FieldOfMemories.ADMIN_TOOL.get());
                    if (!player.getInventory().add(tool)) player.drop(tool, false);
                    context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Админ тулл выдан."), false);
                    return 1;
                }));
    }
}
