package com.fieldofmemories.network;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.DistExecutor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StrawBedNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FieldOfMemories.MOD_ID, "straw_bed"),
            () -> "1", "1"::equals, "1"::equals);
    private static final Map<UUID, BlockPos> RESTING = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> ORIGINAL_POS = new ConcurrentHashMap<>();

    private StrawBedNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(SleepRequest.class, 0)
                .encoder((p, b) -> b.writeBoolean(p.sleep()))
                .decoder(b -> new SleepRequest(b.readBoolean()))
                .consumerMainThread((p, c) -> {
                    ServerPlayer player = c.get().getSender();
                    if (player != null) {
                        BlockPos bed = RESTING.get(player.getUUID());
                        if (bed != null && p.sleep() && player.serverLevel().isNight()
                                && player.serverLevel().getBlockState(bed).getBlock()
                                == FieldOfMemories.STRAW_BED_BLOCK.get()) {
                            RESTING.remove(player.getUUID());
                            ORIGINAL_POS.remove(player.getUUID());
                            player.startSleeping(bed);
                            sendState(player, false);
                        } else if (bed != null && !p.sleep()) {
                            stopRest(player);
                        }
                    }
                    c.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(RestState.class, 1)
                .encoder((p, b) -> b.writeBoolean(p.resting()))
                .decoder(b -> new RestState(b.readBoolean()))
                .consumerMainThread((p, c) -> {
                    DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                            () -> () -> com.fieldofmemories.client.StrawBedClient.setResting(p.resting()));
                    c.get().setPacketHandled(true);
                }).add();
    }

    public static void beginRest(ServerPlayer player, BlockPos bed) {
        if (RESTING.containsKey(player.getUUID())) {
            stopRest(player);
            return;
        }
        BlockState state = player.serverLevel().getBlockState(bed);
        if (state.getBlock() != FieldOfMemories.STRAW_BED_BLOCK.get()) return;
        ORIGINAL_POS.put(player.getUUID(), player.blockPosition());
        RESTING.put(player.getUUID(), bed.immutable());
        player.stopRiding();
        player.setSprinting(false);
        player.setDeltaMovement(0, 0, 0);
        player.teleportTo(bed.getX() + 0.5D, bed.getY() + 0.15D, bed.getZ() + 0.5D);
        player.setYRot(state.getValue(BedBlock.FACING).toYRot());
        player.setXRot(0.0F);
        player.setPose(Pose.SLEEPING);
        sendState(player, true);
    }

    private static void stopRest(ServerPlayer player) {
        BlockPos old = ORIGINAL_POS.remove(player.getUUID());
        RESTING.remove(player.getUUID());
        player.setPose(Pose.STANDING);
        if (old != null) player.teleportTo(old.getX() + 0.5D, old.getY(), old.getZ() + 0.5D);
        player.setDeltaMovement(0, 0, 0);
        sendState(player, false);
    }

    private static void sendState(ServerPlayer player, boolean resting) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RestState(resting));
    }

    public static void requestSleep() { CHANNEL.sendToServer(new SleepRequest(true)); }
    public static void requestWake() { CHANNEL.sendToServer(new SleepRequest(false)); }

    @Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID)
    public static final class ServerEvents {
        @SubscribeEvent
        public static void tick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
            BlockPos bed = RESTING.get(player.getUUID());
            if (bed == null) return;
            if (player.serverLevel().getBlockState(bed).getBlock() != FieldOfMemories.STRAW_BED_BLOCK.get()) {
                stopRest(player);
                return;
            }
            player.setPose(Pose.SLEEPING);
            player.setDeltaMovement(0, 0, 0);
            player.setSprinting(false);
            player.teleportTo(bed.getX() + 0.5D, bed.getY() + 0.15D, bed.getZ() + 0.5D);
        }
    }

    public record SleepRequest(boolean sleep) {}
    public record RestState(boolean resting) {}
}
