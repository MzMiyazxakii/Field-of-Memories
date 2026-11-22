package com.fieldofmemories.item;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.data.MemoryDiscWorldData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;

public class CrossItem extends Item {
    public static final int NORMAL = 0;
    public static final int EMPTY = 1;
    public static final int FILLED = 2;
    private static final String STATE_TAG = "CustomModelData";
    private static final int PROTECTION_CHANCE = 35;

    public CrossItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int getState(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(STATE_TAG) : NORMAL;
    }

    public static void setState(ItemStack stack, int state) {
        if (stack.isEmpty()) return;
        int safeState = Math.max(NORMAL, Math.min(FILLED, state));
        stack.getOrCreateTag().putInt(STATE_TAG, safeState);
        if (safeState == EMPTY) {
            stack.setHoverName(Component.translatable("item.fieldofmemories.cross.dead"));
        } else if (safeState == FILLED) {
            stack.setHoverName(Component.translatable("item.fieldofmemories.cross.soul"));
        } else {
            stack.resetHoverName();
        }
    }

    public static boolean tryProtect(ServerPlayer player, boolean guaranteed) {
        ItemStack cross = findUsableCross(player);
        if (cross.isEmpty()) return false;
        RandomSource random = player.serverLevel().random;
        if (!guaranteed && random.nextInt(100) >= PROTECTION_CHANCE) return false;

        setState(cross, EMPTY);
        player.playSound(SoundEvents.ITEM_BREAK, 1.0F, 0.85F + random.nextFloat() * 0.3F);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return true;
    }

    private static ItemStack findUsableCross(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(FieldOfMemories.CROSS.get()) && getState(stack) == NORMAL) return stack;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(FieldOfMemories.CROSS.get()) && getState(stack) == NORMAL) return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getState(stack) != EMPTY) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
        if (!(level instanceof ServerLevel serverLevel) || !MemoryDiscWorldData.hasReadAll(serverLevel)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 60;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)
                || getState(stack) != EMPTY || remainingTicks % 4 != 0) return;
        RandomSource random = player.getRandom();
        for (int i = 0; i < 5; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.65D + random.nextDouble() * 0.55D;
            double x = player.getX() + Math.cos(angle) * radius;
            double y = player.getY() + 0.35D + random.nextDouble() * 1.45D;
            double z = player.getZ() + Math.sin(angle) * radius;
            Vector3f color = random.nextBoolean()
                    ? new Vector3f(0.62F, 0.12F, 0.95F)
                    : new Vector3f(0.12F, 0.85F, 0.38F);
            ((ServerLevel) level).sendParticles(new DustParticleOptions(color, 0.9F),
                    x, y, z, 1, 0.015D, 0.02D, 0.015D, 0.0D);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player
                && getState(stack) == EMPTY && MemoryDiscWorldData.hasReadAll(player.serverLevel())) {
            setState(stack, FILLED);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        int state = getState(stack);
        if (state == EMPTY) {
            tooltip.add(Component.translatable("tooltip.fieldofmemories.cross.dead"));
        } else if (state == FILLED) {
            tooltip.add(Component.translatable("tooltip.fieldofmemories.cross.soul"));
        }
    }
}
