package com.craftix.hostile_humans.entity.ai.mission;

import com.craftix.hostile_humans.entity.ai.survival.SquadNeed;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.UUID;

/** Mutable server-side state for one squad's single mission slot. */
public final class SquadMissionState {
    private MissionType type = MissionType.NONE;
    @Nullable private UUID campId;
    @Nullable private SquadNeed primaryNeed;
    @Nullable private BlockPos targetPos;
    @Nullable private UUID targetCampId;
    private long startTick;
    private long deadlineTick;
    private long expeditionCooldownUntil;
    private long raidCooldownUntil;
    private long failedRaidCooldownUntil;
    @Nullable private UUID knownEnemyCampId;
    private long knownEnemyCampUntil;
    private int startingMembers;
    private int lostMembers;

    public MissionType type() { return type; }
    public UUID campId() { return campId; }
    public SquadNeed primaryNeed() { return primaryNeed; }
    public BlockPos targetPos() { return targetPos; }
    public UUID targetCampId() { return targetCampId; }
    public long startTick() { return startTick; }
    public long deadlineTick() { return deadlineTick; }
    public long expeditionCooldownUntil() { return expeditionCooldownUntil; }
    public long raidCooldownUntil() { return raidCooldownUntil; }
    public long failedRaidCooldownUntil() { return failedRaidCooldownUntil; }
    public UUID knownEnemyCampId() { return knownEnemyCampId; }
    public long knownEnemyCampUntil() { return knownEnemyCampUntil; }
    public int startingMembers() { return startingMembers; }
    public int lostMembers() { return lostMembers; }

    public void start(MissionType type, UUID campId, @Nullable SquadNeed need, @Nullable BlockPos targetPos,
                      @Nullable UUID targetCampId, long now, long deadline, int members) {
        this.type = type; this.campId = campId; this.primaryNeed = need;
        this.targetPos = targetPos == null ? null : targetPos.immutable(); this.targetCampId = targetCampId;
        this.startTick = now; this.deadlineTick = deadline; this.startingMembers = members; this.lostMembers = 0;
    }

    public void returnToCamp() { type = MissionType.RETURN_TO_CAMP; targetPos = null; targetCampId = null; }
    public void complete(long now) { type = MissionType.NONE; startTick = 0; deadlineTick = 0; expeditionCooldownUntil = now; }
    public void setExpeditionCooldown(long tick) { expeditionCooldownUntil = tick; }
    public void setRaidCooldown(long tick) { raidCooldownUntil = tick; }
    public void setFailedRaidCooldown(long tick) { failedRaidCooldownUntil = tick; }
    public void lostMember() { lostMembers++; }
    public void rememberEnemyCamp(UUID id, long until) { knownEnemyCampId = id; knownEnemyCampUntil = until; }
    public void clearKnownEnemyCamp() { knownEnemyCampId = null; knownEnemyCampUntil = 0; }
    public void loadBasic(MissionType type, UUID campId, SquadNeed need, BlockPos targetPos, UUID targetCampId,
                          long start, long deadline, long expeditionCooldown, long raidCooldown,
                          long failedRaidCooldown, UUID knownEnemyCampId, long knownEnemyCampUntil,
                          int startingMembers, int lostMembers) {
        this.type = type; this.campId = campId; this.primaryNeed = need; this.targetPos = targetPos;
        this.targetCampId = targetCampId; this.startTick = start; this.deadlineTick = deadline;
        this.expeditionCooldownUntil = expeditionCooldown; this.raidCooldownUntil = raidCooldown;
        this.failedRaidCooldownUntil = failedRaidCooldown; this.knownEnemyCampId = knownEnemyCampId;
        this.knownEnemyCampUntil = knownEnemyCampUntil; this.startingMembers = startingMembers; this.lostMembers = lostMembers;
    }
}
