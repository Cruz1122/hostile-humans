package com.craftix.hostile_humans.entity.ai.camp;

import com.craftix.hostile_humans.HostileHumans;
import com.craftix.hostile_humans.persona.PersonaFaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Single persistent registry for physical camps. It never force-loads chunks. */
public final class CampSavedData extends SavedData {
    private static final String FILE_ID = HostileHumans.MOD_ID + "_camps";
    private final Map<UUID, Camp> camps = new HashMap<>();

    public static CampSavedData get(ServerLevel level) {
        MinecraftServer server = level.getServer();
        return server.overworld().getDataStorage().computeIfAbsent(
                CampSavedData::load, CampSavedData::new, FILE_ID);
    }

    public Camp get(UUID id) {
        return camps.get(id);
    }

    public List<Camp> all() {
        return List.copyOf(camps.values());
    }

    public boolean add(Camp camp) {
        if (camps.containsKey(camp.id())) return false;
        camps.put(camp.id(), camp);
        setDirty();
        return true;
    }

    public void update(Camp camp) {
        if (camps.containsKey(camp.id())) {
            camps.put(camp.id(), camp);
            setDirty();
        }
    }

    public boolean remove(UUID id) {
        if (camps.remove(id) == null) return false;
        setDirty();
        return true;
    }

    public List<Camp> nearby(ResourceKey<Level> dimension, BlockPos center, double radius) {
        double max = radius * radius;
        return camps.values().stream()
                .filter(camp -> camp.dimension().equals(dimension)
                        && camp.center().distSqr(center) <= max)
                .toList();
    }

    public boolean hasNearby(ResourceKey<Level> dimension, BlockPos center, double radius) {
        return !nearby(dimension, center, radius).isEmpty();
    }

    public int count(ResourceKey<Level> dimension) {
        int count = 0;
        for (Camp camp : camps.values()) if (camp.dimension().equals(dimension)) count++;
        return count;
    }

    public static CampSavedData load(CompoundTag root) {
        CampSavedData data = new CampSavedData();
        ListTag list = root.getList("Camps", Tag.TAG_COMPOUND);
        for (Tag raw : list) {
            CompoundTag tag = (CompoundTag) raw;
            if (!tag.hasUUID("Id") || !tag.contains("Dimension", Tag.TAG_STRING)
                    || !tag.contains("Faction", Tag.TAG_STRING)) continue;
            try {
                ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                        new ResourceLocation(tag.getString("Dimension")));
                PersonaFaction faction = PersonaFaction.valueOf(tag.getString("Faction"));
                BlockPos center = readPos(tag, "Center");
                if (center == null) continue;
                List<BlockPos> storage = readPositions(tag.getList("Storage", Tag.TAG_COMPOUND));
                data.camps.put(tag.getUUID("Id"), new Camp(tag.getUUID("Id"), dimension, center, faction,
                        storage, readOptionalPos(tag, "Table"), readOptionalPos(tag, "Furnace"),
                        readOptionalPos(tag, "Campfire")));
            } catch (RuntimeException ignored) {
                // A corrupt camp must not prevent the world from loading.
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag list = new ListTag();
        for (Camp camp : camps.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", camp.id());
            tag.putString("Dimension", camp.dimension().location().toString());
            tag.putString("Faction", camp.faction().name());
            writePos(tag, "Center", camp.center());
            ListTag storage = new ListTag();
            for (BlockPos pos : camp.storagePositions()) storage.add(posTag(pos));
            tag.put("Storage", storage);
            if (camp.craftingTablePos() != null) writePos(tag, "Table", camp.craftingTablePos());
            if (camp.furnacePos() != null) writePos(tag, "Furnace", camp.furnacePos());
            if (camp.campfirePos() != null) writePos(tag, "Campfire", camp.campfirePos());
            list.add(tag);
        }
        root.put("Camps", list);
        return root;
    }

    private static BlockPos readPos(CompoundTag root, String key) {
        if (!root.contains(key, Tag.TAG_COMPOUND)) return null;
        CompoundTag pos = root.getCompound(key);
        if (!pos.contains("X", Tag.TAG_INT) || !pos.contains("Y", Tag.TAG_INT) || !pos.contains("Z", Tag.TAG_INT)) return null;
        return new BlockPos(pos.getInt("X"), pos.getInt("Y"), pos.getInt("Z"));
    }

    private static BlockPos readOptionalPos(CompoundTag root, String key) {
        return root.contains(key, Tag.TAG_COMPOUND) ? readPos(root, key) : null;
    }

    private static List<BlockPos> readPositions(ListTag list) {
        List<BlockPos> positions = new ArrayList<>();
        for (Tag raw : list) {
            CompoundTag tag = (CompoundTag) raw;
            positions.add(new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")));
        }
        return positions;
    }

    private static void writePos(CompoundTag root, String key, BlockPos pos) {
        root.put(key, posTag(pos));
    }

    private static CompoundTag posTag(BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        return tag;
    }
}
