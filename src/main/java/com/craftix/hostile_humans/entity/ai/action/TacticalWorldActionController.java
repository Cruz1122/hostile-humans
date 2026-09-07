package com.craftix.hostile_humans.entity.ai.action;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.ai.mission.CampMissionController;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Small single-owner arbiter for server-side world modifications. */
public final class TacticalWorldActionController {
    private static final double MEANINGFUL_PROGRESS_SQR = 0.25D;
    private static final int STALL_TICKS = 20;

    private final Human human;
    private TacticalWorldAction active;
    private int retryCooldown;
    private int failedTicks;
    private int lastTargetId;
    private Vec3 lastPosition = Vec3.ZERO;
    private int placedThisPursuit;
    private int pillarBlocksThisPursuit;
    private int brokenThisPursuit;
    private int recordedActivePlacedBlocks;
    private int recordedActiveBrokenBlocks;

    public TacticalWorldActionController(Human human) {
        this.human = human;
    }

    public void tick() {
        if (human.level().isClientSide) return;
        LivingEntity target = human.getTarget();
        if (target != null && target.isAlive()
                && (!human.hasLineOfSight(target) || target.getY() > human.getY() + 1.0D)
                && human.isUsingItem()) {
            human.stopUsingItem();
        }
        if (retryCooldown > 0) retryCooldown--;
        if (WorldActionSupport.critical(human)) {
            stop(WorldActionResult.PREEMPTED_BY_HIGH_PRIORITY);
            failedTicks = 0;
            return;
        }
        if (human.canHoldRangedCombatPosition(target)) {
            stop(WorldActionResult.PREEMPTED_BY_HIGH_PRIORITY);
            failedTicks = 0;
            return;
        }
        BlockPos objective = target == null ? CampMissionController.worldActionObjective(human) : null;
        if ((target == null || !target.isAlive()) && objective == null) {
            stop(WorldActionResult.ABORTED);
            resetPursuit();
            return;
        }
        int objectiveId = objective == null ? 0 : objective.hashCode();
        int currentTargetId = target == null ? objectiveId : target.getId();
        if (currentTargetId != lastTargetId) {
            stop(WorldActionResult.ABORTED);
            resetPursuit();
            lastTargetId = currentTargetId;
            failedTicks = 0;
            lastPosition = human.position();
        }
        if (active != null) {
            if (activeLimitReached()) {
                stop(WorldActionResult.ABORTED);
                retryCooldown = 20;
                human.getNavigation().recomputePath();
                return;
            }
            WorldActionResult result = active.tick(new WorldActionContext(human, objective));
            recordActiveProgress();
            if (result != WorldActionResult.RUNNING) {
                stop(result);
                retryCooldown = 20;
                human.getNavigation().recomputePath();
            }
            return;
        }
        if (retryCooldown > 0 || placedThisPursuit >= Config.maxBlocksPlacedPerPursuit.get()
                && brokenThisPursuit >= Config.maxBlocksBrokenPerPursuit.get()) return;
        TacticalWorldAction candidate = chooseCandidate(new WorldActionContext(human, objective));
        boolean madeMeaningfulProgress = human.position().distanceToSqr(lastPosition) >= MEANINGFUL_PROGRESS_SQR;
        boolean stalled = human.getNavigation().isStuck()
                || human.getNavigation().isDone() && candidate != null
                || !madeMeaningfulProgress;
        if (stalled) failedTicks++; else failedTicks = 0;
        if (madeMeaningfulProgress) lastPosition = human.position();
        if (failedTicks < STALL_TICKS) return;
        failedTicks = 0;
        if (candidate != null) start(candidate);
        else retryCooldown = 40;
    }

    private TacticalWorldAction chooseCandidate(WorldActionContext context) {
        LivingEntity target = human.getTarget();
        double targetY = target == null ? context.objective().getY() : target.getY();
        if (targetY > human.getY() + 1.0D
                && pillarBlocksThisPursuit < Config.maxPillarBlocksPerPursuit.get()
                && placedThisPursuit < Config.maxBlocksPlacedPerPursuit.get()) {
            PillarUpAction pillar = new PillarUpAction();
            if (pillar.canStart(context)) return pillar;
        }
        if (placedThisPursuit < Config.maxBlocksPlacedPerPursuit.get()) {
            BridgeGapAction bridge = new BridgeGapAction();
            if (bridge.canStart(context)) return bridge;
        }
        if (brokenThisPursuit < Config.maxBlocksBrokenPerPursuit.get()) {
            BreakObstacleAction mining = new BreakObstacleAction();
            if (mining.canStart(context)) return mining;
        }
        return null;
    }

    private void start(TacticalWorldAction action) {
        active = action;
        recordedActivePlacedBlocks = 0;
        recordedActiveBrokenBlocks = 0;
    }

    private void recordActiveProgress() {
        int placed = active.placedBlockCount();
        int placedDelta = Math.max(0, placed - recordedActivePlacedBlocks);
        placedThisPursuit += placedDelta;
        if (active.type() == WorldActionType.PILLAR_UP) pillarBlocksThisPursuit += placedDelta;
        recordedActivePlacedBlocks = Math.max(recordedActivePlacedBlocks, placed);

        int broken = active.brokenBlockCount();
        brokenThisPursuit += Math.max(0, broken - recordedActiveBrokenBlocks);
        recordedActiveBrokenBlocks = Math.max(recordedActiveBrokenBlocks, broken);
    }

    private boolean activeLimitReached() {
        return switch (active.type()) {
            case PILLAR_UP -> placedThisPursuit >= Config.maxBlocksPlacedPerPursuit.get()
                    || pillarBlocksThisPursuit >= Config.maxPillarBlocksPerPursuit.get();
            case BRIDGE_GAP -> placedThisPursuit >= Config.maxBlocksPlacedPerPursuit.get();
            case BREAK_OBSTACLE -> brokenThisPursuit >= Config.maxBlocksBrokenPerPursuit.get();
            case PLACE_COBWEB -> false;
        };
    }

    private void stop(WorldActionResult result) {
        if (active != null) active.stop(new WorldActionContext(human));
        active = null;
        recordedActivePlacedBlocks = 0;
        recordedActiveBrokenBlocks = 0;
    }

    private void resetPursuit() {
        placedThisPursuit = 0;
        pillarBlocksThisPursuit = 0;
        brokenThisPursuit = 0;
        lastTargetId = 0;
    }

    public TacticalWorldAction activeAction() { return active; }
    public int placedThisPursuit() { return placedThisPursuit; }
    public int brokenThisPursuit() { return brokenThisPursuit; }
}
