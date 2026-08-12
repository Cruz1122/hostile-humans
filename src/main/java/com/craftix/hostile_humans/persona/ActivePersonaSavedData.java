package com.craftix.hostile_humans.persona;

import com.craftix.hostile_humans.HostileHumans;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ActivePersonaSavedData extends SavedData {
    private static final String FILE_ID = HostileHumans.MOD_ID + "_active_personas";
    private static final String RESERVATIONS_TAG = "Reservations";
    private final Map<String, UUID> reservations = new HashMap<>();

    public static ActivePersonaSavedData get(ServerLevel level) {
        MinecraftServer server = level.getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) throw new IllegalStateException("Overworld is unavailable");
        return overworld.getDataStorage().computeIfAbsent(
                ActivePersonaSavedData::load, ActivePersonaSavedData::new, FILE_ID);
    }

    public boolean isReserved(String personaId) {
        return reservations.containsKey(personaId);
    }

    public boolean tryReserve(String personaId, UUID entityUuid) {
        UUID existing = reservations.get(personaId);
        if (existing != null) return existing.equals(entityUuid);
        reservations.put(personaId, entityUuid);
        setDirty();
        return true;
    }

    public void release(String personaId, UUID entityUuid) {
        if (reservations.remove(personaId, entityUuid)) setDirty();
    }

    public int size() {
        return reservations.size();
    }

    public static ActivePersonaSavedData load(CompoundTag root) {
        ActivePersonaSavedData data = new ActivePersonaSavedData();
        ListTag entries = root.getList(RESERVATIONS_TAG, CompoundTag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            String personaId = entry.getString("PersonaId");
            if (!personaId.isEmpty() && entry.hasUUID("EntityUuid")) {
                data.reservations.put(personaId, entry.getUUID("EntityUuid"));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag entries = new ListTag();
        reservations.forEach((personaId, entityUuid) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("PersonaId", personaId);
            entry.putUUID("EntityUuid", entityUuid);
            entries.add(entry);
        });
        root.put(RESERVATIONS_TAG, entries);
        return root;
    }
}
