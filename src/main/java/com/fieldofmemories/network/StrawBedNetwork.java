package com.fieldofmemories.network;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;\nimport net.minecraftforge.event.TickEvent;\nimport net.minecraftforge.eventbus.api.SubscribeEvent;\nimport net.minecraftforge.fml.common.Mod;\nimport net.minecraftforge.api.distmarker.Dist;\nimport net.minecraftforge.fml.DistExecutor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StrawBedNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FieldOfMemories.MOD_ID, "straw_bed"),
            () -> "1", "1"::equals, "1"::equals);
    private static final Map<UUID, BlockPos> RESTING = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> ORIGINAL_POS = new ConcurrentHashMap<>();
    private static boolean registered;

    private StrawBedNetwork() {}

    public static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.messageBuilder(SleepRequest.class, 0)
                .encoder((p,b) -> b.writeBoolean(p.sleep()))
                .decoder(b -> new SleepRequest(b.readBoolean()))
                .consumerMainThread((p,c) -> {
                    ServerPlayer player = c.get().getSender();
                    if (player != null) {
                        BlockPos bed = RESTING.get(player.getUUID());
                        if (bed != null && p.sleep() && player.serverLevel().isNight()
                                && player.serverLevel().getBlockState(bed).getBlock() instanceof BedBlock) {
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
    }

    public static void beginRest(ServerPlayer player, BlockPos bed) {
        if (RESTING.containsKey(player.getUUID())) {
            stopRest(player);
            return;
        }
        BlockState state = player.serverLevel().getBlockState(bed);
        if (!(state.getBlock() instanceof BedBlock)) return;
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
        if (old != null) {
            player.teleportTo(old.getX() + 0.5D, old.getY(), old.getZ() + 0.5D);
        }
        player.setDeltaMovement(0, 0, 0);
        sendState(player, false);
    }

    private static void sendState(ServerPlayer player, boolean resting) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RestState(resting));
    }

    public static void requestSleep() { CHANNEL.sendToServer(new SleepRequest(true)); }
    public static void requestWake() { CHANNEL.sendToServer(new SleepRequest(false)); }

    @Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID)\n    public static final class ServerEvents {\n        @SubscribeEvent\n        public static void tick(TickEvent.PlayerTickEvent event) {\n            if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;\n            BlockPos bed = RESTING.get(player.getUUID());\n            if (bed == null) return;\n            if (!player.serverLevel().getBlockState(bed).getBlock().equals(com.fieldofmemories.FieldOfMemories.STRAW_BED_BLOCK.get())) {\n                stopRest(player);\n                return;\n            }\n            player.setPose(Pose.SLEEPING);\n            player.setDeltaMovement(0, 0, 0);\n            player.setSprinting(false);\n            player.teleportTo(bed.getX() + 0.5D, bed.getY() + 0.15D, bed.getZ() + 0.5D);\n        }\n    }\n\n    public record SleepRequest(boolean sleep) {}
    public record RestState(boolean resting) {}
}
