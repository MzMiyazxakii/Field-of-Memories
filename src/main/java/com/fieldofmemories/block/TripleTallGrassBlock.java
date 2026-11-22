package com.fieldofmemories.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TripleTallGrassBlock extends BushBlock {
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    private static final VoxelShape PLANT_SHAPE = net.minecraft.world.level.block.Block.box(2, 0, 2, 14, 16, 14);

    public TripleTallGrassBlock() {
        super(BlockBehaviour.Properties.of().noCollission().instabreak()
                .sound(SoundType.GRASS).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(PART, Part.BOTTOM));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PLANT_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        Part part = state.getValue(PART);
        if (part == Part.BOTTOM) {
            return super.mayPlaceOn(level.getBlockState(pos.below()), level, pos.below());
        }
        BlockState below = level.getBlockState(pos.below());
        return below.is(this) && below.getValue(PART) == (part == Part.MIDDLE ? Part.BOTTOM : Part.MIDDLE);
    }

    public enum Part implements StringRepresentable {
        BOTTOM("bottom"), MIDDLE("middle"), TOP("top");

        private final String name;
        Part(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }
}
