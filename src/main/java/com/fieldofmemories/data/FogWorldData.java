package com.fieldofmemories.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class FogWorldData extends SavedData {
    private static final String DATA_NAME = "fieldofmemories_fog";
    private int distance = 88;

    public static FogWorldData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(FogWorldData::load, FogWorldData::new, DATA_NAME);
    }

    public static int getDistance(ServerLevel level) {
        return get(level).distance;
    }

    public static int changeDistance(ServerLevel level, int amount) {
        FogWorldData data = get(level);
        data.distance = Math.max(48, Math.min(160, data.distance + amount));
        data.setDirty();
        return data.distance;
    }

    private static FogWorldData load(CompoundTag tag) {
        FogWorldData data = new FogWorldData();
        if (tag.contains("Distance")) data.distance = Math.max(48, Math.min(160, tag.getInt("Distance")));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("Distance", distance);
        return tag;
    }
}
