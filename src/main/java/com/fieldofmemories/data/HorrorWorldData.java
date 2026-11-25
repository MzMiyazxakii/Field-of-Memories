package com.fieldofmemories.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public class HorrorWorldData extends SavedData {
    private static final String DATA_NAME = "fieldofmemories_horror_world";
    private static final String ACTIVITY_KEY = "ActivityMultiplier";
    private static final String SCARECROW_CHANCE_KEY = "ScarecrowChance";
    private static final String FACE_CHANCE_KEY = "FaceChance";
    private static final String SPIRIT_CHANCE_KEY = "SpiritChance";
    private static final String MIMIC_CHANCE_KEY = "MimicChance";
    private static final String TREE_DISC_CHANCE_KEY = "TreeDiscChance";
    private static final String HOUSE_CAMERA_CHANCE_KEY = "HouseCameraChance";
    private static final String NEXT_EVENT_KEY = "NextAmbientEvent";
    private static final String SPIRIT_DISC_FAILURES_KEY = "SpiritDiscFailures";
    private static final String EVENT_RAMP_START_DAY_KEY = "EventRampStartDay";
    private static final String HORROR_ACTIVE_BONUS_KEY = "HorrorActiveBonus";

    private int activityMultiplier = 100;
    private int scarecrowChance = 22;
    private int faceChance = 22;
    private int spiritChance = 2;
    private int mimicChance = 1;
    private int treeDiscChance = 25;
    private int houseCameraChance = 32;
    private long nextAmbientEvent;
    private int spiritDiscFailures;
    private int eventRampStartDay = -1;
    private int horrorActiveBonus = 0;

    public static HorrorWorldData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                HorrorWorldData::load, HorrorWorldData::new, DATA_NAME);
    }

    public static int getDangerLevel(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        long worldDays = Math.max(0L, overworld.getGameTime()) / 24000L;
        int storyProgress = com.fieldofmemories.data.MemoryDiscWorldData.getReadCount(overworld);
        // World age and read story discs contribute to one shared danger value.
        return Math.min(100, (int) (worldDays * 1.2D) + storyProgress * 5);
    }

    public static int getActivityMultiplier(ServerLevel level) {
        return get(level).activityMultiplier;
    }

    /**
     * Each world gets a saved 2-4 day quiet opening. After that, event frequency
     * rises gradually until it reaches the normal configured chances.
     */
    public static int getEventChanceScalePercent(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        HorrorWorldData data = get(overworld);
        if (data.eventRampStartDay < 0) {
            data.eventRampStartDay = 2 + overworld.random.nextInt(3);
            data.setDirty();
        }

        long worldDays = Math.max(0L, overworld.getGameTime()) / 24000L;
        if (worldDays < data.eventRampStartDay) return 10;
        return Math.min(100, 10 + (int) (worldDays - data.eventRampStartDay) * 5);
    }

    public static boolean rollEventChance(ServerLevel level, net.minecraft.util.RandomSource random,
                                          double baseChancePercent) {
        if (baseChancePercent <= 0.0D) return false;
        double scaledChance = Math.max(0.0D, Math.min(100.0D,
                Math.min(100.0D, baseChancePercent) * getEventChanceScalePercent(level) / 100.0D
                        + get(level).horrorActiveBonus));
        return random.nextDouble() * 100.0D < scaledChance;
    }

    public static int getHorrorActiveBonus(ServerLevel level) {
        return get(level).horrorActiveBonus;
    }

    public static int changeHorrorActive(ServerLevel level, int amount) {
        HorrorWorldData data = get(level);
        int updated = clamp(data.horrorActiveBonus + amount, -100, 100);
        if (updated != data.horrorActiveBonus) {
            data.horrorActiveBonus = updated;
            data.setDirty();
        }
        return updated;
    }

    public static int getScarecrowChance(ServerLevel level) {
        return get(level).scarecrowChance;
    }

    public static int getFaceChance(ServerLevel level) {
        return get(level).faceChance;
    }

    public static int getSpiritChance(ServerLevel level) {
        return get(level).spiritChance;
    }

    public static int getMimicChance(ServerLevel level) {
        return get(level).mimicChance;
    }

    public static int getTreeDiscChance(ServerLevel level) {
        return get(level).treeDiscChance;
    }

    public static int getHouseCameraChance(ServerLevel level) {
        return get(level).houseCameraChance;
    }

    public static long getNextAmbientEvent(ServerLevel level) {
        return get(level).nextAmbientEvent;
    }

    public static void setNextAmbientEvent(ServerLevel level, long tick) {
        HorrorWorldData data = get(level);
        if (data.nextAmbientEvent != tick) {
            data.nextAmbientEvent = tick;
            data.setDirty();
        }
    }

    public static boolean rollSpiritDiscDrop(ServerLevel level) {
        HorrorWorldData data = get(level);
        data.spiritDiscFailures = Math.min(20, data.spiritDiscFailures + 1);
        boolean dropped = data.spiritDiscFailures >= 20 || level.random.nextFloat() < 0.10F;
        if (dropped) data.spiritDiscFailures = 0;
        data.setDirty();
        return dropped;
    }

    public static void changeSetting(ServerLevel level, int setting, int amount) {
        HorrorWorldData data = get(level);
        int old;
        int updated;
        if (setting == 0) {
            old = data.scarecrowChance;
            updated = clamp(old + amount, 0, 100);
            data.scarecrowChance = updated;
        } else if (setting == 1) {
            old = data.faceChance;
            updated = clamp(old + amount, 0, 100);
            data.faceChance = updated;
        } else if (setting == 2) {
            old = data.activityMultiplier;
            updated = clamp(old + amount, 10, 300);
            data.activityMultiplier = updated;
        } else if (setting == 3) {
            old = data.spiritChance;
            updated = clamp(old + amount, 0, 100);
            data.spiritChance = updated;
        } else if (setting == 4) {
            old = data.mimicChance;
            updated = clamp(old + amount, 0, 100);
            data.mimicChance = updated;
        } else if (setting == 5) {
            old = data.treeDiscChance;
            updated = clamp(old + amount, 0, 1000);
            data.treeDiscChance = updated;
        } else if (setting == 6) {
            old = data.houseCameraChance;
            updated = clamp(old + amount, 0, 100);
            data.houseCameraChance = updated;
        } else {
            return;
        }
        if (old != updated) data.setDirty();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static HorrorWorldData load(CompoundTag tag) {
        HorrorWorldData data = new HorrorWorldData();
        data.activityMultiplier = clamp(tag.getInt(ACTIVITY_KEY) == 0 ? 100 : tag.getInt(ACTIVITY_KEY), 10, 300);
        data.scarecrowChance = clamp(tag.contains(SCARECROW_CHANCE_KEY) ? tag.getInt(SCARECROW_CHANCE_KEY) : 22, 0, 100);
        data.faceChance = clamp(tag.contains(FACE_CHANCE_KEY) ? tag.getInt(FACE_CHANCE_KEY) : 10, 0, 100);
        data.spiritChance = clamp(tag.contains(SPIRIT_CHANCE_KEY) ? tag.getInt(SPIRIT_CHANCE_KEY) : 2, 0, 100);
        data.mimicChance = clamp(tag.contains(MIMIC_CHANCE_KEY) ? tag.getInt(MIMIC_CHANCE_KEY) : 1, 0, 100);
        data.treeDiscChance = clamp(tag.contains(TREE_DISC_CHANCE_KEY) ? tag.getInt(TREE_DISC_CHANCE_KEY) : 25, 0, 1000);
        data.houseCameraChance = clamp(tag.contains(HOUSE_CAMERA_CHANCE_KEY) ? tag.getInt(HOUSE_CAMERA_CHANCE_KEY) : 32, 0, 100);
        data.nextAmbientEvent = Math.max(0L, tag.getLong(NEXT_EVENT_KEY));
        data.spiritDiscFailures = clamp(tag.getInt(SPIRIT_DISC_FAILURES_KEY), 0, 19);
        int savedRampStartDay = tag.getInt(EVENT_RAMP_START_DAY_KEY);
        data.eventRampStartDay = savedRampStartDay >= 2
                ? clamp(savedRampStartDay, 2, 4) : -1;
        data.horrorActiveBonus = clamp(tag.getInt(HORROR_ACTIVE_BONUS_KEY), -100, 100);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(ACTIVITY_KEY, activityMultiplier);
        tag.putInt(SCARECROW_CHANCE_KEY, scarecrowChance);
        tag.putInt(FACE_CHANCE_KEY, faceChance);
        tag.putInt(SPIRIT_CHANCE_KEY, spiritChance);
        tag.putInt(MIMIC_CHANCE_KEY, mimicChance);
        tag.putInt(TREE_DISC_CHANCE_KEY, treeDiscChance);
        tag.putInt(HOUSE_CAMERA_CHANCE_KEY, houseCameraChance);
        tag.putLong(NEXT_EVENT_KEY, nextAmbientEvent);
        tag.putInt(SPIRIT_DISC_FAILURES_KEY, spiritDiscFailures);
        tag.putInt(EVENT_RAMP_START_DAY_KEY, eventRampStartDay);
        tag.putInt(HORROR_ACTIVE_BONUS_KEY, horrorActiveBonus);
        return tag;
    }
}
