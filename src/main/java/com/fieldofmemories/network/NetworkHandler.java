package com.fieldofmemories.network;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.client.MovementClient;
import com.fieldofmemories.data.MemoryDiscWorldData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NetworkHandler {
    private static final String PROTOCOL = "3";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FieldOfMemories.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static final Map<UUID, Integer> PRONE_PLAYERS = new ConcurrentHashMap<>();

    private NetworkHandler() {}

    public static void register() {
        CHANNEL.messageBuilder(ProneStatePacket.class, 0)
                .encoder((packet, buffer) -> buffer.writeBoolean(packet.prone()))
                .decoder(buffer -> new ProneStatePacket(buffer.readBoolean()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) {
                        if (packet.prone()) {
                            PRONE_PLAYERS.put(player.getUUID(), -1);
                            player.setPose(Pose.SWIMMING);
                            player.setDeltaMovement(0, 0, 0);
                        } else {
                            PRONE_PLAYERS.remove(player.getUUID());
                            player.setPose(Pose.STANDING);
                        }
                    }
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(ForceFallPacket.class, 1)
                .encoder((packet, buffer) -> buffer.writeUUID(packet.target()))
                .decoder(buffer -> new ForceFallPacket(buffer.readUUID()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer sender = context.get().getSender();
                    if (sender != null && sender.hasPermissions(2)) {
                        ServerPlayer target = sender.server.getPlayerList().getPlayer(packet.target());
                        if (target != null) {
                            PRONE_PLAYERS.put(target.getUUID(), 40);
                            target.setPose(Pose.SWIMMING);
                            target.setDeltaMovement(0, 0, 0);
                            CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new ForcedFallClientPacket());
                        }
                    }
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(ForcedFallClientPacket.class, 2)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new ForcedFallClientPacket())
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> MovementClient.beginForcedFallFromServer());
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(MemoryDiscActionPacket.class, 3)
                .encoder((packet, buffer) -> { buffer.writeInt(packet.action()); buffer.writeInt(packet.discId()); })
                .decoder(buffer -> new MemoryDiscActionPacket(buffer.readInt(), buffer.readInt()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && player.hasPermissions(2)) {
                        var level = player.serverLevel();
                        if (packet.action() == 0 && packet.discId() >= 0 && packet.discId() < 8) {
                            ItemStack stack = new ItemStack(FieldOfMemories.MEMORY_DISCS[packet.discId()].get());
                            if (!player.getInventory().add(stack)) player.drop(stack, false);
                        } else if (packet.action() == 1) {
                            MemoryDiscWorldData.setSpawnEnabled(level, !MemoryDiscWorldData.isSpawnEnabled(level));
                        } else if (packet.action() == 2) {
                            MemoryDiscWorldData.changeSpawnChance(level, 5);
                        } else if (packet.action() == 3) {
                            MemoryDiscWorldData.changeSpawnChance(level, -5);
                        }
                        sendMemoryDiscStats(player);
                    }
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(MemoryDiscStatsRequest.class, 4)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new MemoryDiscStatsRequest())
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && player.hasPermissions(2)) sendMemoryDiscStats(player);
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(MemoryDiscStatsPacket.class, 5)
                .encoder((packet, buffer) -> {
                    buffer.writeInt(packet.readMask());
                    buffer.writeBoolean(packet.spawnEnabled());
                    buffer.writeInt(packet.spawnChance());
                })
                .decoder(buffer -> new MemoryDiscStatsPacket(buffer.readInt(), buffer.readBoolean(), buffer.readInt()))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            com.fieldofmemories.client.AdminToolScreen.updateMemoryData(
                                    packet.readMask(), packet.spawnEnabled(), packet.spawnChance()));
                    context.get().setPacketHandled(true);
                }).add();
    }

    private static void sendMemoryDiscStats(ServerPlayer player) {
        var level = player.serverLevel();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MemoryDiscStatsPacket(
                MemoryDiscWorldData.getReadMask(level),
                MemoryDiscWorldData.isSpawnEnabled(level),
                MemoryDiscWorldData.getSpawnChance(level)));
    }

    public static void sendProneState(boolean prone) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new ProneStatePacket(prone));
    }

    public static void sendForceFall(UUID target) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new ForceFallPacket(target));
    }

    public static void sendMemoryDiscAction(int action, int discId) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new MemoryDiscActionPacket(action, discId));
    }

    public static void requestMemoryDiscStats() {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new MemoryDiscStatsRequest());
    }

    @Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID)
    public static final class ServerEvents {
        @SubscribeEvent
        public static void onLivingDrops(LivingDropsEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer)) return;
            event.getDrops().removeIf(drop -> drop.getItem().is(FieldOfMemories.ADMIN_TOOL.get()));
        }
        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
            Integer ticks = PRONE_PLAYERS.get(event.player.getUUID());
            if (ticks != null) {
                event.player.setPose(Pose.SWIMMING);
                event.player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                if (ticks > 0) {
                    if (ticks == 1) {
                        PRONE_PLAYERS.remove(event.player.getUUID());
                        event.player.setPose(Pose.STANDING);
                    } else PRONE_PLAYERS.put(event.player.getUUID(), ticks - 1);
                }
            }
        }
        @SubscribeEvent
        public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            PRONE_PLAYERS.remove(event.getEntity().getUUID());
        }
    }

    private record ProneStatePacket(boolean prone) {}
    private record ForcedFallClientPacket() {}
    private record ForceFallPacket(UUID target) {}
    private record MemoryDiscActionPacket(int action, int discId) {}
    private record MemoryDiscStatsRequest() {}
    private record MemoryDiscStatsPacket(int readMask, boolean spawnEnabled, int spawnChance) {}
}
