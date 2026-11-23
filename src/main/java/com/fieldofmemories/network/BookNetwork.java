package com.fieldofmemories.network;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BookNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FieldOfMemories.MOD_ID, "book"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private BookNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(OpenRequest.class, 0)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new OpenRequest())
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) sync(player, player.getPersistentData().getInt("FOMBookEntries"));
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(SyncEntries.class, 1)
                .encoder((packet, buffer) -> buffer.writeInt(packet.mask()))
                .decoder(buffer -> new SyncEntries(buffer.readInt()))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> com.fieldofmemories.client.OldBookScreen.open(packet.mask()));
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void requestOpen() {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new OpenRequest());
    }

    public static void sync(ServerPlayer player, int mask) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncEntries(mask));
    }

    private record OpenRequest() {}
    private record SyncEntries(int mask) {}
}
