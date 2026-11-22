package com.fieldofmemories.event;

import com.fieldofmemories.data.HorrorWorldData;
import com.fieldofmemories.entity.ScarecrowEntity;
import com.fieldofmemories.entity.SmileFaceEntity;
import com.fieldofmemories.entity.PlayerEchoEntity;
import com.fieldofmemories.entity.SpiritEntity;
import com.fieldofmemories.network.HorrorNetwork;
import com.fieldofmemories.worldgen.HomeMemoryTracker;
import com.fieldofmemories.FieldOfMemories;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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
        if (now % 100L != 0L || now < HorrorWorldData.getNextAmbientEvent(level)) return;

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

        manageScarecrow(level, players, random, danger, activity);
        maybeSpawnSpirit(level, players, random, danger, activity);
        maybeSpawnFace(level, players, random, danger, activity);
        maybeSpawnPlayerEcho(level, players, random, danger, activity);
        HorrorSignEvent.tryTrigger(level, players, random, danger, activity);

        double dangerFactor = 1.0D - danger * 0.006D;
        long interval = Math.max(300L, (long) (600.0D * dangerFactor * 100.0D / activity));
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

        int chance = scaleChance(HorrorWorldData.getScarecrowChance(level), danger, activity, 35);
        if (existing != null) {
            for (ServerPlayer player : players) {
                if (isLookingAt(player, existing, 80.0D)) return;
            }
            existing.discard();
            if (random.nextInt(100) < chance) {
                spawnScarecrow(level, players.get(random.nextInt(players.size())), random);
            }
        } else if (random.nextInt(100) < chance) {
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
                : Math.min(8, (configuredChance + danger / 25) * activity / 100);
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