package com.craftix.hostile_humans.entity.ai.mission;

import com.craftix.hostile_humans.HostileHumans;
import com.craftix.hostile_humans.entity.ai.survival.SquadNeed;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent, bounded mission metadata; path and inventory state are deliberately not persisted. */
public final class SquadMissionSavedData extends SavedData {
    private static final String FILE_ID = HostileHumans.MOD_ID + "_squad_missions";
    private final Map<UUID, SquadMissionState> states = new HashMap<>();

    public static SquadMissionSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                SquadMissionSavedData::load, SquadMissionSavedData::new, FILE_ID);
    }

    public SquadMissionState state(UUID squadId) { return states.computeIfAbsent(squadId, ignored -> new SquadMissionState()); }
    public boolean startIfNone(UUID squadId, MissionType type, UUID campId, @Nullable SquadNeed need,
                               @Nullable BlockPos target, @Nullable UUID targetCamp, long now, long deadline, int members) {
        SquadMissionState state = state(squadId);
        if (state.type() != MissionType.NONE) return false;
        state.start(type, campId, need, target, targetCamp, now, deadline, members);
        setDirty();
        return true;
    }
    public void returnToCamp(UUID squadId) { state(squadId).returnToCamp(); setDirty(); }
    public void complete(UUID squadId, long now) { state(squadId).complete(now); setDirty(); }
    public void markDirty() { setDirty(); }

    public static SquadMissionSavedData load(CompoundTag root) {
        SquadMissionSavedData data = new SquadMissionSavedData();
        for (Tag raw : root.getList("Missions", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) raw;
            if (!tag.hasUUID("Squad")) continue;
            try {
                UUID camp = tag.hasUUID("Camp") ? tag.getUUID("Camp") : null;
                SquadNeed need = tag.contains("Need", Tag.TAG_STRING) ? SquadNeed.valueOf(tag.getString("Need")) : null;
                BlockPos target = tag.contains("Target", Tag.TAG_COMPOUND) ? readPos(tag.getCompound("Target")) : null;
                UUID targetCamp = tag.hasUUID("TargetCamp") ? tag.getUUID("TargetCamp") : null;
                UUID known = tag.hasUUID("KnownEnemy") ? tag.getUUID("KnownEnemy") : null;
                MissionType stored = MissionType.valueOf(tag.getString("Type"));
                MissionType restored = stored == MissionType.NONE ? MissionType.NONE : MissionType.RETURN_TO_CAMP;
                SquadMissionState state = data.state(tag.getUUID("Squad"));
                state.loadBasic(restored, camp, need, target, targetCamp, tag.getLong("Start"), tag.getLong("Deadline"),
                        tag.getLong("ExpeditionCooldown"), tag.getLong("RaidCooldown"), tag.getLong("FailedRaidCooldown"),
                        known, tag.getLong("KnownUntil"), tag.getInt("Members"), tag.getInt("Lost"));
            } catch (RuntimeException ignored) {}
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag root) {
        ListTag missions = new ListTag();
        for (Map.Entry<UUID, SquadMissionState> entry : states.entrySet()) {
            SquadMissionState state = entry.getValue();
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Squad", entry.getKey()); tag.putString("Type", state.type().name());
            if (state.campId() != null) tag.putUUID("Camp", state.campId());
            if (state.primaryNeed() != null) tag.putString("Need", state.primaryNeed().name());
            if (state.targetPos() != null) tag.put("Target", posTag(state.targetPos()));
            if (state.targetCampId() != null) tag.putUUID("TargetCamp", state.targetCampId());
            if (state.knownEnemyCampId() != null) tag.putUUID("KnownEnemy", state.knownEnemyCampId());
            tag.putLong("Start", state.startTick()); tag.putLong("Deadline", state.deadlineTick());
            tag.putLong("ExpeditionCooldown", state.expeditionCooldownUntil());
            tag.putLong("RaidCooldown", state.raidCooldownUntil()); tag.putLong("FailedRaidCooldown", state.failedRaidCooldownUntil());
            tag.putLong("KnownUntil", state.knownEnemyCampUntil()); tag.putInt("Members", state.startingMembers()); tag.putInt("Lost", state.lostMembers());
            missions.add(tag);
        }
        root.put("Missions", missions); return root;
    }

    private static BlockPos readPos(CompoundTag tag) { return new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")); }
    private static CompoundTag posTag(BlockPos pos) { CompoundTag tag = new CompoundTag(); tag.putInt("X", pos.getX()); tag.putInt("Y", pos.getY()); tag.putInt("Z", pos.getZ()); return tag; }
}
