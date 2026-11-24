package com.fieldofmemories.network;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.item.RevolverItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RevolverNetwork {
    private static final String PROTOCOL = "1";
    private static final String RELOAD_TICKS = "FOMRevolverReloadTicks";
    private static final String RELOAD_SLOT = "FOMRevolverReloadSlot";
    private static final String RELOAD_HOLY = "FOMRevolverReloadHoly";
    private static final String ACTION_COOLDOWN = "FOMRevolverActionCooldown";
    private static final int RELOAD_COOLDOWN_TICKS = 5;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FieldOfMemories.MOD_ID, "revolver"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private RevolverNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(ReloadRequest.class, 0).encoder((p,b) -> {}).decoder(b -> new ReloadRequest())
                .consumerMainThread((p,c) -> {
                    ServerPlayer player = c.get().getSender();
                    if (player != null) startReload(player);
                    c.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(FireRequest.class, 1).encoder((p,b) -> {}).decoder(b -> new FireRequest())
                .consumerMainThread((p,c) -> {
                    ServerPlayer player = c.get().getSender();
                    if (player != null) fire(player);
                    c.get().setPacketHandled(true);
                }).add();
    }

    public static void requestReload() { CHANNEL.send(PacketDistributor.SERVER.noArg(), new ReloadRequest()); }
    public static void requestFire() { CHANNEL.send(PacketDistributor.SERVER.noArg(), new FireRequest()); }

    private static void startReload(ServerPlayer player) {
        ItemStack revolver = player.getMainHandItem();
        if (!revolver.is(FieldOfMemories.REVOLVER.get()) || player.getPersistentData().getInt(RELOAD_TICKS) > 0
                || player.getPersistentData().getInt(ACTION_COOLDOWN) > 0
                || RevolverItem.getAmmoCount(revolver) >= 4) return;

        boolean holy = false;
        ItemStack selected = ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack candidate = player.getInventory().items.get(i);
            if (candidate.is(FieldOfMemories.HOLY_BULLET.get()) || candidate.is(FieldOfMemories.BULLET.get())) {
                selected = candidate;
                holy = candidate.is(FieldOfMemories.HOLY_BULLET.get());
                break;
            }
        }
        if (selected.isEmpty()) {
            ItemStack offhand = player.getOffhandItem();
            if (offhand.is(FieldOfMemories.HOLY_BULLET.get()) || offhand.is(FieldOfMemories.BULLET.get())) {
                selected = offhand;
                holy = offhand.is(FieldOfMemories.HOLY_BULLET.get());
            }
        }
        if (selected.isEmpty()) return;
        selected.shrink(1);
        player.getInventory().setChanged();
        player.getPersistentData().putInt(RELOAD_TICKS, 20);
        player.getPersistentData().putInt(RELOAD_SLOT, player.getInventory().selected);
        player.getPersistentData().putBoolean(RELOAD_HOLY, holy);
        player.level().playSound(null, player.blockPosition(), FieldOfMemories.BULLET_LOAD_SOUND.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    public static void tickReload(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        var data = player.getPersistentData();
        int ticks = data.getInt(RELOAD_TICKS);
        if (ticks <= 0) return;
        if (ticks > 1) {
            data.putInt(RELOAD_TICKS, ticks - 1);
            return;
        }
        data.remove(RELOAD_TICKS);
        int slot = data.getInt(RELOAD_SLOT);
        boolean holy = data.getBoolean(RELOAD_HOLY);
        data.remove(RELOAD_SLOT);
        data.remove(RELOAD_HOLY);
        ItemStack revolver = slot >= 0 && slot < serverPlayer.getInventory().items.size()
                ? serverPlayer.getInventory().getItem(slot) : ItemStack.EMPTY;
        if (revolver.is(FieldOfMemories.REVOLVER.get()) && RevolverItem.getAmmoCount(revolver) < 4) {
            RevolverItem.addRound(revolver, holy);
            serverPlayer.getInventory().setChanged();
        } else {
            ItemStack refund = new ItemStack(holy ? FieldOfMemories.HOLY_BULLET.get() : FieldOfMemories.BULLET.get());
            if (!serverPlayer.getInventory().add(refund)) serverPlayer.drop(refund, false);
        }
        serverPlayer.getPersistentData().putInt(ACTION_COOLDOWN, RELOAD_COOLDOWN_TICKS);
        serverPlayer.level().playSound(null, serverPlayer.blockPosition(), FieldOfMemories.CLOSE_BARABAN_RV_SOUND.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    private static void fire(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(FieldOfMemories.REVOLVER.get()) || player.getPersistentData().getInt(RELOAD_TICKS) > 0
                || player.getPersistentData().getInt(ACTION_COOLDOWN) > 0) return;
        if (RevolverItem.getAmmoCount(stack) <= 0) {
            player.level().playSound(null, player.blockPosition(), FieldOfMemories.NOT_BULLETS_SOUND.get(),
                    SoundSource.PLAYERS, 0.9F, 1.0F);
            return;
        }
        RevolverItem.removeRound(stack);
        player.getInventory().setChanged();
        player.level().playSound(null, player.blockPosition(), FieldOfMemories.FIRE_RV_SOUND.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);

        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(48.0D));
        AABB search = player.getBoundingBox().expandTowards(look.scale(48.0D)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, start, end, search,
                entity -> entity instanceof LivingEntity && entity != player && entity.isPickable(), 0.0D);
        BlockHitResult blockHit = player.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (entityHit != null && (blockHit.getType() == HitResult.Type.MISS
                || entityHit.getLocation().distanceToSqr(start) <= blockHit.getLocation().distanceToSqr(start))) {
            entityHit.getEntity().hurt(player.damageSources().playerAttack(player), 6.5F);
        }
    }

    private record ReloadRequest() {}
    private record FireRequest() {}
}