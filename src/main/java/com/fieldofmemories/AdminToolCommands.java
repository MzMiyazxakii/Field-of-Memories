package com.fieldofmemories;

import com.fieldofmemories.event.AmbientHorrorEvents;
import com.fieldofmemories.event.HorrorEvents;
import com.fieldofmemories.data.HorrorWorldData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
                            "События: stalker, scarecrow, spirit, face, mimic, rift, pink_rift, black_rift, "
                                    + "door_open, door_jam, knock, door_knock, laugh, pipi, clock, steps, death_sound, distant_death, screamsoul, "
                                    + "spirit_presence, face_scream, cake, glass, recolor, dark, blackout, silence, "
                                    + "stool, workstation, sign, fake_item. "
                                    + "Можно указать игрока: /fomhorror death_sound Steve"), false);
                    return 1;
                });

        root.then(eventCommand("stalker"));
        root.then(eventCommand("door_open"));
        root.then(eventCommand("distant_death"));
        root.then(eventCommand("door_knock"));
        root.then(eventCommand("door_jam"));
        root.then(eventCommand("laugh"));
        root.then(eventCommand("pipi"));
        root.then(eventCommand("clock"));
        root.then(eventCommand("steps"));
        root.then(eventCommand("cake"));
        root.then(eventCommand("glass"));
        root.then(eventCommand("dark"));
        root.then(eventCommand("blackout"));
        root.then(eventCommand("silence"));
        root.then(eventCommand("scarecrow"));
        root.then(eventCommand("spirit"));
        root.then(eventCommand("face"));
        root.then(eventCommand("mimic"));
        root.then(eventCommand("rift"));
        root.then(eventCommand("pink_rift"));
        root.then(eventCommand("black_rift"));
        root.then(eventCommand("knock"));
        root.then(eventCommand("death_sound"));
        root.then(eventCommand("screamsoul"));
        root.then(eventCommand("spirit_presence"));
        root.then(eventCommand("face_scream"));
        root.then(eventCommand("recolor"));
        root.then(eventCommand("bed_carpet"));
        root.then(eventCommand("stool"));
        root.then(eventCommand("workstation"));
        root.then(eventCommand("sign"));
        root.then(eventCommand("fake_item"));
        event.getDispatcher().register(root);

        event.getDispatcher().register(Commands.literal("sethorroractive")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status")
                        .executes(context -> {
                            ServerLevel level = context.getSource().getLevel();
                            int bonus = HorrorWorldData.getHorrorActiveBonus(level);
                            int scale = HorrorWorldData.getEventChanceScalePercent(level);
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "Общий шанс хоррор-событий: активность мира " + scale
                                            + "%, бонус команды " + (bonus >= 0 ? "+" : "") + bonus + "%."), false);
                            return 1;
                        }))
                .then(Commands.argument("percent", IntegerArgumentType.integer(-100, 100))
                        .executes(context -> {
                            ServerLevel level = context.getSource().getLevel();
                            int amount = IntegerArgumentType.getInteger(context, "percent");
                            int updated = HorrorWorldData.changeHorrorActive(level, amount);
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "Общий бонус к шансам хоррор-событий: " + (updated >= 0 ? "+" : "") + updated + "%."), true);
                            return 1;
                        })));


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
        boolean triggered = AmbientHorrorEvents.triggerAdminEvent(level, target, event)
                || HorrorEvents.triggerAdminEvent(level, target, event);
        if (!triggered) {
            source.sendFailure(Component.literal("Не удалось запустить событие '" + event
                    + "': рядом с игроком нет подходящего окна, двери или места."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Событие '" + event + "' запущено для " + target.getGameProfile().getName() + "."), true);
        return 1;
    }
}
