package com.fieldofmemories.worldgen;

import com.fieldofmemories.FieldOfMemories;
import com.fieldofmemories.data.HorrorWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class AbandonedSettlementFeature extends Feature<NoneFeatureConfiguration> {
    private static final ResourceKey<Biome> FIELD = ResourceKey.create(Registries.BIOME, new ResourceLocation(FieldOfMemories.MOD_ID, "gloomy_field"));
    private static final ResourceKey<Biome> EDGE = ResourceKey.create(Registries.BIOME, new ResourceLocation(FieldOfMemories.MOD_ID, "gloomy_edge"));
    private static final ResourceKey<Biome> FOREST = ResourceKey.create(Registries.BIOME, new ResourceLocation(FieldOfMemories.MOD_ID, "gloomy_forest"));

    public AbandonedSettlementFeature(com.mojang.serialization.Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
        WorldGenLevel l=c.level(); RandomSource r=c.random(); ChunkPos ch=new ChunkPos(c.origin());
        int x=ch.getMinBlockX()+8,z=ch.getMinBlockZ()+8; BlockPos g=surface(l,x,z); if(g==null)return false;
        boolean forest=l.getBiome(g).is(FOREST), edge=l.getBiome(g).is(EDGE), valid=forest||l.getBiome(g).is(FIELD)||edge;
        if (valid && r.nextFloat() < 0.08F && placeLootContainer(l, x, z, com.fieldofmemories.FieldOfMemories.CARDBOARD_BOX_BLOCK.get())) return true;
        if(valid&&r.nextFloat()<(edge?0.0012F:0.0025F)){ if(!forest&&r.nextFloat()<0.30F) village(l,x,z,r); else house(l,x,z,r.nextInt(3),r); return true; }
        if(valid&&r.nextFloat()<(edge?0.0025F:0.006F)){ if(forest||r.nextBoolean())stable(l,x,z); else well(l,x,z); return true; }
        if(valid&&r.nextFloat()<(edge?0.003F:0.008F)){cart(l,x,z,r);return true;}
        if(forest&&r.nextFloat()<0.018F)lamp(l,x,z);
        else if(r.nextFloat()<(edge?0.0015F:0.006F))pillar(l,x,z,r);
        else if(!forest&&r.nextFloat()<(edge?0.004F:0.012F))garden(l,x,z,r);
        else if(!forest&&r.nextFloat()<(edge?0.003F:0.008F))market(l,x,z,r);
        else if(!forest&&r.nextFloat()<(edge?0.003F:0.008F))hayGolem(l,x,z);
        else if(r.nextFloat()<(edge?0.002F:0.006F))well(l,x,z);
        return true;
    }

    private static boolean placeLootContainer(WorldGenLevel l,int x,int z,Block b){BlockPos p=surface(l,x,z);if(p==null||!l.getBlockState(p.above()).isAir())return false;l.setBlock(p.above(),b.defaultBlockState(),3);return true;}
    private static BlockPos surface(WorldGenLevel l,int x,int z){
        int y=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1;if(y<l.getMinBuildHeight())return null;
        BlockPos p=new BlockPos(x,y,z);Block b=l.getBlockState(p).getBlock();
        return b==Blocks.GRASS_BLOCK||b==Blocks.DIRT||b==Blocks.COARSE_DIRT||b==Blocks.ROOTED_DIRT||b==Blocks.PODZOL||b==Blocks.MYCELIUM?p:null;
    }
    private static boolean clear(WorldGenLevel l,int x,int z,int rad,int h){
        BlockPos base=surface(l,x,z);if(base==null)return false;int y=base.getY();
        for(int dx=-rad;dx<=rad;dx++)for(int dz=-rad;dz<=rad;dz++){
            BlockPos p=surface(l,x+dx,z+dz);if(p==null||Math.abs(p.getY()-y)>1)return false;
            for(int dy=1;dy<=h;dy++){BlockState s=l.getBlockState(new BlockPos(x+dx,y+dy,z+dz));if(!s.isAir()&&!s.canBeReplaced())return false;}
        }return true;
    }
    private static void put(WorldGenLevel l,int x,int y,int z,Block b){l.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),3);}
    private static void put(WorldGenLevel l,int x,int y,int z,BlockState s){l.setBlock(new BlockPos(x,y,z),s,3);}
    private static void fill(WorldGenLevel l,int x,int y,int z,int w,int d,Block b){for(int a=0;a<w;a++)for(int q=0;q<d;q++)put(l,x+a,y,z+q,b);}

    private static void house(WorldGenLevel l,int x,int z,int v,RandomSource random){
        if(!clear(l,x,z,4,6))return;BlockPos p=surface(l,x,z);if(p==null)return;int y=p.getY(),sx=x-2,sz=z-2;
        Block floor=v==1?Blocks.COBBLESTONE:Blocks.SPRUCE_PLANKS,wall=v==2?Blocks.OAK_PLANKS:Blocks.SPRUCE_PLANKS,log=v==1?Blocks.OAK_LOG:Blocks.SPRUCE_LOG,roof=v==2?Blocks.DARK_OAK_PLANKS:Blocks.SPRUCE_PLANKS;
        clearHouseInterior(l, sx, y, sz);
        fill(l,sx,y,sz,5,5,floor);
        for(int dx=0;dx<5;dx++)for(int dz=0;dz<5;dz++){boolean c=(dx==0||dx==4)&&(dz==0||dz==4);for(int dy=1;dy<=3;dy++){
            if(c)put(l,sx+dx,y+dy,sz+dz,log);
            else if((dx==0||dx==4||dz==0||dz==4)&&!(dy==2&&((dx==2&&(dz==0||dz==4))||(dz==2&&(dx==0||dx==4)))))put(l,sx+dx,y+dy,sz+dz,wall);
        }}
        put(l,sx+2,y+1,sz,Blocks.AIR);put(l,sx+2,y+2,sz,Blocks.AIR);put(l,sx+2,y+2,sz+4,Blocks.GLASS_PANE);put(l,sx,y+2,sz+2,Blocks.GLASS_PANE);
        fill(l,sx-1,y+4,sz-1,7,7,roof);
        for(int dx=0;dx<5;dx++){put(l,sx+dx,y+5,sz-1,Blocks.SPRUCE_SLAB);put(l,sx+dx,y+5,sz+5,Blocks.SPRUCE_SLAB);}
        placeLootContainer(l, new BlockPos(sx+1,y+1,sz+1), Blocks.BARREL, random);
        placeLootContainer(l, new BlockPos(sx+3,y+1,sz+3), Blocks.CHEST, random);
        if(v==2)put(l,sx+3,y+1,sz+2,Blocks.CRAFTING_TABLE);
        placeCameraFrame(l, sx + 2, y, sz + 2, random);
    }
    private static void village(WorldGenLevel l,int x,int z,RandomSource random){
        if(!clear(l,x,z,7,6))return;BlockPos p=surface(l,x,z);if(p==null)return;int y=p.getY();
        houseAt(l,x-4,y,z-3,0,random);houseAt(l,x+4,y,z-3,1,random);houseAt(l,x,y,z+4,2,random);
        for(int d=-1;d<=1;d++){put(l,x+d,y,z,Blocks.DIRT_PATH);put(l,x,y,z+d,Blocks.DIRT_PATH);}
        wellAt(l,x,y,z);put(l,x-7,y+1,z+1,Blocks.SPRUCE_FENCE);put(l,x+7,y+1,z+1,Blocks.SPRUCE_FENCE);
    }
    private static void houseAt(WorldGenLevel l,int x,int y,int z,int v,RandomSource random){ // village houses use same compact layout
        int sx=x-2,sz=z-2;Block wall=v==1?Blocks.OAK_PLANKS:Blocks.SPRUCE_PLANKS,log=v==1?Blocks.OAK_LOG:Blocks.SPRUCE_LOG;
        clearHouseInterior(l, sx, y, sz);
        fill(l,sx,y,sz,5,5,Blocks.SPRUCE_PLANKS);
        for(int dx=0;dx<5;dx++)for(int dz=0;dz<5;dz++)for(int dy=1;dy<=3;dy++){
            if((dx==0||dx==4)&&(dz==0||dz==4))put(l,sx+dx,y+dy,sz+dz,log);
            else if((dx==0||dx==4||dz==0||dz==4)&&!(dy==2&&dx==2))put(l,sx+dx,y+dy,sz+dz,wall);
        }
        put(l,sx+2,y+1,sz,Blocks.AIR);put(l,sx+2,y+2,sz,Blocks.AIR);put(l,sx+2,y+2,sz+4,Blocks.GLASS_PANE);
        fill(l,sx-1,y+4,sz-1,7,7,Blocks.SPRUCE_PLANKS);
        placeLootContainer(l, new BlockPos(sx+1,y+1,sz+1), Blocks.BARREL, random);
        placeLootContainer(l, new BlockPos(sx+3,y+1,sz+3), Blocks.CHEST, random);
        placeCameraFrame(l, sx + 2, y, sz + 2, random);
    }

    private static void clearHouseInterior(WorldGenLevel level, int startX, int floorY, int startZ) {
        // Clear replaceable plants from the room before placing the floor and walls.
        // This prevents tall grass and ferns from being trapped inside newly generated houses.
        for (int dx = 1; dx <= 3; dx++) {
            for (int dz = 1; dz <= 3; dz++) {
                for (int dy = 1; dy <= 4; dy++) {
                    BlockPos pos = new BlockPos(startX + dx, floorY + dy, startZ + dz);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && state.getFluidState().isEmpty() && state.canBeReplaced()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void placeCameraFrame(WorldGenLevel level, int floorX, int floorY, int floorZ,
                                         RandomSource random) {
        // Not every abandoned house contains a camera.
        float cameraChance = level.getLevel() instanceof ServerLevel serverLevel
                ? HorrorWorldData.getHouseCameraChance(serverLevel) / 100.0F : 0.32F;
        if (random.nextFloat() >= cameraChance) return;
        BlockPos floor = new BlockPos(floorX, floorY, floorZ);
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) return;

        ItemFrame frame = new ItemFrame(level.getLevel(), floor, Direction.UP);
        frame.setInvisible(true);
        ItemStack camera = random.nextBoolean()
                ? new ItemStack(FieldOfMemories.VIDEO_CAMERA.get())
                : new ItemStack(FieldOfMemories.OLD_CAMERA.get());
        frame.setItem(camera, false);
        level.addFreshEntity(frame);
    }

    private static void placeLootContainer(WorldGenLevel level, BlockPos pos, Block block, RandomSource random) {
        level.setBlock(pos, block.defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity container) {
            container.setLootTable(new ResourceLocation(FieldOfMemories.MOD_ID, block == Blocks.CHEST ? "chests/abandoned_house" : "chests/abandoned_house_barrel"), random.nextLong());
        }
    }

    private static void well(WorldGenLevel l,int x,int z){if(!clear(l,x,z,2,5))return;BlockPos p=surface(l,x,z);if(p!=null)wellAt(l,x,p.getY(),z);}
    private static void wellAt(WorldGenLevel l,int x,int y,int z){
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if(Math.abs(dx)==1||Math.abs(dz)==1){put(l,x+dx,y+1,z+dz,Blocks.MOSSY_COBBLESTONE);put(l,x+dx,y+2,z+dz,Blocks.STONE_BRICKS);}else put(l,x,y+1,z,Blocks.WATER);
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(Math.abs(dx)==2||Math.abs(dz)==2)put(l,x+dx,y+4,z+dz,Blocks.SPRUCE_SLAB);
        for(int[] c:new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}})put(l,x+c[0],y+3,z+c[1],Blocks.SPRUCE_FENCE);
    }
    private static void stable(WorldGenLevel l,int x,int z){
        if(!clear(l,x,z,4,5))return;BlockPos p=surface(l,x,z);if(p==null)return;int y=p.getY();fill(l,x-3,y,z-2,7,5,Blocks.COARSE_DIRT);
        for(int dx:new int[]{-3,3})for(int dz:new int[]{-2,2})for(int dy=1;dy<=3;dy++)put(l,x+dx,y+dy,z+dz,Blocks.SPRUCE_LOG);
        for(int dx=-3;dx<=3;dx++){put(l,x+dx,y+4,z-2,Blocks.SPRUCE_PLANKS);put(l,x+dx,y+4,z+2,Blocks.SPRUCE_PLANKS);put(l,x+dx,y+4,z,Blocks.SPRUCE_SLAB);}
        for(int dz=-2;dz<=2;dz++){put(l,x-3,y+4,z+dz,Blocks.SPRUCE_PLANKS);put(l,x+3,y+4,z+dz,Blocks.SPRUCE_PLANKS);}
        for(int dx=-2;dx<=2;dx++){put(l,x+dx,y+1,z-2,Blocks.SPRUCE_FENCE);put(l,x+dx,y+1,z+2,Blocks.SPRUCE_FENCE);}
        put(l,x-2,y+1,z,Blocks.HAY_BLOCK);put(l,x+2,y+1,z,Blocks.HAY_BLOCK);put(l,x,y+1,z+1,Blocks.BARREL);
    }
    private static void cart(WorldGenLevel l,int x,int z,RandomSource r){
        if(!clear(l,x,z,2,3))return;BlockPos p=surface(l,x,z);if(p==null)return;int y=p.getY();
        for(int dx:new int[]{-1,1})for(int dz:new int[]{-1,1})put(l,x+dx,y+1,z+dz,Blocks.SPRUCE_LOG);
        fill(l,x-1,y+2,z-1,3,3,Blocks.SPRUCE_SLAB);
        for(int dx=-1;dx<=1;dx++){put(l,x+dx,y+3,z-1,Blocks.SPRUCE_TRAPDOOR);put(l,x+dx,y+3,z+1,Blocks.SPRUCE_TRAPDOOR);}
        put(l,x,y+3,z,r.nextBoolean()?Blocks.HAY_BLOCK:Blocks.BARREL);put(l,x,y+1,z-2,Blocks.SPRUCE_FENCE);put(l,x,y+1,z-3,Blocks.SPRUCE_FENCE);
    }
    private static void pillar(WorldGenLevel l,int x,int z,RandomSource r){
        if(!clear(l,x,z,1,5))return;BlockPos p=surface(l,x,z);if(p==null)return;int h=3+r.nextInt(3);
        for(int d=1;d<=h;d++)put(l,x,p.getY()+d,z,Blocks.SPRUCE_FENCE);if(r.nextBoolean())put(l,x,p.getY()+h+1,z,Blocks.LANTERN);
    }
    private static void lamp(WorldGenLevel l,int x,int z){
        if(!clear(l,x,z,0,5))return;BlockPos p=surface(l,x,z);if(p==null)return;int y=p.getY();
        for(int d=1;d<=4;d++)put(l,x,y+d,z,Blocks.SPRUCE_FENCE);put(l,x,y+4,z+1,Blocks.SPRUCE_FENCE);put(l,x,y+3,z+1,Blocks.LANTERN);
    }
    private static void garden(WorldGenLevel l,int x,int z,RandomSource r){
        if(!clear(l,x,z,4,2))return;BlockPos p=surface(l,x,z);if(p==null)return;int y=p.getY();
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
            if(Math.abs(dx)==3||Math.abs(dz)==3)put(l,x+dx,y+1,z+dz,Blocks.OAK_FENCE);
            else{put(l,x+dx,y,z+dz,Blocks.FARMLAND);if((dx+dz)%2==0){BlockState crop=(r.nextBoolean()?Blocks.CARROTS:Blocks.POTATOES).defaultBlockState().setValue(CropBlock.AGE,r.nextInt(4));put(l,x+dx,y+1,z+dz,crop);}}
        }put(l,x,y+1,z-3,Blocks.AIR);
    }
    private static void market(WorldGenLevel l,int x,int z,RandomSource r){
        if(!clear(l,x,z,2,4))return;BlockPos p=surface(l,x,z);if(p==null)return;int y=p.getY();
        put(l,x-1,y+1,z,Blocks.BARREL);put(l,x+1,y+1,z,Blocks.BARREL);fill(l,x-1,y+2,z-1,3,3,Blocks.SPRUCE_SLAB);
        for(int dx:new int[]{-1,1})for(int dz:new int[]{-1,1})put(l,x+dx,y+3,z+dz,Blocks.SPRUCE_FENCE);
        fill(l,x-2,y+4,z-2,5,5,r.nextBoolean()?Blocks.WHITE_WOOL:Blocks.GRAY_WOOL);put(l,x,y+3,z,Blocks.CRAFTING_TABLE);
    }
    private static void hayGolem(WorldGenLevel l,int x,int z){
        if(!clear(l,x,z,2,4))return;BlockPos p=surface(l,x,z);if(p==null)return;int y=p.getY();
        put(l,x,y+1,z,Blocks.HAY_BLOCK);put(l,x,y+2,z,Blocks.HAY_BLOCK);put(l,x-1,y+2,z,Blocks.HAY_BLOCK);put(l,x+1,y+2,z,Blocks.HAY_BLOCK);put(l,x,y+3,z,Blocks.CARVED_PUMPKIN);
    }
}