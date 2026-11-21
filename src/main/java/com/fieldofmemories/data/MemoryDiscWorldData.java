package com.fieldofmemories.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class MemoryDiscWorldData extends SavedData {
    private static final String DATA_NAME = "fieldofmemories_memory_discs";
    private static final String MASK_KEY = "ReadDiscMask";
    private static final String SPAWN_ENABLED_KEY = "SpawnEnabled";
    private static final String SPAWN_CHANCE_KEY = "SpawnChance";
    private int readMask;
    private boolean spawnEnabled = true;
    private int spawnChance = 10;

    public static MemoryDiscWorldData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                MemoryDiscWorldData::load, MemoryDiscWorldData::new, DATA_NAME);
    }

    public static void markRead(ServerLevel level, int discId) {
        MemoryDiscWorldData data = get(level);
        int bit = 1 << discId;
        if ((data.readMask & bit) == 0) {
            data.readMask |= bit;
            data.setDirty();
        }
    }

    public static boolean hasRead(ServerLevel level, int discId) {
        return (get(level).readMask & (1 << discId)) != 0;
    }

    public static int getReadCount(ServerLevel level) {
        return Integer.bitCount(get(level).readMask & 0xFF);
    }

    public static int getReadMask(ServerLevel level) {
        return get(level).readMask & 0xFF;
    }

    public static boolean hasReadAll(ServerLevel level) {
        return (get(level).readMask & 0xFF) == 0xFF;
    }

    public static boolean isSpawnEnabled(ServerLevel level) {
        return get(level).spawnEnabled;
    }

    public static int getSpawnChance(ServerLevel level) {
        return get(level).spawnChance;
    }

    public static void setSpawnEnabled(ServerLevel level, boolean enabled) {
        MemoryDiscWorldData data = get(level);
        if (data.spawnEnabled != enabled) {
            data.spawnEnabled = enabled;
            data.setDirty();
        }
    }

    public static void changeSpawnChance(ServerLevel level, int amount) {
        MemoryDiscWorldData data = get(level);
        int updated = Math.max(0, Math.min(100, data.spawnChance + amount));
        if (updated != data.spawnChance) {
            data.spawnChance = updated;
            data.setDirty();
        }
    }

    private static MemoryDiscWorldData load(CompoundTag tag) {
        MemoryDiscWorldData data = new MemoryDiscWorldData();
        data.readMask = tag.getInt(MASK_KEY) & 0xFF;
        if (tag.contains(SPAWN_ENABLED_KEY)) data.spawnEnabled = tag.getBoolean(SPAWN_ENABLED_KEY);
        if (tag.contains(SPAWN_CHANCE_KEY)) data.spawnChance = Math.max(0, Math.min(100, tag.getInt(SPAWN_CHANCE_KEY)));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(MASK_KEY, readMask);
        tag.putBoolean(SPAWN_ENABLED_KEY, spawnEnabled);
        tag.putInt(SPAWN_CHANCE_KEY, spawnChance);
        return tag;
    }
}
