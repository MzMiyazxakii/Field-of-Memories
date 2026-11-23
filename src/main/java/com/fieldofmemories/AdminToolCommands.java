package com.fieldofmemories;

import com.fieldofmemories.event.AmbientHorrorEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID)
public final class AdminToolCommands {
    private AdminToolCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal("fomhorror")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(
                            "События: stalker, door_open, laugh, pipi, clock, steps, cake, glass, dark, blackout, silence. "
                                    + "Можно указать игрока: /fomhorror cake Steve"), false);
                    return 1;
                });

        root.then(eventCommand("stalker"));
        root.then(eventCommand("door_open"));
        root.then(eventCommand("laugh"));
        root.then(eventCommand("pipi"));
        root.then(eventCommand("clock"));
        root.then(eventCommand("steps"));
        root.then(eventCommand("cake"));
        root.then(eventCommand("glass"));
        root.then(eventCommand("dark"));
        root.then(eventCommand("blackout"));
        root.then(eventCommand("silence"));
        event.getDispatcher().register(root);

        event.getDispatcher().register(Commands.literal("admintool")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ItemStack tool = new ItemStack(FieldOfMemories.ADMIN_TOOL.get());
                    if (!player.getInventory().add(tool)) player.drop(tool, false);
                    context.getSource().sendSuccess(() -> Component.literal("Админ тулл выдан."), false);
                    return 1;
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> eventCommand(String event) {
        return Commands.literal(event)
                .executes(context -> executeEvent(context.getSource(), null, event))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> executeEvent(context.getSource(),
                                EntityArgument.getPlayer(context, "player"), event)));
    }

    private static int executeEvent(CommandSourceStack source, ServerPlayer selected, String event) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = selected != null ? selected : source.getPlayerOrException();
        if (!(target.level() instanceof ServerLevel level)) return 0;
        boolean triggered = AmbientHorrorEvents.triggerAdminEvent(level, target, event);
        if (!triggered) {
            source.sendFailure(Component.literal("Не удалось запустить событие '" + event
                    + "': рядом с игроком нет подходящего окна, двери или места."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Событие '" + event + "' запущено для " + target.getGameProfile().getName() + "."), true);
        return 1;
    }
}
