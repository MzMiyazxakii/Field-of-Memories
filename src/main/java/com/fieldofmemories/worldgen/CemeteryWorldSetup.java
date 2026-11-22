package com.fieldofmemories.worldgen;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FieldOfMemories.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CemeteryWorldSetup {
    private CemeteryWorldSetup() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        BlockPos oldSpawn = level.getSharedSpawnPos();
        level.getChunkAt(oldSpawn);
        BlockPos existingScarecrow = findScarecrow(level, oldSpawn);
        int x = existingScarecrow == null ? oldSpawn.getX() : existingScarecrow.getX();
        int z = existingScarecrow == null ? oldSpawn.getZ() : existingScarecrow.getZ();
        int y = existingScarecrow == null
                ? level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1
                : existingScarecrow.getY() - 1;
        if (y <= level.getMinBuildHeight()) y = level.getSeaLevel();

        if (existingScarecrow == null) {
            flatten(level, x, y, z);
            build(level, x, y, z);
        }
        // Keep the starting position inside the gate, looking toward the scarecrow.
        level.setDefaultSpawnPos(new BlockPos(x, y + 1, z + 12), 180.0F);
    }

    private static BlockPos findScarecrow(ServerLevel level, BlockPos spawn) {
        for (int dx = -10; dx <= 10; dx++) for (int dz = -10; dz <= 10; dz++) {
            for (int dy = -3; dy <= 3; dy++) {
                BlockPos candidate = spawn.offset(dx, dy, dz);
                if (level.getBlockState(candidate).is(FieldOfMemories.SCARECROW.get())) return candidate;
            }
        }
        return null;
    }

    private static void flatten(ServerLevel level, int x, int y, int z) {
        for (int dx = -16; dx <= 16; dx++) for (int dz = -16; dz <= 16; dz++) {
            int wx = x + dx, wz = z + dz;
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz) - 1;
            for (int yy = Math.min(surface, y + 8); yy > y; yy--) {
                BlockPos p = new BlockPos(wx, yy, wz);
                if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
            if (surface < y) for (int yy = surface + 1; yy <= y; yy++)
                level.setBlock(new BlockPos(wx, yy, wz), Blocks.DIRT.defaultBlockState(), 3);
            BlockPos floor = new BlockPos(wx, y, wz);
            if (!level.getBlockState(floor).is(Blocks.BEDROCK))
                level.setBlock(floor, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            for (int yy = y + 1; yy <= y + 8; yy++) {
                BlockPos above = new BlockPos(wx, yy, wz);
                if (!level.getBlockState(above).isAir()) level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void put(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 3);
    }

    private static void build(ServerLevel level, int x, int y, int z) {
        // A high, enclosed perimeter replaces the old low fence.
        for (int dx = -16; dx <= 16; dx++) for (int dz = -16; dz <= 16; dz++) {
            if (Math.abs(dx) != 16 && Math.abs(dz) != 16) continue;
            if (dx == 0 && dz == 16) continue;
            put(level, x + dx, y + 1, z + dz, Blocks.MOSSY_COBBLESTONE_WALL);
            put(level, x + dx, y + 2, z + dz, Blocks.SPRUCE_FENCE);
        }
        put(level, x, y + 1, z + 16, Blocks.SPRUCE_FENCE_GATE);
        put(level, x, y + 2, z + 16, Blocks.AIR);

        // Tall corner pylons and cold soul lanterns make the entrance visible from afar.
        for (int[] corner : new int[][]{{-15,-15},{-15,15},{15,-15},{15,15}}) {
            int px = x + corner[0], pz = z + corner[1];
            for (int dy = 1; dy <= 6; dy++) {
                put(level, px, y + dy, pz, Blocks.DEEPSLATE_BRICK_WALL);
            }
            put(level, px, y + 7, pz, Blocks.SOUL_LANTERN);
            put(level, px + 1, y + 3, pz, Blocks.COBWEB);
        }
        for (int[] side : new int[][]{{0,-16},{-16,0},{16,0}}) {
            int px = x + side[0], pz = z + side[1];
            for (int dy = 1; dy <= 4; dy++) put(level, px, y + dy, pz, Blocks.SPRUCE_LOG);
            put(level, px, y + 5, pz, Blocks.SOUL_LANTERN);
        }

        // Broken path, raised central plinth and the scarecrow.
        for (int dz = 3; dz <= 15; dz++) {
            put(level, x, y, z + dz, dz % 3 == 0 ? Blocks.COARSE_DIRT : Blocks.DIRT_PATH);
        }
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            put(level, x + dx, y, z + dz, Blocks.MOSSY_STONE_BRICKS);
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (Math.abs(dx) == 1 || Math.abs(dz) == 1)
                put(level, x + dx, y + 1, z + dz, Blocks.DEEPSLATE_BRICKS);
        }
        put(level, x, y + 1, z, FieldOfMemories.SCARECROW.get());
        put(level, x, y + 2, z, Blocks.CHAIN);
        put(level, x, y + 3, z, Blocks.SOUL_LANTERN);

        // Uneven rows of old graves, with cross-shaped markers and occasional cobwebs.
        int[][] graves = {
                {-11,-10},{-8,-10},{-5,-10},{-2,-10},{2,-10},{5,-10},{8,-10},{11,-10},
                {-11,-5},{-8,-5},{-5,-5},{5,-5},{8,-5},{11,-5},
                {-11,1},{-8,1},{-5,1},{5,1},{8,1},{11,1},
                {-10,7},{-7,7},{-4,7},{4,7},{7,7},{10,7}
        };
        for (int i = 0; i < graves.length; i++) {
            int gx = x + graves[i][0], gz = z + graves[i][1];
            put(level, gx, y, gz, Blocks.MOSSY_COBBLESTONE);
            put(level, gx, y + 1, gz, i % 3 == 0 ? Blocks.POLISHED_DEEPSLATE_WALL : Blocks.COBBLESTONE_WALL);
            if (i % 2 == 0) put(level, gx, y + 2, gz, Blocks.SPRUCE_FENCE);
            put(level, gx, y, gz + 1, Blocks.MOSSY_STONE_BRICK_SLAB);
            if (i % 4 == 0) put(level, gx + 1, y + 1, gz, Blocks.COBWEB);
        }

        // A few dead-looking wooden silhouettes around the inner yard.
        for (int[] tree : new int[][]{{-13,-2},{13,-2},{-13,9},{13,9}}) {
            int tx = x + tree[0], tz = z + tree[1];
            for (int dy = 1; dy <= 5; dy++) put(level, tx, y + dy, tz, Blocks.STRIPPED_SPRUCE_LOG);
            put(level, tx - 2, y + 4, tz, Blocks.SPRUCE_FENCE);
            put(level, tx + 2, y + 3, tz, Blocks.SPRUCE_FENCE);
            put(level, tx, y + 5, tz - 2, Blocks.SPRUCE_FENCE);
            put(level, tx, y + 4, tz + 2, Blocks.SPRUCE_FENCE);
            put(level, tx + 1, y + 2, tz + 1, Blocks.COBWEB);
        }
    }
}
