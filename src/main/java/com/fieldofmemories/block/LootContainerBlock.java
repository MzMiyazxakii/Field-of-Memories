package com.fieldofmemories.block;

import com.fieldofmemories.data.BoxLootData;
import com.fieldofmemories.FieldOfMemories;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class LootContainerBlock extends Block {
    private static final VoxelShape BAG = Block.box(1, 0, 1, 15, 7, 15);
    private static final VoxelShape BOX = Block.box(2, 0, 1, 14, 8, 16);
    private final boolean legacyBackpack;

    public LootContainerBlock(boolean legacyBackpack) {
        super(BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.WOOD).noOcclusion());
        this.legacyBackpack = legacyBackpack;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return legacyBackpack ? BAG : BOX;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of();
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (level instanceof ServerLevel server && !legacyBackpack) {
            dropLoot(server, pos);
        }
    }

    private static void dropLoot(ServerLevel level, BlockPos pos) {
        BoxLootData table = BoxLootData.get(level);
        int rolls = 3 + level.random.nextInt(4);
        for (int i = 0; i < rolls; i++) {
            ItemStack stack = table.roll(level.random);
            if (stack.isEmpty()) continue;
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5D,
                    pos.getY() + 0.35D, pos.getZ() + 0.5D, stack);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
        if (level.random.nextFloat() < 0.012F) {
            Block.popResource(level, pos, new ItemStack(FieldOfMemories.MEMORY_DISCS[level.random.nextBoolean() ? 0 : 1].get()));
        }
    }
}
