package com.fieldofmemories.network;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.data.HorrorWorldData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class HorrorNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FieldOfMemories.MOD_ID, "horror"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private HorrorNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(RequestDataPacket.class, 0)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new RequestDataPacket())
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && player.hasPermissions(2)) sendData(player);
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(AdjustPacket.class, 1)
                .encoder((packet, buffer) -> {
                    buffer.writeInt(packet.setting());
                    buffer.writeInt(packet.amount());
                })
                .decoder(buffer -> new AdjustPacket(buffer.readInt(), buffer.readInt()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && player.hasPermissions(2)
                            && packet.amount() >= -20 && packet.amount() <= 20) {
                        HorrorWorldData.changeSetting(player.server.overworld(), packet.setting(), packet.amount());
                        sendData(player);
                    }
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(SyncDataPacket.class, 2)
                .encoder((packet, buffer) -> {
                    buffer.writeInt(packet.danger());
                    buffer.writeInt(packet.activity());
                    buffer.writeInt(packet.scarecrowChance());
                    buffer.writeInt(packet.faceChance());
                })
                .decoder(buffer -> new SyncDataPacket(buffer.readInt(), buffer.readInt(),
                        buffer.readInt(), buffer.readInt()))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            com.fieldofmemories.client.AdminToolScreen.updateHorrorData(
                                    packet.danger(), packet.activity(), packet.scarecrowChance(), packet.faceChance()));
                    context.get().setPacketHandled(true);
                }).add();
    }

    private static void sendData(ServerPlayer player) {
        var level = player.server.overworld();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncDataPacket(
                HorrorWorldData.getDangerLevel(level),
                HorrorWorldData.getActivityMultiplier(level),
                HorrorWorldData.getScarecrowChance(level),
                HorrorWorldData.getFaceChance(level)));
    }

    public static void requestData() {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new RequestDataPacket());
    }

    public static void adjust(int setting, int amount) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new AdjustPacket(setting, amount));
    }

    private record RequestDataPacket() {}
    private record AdjustPacket(int setting, int amount) {}
    private record SyncDataPacket(int danger, int activity, int scarecrowChance, int faceChance) {}
}
