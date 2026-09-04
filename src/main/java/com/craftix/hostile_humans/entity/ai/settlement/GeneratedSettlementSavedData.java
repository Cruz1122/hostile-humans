package com.craftix.hostile_humans.entity.ai.settlement;

import com.craftix.hostile_humans.HostileHumans;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** Persistent idempotency registry for settlements initialized after world generation. */
public final class GeneratedSettlementSavedData extends SavedData {
    private static final String FILE_ID = HostileHumans.MOD_ID + "_generated_settlements";
    private final Set<String> initialized = new HashSet<>();

    public static GeneratedSettlementSavedData get(ServerLevel level) {
        MinecraftServer server = level.getServer();
        return server.overworld().getDataStorage().computeIfAbsent(
                GeneratedSettlementSavedData::load, GeneratedSettlementSavedData::new, FILE_ID);
    }

    public boolean isInitialized(String key) {
        return initialized.contains(key);
    }

    public void markInitialized(String key) {
        if (initialized.add(key)) setDirty();
    }

    public static GeneratedSettlementSavedData load(CompoundTag root) {
        GeneratedSettlementSavedData data = new GeneratedSettlementSavedData();
        ListTag settlements = root.getList("Settlements", Tag.TAG_STRING);
        for (Tag tag : settlements) data.initialized.add(tag.getAsString());
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag settlements = new ListTag();
        for (String key : initialized) settlements.add(StringTag.valueOf(key));
        root.put("Settlements", settlements);
        return root;
    }
}
