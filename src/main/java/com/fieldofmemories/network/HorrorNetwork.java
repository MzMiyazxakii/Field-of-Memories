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
import java.util.UUID;

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

        CHANNEL.messageBuilder(ScreamPacket.class, 3)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new ScreamPacket())
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            com.fieldofmemories.client.HorrorClient.startScream());
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(ForceScreamRequestPacket.class, 4)
                .encoder((packet, buffer) -> buffer.writeUUID(packet.target()))
                .decoder(buffer -> new ForceScreamRequestPacket(buffer.readUUID()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer sender = context.get().getSender();
                    if (sender != null && sender.hasPermissions(2)) {
                        ServerPlayer target = sender.server.getPlayerList().getPlayer(packet.target());
                        if (target != null) triggerScream(target);
                    }
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(GiveAdminItemPacket.class, 5)
                .encoder((packet, buffer) -> buffer.writeUtf(packet.itemId()))
                .decoder(buffer -> new GiveAdminItemPacket(buffer.readUtf(64)))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && player.hasPermissions(2)) {
                        net.minecraft.world.item.ItemStack stack = switch (packet.itemId()) {
                            case "video_camera" -> new net.minecraft.world.item.ItemStack(FieldOfMemories.VIDEO_CAMERA.get());
                            case "straw_bed" -> new net.minecraft.world.item.ItemStack(FieldOfMemories.STRAW_BED_ITEM.get());
                            default -> net.minecraft.world.item.ItemStack.EMPTY;
                        };
                        if (!stack.isEmpty() && !player.getInventory().add(stack)) player.drop(stack, false);
                    }
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(UseStrawBedPacket.class, 6)
                .encoder((packet, buffer) -> buffer.writeBlockPos(packet.pos()))
                .decoder(buffer -> new UseStrawBedPacket(buffer.readBlockPos()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && !player.isSleeping()) {
                        var bedState = player.level().getBlockState(packet.pos());
                        if (bedState.is(FieldOfMemories.STRAW_BED_BLOCK.get())) {
                            var headPos = com.fieldofmemories.block.StrawBedBlock.getHeadPos(bedState, packet.pos());
                            var headState = player.level().getBlockState(headPos);
                            if (player.distanceToSqr(headPos.getX() + 0.5D, headPos.getY() + 0.5D,
                                    headPos.getZ() + 0.5D) <= 36.0D
                                    && headState.is(FieldOfMemories.STRAW_BED_BLOCK.get())
                                    && !headState.getValue(net.minecraft.world.level.block.BedBlock.OCCUPIED)) {
                                player.startSleeping(headPos);
                            }
                        }
                    }
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void triggerScream(ServerPlayer target) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new ScreamPacket());
        target.hurt(target.damageSources().magic(), 4.0F);
        target.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    public static void requestScream(UUID target) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new ForceScreamRequestPacket(target));
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

    public static void giveAdminItem(String itemId) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new GiveAdminItemPacket(itemId));
    }

    public static void requestUseStrawBed(net.minecraft.core.BlockPos pos) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new UseStrawBedPacket(pos));
    }

    public static void adjust(int setting, int amount) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new AdjustPacket(setting, amount));
    }

    private record RequestDataPacket() {}
    private record AdjustPacket(int setting, int amount) {}
    private record SyncDataPacket(int danger, int activity, int scarecrowChance, int faceChance) {}
    private record ScreamPacket() {}
    private record ForceScreamRequestPacket(UUID target) {}
    private record GiveAdminItemPacket(String itemId) {}
    private record UseStrawBedPacket(net.minecraft.core.BlockPos pos) {}
}
