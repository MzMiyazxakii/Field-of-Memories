package com.fieldofmemories.item;

import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class VideoCameraItem extends Item {
    private static final String OPEN_TAG = "FieldOfMemoriesCameraOpen";
    private static final String BATTERY_TAG = "FieldOfMemoriesCameraBattery";
    public static final int MAX_BATTERY = 1200;

    public VideoCameraItem(Properties properties) {
        super(properties);
    }

    public static boolean isOpen(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(OPEN_TAG);
    }

    public static int getBattery(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(BATTERY_TAG)
                ? Math.max(0, Math.min(MAX_BATTERY, stack.getTag().getInt(BATTERY_TAG)))
                : MAX_BATTERY;
    }

    public static void setBattery(ItemStack stack, int value) {
        stack.getOrCreateTag().putInt(BATTERY_TAG, Math.max(0, Math.min(MAX_BATTERY, value)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (!stack.getOrCreateTag().contains(BATTERY_TAG)) setBattery(stack, MAX_BATTERY);
            stack.getOrCreateTag().putBoolean(OPEN_TAG, !isOpen(stack));
            player.getCooldowns().addCooldown(this, 8);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
