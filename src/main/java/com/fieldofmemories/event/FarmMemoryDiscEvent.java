package com.fieldofmemories.event;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.data.BookProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FarmMemoryDiscEvent {
    private static final String HARVEST_COUNT_KEY = "FieldOfMemoriesFarmDiscHarvests";
    private static final String FOUND_KEY = "FieldOfMemoriesFarmDiscFound";
    private static final int GUARANTEED_HARVEST = 120;
    private static final float DROP_CHANCE = 0.01F;

    private FarmMemoryDiscEvent() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCropHarvest(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || player.isCreative() || player.isSpectator()) return;

        BlockState state = event.getState();
        boolean matureRegularCrop = (state.is(Blocks.WHEAT) || state.is(Blocks.CARROTS)
                || state.is(Blocks.POTATOES))
                && state.getValue(BlockStateProperties.AGE_7) == 7;
        boolean matureBeetroot = state.is(Blocks.BEETROOTS)
                && state.getValue(BlockStateProperties.AGE_3) == 3;
        if (!matureRegularCrop && !matureBeetroot) return;

        BookProgress.discover(player, BookProgress.MEMORY_DISCS);

        if (player.getPersistentData().getBoolean(FOUND_KEY)) return;

        ItemStack disc = new ItemStack(FieldOfMemories.MEMORY_DISC_7.get());
        if (player.getInventory().contains(disc)) {
            player.getPersistentData().putBoolean(FOUND_KEY, true);
            return;
        }

        int harvested = player.getPersistentData().getInt(HARVEST_COUNT_KEY) + 1;
        player.getPersistentData().putInt(HARVEST_COUNT_KEY, harvested);

        if (harvested >= GUARANTEED_HARVEST || player.getRandom().nextFloat() < DROP_CHANCE) {
            player.getPersistentData().putBoolean(FOUND_KEY, true);
            BlockPos pos = event.getPos();
            ItemEntity item = new ItemEntity(player.level(),
                    pos.getX() + 0.5D, pos.getY() + 0.65D, pos.getZ() + 0.5D,
                    disc.copy());
            item.setDefaultPickUpDelay();
            item.setDeltaMovement(0.0D, 0.18D, 0.0D);
            player.level().addFreshEntity(item);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        var originalData = event.getOriginal().getPersistentData();
        var newData = event.getEntity().getPersistentData();
        if (originalData.contains(HARVEST_COUNT_KEY)) {
            newData.putInt(HARVEST_COUNT_KEY, originalData.getInt(HARVEST_COUNT_KEY));
        }
        if (originalData.getBoolean(FOUND_KEY)) {
            newData.putBoolean(FOUND_KEY, true);
        }
    }

    @SubscribeEvent
    public static void onMemoryDiscPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getItem().getItem().is(FieldOfMemories.MEMORY_DISC_7.get())) {
            player.getPersistentData().putBoolean(FOUND_KEY, true);
        }
    }
}
