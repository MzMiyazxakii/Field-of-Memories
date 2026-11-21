package com.fieldofmemories.network;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import com.fieldofmemories.client.MovementClient;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NetworkHandler {
    private static final String PROTOCOL = "2";
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
                })
                .add();
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
                            CHANNEL.send(PacketDistributor.PLAYER.with(() -> target),
                                    new ForcedFallClientPacket());
                        }
                    }
                    context.get().setPacketHandled(true);
                })
                .add();
        CHANNEL.messageBuilder(ForcedFallClientPacket.class, 2)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new ForcedFallClientPacket())
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> MovementClient.beginForcedFallFromServer());
                    context.get().setPacketHandled(true);
                })
                .add();
    }

    public static void sendProneState(boolean prone) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new ProneStatePacket(prone));
    }

    public static void sendForceFall(UUID target) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new ForceFallPacket(target));
    }

    @Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID)
    public static final class ServerEvents {
        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
            Integer ticks = PRONE_PLAYERS.get(event.player.getUUID());
            if (ticks != null) {
                event.player.setPose(Pose.SWIMMING);
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
}
