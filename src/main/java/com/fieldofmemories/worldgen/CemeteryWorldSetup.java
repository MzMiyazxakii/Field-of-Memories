package com.fieldofmemories.worldgen;

import com.fieldofmemories.FieldOfMemories;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
        // Put the world spawn just inside the cemetery entrance, facing the scarecrow.
        level.setDefaultSpawnPos(new BlockPos(x, y + 1, z + 6), 180.0F);
    }

    private static BlockPos findScarecrow(ServerLevel level, BlockPos spawn) {
        // The default spawn is intentionally offset from the cemetery center.
        // Find the existing landmark on later server starts so the cemetery is never stacked.
        for (int dx = -10; dx <= 10; dx++) for (int dz = -10; dz <= 10; dz++) {
            for (int dy = -3; dy <= 3; dy++) {
                BlockPos candidate = spawn.offset(dx, dy, dz);
                if (level.getBlockState(candidate).is(FieldOfMemories.SCARECROW.get())) return candidate;
            }
        }
        return null;
    }

    private static void flatten(ServerLevel level, int x, int y, int z) {
        for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) {
            int wx = x + dx, wz = z + dz;
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz) - 1;
            for (int yy = Math.min(surface, y + 7); yy > y; yy--) {
                BlockPos p = new BlockPos(wx, yy, wz);
                if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
            if (surface < y) for (int yy = surface + 1; yy <= y; yy++)
                level.setBlock(new BlockPos(wx, yy, wz), Blocks.DIRT.defaultBlockState(), 3);
            BlockPos floor = new BlockPos(wx, y, wz);
            if (!level.getBlockState(floor).is(Blocks.BEDROCK))
                level.setBlock(floor, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            for (int yy = y + 1; yy <= y + 6; yy++) {
                BlockPos above = new BlockPos(wx, yy, wz);
                if (!level.getBlockState(above).isAir()) level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void build(ServerLevel level, int x, int y, int z) {
        // Enclosing fence and a single gate.
        for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) {
            if (Math.abs(dx) != 8 && Math.abs(dz) != 8) continue;
            if (dx == 0 && dz == 8) continue;
            level.setBlock(new BlockPos(x + dx, y + 1, z + dz), Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
        }
        level.setBlock(new BlockPos(x, y + 1, z + 8), Blocks.SPRUCE_FENCE_GATE.defaultBlockState(), 3);

        // Worn path and the central pedestal.
        for (int dz = 2; dz <= 7; dz++) level.setBlock(new BlockPos(x, y, z + dz), Blocks.DIRT_PATH.defaultBlockState(), 3);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            level.setBlock(new BlockPos(x + dx, y, z + dz), Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 3);
        level.setBlock(new BlockPos(x, y + 1, z), FieldOfMemories.SCARECROW.get().defaultBlockState(), 3);

        // Eight weathered grave markers.
        int[][] graves = {{-5,-5},{-3,-5},{3,-5},{5,-5},{-5,1},{-3,1},{3,1},{5,1}};
        for (int[] g : graves) {
            int gx = x + g[0], gz = z + g[1];
            level.setBlock(new BlockPos(gx, y, gz), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3);
            level.setBlock(new BlockPos(gx, y + 1, gz), Blocks.COBBLESTONE_WALL.defaultBlockState(), 3);
            level.setBlock(new BlockPos(gx, y, gz + 1), Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState(), 3);
        }
        for (int[] c : new int[][]{{-8,-8},{-8,8},{8,-8},{8,8}}) {
            level.setBlock(new BlockPos(x + c[0], y + 2, z + c[1]), Blocks.LANTERN.defaultBlockState(), 3);
        }
    }
}