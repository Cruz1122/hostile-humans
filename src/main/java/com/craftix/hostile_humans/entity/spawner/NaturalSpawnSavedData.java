package com.craftix.hostile_humans.entity.spawner;

import com.craftix.hostile_humans.HostileHumans;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent world-wide state for one-time natural encounter events. */
public final class NaturalSpawnSavedData extends SavedData {
    private static final String FILE_ID = HostileHumans.MOD_ID + "_natural_spawns";
    private static final String INITIAL_END_ISLAND_POPULATED = "InitialEndIslandPopulated";

    private boolean initialEndIslandPopulated;

    public static NaturalSpawnSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                NaturalSpawnSavedData::load,
                NaturalSpawnSavedData::new,
                FILE_ID);
    }

    public boolean isInitialEndIslandPopulated() {
        return initialEndIslandPopulated;
    }

    public void markInitialEndIslandPopulated() {
        if (!initialEndIslandPopulated) {
            initialEndIslandPopulated = true;
            setDirty();
        }
    }

    public static NaturalSpawnSavedData load(CompoundTag root) {
        NaturalSpawnSavedData data = new NaturalSpawnSavedData();
        data.initialEndIslandPopulated = root.getBoolean(INITIAL_END_ISLAND_POPULATED);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        root.putBoolean(INITIAL_END_ISLAND_POPULATED, initialEndIslandPopulated);
        return root;
    }
}
