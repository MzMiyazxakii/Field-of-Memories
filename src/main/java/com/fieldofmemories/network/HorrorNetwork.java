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

        CHANNEL.messageBuilder(CameraBatterySlotPacket.class, 9)
                .encoder((packet, buffer) -> {
                    buffer.writeInt(packet.slot());
                    buffer.writeBoolean(packet.remove());
                })
                .decoder(buffer -> new CameraBatterySlotPacket(buffer.readInt(), buffer.readBoolean()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && !packet.remove() && player.containerMenu == player.inventoryMenu
                            && packet.slot() >= 0 && packet.slot() < player.inventoryMenu.slots.size()) {
                        var slot = player.inventoryMenu.slots.get(packet.slot());
                        var camera = slot.getItem();
                        var carried = player.inventoryMenu.getCarried();
                        boolean video = camera.getItem() instanceof com.fieldofmemories.item.VideoCameraItem;
                        boolean oldCamera = camera.getItem() instanceof com.fieldofmemories.item.OldCameraItem;
                        boolean inserted = video
                                ? com.fieldofmemories.item.VideoCameraItem.hasInsertedBattery(camera)
                                : oldCamera && com.fieldofmemories.item.OldCameraItem.hasInsertedBattery(camera);
                        int charge = video ? com.fieldofmemories.item.VideoCameraItem.getBattery(camera)
                                : oldCamera ? com.fieldofmemories.item.OldCameraItem.getCharge(camera) : 0;
                        int max = video ? com.fieldofmemories.item.VideoCameraItem.MAX_BATTERY
                                : oldCamera ? com.fieldofmemories.item.OldCameraItem.MAX_CHARGE : 0;
                        if ((video || oldCamera) && carried.is(FieldOfMemories.BATTERY.get())
                                && !inserted && charge < max) {
                            carried.shrink(1);
                            if (carried.isEmpty()) player.inventoryMenu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
                            if (video) com.fieldofmemories.item.VideoCameraItem.setInsertedBattery(camera, true);
                            else com.fieldofmemories.item.OldCameraItem.setInsertedBattery(camera, true);
                            slot.setChanged();
                            player.inventoryMenu.broadcastChanges();
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


    private static void fireOldCameraFlash(ServerPlayer player) {
        net.minecraft.world.item.ItemStack camera = player.getMainHandItem().getItem()
                instanceof com.fieldofmemories.item.OldCameraItem ? player.getMainHandItem()
                : player.getOffhandItem().getItem() instanceof com.fieldofmemories.item.OldCameraItem
                ? player.getOffhandItem() : net.minecraft.world.item.ItemStack.EMPTY;
        if (camera.isEmpty()) return;
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

            if (target instanceof com.fieldofmemories.entity.PlayerEchoEntity
                    || target instanceof com.fieldofmemories.entity.SmileFaceEntity) {
                target.discard();
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

    private static boolean hasUsableCamera(ServerPlayer player) {
        for (var hand : net.minecraft.world.InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof com.fieldofmemories.item.VideoCameraItem
                    && com.fieldofmemories.item.VideoCameraItem.isOpen(stack)
                    && com.fieldofmemories.item.VideoCameraItem.getBattery(stack) > 0) return true;
        }
        return false;
    }

    public static void requestCameraBatterySlot(int slot, boolean remove) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new CameraBatterySlotPacket(slot, remove));
    }

    public static void requestCameraViewing(boolean viewing) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new CameraViewPacket(viewing));
    }

    public static void stopCameraViewing(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CameraViewStopPacket());
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
    private record CameraBatterySlotPacket(int slot, boolean remove) {}
    private record CameraViewPacket(boolean viewing) {}
    private record CameraViewStopPacket() {}
    private record OldCameraFlashPacket() {}
}
