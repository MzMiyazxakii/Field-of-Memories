package com.fieldofmemories.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;

public class RevolverItem extends Item {
    private static final String AMMO_TAG = "FOMRevolverAmmo";
    public RevolverItem(Properties properties) { super(properties); }

    public static int getAmmoCount(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : Math.min(4, tag.getList(AMMO_TAG, 10).size());
    }

    // The first stored round is the next one that will be fired.
    public static boolean isNextRoundHoly(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return false;
        ListTag ammo = tag.getList(AMMO_TAG, 10);
        return !ammo.isEmpty() && ammo.getCompound(0).getBoolean("Holy");
    }

    public static int getHolyAmmoCount(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 0;
        ListTag ammo = tag.getList(AMMO_TAG, 10);
        int count = 0;
        for (int i = 0; i < ammo.size(); i++) {
            if (ammo.getCompound(i).getBoolean("Holy")) count++;
        }
        return count;
    }

    public static boolean hasHolyAmmo(ItemStack stack) {
        return getHolyAmmoCount(stack) > 0;
    }

    public static boolean addRound(ItemStack stack, boolean holy) {
        if (getAmmoCount(stack) >= 4) return false;
        CompoundTag tag = stack.getOrCreateTag();
        ListTag ammo = tag.getList(AMMO_TAG, 10);
        CompoundTag round = new CompoundTag();
        round.putBoolean("Holy", holy);
        ammo.add(round);
        tag.put(AMMO_TAG, ammo);
        return true;
    }

    public static boolean removeRound(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return false;
        ListTag ammo = tag.getList(AMMO_TAG, 10);
        if (ammo.isEmpty()) return false;
        ammo.remove(0);
        tag.put(AMMO_TAG, ammo);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.fieldofmemories.gotya_revolver.description").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}