package com.fieldofmemories.network;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.client.MovementClient;
import com.fieldofmemories.data.MemoryDiscWorldData;
import com.fieldofmemories.data.FogWorldData;
import com.fieldofmemories.data.BoxLootData;
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
    private static final String PROTOCOL = "6";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FieldOfMemories.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static final String PENDING_DISC_TAG = "FieldOfMemoriesPendingDisc";
    private static final String PENDING_DISC_HAND_TAG = "FieldOfMemoriesPendingDiscHand";
    private static final Map<UUID, Integer> PRONE_PLAYERS = new ConcurrentHashMap<>();
    private static final Map<UUID, ActiveMemoryDisc> ACTIVE_MEMORY_DISCS = new ConcurrentHashMap<>();
    private static final Map<UUID, StoolSession> STOOL_SESSIONS = new ConcurrentHashMap<>();

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

        CHANNEL.messageBuilder(FogAdjustPacket.class, 8)
                .encoder((packet, buffer) -> buffer.writeInt(packet.amount()))
                .decoder(buffer -> new FogAdjustPacket(buffer.readInt()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && player.hasPermissions(2)) {
                        int distance = FogWorldData.changeDistance(player.serverLevel(), packet.amount());
                        for (ServerPlayer online : player.server.getPlayerList().getPlayers()) {
                            CHANNEL.send(PacketDistributor.PLAYER.with(() -> online), new FogSyncPacket(distance));
                        }
                    }
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(FogSyncPacket.class, 9)
                .encoder((packet, buffer) -> buffer.writeInt(packet.distance()))
                .decoder(buffer -> new FogSyncPacket(buffer.readInt()))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> com.fieldofmemories.client.FogClient.setDistance(packet.distance()));
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(BoxLootRequestPacket.class, 10)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new BoxLootRequestPacket())
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && player.hasPermissions(2)) sendBoxLootTable(player);
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(BoxLootEditPacket.class, 11)
                .encoder((packet, buffer) -> {
                    buffer.writeInt(packet.action());
                    buffer.writeUtf(packet.itemId());
                })
                .decoder(buffer -> new BoxLootEditPacket(buffer.readInt(), buffer.readUtf()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && player.hasPermissions(2)) {
                        BoxLootData.get(player.serverLevel()).edit(packet.itemId(), packet.action());
                        sendBoxLootTable(player);
                    }
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(BoxLootSyncPacket.class, 12)
                .encoder((packet, buffer) -> buffer.writeUtf(packet.entries()))
                .decoder(buffer -> new BoxLootSyncPacket(buffer.readUtf()))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> com.fieldofmemories.client.AdminToolScreen.updateBoxLootData(packet.entries()));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(StoolStartPacket.class, 13)
                .encoder((packet, buffer) -> buffer.writeUtf(packet.sequence()))
                .decoder(buffer -> new StoolStartPacket(buffer.readUtf(64)))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            net.minecraft.client.Minecraft.getInstance().setScreen(
                                    new com.fieldofmemories.client.StoolMinigameScreen(packet.sequence())));
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(StoolFinishPacket.class, 14)
                .encoder((packet, buffer) -> buffer.writeInt(packet.progress()))
                .decoder(buffer -> new StoolFinishPacket(buffer.readInt()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) finishStoolMinigameServer(player, packet.progress());
                    context.get().setPacketHandled(true);
                }).add();
    }


    public static void startStoolMinigame(ServerPlayer player, net.minecraft.core.BlockPos pos) {
        if (STOOL_SESSIONS.containsKey(player.getUUID())) return;
        StringBuilder sequence = new StringBuilder(20);
        String[] directions = {"U", "D", "L", "R"};
        for (int i = 0; i < 20; i++) sequence.append(directions[player.serverLevel().random.nextInt(4)]);
        String pattern = sequence.toString();
        STOOL_SESSIONS.put(player.getUUID(), new StoolSession(pos.immutable(), player.serverLevel().getGameTime(), pattern));
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StoolStartPacket(pattern));
    }

    public static void finishStoolMinigame(int progress) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new StoolFinishPacket(progress));
    }

    private static void finishStoolMinigameServer(ServerPlayer player, int progress) {
        StoolSession session = STOOL_SESSIONS.remove(player.getUUID());
        if (session == null) return;
        long elapsed = player.serverLevel().getGameTime() - session.startedAt();
        if (elapsed < 780 || progress < 20 || !player.blockPosition().closerThan(session.pos(), 5.0D)) return;
        var level = player.serverLevel();
        if (!level.getBlockState(session.pos()).is(FieldOfMemories.ANOMALOUS_STOOL_BLOCK.get())) return;
        level.destroyBlock(session.pos(), false);
        net.minecraft.world.item.ItemStack reward = rollStoolReward(level.random);
        if (!reward.isEmpty()) net.minecraft.world.level.block.Block.popResource(level, session.pos(), reward);
    }

    private static ItemStack rollStoolReward(net.minecraft.util.RandomSource random) {
        int reward = random.nextInt(13);
        return switch (reward) {
            case 0 -> new ItemStack(net.minecraft.world.item.Items.DIAMOND, 1 + random.nextInt(3));
            case 1 -> new ItemStack(net.minecraft.world.item.Items.NETHERITE_INGOT);
            case 2 -> {
                ItemStack[] armor = {new ItemStack(net.minecraft.world.item.Items.IRON_HELMET), new ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE),
                        new ItemStack(net.minecraft.world.item.Items.IRON_LEGGINGS), new ItemStack(net.minecraft.world.item.Items.IRON_BOOTS),
                        new ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET), new ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE),
                        new ItemStack(net.minecraft.world.item.Items.DIAMOND_LEGGINGS), new ItemStack(net.minecraft.world.item.Items.DIAMOND_BOOTS),
                        new ItemStack(net.minecraft.world.item.Items.NETHERITE_HELMET), new ItemStack(net.minecraft.world.item.Items.NETHERITE_CHESTPLATE),
                        new ItemStack(net.minecraft.world.item.Items.NETHERITE_LEGGINGS), new ItemStack(net.minecraft.world.item.Items.NETHERITE_BOOTS)};
                yield armor[random.nextInt(armor.length)];
            }
            case 3 -> new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 3 + random.nextInt(10));
            case 4 -> new ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE, 1 + random.nextInt(5));
            case 5 -> new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING);
            case 6 -> new ItemStack(net.minecraft.world.item.Items.NETHER_STAR);
            case 7 -> new ItemStack(FieldOfMemories.MEMORY_DISC_6.get());
            case 8 -> new ItemStack(net.minecraft.world.item.Items.COAL_BLOCK, 1 + random.nextInt(6));
            case 9 -> new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 1 + random.nextInt(7));
            case 10 -> new ItemStack(net.minecraft.world.item.Items.GOAT_HORN);
            case 11 -> new ItemStack(net.minecraft.world.item.Items.ENDER_PEARL, 1 + random.nextInt(5));
            default -> new ItemStack(net.minecraft.world.item.Items.BLAZE_ROD, 1 + random.nextInt(4));
        };
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
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MemoryDiscStatePacket(2, session.discId(), -1));
        }
    }

    public static void sendProneState(boolean prone) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new ProneStatePacket(prone));
    }

    public static void sendForceFall(UUID target) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new ForceFallPacket(target));
    }

    public static void forceFall(ServerPlayer target) {
        if (target == null || !target.isAlive() || target.isCreative() || target.isSpectator()) return;
        PRONE_PLAYERS.put(target.getUUID(), 40);
        target.setPose(Pose.SWIMMING);
        target.setDeltaMovement(0.0D, 0.0D, 0.0D);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new ForcedFallClientPacket());
    }

    private static void sendBoxLootTable(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new BoxLootSyncPacket(BoxLootData.get(player.serverLevel()).serializeForClient()));
    }

    public static void requestBoxLootTable() {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new BoxLootRequestPacket());
    }

    public static void editBoxLoot(String itemId, int action) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new BoxLootEditPacket(action, itemId));
    }

    public static void changeFogDistance(int amount) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new FogAdjustPacket(amount));
    }

    public static void sendFogSetting(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new FogSyncPacket(FogWorldData.getDistance(player.serverLevel())));
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
            if (event.getEntity() instanceof ServerPlayer player) {
                restorePendingMemoryDisc(player);
                sendFogSetting(player);
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
    private record FogAdjustPacket(int amount) {}
    private record FogSyncPacket(int distance) {}
    private record BoxLootRequestPacket() {}
    private record BoxLootEditPacket(int action, String itemId) {}
    private record BoxLootSyncPacket(String entries) {}
    private record StoolStartPacket(String sequence) {}
    private record StoolFinishPacket(int progress) {}
    private record StoolSession(net.minecraft.core.BlockPos pos, long startedAt, String sequence) {}
    private record ActiveMemoryDisc(int discId, ItemStack stack, InteractionHand hand, int fragmentIndex) {}
}
