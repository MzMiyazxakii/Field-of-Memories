package com.fieldofmemories.network;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.client.MovementClient;
import com.fieldofmemories.data.MemoryDiscWorldData;
import com.fieldofmemories.item.MemoryDiscItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NetworkHandler {
    private static final String PROTOCOL = "5";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FieldOfMemories.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static final String PENDING_DISC_TAG = "FieldOfMemoriesPendingDisc";
    private static final String PENDING_DISC_HAND_TAG = "FieldOfMemoriesPendingDiscHand";
    private static final Map<UUID, Integer> PRONE_PLAYERS = new ConcurrentHashMap<>();
    private static final Map<UUID, ActiveMemoryDisc> ACTIVE_MEMORY_DISCS = new ConcurrentHashMap<>();

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

        CHANNEL.messageBuilder(MemoryDiscRequestPacket.class, 6)
                .encoder((packet, buffer) -> { buffer.writeBoolean(packet.next()); buffer.writeInt(packet.expectedFragmentIndex()); })
                .decoder(buffer -> new MemoryDiscRequestPacket(buffer.readBoolean(), buffer.readInt()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) {
                        if (packet.next()) advanceMemoryDisc(player, packet.expectedFragmentIndex());
                        else startMemoryDisc(player);
                    }
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(MemoryDiscStatePacket.class, 7)
                .encoder((packet, buffer) -> {
                    buffer.writeInt(packet.state());
                    buffer.writeInt(packet.discId());
                    buffer.writeInt(packet.fragmentIndex());
                })
                .decoder(buffer -> new MemoryDiscStatePacket(buffer.readInt(), buffer.readInt(), buffer.readInt()))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            com.fieldofmemories.client.MemoryDiscClient.handleServerState(
                                    packet.state(), packet.discId(), packet.fragmentIndex()));
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(MemoryHeadVisualPacket.class, 8)
                .encoder((packet, buffer) -> {
                    buffer.writeUUID(packet.playerId());
                    buffer.writeBoolean(packet.visible());
                })
                .decoder(buffer -> new MemoryHeadVisualPacket(buffer.readUUID(), buffer.readBoolean()))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            com.fieldofmemories.client.MemoryHeadRenderer.setVisible(packet.playerId(), packet.visible()));
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


    private static void startMemoryDisc(ServerPlayer player) {
        if (ACTIVE_MEMORY_DISCS.containsKey(player.getUUID())) return;
        InteractionHand hand = null;
        if (player.getMainHandItem().getItem() instanceof MemoryDiscItem) hand = InteractionHand.MAIN_HAND;
        else if (player.getOffhandItem().getItem() instanceof MemoryDiscItem) hand = InteractionHand.OFF_HAND;
        if (hand == null) return;

        ItemStack held = player.getItemInHand(hand);
        ItemStack reservedDisc = held.copy();
        reservedDisc.setCount(1);
        held.shrink(1);
        if (held.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
        player.getInventory().setChanged();

        int id = ((MemoryDiscItem) reservedDisc.getItem()).getDiscId();
        player.getPersistentData().put(PENDING_DISC_TAG, reservedDisc.save(new CompoundTag()));
        player.getPersistentData().putInt(PENDING_DISC_HAND_TAG, hand == InteractionHand.MAIN_HAND ? 0 : 1);
        ACTIVE_MEMORY_DISCS.put(player.getUUID(), new ActiveMemoryDisc(id, reservedDisc, hand, -1));
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MemoryDiscStatePacket(0, id, -1));
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new MemoryHeadVisualPacket(player.getUUID(), true));
    }

    private static void advanceMemoryDisc(ServerPlayer player, int expectedFragmentIndex) {
        ActiveMemoryDisc session = ACTIVE_MEMORY_DISCS.get(player.getUUID());
        if (session == null || session.fragmentIndex() != expectedFragmentIndex) return;
        List<String> fragments = MemoryDiscItem.getFragments(session.discId());
        int next = session.fragmentIndex() + 1;
        if (next < fragments.size()) {
            ACTIVE_MEMORY_DISCS.put(player.getUUID(),
                    new ActiveMemoryDisc(session.discId(), session.stack(), session.hand(), next));
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MemoryDiscStatePacket(1, session.discId(), next));
        } else {
            ACTIVE_MEMORY_DISCS.remove(player.getUUID());
            MemoryDiscWorldData.markRead(player.serverLevel(), session.discId());
            for (ServerPlayer online : player.server.getPlayerList().getPlayers()) {
                if (online.hasPermissions(2)) sendMemoryDiscStats(online);
            }
            restoreMemoryDisc(player, session);
            CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new MemoryHeadVisualPacket(player.getUUID(), false));
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MemoryDiscStatePacket(2, session.discId(), -1));
        }
    }

    private static void restoreMemoryDisc(ServerPlayer player, ActiveMemoryDisc session) {
        restoreStack(player, session.stack(), session.hand());
        clearPendingMemoryDisc(player);
    }

    private static void restoreStack(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        if (stack.isEmpty()) return;
        if (player.getItemInHand(hand).isEmpty()) {
            player.setItemInHand(hand, stack);
        } else if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.getInventory().setChanged();
    }

    private static void clearPendingMemoryDisc(ServerPlayer player) {
        player.getPersistentData().remove(PENDING_DISC_TAG);
        player.getPersistentData().remove(PENDING_DISC_HAND_TAG);
    }

    private static void restorePendingMemoryDisc(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(PENDING_DISC_TAG)) return;
        ItemStack stack = ItemStack.of(data.getCompound(PENDING_DISC_TAG));
        InteractionHand hand = data.getInt(PENDING_DISC_HAND_TAG) == 0
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        clearPendingMemoryDisc(player);
        restoreStack(player, stack, hand);
    }

    private static void cancelMemoryDisc(ServerPlayer player) {
        ActiveMemoryDisc session = ACTIVE_MEMORY_DISCS.remove(player.getUUID());
        if (session != null) {
            restoreMemoryDisc(player, session);
            CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new MemoryHeadVisualPacket(player.getUUID(), false));
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MemoryDiscStatePacket(2, session.discId(), -1));
        }
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

    public static void requestMemoryDiscStart() {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new MemoryDiscRequestPacket(false, -1));
    }

    public static void requestMemoryDiscNext(int expectedFragmentIndex) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new MemoryDiscRequestPacket(true, expectedFragmentIndex));
    }

    @Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID)
    public static final class ServerEvents {
        @SubscribeEvent
        public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) restorePendingMemoryDisc(player);
        }

        @SubscribeEvent
        public static void onStartTracking(PlayerEvent.StartTracking event) {
            if (event.getTarget() instanceof ServerPlayer target
                    && event.getEntity() instanceof ServerPlayer watcher
                    && ACTIVE_MEMORY_DISCS.containsKey(target.getUUID())) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> watcher),
                        new MemoryHeadVisualPacket(target.getUUID(), true));
            }
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) cancelMemoryDisc(player);
        }

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
            if (event.getEntity() instanceof ServerPlayer player) cancelMemoryDisc(player);
        }
    }

    private record ProneStatePacket(boolean prone) {}
    private record ForcedFallClientPacket() {}
    private record ForceFallPacket(UUID target) {}
    private record MemoryDiscActionPacket(int action, int discId) {}
    private record MemoryDiscStatsRequest() {}
    private record MemoryDiscStatsPacket(int readMask, boolean spawnEnabled, int spawnChance) {}
    private record MemoryDiscRequestPacket(boolean next, int expectedFragmentIndex) {}
    private record MemoryDiscStatePacket(int state, int discId, int fragmentIndex) {}
    private record MemoryHeadVisualPacket(UUID playerId, boolean visible) {}
    private record ActiveMemoryDisc(int discId, ItemStack stack, InteractionHand hand, int fragmentIndex) {}
}
