package com.fieldofmemories.network;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class AmbientHorrorNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FieldOfMemories.MOD_ID, "ambient_horror"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private AmbientHorrorNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(EffectPacket.class, 0)
                .encoder((packet, buffer) -> {
                    buffer.writeInt(packet.effect());
                    buffer.writeInt(packet.duration());
                })
                .decoder(buffer -> new EffectPacket(buffer.readInt(), buffer.readInt()))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            com.fieldofmemories.client.AmbientHorrorClient.startEffect(
                                    packet.effect(), packet.duration()));
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void send(ServerPlayer player, int effect, int duration) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EffectPacket(effect, duration));
    }

    public static void sendToLevel(ServerLevel level, int effect, int duration) {
        for (ServerPlayer player : level.players()) send(player, effect, duration);
    }

    private record EffectPacket(int effect, int duration) {}
}
