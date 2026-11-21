package com.fieldofmemories.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class MemoryDiscWorldData extends SavedData {
    private static final String DATA_NAME = "fieldofmemories_memory_discs";
    private static final String MASK_KEY = "ReadDiscMask";
    private int readMask;

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

    public static boolean hasReadAll(ServerLevel level) {
        return (get(level).readMask & 0xFF) == 0xFF;
    }

    private static MemoryDiscWorldData load(CompoundTag tag) {
        MemoryDiscWorldData data = new MemoryDiscWorldData();
        data.readMask = tag.getInt(MASK_KEY) & 0xFF;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(MASK_KEY, readMask);
        return tag;
    }
}
