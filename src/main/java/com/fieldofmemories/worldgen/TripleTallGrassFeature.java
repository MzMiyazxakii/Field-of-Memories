package com.fieldofmemories.worldgen;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.block.TripleTallGrassBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class TripleTallGrassFeature extends Feature<NoneFeatureConfiguration> {
    private static final ResourceKey<Biome> FIELD = ResourceKey.create(
            Registries.BIOME, new ResourceLocation(FieldOfMemories.MOD_ID, "gloomy_field"));

    public TripleTallGrassFeature(com.mojang.serialization.Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        boolean placed = false;

        for (int i = 0; i < 48; i++) {
            int x = origin.getX() + random.nextInt(16);
            int z = origin.getZ() + random.nextInt(16);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos ground = new BlockPos(x, y - 1, z);
            if (!level.getBiome(ground).is(FIELD)) continue;

            BlockState floor = level.getBlockState(ground);
            if (!(floor.is(Blocks.GRASS_BLOCK) || floor.is(Blocks.DIRT)
                    || floor.is(Blocks.COARSE_DIRT) || floor.is(Blocks.ROOTED_DIRT))) continue;
            if (!level.getBlockState(ground.above()).isAir()
                    || !level.getBlockState(ground.above(2)).isAir()
                    || !level.getBlockState(ground.above(3)).isAir()) continue;

            level.setBlock(ground.above(), FieldOfMemories.TRIPLE_TALL_GRASS.get()
                    .defaultBlockState().setValue(TripleTallGrassBlock.PART, TripleTallGrassBlock.Part.BOTTOM), 2);
            level.setBlock(ground.above(2), FieldOfMemories.TRIPLE_TALL_GRASS.get()
                    .defaultBlockState().setValue(TripleTallGrassBlock.PART, TripleTallGrassBlock.Part.MIDDLE), 2);
            level.setBlock(ground.above(3), FieldOfMemories.TRIPLE_TALL_GRASS.get()
                    .defaultBlockState().setValue(TripleTallGrassBlock.PART, TripleTallGrassBlock.Part.TOP), 2);
            placed = true;
        }
        return placed;
    }
}
