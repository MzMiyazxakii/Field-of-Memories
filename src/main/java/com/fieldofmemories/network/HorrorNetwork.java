package com.fieldofmemories.network;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.data.HorrorWorldData;
import com.fieldofmemories.data.BookProgress;
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
                    buffer.writeInt(packet.spiritChance());
                    buffer.writeInt(packet.mimicChance());
                    buffer.writeInt(packet.treeDiscChance());
                    buffer.writeInt(packet.houseCameraChance());
                })
                .decoder(buffer -> new SyncDataPacket(buffer.readInt(), buffer.readInt(),
                        buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(),
                        buffer.readInt(), buffer.readInt()))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            com.fieldofmemories.client.AdminToolScreen.updateHorrorData(
                                    packet.danger(), packet.activity(), packet.scarecrowChance(), packet.faceChance(),
                                    packet.spiritChance(), packet.mimicChance(), packet.treeDiscChance(), packet.houseCameraChance()));
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(GazePacket.class, 15)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new GazePacket())
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            com.fieldofmemories.client.GazeClient.start());
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
                            case "battery" -> new net.minecraft.world.item.ItemStack(FieldOfMemories.BATTERY.get());
                            case "video_camera" -> new net.minecraft.world.item.ItemStack(FieldOfMemories.VIDEO_CAMERA.get());
                            case "old_camera" -> new net.minecraft.world.item.ItemStack(FieldOfMemories.OLD_CAMERA.get());
                            case "anomalous_stool" -> new net.minecraft.world.item.ItemStack(FieldOfMemories.ANOMALOUS_STOOL.get());
                            default -> net.minecraft.world.item.ItemStack.EMPTY;
                        };
                        if (!stack.isEmpty() && !player.getInventory().add(stack)) player.drop(stack, false);
                    }
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(ForceRiftsPacket.class, 13)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new ForceRiftsPacket())
                .consumerMainThread((packet, context) -> {
                    ServerPlayer sender = context.get().getSender();
                    if (sender != null && sender.hasPermissions(2)) {
                        var level = sender.serverLevel();
                        for (int type = 0; type < 2; type++) {
                            double angle = level.random.nextDouble() * Math.PI * 2.0D;
                            double distance = 5.0D + level.random.nextDouble() * 3.0D;
                            int x = (int)Math.floor(sender.getX() + Math.cos(angle) * distance);
                            int z = (int)Math.floor(sender.getZ() + Math.sin(angle) * distance);
                            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                            var rift = FieldOfMemories.RIFT_ENTITY.get().create(level);
                            if (rift != null) {
                                rift.setBlackRift(type == 1);
                                rift.setLifetime(600);
                                rift.moveTo(x + 0.5D, y, z + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
                                level.addFreshEntity(rift);
                            }
                        }
                    }
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(ForcePinkRiftPacket.class, 14)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new ForcePinkRiftPacket())
                .consumerMainThread((packet, context) -> {
                    ServerPlayer sender = context.get().getSender();
                    if (sender != null && sender.hasPermissions(2)) {
                        var level = sender.serverLevel();
                        double angle = level.random.nextDouble() * Math.PI * 2.0D;
                        double distance = 5.0D + level.random.nextDouble() * 3.0D;
                        int x = (int) Math.floor(sender.getX() + Math.cos(angle) * distance);
                        int z = (int) Math.floor(sender.getZ() + Math.sin(angle) * distance);
                        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                        var rift = FieldOfMemories.RIFT_ENTITY.get().create(level);
                        if (rift != null) {
                            rift.setPinkRift(true);
                            rift.setBlackRift(false);
                            rift.setLifetime(600);
                            rift.moveTo(x + 0.5D, y, z + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
                            if (level.noCollision(rift)) level.addFreshEntity(rift);
                        }
                    }
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(ForceMimicRequestPacket.class, 11)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new ForceMimicRequestPacket())
                .consumerMainThread((packet, context) -> {
                    ServerPlayer sender = context.get().getSender();
                    if (sender != null && sender.hasPermissions(2)) {
                        var level = sender.serverLevel();
                        var random = level.random;
                        for (int attempt = 0; attempt < 8; attempt++) {
                            double angle = random.nextDouble() * Math.PI * 2.0D;
                            double distance = 6.0D + random.nextDouble() * 4.0D;
                            int x = (int) Math.floor(sender.getX() + Math.cos(angle) * distance);
                            int z = (int) Math.floor(sender.getZ() + Math.sin(angle) * distance);
                            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                            var echo = FieldOfMemories.PLAYER_ECHO_ENTITY.get().create(level);
                            if (echo == null) break;
                            echo.setSkinOwner(sender.getUUID());
                            echo.moveTo(x + 0.5D, y, z + 0.5D, random.nextFloat() * 360.0F, 0.0F);
                            if (level.noCollision(echo) && level.addFreshEntity(echo)) break;
                        }
                    }
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(OldCameraFlashPacket.class, 10)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new OldCameraFlashPacket())
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) fireOldCameraFlash(player);
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(CameraViewPacket.class, 7)
                .encoder((packet, buffer) -> buffer.writeBoolean(packet.viewing()))
                .decoder(buffer -> new CameraViewPacket(buffer.readBoolean()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) {
                        boolean allowed = packet.viewing()
                                && hasUsableCamera(player);
                        player.getPersistentData().putBoolean("FOMCameraViewing", allowed);
                        if (!allowed) player.getPersistentData().remove("FOMCameraViewing");
                    }
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(BatteryInsertPacket.class, 12)
                .encoder((packet, buffer) -> {
                    buffer.writeBoolean(packet.video());
                    buffer.writeBoolean(packet.offHand());
                })
                .decoder(buffer -> new BatteryInsertPacket(buffer.readBoolean(), buffer.readBoolean()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) {
                        var hand = packet.offHand()
                                ? net.minecraft.world.InteractionHand.OFF_HAND
                                : net.minecraft.world.InteractionHand.MAIN_HAND;
                        var battery = player.getItemInHand(hand);
                        if (battery.is(FieldOfMemories.BATTERY.get())) {
                            var camera = findRechargeableCamera(player, packet.video());
                            if (!camera.isEmpty()) {
                                battery.shrink(1);
                                if (battery.isEmpty()) player.setItemInHand(hand, net.minecraft.world.item.ItemStack.EMPTY);
                                if (packet.video()) {
                                    com.fieldofmemories.item.VideoCameraItem.setInsertedBattery(camera, true);
                                } else {
                                    com.fieldofmemories.item.OldCameraItem.setInsertedBattery(camera, true);
                                }
                                player.getInventory().setChanged();
                                player.containerMenu.broadcastChanges();
                            }
                        }
                    }
                    context.get().setPacketHandled(true);
                }).add();

        CHANNEL.messageBuilder(CameraViewStopPacket.class, 8)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new CameraViewStopPacket())
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            com.fieldofmemories.client.VideoCameraClient.forceStopFromServer());
                    context.get().setPacketHandled(true);
                }).add();

    }


    private static void fireOldCameraFlash(ServerPlayer player) {
        net.minecraft.world.item.ItemStack camera = player.getMainHandItem().getItem()
                instanceof com.fieldofmemories.item.OldCameraItem ? player.getMainHandItem()
                : player.getOffhandItem().getItem() instanceof com.fieldofmemories.item.OldCameraItem
                ? player.getOffhandItem() : net.minecraft.world.item.ItemStack.EMPTY;
        if (camera.isEmpty()) return;
        BookProgress.discover(player, BookProgress.OLD_CAMERA);
        long now = player.level().getGameTime();
        long next = player.getPersistentData().getLong("FOMOldCameraNextFlash");
        if (now < next) return;
        int charge = com.fieldofmemories.item.OldCameraItem.getCharge(camera);
        if (charge < 10) return;
        com.fieldofmemories.item.OldCameraItem.setCharge(camera, charge - 10);
        player.getPersistentData().putLong("FOMOldCameraNextFlash", now + 10L);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();

        net.minecraft.server.level.ServerLevel level = player.serverLevel();
        net.minecraft.world.phys.Vec3 direction = player.getLookAngle().normalize();
        net.minecraft.world.phys.Vec3 origin = player.getEyePosition().add(direction.scale(1.4D));
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                origin.x, origin.y, origin.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                origin.x, origin.y, origin.z, 64, 1.0D, 0.8D, 1.0D, 0.12D);
        level.playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.75F, 1.55F);

        net.minecraft.world.phys.AABB area = player.getBoundingBox().expandTowards(direction.scale(24.0D))
                .inflate(5.0D, 3.5D, 5.0D);
        for (net.minecraft.world.entity.Entity target : level.getEntitiesOfClass(
                net.minecraft.world.entity.Entity.class, area,
                entity -> entity != player && !entity.isRemoved())) {
            net.minecraft.world.phys.Vec3 toTarget = target.getBoundingBox().getCenter()
                    .subtract(player.getEyePosition());
            double distanceSqr = toTarget.lengthSqr();
            if (distanceSqr > 576.0D || distanceSqr < 0.01D) continue;
            if (direction.dot(toTarget.normalize()) < 0.70D || !player.hasLineOfSight(target)) continue;

            if (target instanceof com.fieldofmemories.entity.PlayerEchoEntity) {
                sendBanishParticles(level, target);
                target.discard();
                continue;
            }
            if (target instanceof com.fieldofmemories.entity.SpiritEntity spirit) {
                if (HorrorWorldData.rollSpiritDiscDrop(level)) {
                    net.minecraft.world.entity.item.ItemEntity reward = new net.minecraft.world.entity.item.ItemEntity(
                            level, spirit.getX(), spirit.getY() + 0.35D, spirit.getZ(),
                            new net.minecraft.world.item.ItemStack(FieldOfMemories.MEMORY_DISC_8.get()));
                    level.addFreshEntity(reward);
                }
                sendBanishParticles(level, spirit);
                spirit.discard();
                continue;
            }
            if (target instanceof com.fieldofmemories.entity.SmileFaceEntity face) {
                BookProgress.discover(player, BookProgress.FACE_SHARDS);
                int photos = player.getPersistentData().getInt("FOMClosedDoorPhotoCount") + 1;
                boolean guaranteed = photos >= 20;
                boolean dropped = guaranteed || level.random.nextFloat() < 0.10F;
                if (dropped) {
                    net.minecraft.world.entity.item.ItemEntity reward = new net.minecraft.world.entity.item.ItemEntity(
                            level, face.getX(), face.getY() + 0.35D, face.getZ(),
                            new net.minecraft.world.item.ItemStack(FieldOfMemories.MEMORY_DISC_5.get()));
                    level.addFreshEntity(reward);
                    player.getPersistentData().putInt("FOMClosedDoorPhotoCount", 0);
                } else {
                    player.getPersistentData().putInt("FOMClosedDoorPhotoCount", photos);
                }
                sendBanishParticles(level, face);
                face.discard();
                continue;
            }
            if (target instanceof net.minecraft.world.entity.LivingEntity living) {
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.BLINDNESS, 50, 0, false, false, true));
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.CONFUSION, 35, 0, false, false, true));
            }
        }
    }

    public static void requestMimic() {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new ForceMimicRequestPacket());
    }

    private static void sendBanishParticles(net.minecraft.server.level.ServerLevel level,
                                             net.minecraft.world.entity.Entity entity) {
        double x = entity.getX();
        double y = entity.getY() + entity.getBbHeight() * 0.5D;
        double z = entity.getZ();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK,
                x, y, z, 42, 0.45D, 0.7D, 0.45D, 0.035D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                x, y, z, 22, 0.35D, 0.6D, 0.35D, 0.018D);
    }

    public static void requestOldCameraFlash() {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new OldCameraFlashPacket());
    }

    public static void triggerScream(ServerPlayer target) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new ScreamPacket());
        target.hurt(target.damageSources().magic(), 4.0F);
        target.setDeltaMovement(0.0D, 0.0D, 0.0D);
        var level = target.serverLevel();
        if (level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            HorrorWorldData.setNextAmbientEvent(level, level.getGameTime() + 300L);
        }
    }

    public static void triggerGaze(ServerPlayer target) {\n        CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new GazePacket());\n    }\n\n    public static void requestScream(UUID target) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new ForceScreamRequestPacket(target));
    }

    private static void sendData(ServerPlayer player) {
        var level = player.server.overworld();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncDataPacket(
                HorrorWorldData.getDangerLevel(level),
                HorrorWorldData.getActivityMultiplier(level),
                HorrorWorldData.getScarecrowChance(level),
                HorrorWorldData.getFaceChance(level),
                HorrorWorldData.getSpiritChance(level),
                HorrorWorldData.getMimicChance(level),
                HorrorWorldData.getTreeDiscChance(level),
                HorrorWorldData.getHouseCameraChance(level)));
    }

    public static void requestRifts() { CHANNEL.send(PacketDistributor.SERVER.noArg(), new ForceRiftsPacket()); }

    public static void requestPinkRift() {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new ForcePinkRiftPacket());
    }

    public static void requestData() {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new RequestDataPacket());
    }

    public static void giveAdminItem(String itemId) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new GiveAdminItemPacket(itemId));
    }

    private static boolean hasUsableCamera(ServerPlayer player) {
        for (var hand : net.minecraft.world.InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof com.fieldofmemories.item.VideoCameraItem
                    && com.fieldofmemories.item.VideoCameraItem.isOpen(stack)
                    && com.fieldofmemories.item.VideoCameraItem.getBattery(stack) > 0) return true;
        }
        return false;
    }

    public static void requestBatteryInsert(boolean video, net.minecraft.world.InteractionHand hand) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(),
                new BatteryInsertPacket(video, hand == net.minecraft.world.InteractionHand.OFF_HAND));
    }

    private static net.minecraft.world.item.ItemStack findRechargeableCamera(ServerPlayer player, boolean video) {
        for (var stack : player.getInventory().items) {
            if (canRecharge(stack, video)) return stack;
        }
        var offhand = player.getOffhandItem();
        return canRecharge(offhand, video) ? offhand : net.minecraft.world.item.ItemStack.EMPTY;
    }

    private static boolean canRecharge(net.minecraft.world.item.ItemStack stack, boolean video) {
        if (video) {
            return stack.getItem() instanceof com.fieldofmemories.item.VideoCameraItem
                    && !com.fieldofmemories.item.VideoCameraItem.hasInsertedBattery(stack)
                    && com.fieldofmemories.item.VideoCameraItem.getBattery(stack)
                    < com.fieldofmemories.item.VideoCameraItem.MAX_BATTERY;
        }
        return stack.getItem() instanceof com.fieldofmemories.item.OldCameraItem
                && !com.fieldofmemories.item.OldCameraItem.hasInsertedBattery(stack)
                && com.fieldofmemories.item.OldCameraItem.getCharge(stack)
                < com.fieldofmemories.item.OldCameraItem.MAX_CHARGE;
    }

    public static void requestCameraViewing(boolean viewing) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new CameraViewPacket(viewing));
    }

    public static void stopCameraViewing(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CameraViewStopPacket());
    }

    public static void adjust(int setting, int amount) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new AdjustPacket(setting, amount));
    }

    private record RequestDataPacket() {}
    private record AdjustPacket(int setting, int amount) {}
    private record SyncDataPacket(int danger, int activity, int scarecrowChance, int faceChance,
                                  int spiritChance, int mimicChance, int treeDiscChance, int houseCameraChance) {}
    private record ScreamPacket() {}\n    private record GazePacket() {}
    private record ForceScreamRequestPacket(UUID target) {}
    private record GiveAdminItemPacket(String itemId) {}
    private record BatteryInsertPacket(boolean video, boolean offHand) {}
    private record CameraViewPacket(boolean viewing) {}
    private record CameraViewStopPacket() {}
    private record OldCameraFlashPacket() {}
    private record ForceMimicRequestPacket() {}
    private record ForceRiftsPacket() {}
    private record ForcePinkRiftPacket() {}
}
