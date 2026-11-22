package com.fieldofmemories.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StrawBedBlock extends BedBlock {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D);

    public StrawBedBlock(DyeColor color, Properties properties) {
        super(color, properties);
    }

    public static BlockPos getHeadPos(BlockState state, BlockPos pos) {
        return state.getValue(PART) == BedPart.HEAD ? pos : pos.relative(state.getValue(FACING));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // BedBlock normally uses the vanilla animated bed renderer. This custom
        // bed has no vanilla bed texture/renderer, so render its supplied block model.
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (player.isSleeping()) return InteractionResult.PASS;
        BlockPos headPos = getHeadPos(state, pos);
        BlockState headState = level.getBlockState(headPos);
        if (!headState.is(this) || headState.getValue(OCCUPIED)) return InteractionResult.PASS;
        if (!level.isClientSide) player.startSleeping(headPos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
