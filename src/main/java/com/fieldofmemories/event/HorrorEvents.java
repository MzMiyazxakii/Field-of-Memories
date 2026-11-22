package com.fieldofmemories.event;

import com.fieldofmemories.data.HorrorWorldData;
import com.fieldofmemories.entity.ScarecrowEntity;
import com.fieldofmemories.entity.SmileFaceEntity;
import com.fieldofmemories.entity.PlayerEchoEntity;
import com.fieldofmemories.entity.SpiritEntity;
import com.fieldofmemories.entity.RiftEntity;
import com.fieldofmemories.block.AnomalousStoolBlock;
import com.fieldofmemories.network.HorrorNetwork;
import com.fieldofmemories.worldgen.HomeMemoryTracker;
import com.fieldofmemories.FieldOfMemories;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "fieldofmemories", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HorrorEvents {
    private HorrorEvents() {}

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || !level.dimension().equals(Level.OVERWORLD)) return;

        long now = level.getGameTime();
        if (now % 100L != 0L) return;

        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.serverLevel() == level && !player.isCreative() && !player.isSpectator()) {
                players.add(player);
            }
        }
        if (players.isEmpty()) return;

        int danger = HorrorWorldData.getDangerLevel(level);
        int activity = HorrorWorldData.getActivityMultiplier(level);
        RandomSource random = level.random;
        FakeItemEvent.trySpawn(level, players, random, danger, activity);

        if (now < HorrorWorldData.getNextAmbientEvent(level)) return;

        manageScarecrow(level, players, random, danger, activity);
        maybeSpawnSpirit(level, players, random, danger, activity);
        maybeSpawnFace(level, players, random, danger, activity);
        maybeSpawnPlayerEcho(level, players, random, danger, activity);
        maybeSpawnRift(level, players, random, danger, activity);
        maybePlayDistantDeath(level, players, random);
        maybeTriggerHomeEvent(level, players, random);
        maybeSpawnWindowStalker(level, players, random, danger, activity);
        manageAnomalousStool(level, players, random, activity);
        HorrorSignEvent.tryTrigger(level, players, random, danger, activity);

        double dangerFactor = 1.0D - danger * 0.006D;
        // A world event window every 2-3 minutes: enough downtime between scares,
        // without making ordinary play wait tens of real-world minutes for a roll.
        long interval = 2400L + random.nextInt(1201);
        HorrorWorldData.setNextAmbientEvent(level, now + interval);
    }

    private static <T extends Entity> List<T> getLoadedEntities(ServerLevel level, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (type.isInstance(entity) && entity.isAlive()) found.add(type.cast(entity));
        }
        return found;
    }

    private static void manageScarecrow(ServerLevel level, List<ServerPlayer> players,
                                        RandomSource random, int danger, int activity) {
        List<ScarecrowEntity> all = getLoadedEntities(level, ScarecrowEntity.class);
        ScarecrowEntity existing = all.isEmpty() ? null : all.get(0);
        for (int i = 1; i < all.size(); i++) all.get(i).discard();

        int configuredChance = HorrorWorldData.getScarecrowChance(level);
        int chance = configuredChance == 0 ? 0
                : Math.min(70, 18 + scaleChance(configuredChance, danger, activity, 65));
        // Let a spawned scarecrow remain as a distant watcher for up to 90 seconds.
        if (existing != null) return;
        if (random.nextInt(100) < chance) {
            spawnScarecrow(level, players.get(random.nextInt(players.size())), random);
        }
    }

    private static void maybeSpawnSpirit(ServerLevel level, List<ServerPlayer> players,
                                         RandomSource random, int danger, int activity) {
        List<SpiritEntity> existing = getLoadedEntities(level, SpiritEntity.class);
        if (!existing.isEmpty()) {
            for (int i = 1; i < existing.size(); i++) existing.get(i).discard();
            return;
        }

        int configuredChance = HorrorWorldData.getSpiritChance(level);
        int chance = configuredChance == 0 ? 0
                : Math.min(10, (configuredChance + danger / 20) * activity / 100);
        if (random.nextInt(100) >= chance) return;

        ServerPlayer target = players.get(random.nextInt(players.size()));
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = 14.0D + random.nextDouble() * 10.0D;
        int x = (int) Math.floor(target.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(target.getZ() + Math.sin(angle) * distance);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

        SpiritEntity spirit = FieldOfMemories.SPIRIT_ENTITY.get().create(level);
        if (spirit == null) return;
        spirit.moveTo(x + 0.5D, y, z + 0.5D, random.nextFloat() * 360.0F, 0.0F);
        if (level.noCollision(spirit)) level.addFreshEntity(spirit);
    }

    private static void maybeSpawnRift(ServerLevel level, List<ServerPlayer> players,
                                       RandomSource random, int danger, int activity) {
        if (!getLoadedEntities(level, RiftEntity.class).isEmpty()) return;
        int chance = Math.min(12, (2 + danger / 18) * activity / 100);
        if (random.nextInt(100) >= chance) return;
        ServerPlayer target = players.get(random.nextInt(players.size()));
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = 18.0D + random.nextDouble() * 30.0D;
        int x = (int) Math.floor(target.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(target.getZ() + Math.sin(angle) * distance);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        RiftEntity rift = FieldOfMemories.RIFT_ENTITY.get().create(level);
        if (rift == null) return;
        boolean pink = random.nextInt(4) == 0;
        rift.setPinkRift(pink);
        rift.setBlackRift(!pink && random.nextBoolean());
        rift.moveTo(x + 0.5D, y + 0.1D, z + 0.5D, 0.0F, 0.0F);
        rift.setLifetime(pink ? 240 + random.nextInt(361) : 240 + random.nextInt(361));
        if (level.noCollision(rift)) level.addFreshEntity(rift);
    }

    private static void manageAnomalousStool(ServerLevel level, List<ServerPlayer> players,
                                             RandomSource random, int activity) {
        List<BlockPos> stools = new ArrayList<>();
        for (ServerPlayer player : players) {
            BlockPos center = player.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-40, -12, -40),
                    center.offset(40, 12, 40))) {
                if (level.getBlockState(pos).is(FieldOfMemories.ANOMALOUS_STOOL_BLOCK.get())) {
                    stools.add(pos.immutable());
                }
            }
        }
        if (!stools.isEmpty()) {
            if (random.nextInt(100) < 8) {
                BlockPos vanish = stools.get(random.nextInt(stools.size()));
                level.destroyBlock(vanish, false);
            }
            return;
        }
        int chance = Math.min(18, Math.max(1, activity / 12));
        if (random.nextInt(100) >= chance) return;
        ServerPlayer target = players.get(random.nextInt(players.size()));
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 16.0D + random.nextDouble() * 24.0D;
            int x = (int) Math.floor(target.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(target.getZ() + Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos).isAir()
                    && level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
                level.setBlock(pos, FieldOfMemories.ANOMALOUS_STOOL_BLOCK.get().defaultBlockState(), 3);
                return;
            }
        }
    }

    private static void maybeSpawnFace(ServerLevel level, List<ServerPlayer> players,
                                       RandomSource random, int danger, int activity) {
        if (level.isNight()) {
            if (activity < 150) return;
        } else if (activity < 200) {
            return;
        }
        int configuredChance = HorrorWorldData.getFaceChance(level);
        int chance = configuredChance == 0 ? 0
                : Math.min(90, (configuredChance + 5 + danger / 4) * (activity - 100) / 100);
        if (random.nextInt(100) >= chance) return;

        ServerPlayer target = players.get(random.nextInt(players.size()));
        if (!level.getEntitiesOfClass(SmileFaceEntity.class,
                target.getBoundingBox().inflate(56.0D, 32.0D, 56.0D)).isEmpty()) return;

        BlockPos anchor = HomeMemoryTracker.getHorrorAnchor(target);
        double centerX = anchor == null ? target.getX() : anchor.getX() + 0.5D;
        double centerZ = anchor == null ? target.getZ() : anchor.getZ() + 0.5D;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = 20.0D + random.nextDouble() * 12.0D;
        int x = (int) Math.floor(centerX + Math.cos(angle) * distance);
        int z = (int) Math.floor(centerZ + Math.sin(angle) * distance);
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        SmileFaceEntity face = FieldOfMemories.SMILE_FACE_ENTITY.get().create(level);
        if (face != null) {
            face.moveTo(x + 0.5D, ground + 3.0D + random.nextDouble() * 2.0D,
                    z + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(face);
        }
    }

    private static void maybeSpawnPlayerEcho(ServerLevel level, List<ServerPlayer> players,
                                             RandomSource random, int danger, int activity) {
        if (players.size() < 2) return;
        int configuredChance = HorrorWorldData.getMimicChance(level);
        int chance = configuredChance == 0 ? 0
                : Math.min(40, (configuredChance + danger / 10) * activity / 100);
        if (random.nextInt(100) >= chance) return;

        ServerPlayer target = players.get(random.nextInt(players.size()));
        if (!level.getEntitiesOfClass(PlayerEchoEntity.class,
                target.getBoundingBox().inflate(64.0D, 24.0D, 64.0D)).isEmpty()) return;

        List<ServerPlayer> possibleSkins = new ArrayList<>(players);
        possibleSkins.remove(target);
        if (possibleSkins.isEmpty()) return;
        ServerPlayer skinOwner = possibleSkins.get(random.nextInt(possibleSkins.size()));

        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = 18.0D + random.nextDouble() * 10.0D;
        int x = (int) Math.floor(target.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(target.getZ() + Math.sin(angle) * distance);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

        PlayerEchoEntity echo = FieldOfMemories.PLAYER_ECHO_ENTITY.get().create(level);
        if (echo == null) return;
        echo.setSkinOwner(skinOwner.getUUID());
        echo.moveTo(x + 0.5D, y, z + 0.5D, random.nextFloat() * 360.0F, 0.0F);
        if (level.noCollision(echo)) level.addFreshEntity(echo);
    }

    private static void spawnScarecrow(ServerLevel level, ServerPlayer target, RandomSource random) {
        BlockPos anchor = HomeMemoryTracker.getHorrorAnchor(target);
        double centerX = anchor == null ? target.getX() : anchor.getX() + 0.5D;
        double centerZ = anchor == null ? target.getZ() : anchor.getZ() + 0.5D;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = 24.0D + random.nextDouble() * 18.0D;
        int x = (int) Math.floor(centerX + Math.cos(angle) * distance);
        int z = (int) Math.floor(centerZ + Math.sin(angle) * distance);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        ScarecrowEntity scarecrow = FieldOfMemories.SCARECROW_ENTITY.get().create(level);
        if (scarecrow != null) {
            scarecrow.moveTo(x + 0.5D, y, z + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(scarecrow);
        }
    }


    private static void maybePlayDistantDeath(ServerLevel level, List<ServerPlayer> players, RandomSource random) {
        if (random.nextInt(4) != 0) return;
        ServerPlayer target = players.get(random.nextInt(players.size()));
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = 10.0D + random.nextDouble() * 18.0D;
        BlockPos soundPos = BlockPos.containing(target.getX() + Math.cos(angle) * distance,
                target.getY(), target.getZ() + Math.sin(angle) * distance);
        var sound = random.nextBoolean() ? SoundEvents.COW_DEATH : SoundEvents.VILLAGER_DEATH;
        level.playSound(null, soundPos, sound, SoundSource.AMBIENT, 1.2F, 0.85F + random.nextFloat() * 0.3F);
    }

    private static void maybeTriggerHomeEvent(ServerLevel level, List<ServerPlayer> players, RandomSource random) {
        List<ServerPlayer> atHome = new ArrayList<>();
        for (ServerPlayer player : players) {
            if (HomeMemoryTracker.getHorrorAnchor(player) != null) atHome.add(player);
        }
        if (atHome.isEmpty()) return;
        ServerPlayer target = atHome.get(random.nextInt(atHome.size()));
        BlockPos home = HomeMemoryTracker.getHorrorAnchor(target);
        if (home == null) return;

        if (random.nextInt(100) < 18) {
            BlockPos doorPos = BlockPos.findClosestMatch(home, 14, 6, pos -> {
                BlockState state = level.getBlockState(pos);
                return state.getBlock() instanceof DoorBlock && !state.getValue(DoorBlock.OPEN);
            }).orElse(null);
            if (doorPos != null) {
                BlockState state = level.getBlockState(doorPos);
                if (random.nextInt(100) < 72) {
                    level.playSound(null, doorPos, random.nextBoolean()
                                    ? FieldOfMemories.KNOCK_1.get() : FieldOfMemories.KNOCK_2.get(),
                            SoundSource.BLOCKS, 2.2F, 0.9F + random.nextFloat() * 0.2F);
                } else if (state.getBlock() instanceof DoorBlock door) {
                    door.setOpen(null, level, state, doorPos, true);
                    level.playSound(null, doorPos, SoundEvents.WOODEN_DOOR_OPEN,
                            SoundSource.BLOCKS, 2.0F, 0.8F + random.nextFloat() * 0.25F);
                }
            }
        }
        if (random.nextInt(100) < 9) moveHouseWorkstation(level, home, random);
    }

    private static void moveHouseWorkstation(ServerLevel level, BlockPos home, RandomSource random) {
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(home.offset(-12, -5, -12), home.offset(12, 5, 12))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.FURNACE) || state.is(Blocks.SMOKER) || state.is(Blocks.BLAST_FURNACE)
                    || state.is(Blocks.CRAFTING_TABLE) || state.is(Blocks.CARTOGRAPHY_TABLE)
                    || state.is(Blocks.FLETCHING_TABLE) || state.is(Blocks.LOOM)
                    || state.is(Blocks.SMITHING_TABLE) || state.is(Blocks.GRINDSTONE)
                    || state.is(Blocks.STONECUTTER) || state.is(Blocks.LECTERN)
                    || state.is(Blocks.BREWING_STAND)) {
                candidates.add(pos.immutable());
            }
        }
        if (candidates.isEmpty()) return;
        BlockPos source = candidates.get(random.nextInt(candidates.size()));
        BlockState state = level.getBlockState(source);
        BlockEntity sourceEntity = level.getBlockEntity(source);
        CompoundTag saved = sourceEntity == null ? null : sourceEntity.saveWithFullMetadata();

        for (int attempt = 0; attempt < 24; attempt++) {
            BlockPos destination = source.offset(random.nextInt(9) - 4,
                    random.nextInt(3) - 1, random.nextInt(9) - 4);
            if (destination.equals(source) || !level.isLoaded(destination)
                    || !level.getBlockState(destination).isAir()
                    || !level.getBlockState(destination.below()).isSolidRender(level, destination.below())) continue;
            if (!level.getBlockState(destination.above()).isAir()) continue;

            if (!level.setBlock(destination, state, 3)) continue;
            if (saved != null) {
                BlockEntity moved = level.getBlockEntity(destination);
                if (moved == null) {
                    level.setBlock(destination, Blocks.AIR.defaultBlockState(), 3);
                    return;
                }
                saved.putInt("x", destination.getX());
                saved.putInt("y", destination.getY());
                saved.putInt("z", destination.getZ());
                moved.load(saved);
                moved.setChanged();
            }
            level.setBlock(source, Blocks.AIR.defaultBlockState(), 3);
            return;
        }
    }

    private static void maybeSpawnWindowStalker(ServerLevel level, List<ServerPlayer> players,
                                                 RandomSource random, int danger, int activity) {
        if (!getLoadedEntities(level, com.fieldofmemories.entity.WindowStalkerEntity.class).isEmpty()) return;
        int chance = Math.min(16, (3 + danger / 16) * activity / 100);
        if (random.nextInt(100) >= chance) return;
        List<ServerPlayer> atHome = new ArrayList<>();
        for (ServerPlayer player : players) {
            if (HomeMemoryTracker.getHorrorAnchor(player) != null) atHome.add(player);
        }
        if (atHome.isEmpty()) return;
        ServerPlayer target = atHome.get(random.nextInt(atHome.size()));
        BlockPos home = HomeMemoryTracker.getHorrorAnchor(target);
        if (home == null) return;

        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 10.0D + random.nextDouble() * 7.0D;
            int x = (int) Math.floor(home.getX() + 0.5D + Math.cos(angle) * distance);
            int z = (int) Math.floor(home.getZ() + 0.5D + Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos spawn = new BlockPos(x, y, z);
            boolean tooClose = false;
            for (ServerPlayer player : players) {
                if (player.distanceToSqr(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D) < 9.0D * 9.0D) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose || !level.getBlockState(spawn).isAir()
                    || !level.getBlockState(spawn.below()).isSolidRender(level, spawn.below())) continue;
            com.fieldofmemories.entity.WindowStalkerEntity stalker =
                    FieldOfMemories.WINDOW_STALKER_ENTITY.get().create(level);
            if (stalker == null) return;
            stalker.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
                    random.nextFloat() * 360.0F, 0.0F);
            if (level.noCollision(stalker)) {
                level.addFreshEntity(stalker);
                return;
            }
        }
    }

    private static int scaleChance(int base, int danger, int activity, int startingDanger) {
        int dangerFactor = startingDanger + (100 - startingDanger) * danger / 100;
        return Math.max(0, Math.min(100, base * dangerFactor * activity / 10000));
    }

    private static boolean isLookingAt(ServerPlayer player, ScarecrowEntity entity, double maxDistance) {
        if (player.distanceToSqr(entity) > maxDistance * maxDistance || !player.hasLineOfSight(entity)) return false;
        Vec3 direction = entity.position().subtract(player.getEyePosition()).normalize();
        return direction.dot(player.getLookAngle()) > 0.985D;
    }
}