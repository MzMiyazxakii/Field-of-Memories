package com.fieldofmemories.item;

import com.fieldofmemories.entity.HolyWaterProjectile;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class HolyWaterItem extends Item {
    public HolyWaterItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            HolyWaterProjectile projectile = new HolyWaterProjectile(level, player);
            projectile.setItem(stack);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.7F, 1.0F);
            level.addFreshEntity(projectile);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public boolean hasCraftingRemainingItem() { return true; }
    @Override public ItemStack getCraftingRemainingItem(ItemStack stack) { return new ItemStack(Items.GLASS_BOTTLE); }
}