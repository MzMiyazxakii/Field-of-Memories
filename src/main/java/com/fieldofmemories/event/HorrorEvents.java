package com.fieldofmemories.event;

import com.fieldofmemories.data.HorrorWorldData;
import com.fieldofmemories.data.BookProgress;
import com.fieldofmemories.entity.ScarecrowEntity;
import com.fieldofmemories.entity.SmileFaceEntity;
import com.fieldofmemories.entity.PlayerEchoEntity;
import com.fieldofmemories.entity.RunningShadowEntity;
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

        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.serverLevel() == level && !player.isCreative() && !player.isSpectator()) {
                players.add(player);
            }
        }
        if (players.isEmpty()) return;

        // Check visibility every tick so the scarecrow cannot vanish during a glance.
        updateScarecrowPresence(level, players, level.random, now % 100L == 0L);
        if (now % 100L != 0L) return;

        int danger = HorrorWorldData.getDangerLevel(level);
        int activity = HorrorWorldData.getActivityMultiplier(level);
        RandomSource random = level.random;
        FakeItemEvent.trySpawn(level, players, random, danger, activity);

        if (now < HorrorWorldData.getNextAmbientEvent(level)) return;

        manageScarecrow(level, players, random, danger, activity);
        maybeSpawnSpirit(level, players, random, danger, activity);
        maybeSpawnFace(level, players, random, danger, activity);
        maybeSpawnPlayerEcho(level, players, random, danger, activity);
        maybeSpawnRunningShadow(level, players, random);
        maybeSpawnRift(level, players, random, danger, activity);
        maybePlayDistantDeath(level, players, random);
        maybeTriggerHomeEvent(level, players, random);
        maybeSpawnWindowStalker(level, players, random, danger, activity);
        manageAnomalousStool(level, players, random, activity);
        HorrorSignEvent.tryTrigger(level, players, random, danger, activity);
        AmbientHorrorEvents.tryTrigger(level, players, random, danger, activity);

        double dangerFactor = 1.0D - danger * 0.006D;
        // A world event window every 2-3 minutes: enough downtime between scares,
        // without making ordinary play wait tens of real-world minutes for a roll.
        long interval = 2400L + random.nextInt(1201);
        HorrorWorldData.setNextAmbientEvent(level, now + interval);
    }


    public static boolean triggerAdminEvent(ServerLevel level, ServerPlayer target, String event) {
        RandomSource random = level.random;
        if ("scarecrow".equals(event)) {
            if (!getLoadedEntities(level, ScarecrowEntity.class).isEmpty()) return false;
            spawnScarecrow(level, target, random);
            return !getLoadedEntities(level, ScarecrowEntity.class).isEmpty();
        }
        if ("spirit".equals(event)) {
            return spawnAdminEntity(level, target, "spirit");
        }
        if ("face".equals(event)) {
            return spawnAdminEntity(level, target, "face");
        }
        if ("mimic".equals(event)) {
            return spawnAdminEntity(level, target, "mimic");
        }
        if ("rift".equals(event) || "pink_rift".equals(event) || "black_rift".equals(event)) {
            return spawnAdminEntity(level, target, event);
        }
        if ("stool".equals(event)) {
            for (int attempt = 0; attempt < 32; attempt++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double distance = 5.0D + random.nextDouble() * 8.0D;
                int x = (int) Math.floor(target.getX() + Math.cos(angle) * distance);
                int z = (int) Math.floor(target.getZ() + Math.sin(angle) * distance);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos pos = new BlockPos(x, y, z);
                if (level.isLoaded(pos) && level.getBlockState(pos).isAir()
                        && level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
                    return level.setBlock(pos, FieldOfMemories.ANOMALOUS_STOOL_BLOCK.get().defaultBlockState(), 3);
                }
            }
            return false;
        }
        if ("workstation".equals(event)) {
            BlockPos home = HomeMemoryTracker.getHorrorAnchor(target);
            if (home == null) return false;
            return moveHouseWorkstation(level, home, random);
        }
        if ("sign".equals(event)) {
            return HorrorSignEvent.triggerAdmin(level, target, random);
        }
        if ("fake_item".equals(event)) {
            return FakeItemEvent.spawnForAdmin(level, target);
        }
        return false;
    }

    private static boolean spawnAdminEntity(ServerLevel level, ServerPlayer target, String kind) {
        RandomSource random = level.random;
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 5.0D + random.nextDouble() * 8.0D;
            int x = (int) Math.floor(target.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(target.getZ() + Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isLoaded(pos) || !level.getBlockState(pos).isAir()
                    || !level.getBlockState(pos.above()).isAir()
                    || !level.getBlockState(pos.below()).isSolidRender(level, pos.below())) continue;
            Entity entity;
            switch (kind) {
                case "spirit" -> entity = FieldOfMemories.SPIRIT_ENTITY.get().create(level);
                case "face" -> {
                    SmileFaceEntity face = FieldOfMemories.SMILE_FACE_ENTITY.get().create(level);
                    if (face == null) return false;
                    face.moveTo(x + 0.5D, y + 3.0D, z + 0.5D, random.nextFloat() * 360.0F, 0.0F);
                    return level.addFreshEntity(face);
                }
                case "mimic" -> {
                    PlayerEchoEntity echo = FieldOfMemories.PLAYER_ECHO_ENTITY.get().create(level);
                    if (echo == null) return false;
                    echo.setSkinOwner(target.getUUID());
                    echo.moveTo(x + 0.5D, y, z + 0.5D, random.nextFloat() * 360.0F, 0.0F);
                    return level.noCollision(echo) && level.addFreshEntity(echo);
                }
                case "rift", "pink_rift", "black_rift" -> {
                    RiftEntity rift = FieldOfMemories.RIFT_ENTITY.get().create(level);
                    if (rift == null) return false;
                    rift.setPinkRift("pink_rift".equals(kind));
                    rift.setBlackRift("black_rift".equals(kind));
                    rift.setLifetime(600);
                    rift.moveTo(x + 0.5D, y + 0.1D, z + 0.5D, random.nextFloat() * 360.0F, 0.0F);
                    return level.addFreshEntity(rift);
                }
                default -> { return false; }
            }
            if (entity == null) return false;
            entity.moveTo(x + 0.5D, y, z + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            return level.noCollision(entity) && level.addFreshEntity(entity);
        }
        return false;
    }

    private static <T extends Entity> List<T> getLoadedEntities(ServerLevel level, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (type.isInstance(entity) && entity.isAlive()) found.add(type.cast(entity));
        }
        return found;
    }

    private static void updateScarecrowPresence(ServerLevel level, List<ServerPlayer> players,
                                                 RandomSource random, boolean mayVanish) {
        List<ScarecrowEntity> scarecrows = getLoadedEntities(level, ScarecrowEntity.class);
        for (int i = 1; i < scarecrows.size(); i++) scarecrows.get(i).discard();
        if (scarecrows.isEmpty()) return;

        ScarecrowEntity scarecrow = scarecrows.get(0);
        boolean inView = false;
        for (ServerPlayer player : players) {
            if (isInView(player, scarecrow, 96.0D)) {
                inView = true;
                break;
            }
        }
        // It remains still while visible; only roll to vanish while out of frame.
        if (!inView && mayVanish && random.nextInt(100) < 8) {
            scarecrow.discard();
        }
    }

    private static boolean isInView(ServerPlayer player, ScarecrowEntity entity, double maxDistance) {
        if (player.distanceToSqr(entity) > maxDistance * maxDistance || !player.hasLineOfSight(entity)) {
            return false;
        }
        Vec3 target = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        Vec3 direction = target.subtract(player.getEyePosition()).normalize();
        // About an 80-degree viewing cone, covering the visible center of the screen.
        return direction.dot(player.getLookAngle()) > 0.766D;
    }

    private static void manageScarecrow(ServerLevel level, List<ServerPlayer> players,
                                        RandomSource random, int danger, int activity) {
        List<ScarecrowEntity> all = getLoadedEntities(level, ScarecrowEntity.class);
        if (!all.isEmpty()) return;

        int configuredChance = HorrorWorldData.getScarecrowChance(level);
        int chance = configuredChance == 0 ? 0
                : Math.min(70, 18 + scaleChance(configuredChance, danger, activity, 65));
        if (HorrorWorldData.rollEventChance(level, random, chance)) {
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
        if (!HorrorWorldData.rollEventChance(level, random, chance)) return;

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
        if (!HorrorWorldData.rollEventChance(level, random, chance)) return;
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
        if (!HorrorWorldData.rollEventChance(level, random, chance)) return;
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
        if (!HorrorWorldData.rollEventChance(level, random, chance)) return;

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
        if (!HorrorWorldData.rollEventChance(level, random, chance)) return;

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

    private static void maybeSpawnRunningShadow(ServerLevel level, List<ServerPlayer> players,
                                                RandomSource random) {
        if (!HorrorWorldData.rollEventChance(level, random, 8.0D)) return;
        ServerPlayer target = players.get(random.nextInt(players.size()));
        if (!target.onGround() || target.isSleeping()) return;

        Vec3 look = target.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0D, look.z);
        if (forward.lengthSqr() < 0.001D) return;
        forward = forward.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 start = target.position().add(forward.scale(9.0D + random.nextDouble() * 3.0D))
                .add(side.scale(-7.0D));
        int x = net.minecraft.util.Mth.floor(start.x);
        int z = net.minecraft.util.Mth.floor(start.z);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos ground = new BlockPos(x, y - 1, z);
        if (!level.isLoaded(ground) || !level.getBlockState(ground).isSolidRender(level, ground)) return;

        RunningShadowEntity shadow = FieldOfMemories.RUNNING_SHADOW_ENTITY.get().create(level);
        if (shadow == null) return;
        shadow.moveTo(x + 0.5D, y, z + 0.5D,
                (float) Math.toDegrees(Math.atan2(-forward.x, forward.z)), 0.0F);
        shadow.setRunDirection(side.scale(0.82D));
        shadow.setTargetPlayer(target.getUUID());
        if (level.noCollision(shadow)) level.addFreshEntity(shadow);
    }

    private static void spawnScarecrow(ServerLevel level, ServerPlayer target, RandomSource random) {
        for (int attempt = 0; attempt < 32; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            // Keep it well outside the immediate play area, regardless of the home anchor.
            double distance = 48.0D + random.nextDouble() * 24.0D;
            int x = (int) Math.floor(target.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(target.getZ() + Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos ground = new BlockPos(x, y - 1, z);
            BlockPos feet = new BlockPos(x, y, z);

            boolean tooClose = false;
            for (ServerPlayer player : playersInLevel(level)) {
                if (player.distanceToSqr(x + 0.5D, y, z + 0.5D) < 40.0D * 40.0D) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose || !level.getBlockState(ground).isSolidRender(level, ground)
                    || !level.getBlockState(feet).isAir()
                    || !level.getBlockState(feet.above()).isAir()
                    || !level.getFluidState(feet).isEmpty()
                    || !level.getFluidState(feet.above()).isEmpty()) continue;

            ScarecrowEntity scarecrow = FieldOfMemories.SCARECROW_ENTITY.get().create(level);
            if (scarecrow == null) return;
            // The model's feet extend slightly below the entity origin; lift it clear of the ground.
            scarecrow.moveTo(x + 0.5D, y + 0.18D, z + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            if (level.noCollision(scarecrow)) {
                level.addFreshEntity(scarecrow);
                return;
            }
            scarecrow.discard();
        }
    }

    private static List<ServerPlayer> playersInLevel(ServerLevel level) {
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.serverLevel() == level && !player.isCreative() && !player.isSpectator()) {
                players.add(player);
            }
        }
        return players;
    }


    private static void maybePlayDistantDeath(ServerLevel level, List<ServerPlayer> players, RandomSource random) {
        if (!HorrorWorldData.rollEventChance(level, random, 25.0D)) return;
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

        if (HorrorWorldData.rollEventChance(level, random, 18.0D)) {
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
        if (HorrorWorldData.rollEventChance(level, random, 9.0D)) moveHouseWorkstation(level, home, random);
    }

    private static boolean moveHouseWorkstation(ServerLevel level, BlockPos home, RandomSource random) {
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
        if (candidates.isEmpty()) return false;
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
                    return false;
                }
                saved.putInt("x", destination.getX());
                saved.putInt("y", destination.getY());
                saved.putInt("z", destination.getZ());
                moved.load(saved);
                moved.setChanged();
            }
            level.setBlock(source, Blocks.AIR.defaultBlockState(), 3);
            return true;
        }
        return false;
    }

    private static void maybeSpawnWindowStalker(ServerLevel level, List<ServerPlayer> players,
                                                 RandomSource random, int danger, int activity) {
        if (!getLoadedEntities(level, com.fieldofmemories.entity.WindowStalkerEntity.class).isEmpty()) return;
        int chance = Math.min(16, (3 + danger / 16) * activity / 100);
        if (!HorrorWorldData.rollEventChance(level, random, chance)) return;
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