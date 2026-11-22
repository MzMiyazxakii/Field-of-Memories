package com.fieldofmemories.worldgen;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import com.fieldofmemories.block.TreeLeafletBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class FieldDecorFeature extends Feature<NoneFeatureConfiguration> {
    private static final ResourceKey<Biome> FIELD_BIOME =
            ResourceKey.create(Registries.BIOME, new ResourceLocation(FieldOfMemories.MOD_ID, "gloomy_field"));
    private static final ResourceKey<Biome> EDGE_BIOME =
            ResourceKey.create(Registries.BIOME, new ResourceLocation(FieldOfMemories.MOD_ID, "gloomy_edge"));
    private static final ResourceKey<Biome> FOREST_BIOME =
            ResourceKey.create(Registries.BIOME, new ResourceLocation(FieldOfMemories.MOD_ID, "gloomy_forest"));

    public FieldDecorFeature(com.mojang.serialization.Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        ChunkPos chunk = new ChunkPos(context.origin());
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();

        // A continuous, gently winding main track, calculated from world coordinates
        // so neighboring chunks join without visible gaps.
        for (int z = minZ; z < minZ + 16; z++) {
            for (int x = minX; x < minX + 16; x++) {
                BlockPos ground = surface(level, x, z);
                if (ground == null || !isOurBiome(level, ground)) continue;
                if (level.getBiome(ground).is(EDGE_BIOME)) {
                    // A continuous, gently winding abandoned asphalt road through the edge biome.
                    double centerZ = roadCenterZ(x);
                    double distance = Math.abs(z - centerZ);
                    if (distance <= 2.7D) {
                        BlockState road = Blocks.BLACK_CONCRETE.defaultBlockState();
                        if (distance >= 2.0D) road = Blocks.WHITE_CONCRETE.defaultBlockState();
                        if (Math.abs(z - Math.round(centerZ)) < 0.5D && Math.floorMod(x, 18) < 5) {
                            road = Blocks.YELLOW_CONCRETE.defaultBlockState();
                        }
                        replaceGround(level, ground, road);
                    } else if (distance <= 3.7D) {
                        replaceGround(level, ground, Blocks.GRAVEL.defaultBlockState());
                    }
                } else {
                    double roadX = roadCenterX(z);
                    double distance = Math.abs(x - roadX);
                    boolean trail = isBranchTrail(x, z);
                    if (distance <= 1.8D) {
                        replaceGround(level, ground, Blocks.DIRT_PATH.defaultBlockState());
                    } else if (distance <= 2.8D) {
                        replaceGround(level, ground, Blocks.COARSE_DIRT.defaultBlockState());
                    } else if (trail && Math.abs(x - roadX) > 3.0D) {
                        replaceGround(level, ground, Blocks.DIRT_PATH.defaultBlockState());
                    }
                }
            }
        }

        // The transition biome is intentionally more cluttered than the open field.
        BlockPos chunkCenter = surface(level, minX + 8, minZ + 8);
        boolean edgeChunk = chunkCenter != null && level.getBiome(chunkCenter).is(EDGE_BIOME);
        float density = edgeChunk ? 1.65F : 1.0F;
        if (random.nextFloat() < 0.055F * density) {
            placeAbandonedPlot(level, random, minX, minZ);
        }
        if (random.nextFloat() < 0.065F * density) {
            placeBlankSign(level, random, minX, minZ);
        }
        if (random.nextFloat() < 0.14F * (edgeChunk ? 1.35F : 1.0F)) {
            placeHayStack(level, random, minX, minZ);
        }
        if (random.nextFloat() < 0.12F * (edgeChunk ? 1.4F : 1.0F)) {
            placeLoosePumpkins(level, random, minX, minZ);
        }

        // Leaflets are attached after the forest trees have generated.
        placeTreeLeaflets(level, random, minX, minZ);
        return true;
    }


    private static void placeTreeLeaflets(WorldGenLevel level, RandomSource random, int minX, int minZ) {
        BlockState leaflet = FieldOfMemories.TREE_LEAFLET_BLOCK.get().defaultBlockState();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                int bottomY = Math.max(level.getMinBuildHeight(), topY - 28);
                for (int y = bottomY; y < topY; y++) {
                    BlockPos logPos = new BlockPos(x, y, z);
                    if (!level.getBiome(logPos).is(FOREST_BIOME) && !level.getBiome(logPos).is(EDGE_BIOME)) continue;
                    if (!level.getBlockState(logPos).is(BlockTags.LOGS)) continue;

                    // A rare collectible can hang from the upper end of a tree trunk.
                    // The invisible item frame keeps the disc visually attached to the bark.
                    if (!level.getBlockState(logPos.above()).is(BlockTags.LOGS)
                            && random.nextFloat() < 0.025F) {
                        placeHangingMemoryDisc(level, logPos, random);
                    }

                    if (random.nextFloat() > 0.22F) continue;
                    Direction start = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                    for (int offset = 0; offset < 4; offset++) {
                        Direction facing = Direction.from2DDataValue((start.get2DDataValue() + offset) & 3);
                        BlockPos leafletPos = logPos.relative(facing);
                        if (!level.getBlockState(leafletPos).isAir()) continue;
                        BlockState state = leaflet.setValue(TreeLeafletBlock.FACING, facing);
                        if (!state.canSurvive(level, leafletPos)) continue;
                        level.setBlock(leafletPos, state, 2);
                        break;
                    }
                }
            }
        }
    }

    private static void placeHangingMemoryDisc(WorldGenLevel level, BlockPos logPos, RandomSource random) {
        Direction start = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        for (int offset = 0; offset < 4; offset++) {
            Direction facing = Direction.from2DDataValue((start.get2DDataValue() + offset) & 3);
            BlockPos framePos = logPos.relative(facing);
            if (!level.getBlockState(framePos).isAir()) continue;
            if (!level.getBlockState(logPos).isFaceSturdy(level, logPos, facing)) continue;

            ItemFrame frame = new ItemFrame(level.getLevel(), framePos, facing);
            frame.setInvisible(true);
            frame.setFixed(true);
            frame.setItem(new ItemStack(FieldOfMemories.MEMORY_DISC_4.get()), false);
            if (level.addFreshEntity(frame)) return;
        }
    }

    private static double roadCenterX(int z) {
        return 24.0D + 48.0D * Math.sin(z * 0.0018D) + 19.0D * Math.sin(z * 0.0051D);
    }

    private static double roadCenterZ(int x) {
        return 112.0D + 44.0D * Math.sin(x * 0.00115D) + 17.0D * Math.sin(x * 0.0042D);
    }

    private static boolean isBranchTrail(int x, int z) {
        int section = Math.floorDiv(z - 80, 240);
        for (int k = section - 1; k <= section + 1; k++) {
            int startZ = k * 240 + 80;
            double startX = roadCenterX(startZ);
            double dx = x - startX;
            if (Math.abs(dx) > 96.0D) continue;
            double pathZ = startZ + Math.copySign(Math.abs(dx) * 0.58D, dx)
                    + 6.0D * Math.sin((x + k * 37) * 0.025D);
            if (Math.abs(z - pathZ) <= 0.85D) return true;
        }
        return false;
    }

    private static BlockPos surface(WorldGenLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        if (y < level.getMinBuildHeight()) return null;
        return new BlockPos(x, y, z);
    }

    private static boolean isOurBiome(WorldGenLevel level, BlockPos pos) {
        return level.getBiome(pos).is(FIELD_BIOME) || level.getBiome(pos).is(EDGE_BIOME);
    }

    private static boolean isReplaceableGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.FARMLAND);
    }

    private static void replaceGround(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (!isReplaceableGround(level.getBlockState(pos))) return;

        level.setBlock(pos, state, 2);

        // Remove grass, flowers and other replaceable vegetation growing over the road.
        // Check two blocks so the upper half of double-height plants is cleared too.
        for (int dy = 1; dy <= 2; dy++) {
            BlockPos above = pos.above(dy);
            BlockState aboveState = level.getBlockState(above);
            if (!aboveState.isAir() && aboveState.getFluidState().isEmpty()
                    && aboveState.canBeReplaced()) {
                level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private static void placeAbandonedPlot(WorldGenLevel level, RandomSource random, int minX, int minZ) {
        int centerX = minX + 4 + random.nextInt(8);
        int centerZ = minZ + 4 + random.nextInt(8);
        boolean rotate = random.nextBoolean();
        for (int row = -3; row <= 3; row++) {
            for (int column = -2; column <= 2; column++) {
                int x = centerX + (rotate ? row : column);
                int z = centerZ + (rotate ? column : row);
                BlockPos ground = surface(level, x, z);
                if (ground == null || !isOurBiome(level, ground)) continue;
                if (!isReplaceableGround(level.getBlockState(ground))) continue;
                if (Math.abs(row) == 3 && random.nextFloat() < 0.45F) continue;

                if (Math.floorMod(row, 2) == 0) {
                    level.setBlock(ground, Blocks.FARMLAND.defaultBlockState(), 2);
                    if (random.nextFloat() < 0.38F && level.getBlockState(ground.above()).isAir()) {
                        BlockState crop = Blocks.WHEAT.defaultBlockState()
                                .setValue(CropBlock.AGE, random.nextInt(4));
                        level.setBlock(ground.above(), crop, 2);
                    }
                } else if (random.nextFloat() < 0.65F) {
                    level.setBlock(ground, Blocks.COARSE_DIRT.defaultBlockState(), 2);
                }
            }
        }
    }

    private static void placeBlankSign(WorldGenLevel level, RandomSource random, int minX, int minZ) {
        int x = minX + 2 + random.nextInt(12);
        int z = minZ + 2 + random.nextInt(12);
        BlockPos ground = surface(level, x, z);
        if (ground == null || !isOurBiome(level, ground)) return;
        if (!isReplaceableGround(level.getBlockState(ground))) return;
        BlockPos signPos = ground.above();
        if (!level.getBlockState(signPos).isAir()) return;
        level.setBlock(signPos, Blocks.OAK_SIGN.defaultBlockState(), 2);
    }

    private static void placeHayStack(WorldGenLevel level, RandomSource random, int minX, int minZ) {
        int x = minX + 2 + random.nextInt(12);
        int z = minZ + 2 + random.nextInt(12);
        BlockPos ground = surface(level, x, z);
        if (ground == null || !isOurBiome(level, ground)) return;
        if (!isReplaceableGround(level.getBlockState(ground))) return;

        int width = random.nextInt(2) + 1;
        int depth = random.nextInt(2) + 1;
        int height = random.nextInt(3) + 1;
        for (int layer = 0; layer < height; layer++) {
            int layerWidth = layer == height - 1 ? 1 : width;
            int layerDepth = layer == height - 1 ? 1 : depth;
            for (int dx = 0; dx < layerWidth; dx++) {
                for (int dz = 0; dz < layerDepth; dz++) {
                    BlockPos bale = ground.above(layer + 1).offset(dx, 0, dz);
                    if (level.getBlockState(bale).isAir()) {
                        level.setBlock(bale, Blocks.HAY_BLOCK.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static void placeLoosePumpkins(WorldGenLevel level, RandomSource random, int minX, int minZ) {
        int x = minX + 2 + random.nextInt(12);
        int z = minZ + 2 + random.nextInt(12);
        int amount = 1 + random.nextInt(3);
        for (int i = 0; i < amount; i++) {
            int px = x + random.nextInt(5) - 2;
            int pz = z + random.nextInt(5) - 2;
            BlockPos ground = surface(level, px, pz);
            if (ground == null || !isOurBiome(level, ground)) continue;
            if (!isReplaceableGround(level.getBlockState(ground))) continue;
            if (level.getBlockState(ground.above()).isAir()) {
                level.setBlock(ground.above(), Blocks.PUMPKIN.defaultBlockState(), 2);
            }
        }
    }
}
