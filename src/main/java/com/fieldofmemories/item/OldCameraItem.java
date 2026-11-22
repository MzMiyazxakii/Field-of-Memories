package com.fieldofmemories.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

public class OldCameraItem extends Item {
    private static final String CHARGE_TAG = "FOMOldCameraCharge";
    private static final String INSERTED_BATTERY_TAG = "FOMOldCameraInsertedBattery";
    public static final int MAX_CHARGE = 100;

    public OldCameraItem(Properties properties) {
        super(properties);
    }

    public static int getCharge(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(CHARGE_TAG)
                ? Math.max(0, Math.min(MAX_CHARGE, stack.getTag().getInt(CHARGE_TAG)))
                : MAX_CHARGE;
    }

    public static void setCharge(ItemStack stack, int value) {
        stack.getOrCreateTag().putInt(CHARGE_TAG, Math.max(0, Math.min(MAX_CHARGE, value)));
    }

    public static boolean hasInsertedBattery(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(INSERTED_BATTERY_TAG);
    }

    public static void setInsertedBattery(ItemStack stack, boolean inserted) {
        stack.getOrCreateTag().putBoolean(INSERTED_BATTERY_TAG, inserted);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity,
                              int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof ServerPlayer)) return;
        if (!hasInsertedBattery(stack)) return;
        int charge = getCharge(stack);
        if (charge < MAX_CHARGE) {
            setCharge(stack, Math.min(MAX_CHARGE, charge + 2));
            if (getCharge(stack) >= MAX_CHARGE) setInsertedBattery(stack, false);
        } else {
            setInsertedBattery(stack, false);
        }
    }
}
