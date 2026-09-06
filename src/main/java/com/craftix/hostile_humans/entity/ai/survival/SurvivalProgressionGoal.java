package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.HostileHumans;
import com.craftix.hostile_humans.entity.ai.action.MiningToolSelector;
import com.craftix.hostile_humans.entity.ai.action.WorldActionResult;
import com.craftix.hostile_humans.entity.ai.action.WorldActionSupport;
import com.craftix.hostile_humans.entity.ai.control.HumanEntityWalkControl;
import com.craftix.hostile_humans.entity.ai.squad.SquadManager;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.type.human.HumanLootPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Arrays;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

/** Small needs -> opportunity -> action loop. Combat and critical existing goals preempt it. */
public final class SurvivalProgressionGoal extends Goal {
    public static final String DEBUG_TAG = "hh_survival_debug";
    private static final int DEBUG_HEARTBEAT_TICKS = 100;
    private enum Mode { RESOURCE, STATION, CRAFTING, HUNT, EXPLORE }
    private static final int HUNT_PATH_TIMEOUT_TICKS = 100;
    private static final int RESOURCE_PATH_TIMEOUT_TICKS = 80;
    private static final int STATION_PATH_TIMEOUT_TICKS = 60;
    private static final int EXPLORE_TIMEOUT_TICKS = 140;
    private static final int CRAFTING_ACTION_TIMEOUT_TICKS = 400;
    private static final int STATION_ENTRY_DELAY_TICKS = 10;
    private static final int CRAFTING_PACE_TICKS = 20;
    private static final int FURNACE_INTERACTION_PACE_TICKS = 10;
    private static final int RESOURCE_FORWARD_NUDGE_TICKS = 20;
    private static final int MAX_WOOD_BATCH_ACTIONS = 8;
    private static final int RESOURCE_FORWARD_NUDGE_DURATION_TICKS = 14;
    private static final double RESOURCE_MOVEMENT_EPSILON_SQR = 0.0004D;
    private static final double RESOURCE_FORWARD_NUDGE_DISTANCE = 3.0D;
    private static final double RESOURCE_FORWARD_NUDGE_SPEED = 3.2D;
    private static final double RESOURCE_INTERACTION_TOLERANCE_SQR = 2.25D;

    private final Human human;
    private final SurvivalController controller;
    private SquadNeed need;
    private Mode mode;
    private BlockPos targetPos;
    private Entity animal;
    private SquadNeed resourceBatchNeed;
    private int resourceBatchActions;
    private ProgressiveBlockBreaker breaker;
    private int actionTicks;
    private int huntStalledTicks;
    private int huntAttackCooldown;
    private double closestAnimalDistanceSqr;
    private int nextDecisionTick;
    private int navigationStalledTicks;
    private BlockPos resourceProgressTarget;
    private Vec3 lastResourcePosition;
    private int resourceNoProgressTicks;
    private Vec3 resourceNudgeTarget;
    private int resourceNudgeTicks;
    private int stationInteractionCooldownTicks;
    private final Map<BlockPos, Integer> failedResourceUntil = new HashMap<>();
    private final Map<net.minecraft.world.level.block.Block, Optional<BlockPos>> stationCache = new HashMap<>();
    private final Map<net.minecraft.world.level.block.Block, Boolean> nearbyStationCache = new HashMap<>();
    private int stationCacheTick = Integer.MIN_VALUE;
    private String lastDebugSignature;
    private int nextDebugHeartbeatTick;
    private Component debugOriginalName;
    private boolean debugOriginalNameVisible;
    private boolean debugNameApplied;

    public SurvivalProgressionGoal(Human human) {
        this.human = human;
        this.controller = new SurvivalController(human);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public SurvivalSnapshot snapshot() {
        return controller.snapshot();
    }

    @Override
    public boolean canUse() {
        // Survival decisions are deliberately less urgent than combat and
        // should not perform world scans every time GoalSelector checks us.
        if (!activeActionEligible() || human.isInvestigatingSound()) return false;
        // A preempting goal may temporarily own MOVE. Keep the selected action
        // instead of rebuilding the whole plan after every such interruption.
        if (controller.isSuspended()) return resumableAction();
        if (human.tickCount < nextDecisionTick) return false;
        if (!controller.requestAssessment()) return false;
        failedResourceUntil.entrySet().removeIf(entry -> entry.getValue() <= human.tickCount);
        nextDecisionTick = nextDecisionTick();
        controller.beginPlanning();
        if (planNextAction()) return true;
        controller.plan(null);
        return false;
    }

    /**
     * Performs one bounded planning pass. Selection mutates only this goal's
     * intent/lease; world mutation is left to the active task tick.
     */
    private boolean planNextAction() {
        return planNextAction(!human.isInvestigatingSound());
    }

    private boolean planNextAction(boolean allowExploration) {
        if (human.isInvestigatingSound()) return false;
        SquadNeeds needs = SquadNeedsEvaluator.evaluate(human);
        SurvivalInventory.discardDuplicateTieredTools(human);
        if (tryShare(needs)) {
            controller.completeImmediate(new SurvivalIntent(SurvivalTask.SHARE, SurvivalObjective.FOOD));
            return false;
        }
        if (planBatchedResource(needs)) return true;
        // Food is a fallback reserve, not a reason to interrupt progression.
        // Once it is the only outstanding need, prefer a hunt over optional
        // gear crafting that happens to be possible at a nearby table.
        if (needs.needs(SquadNeed.FOOD) && needs.highestPriority().orElse(null) == SquadNeed.FOOD
                && selectAnimal()) {
            controller.plan(new SurvivalIntent(SurvivalTask.HUNT, SurvivalObjective.FOOD));
            return true;
        }
        if (prepareImmediateCraftingMaterials()) return false;
        // Table crafting is a real station action. Do not complete the gear
        // recipes in the planning pass, otherwise a whole set of tools or
        // armor can appear in one server tick without facing the table.
        if (selectCraftingTable()) {
            controller.plan(new SurvivalIntent(SurvivalTask.CRAFT, SurvivalObjective.IRON_GEAR));
            return true;
        }

        List<SquadNeed> neededResources = Arrays.stream(SquadNeed.values()).filter(needs::needs).toList();
        Optional<LocalResourceScanner.ResourceTarget> resource = LocalResourceScanner.findReachableFirst(human, neededResources,
                pos -> failedResourceUntil.getOrDefault(pos, 0) > human.tickCount);
        if (resource.isPresent()) {
            LocalResourceScanner.ResourceTarget selected = resource.get();
            if (SurvivalClaimManager.claimResource(human, selected.pos())) {
                need = selected.need();
                targetPos = selected.pos();
                mode = Mode.RESOURCE;
                resourceBatchNeed = selected.need();
                resourceBatchActions = 0;
                controller.plan(new SurvivalIntent(SurvivalTask.GATHER, objectiveFor(selected.need())));
                return true;
            }
        }
        if (selectFurnace(needs)) {
            controller.plan(new SurvivalIntent(SurvivalTask.SMELT, SurvivalObjective.IRON_GEAR));
            return true;
        }
        need = needs.highestPriority().orElse(null);
        if (!allowExploration || need == null || !selectExplorationTarget()) return false;
        controller.plan(new SurvivalIntent(SurvivalTask.EXPLORE, SurvivalObjective.EXPLORATION));
        return true;
    }

    private static SurvivalObjective objectiveFor(SquadNeed need) {
        return SurvivalPlanner.objectiveFor(need);
    }

    @Override
    public boolean canContinueToUse() {
        // Exploration and hunting are fallback work. Reconsider them as soon
        // as a newly nearby ore target becomes actionable instead of walking
        // past it until the current action times out.
        if (mode == Mode.EXPLORE && actionTicks % 10 == 0
                && hasNearbyPriorityResource()) return false;
        if (mode == null) return false;
        if (mode == Mode.HUNT) {
            return activeActionEligible() && actionTicks < 160
                    && (animal != null || controller.snapshot().state() == SurvivalState.PLANNING);
        }
        if (combatMovementActive()) return false;
        // A successful mine leaves a real drop beside the human. Yield the
        // MOVE flag while the action is in PLANNING so the registered loot
        // goal (priority 6) can collect it before survival picks a new target
        // (priority 5).
        if (mode == Mode.RESOURCE && controller.snapshot().state() == SurvivalState.PLANNING
                && hasNearbyUsefulLoot()) return false;
        boolean actionEligible = mode == Mode.EXPLORE ? eligible() : activeActionEligible();
        int timeout = switch (mode) {
            case EXPLORE -> EXPLORE_TIMEOUT_TICKS;
            case CRAFTING -> CRAFTING_ACTION_TIMEOUT_TICKS;
            default -> 160;
        };
        return actionEligible && actionTicks < timeout;
    }

    @Override
    public void start() {
        if (controller.isSuspended()) controller.resumeAfterInterruption();
        actionTicks = 0;
        navigationStalledTicks = 0;
        resetResourceProgress();
        stationInteractionCooldownTicks = mode == Mode.STATION ? FURNACE_INTERACTION_PACE_TICKS
                : mode == Mode.CRAFTING ? STATION_ENTRY_DELAY_TICKS : 0;
        controller.ensureActing();
        if (mode == Mode.HUNT && animal != null) {
            huntStalledTicks = 0;
            huntAttackCooldown = Math.floorMod(human.getUUID().hashCode(), 10);
            closestAnimalDistanceSqr = human.distanceToSqr(animal);
            moveToAnimal();
            return;
        }
        if (targetPos != null) moveToTarget();
    }

    @Override
    public void tick() {
        actionTicks++;
        if (mode == Mode.HUNT) {
            if (animal == null && controller.snapshot().state() == SurvivalState.PLANNING) {
                if (!planNextAction()) {
                    controller.plan(null);
                    mode = null;
                }
                return;
            }
            tickHunt();
            return;
        }
        if (mode == null) return;
        if (combatMovementActive()) {
            clearOwnedMovementRequest();
            controller.suspend();
            return;
        }
        if (mode == Mode.EXPLORE ? !eligible() : !activeActionEligible()) {
            controller.suspend();
            return;
        }
        if (controller.snapshot().state() == SurvivalState.PLANNING && targetPos == null) {
            if (!planNextAction()) {
                controller.plan(null);
                mode = null;
            }
            return;
        }
        if (mode == Mode.RESOURCE) tickResource();
        else if (mode == Mode.STATION) tickStation();
        else if (mode == Mode.CRAFTING) tickCrafting();
        else if (mode == Mode.EXPLORE && human.getNavigation().isDone()) {
            SquadNeedsEvaluator.invalidate(human);
            targetPos = null;
            mode = null;
            nextDecisionTick = nextDecisionTick();
        }
    }

    @Override
    public void stop() {
        boolean resume = resumableAction();
        if (breaker != null) breaker.abort();
        if (targetPos != null) {
            SurvivalClaimManager.releaseResource(human, targetPos);
            SurvivalClaimManager.releaseStation(human, targetPos);
        }
        clearOwnedMovementRequest();
        breaker = null;
        if (resume) {
            // Claims are reacquired by the active tick after the preempting
            // goal releases MOVE. The target itself remains available for the
            // resumed path calculation.
            controller.suspend();
        } else {
            clearAction();
            controller.reset();
        }
    }

    /** Called by Human.setTarget before the next goal-selector pass. */
    public void interruptForCombat() {
        if (mode == null) return;
        clearOwnedMovementRequest();
        controller.suspend();
    }

    private void tickHunt() {
        if (!activeActionEligible() || animal == null) {
            mode = null;
            return;
        }
        if (!animal.isAlive()) {
            if (controller.snapshot().state() == SurvivalState.NAVIGATE) controller.arrived();
            if (controller.snapshot().state() == SurvivalState.ACT) controller.collect();
            if (controller.snapshot().state() == SurvivalState.COLLECT) controller.verify();
            if (controller.snapshot().state() == SurvivalState.VERIFY) controller.succeeded();
            animal = null;
            return;
        }
        if (!equipHuntingWeapon()) {
            abortAction();
            return;
        }
        var target = (net.minecraft.world.entity.LivingEntity) animal;
        human.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distanceSqr = human.distanceToSqr(target);
        double attackRangeSqr = Math.max(9.0D, human.getMeleeAttackRangeSqr(target));
        if (distanceSqr <= attackRangeSqr) {
            human.getNavigation().stop();
            if (controller.snapshot().state() == SurvivalState.NAVIGATE) controller.arrived();
            if (huntAttackCooldown-- <= 0) {
                boolean hit = human.doHurtTarget(target);
                if (!hit) {
                    hit = target.hurt(human.damageSources().mobAttack(human), 1.0F);
                }
                if (hit) huntAttackCooldown = 10;
            }
            huntStalledTicks = 0;
            closestAnimalDistanceSqr = distanceSqr;
            return;
        }
        if (distanceSqr + 0.25D < closestAnimalDistanceSqr) {
            closestAnimalDistanceSqr = distanceSqr;
            huntStalledTicks = 0;
        } else if (++huntStalledTicks >= HUNT_PATH_TIMEOUT_TICKS) {
            abortAction();
            return;
        }
        if (human.getNavigation().isDone() || actionTicks % 10 == 0) moveToAnimal();
    }

    private void tickResource() {
        if (targetPos == null || !LocalResourceScanner.matches(human.level().getBlockState(targetPos), need)) {
            if (targetPos != null) SurvivalClaimManager.releaseResource(human, targetPos);
            targetPos = null;
            mode = null;
            controller.fail(SurvivalFailureReason.TARGET_CHANGED, 10);
            nextDecisionTick = nextDecisionTick();
            return;
        }
        trackResourceProgress();
        if (controller.snapshot().state() == SurvivalState.ACQUIRE) controller.acquired();
        if (!SurvivalClaimManager.claimResource(human, targetPos)) {
            abortAction();
            controller.fail(SurvivalFailureReason.CLAIM_UNAVAILABLE, 10);
            return;
        }
        if (!equipMiningTool()) {
            abortAction();
            controller.fail(SurvivalFailureReason.TOOL_UNAVAILABLE, 10);
            return;
        }
        if (resourceNudgeTicks > 0 && !withinResourceMiningRange()) {
            continueResourceNudge();
            return;
        }
        resourceNudgeTarget = null;
        resourceNudgeTicks = 0;
        if (!withinResourceMiningRange()) {
            if (resourceNoProgressTicks >= RESOURCE_FORWARD_NUDGE_TICKS) {
                boolean nudged = nudgeTowardLookDirection();
                resourceNoProgressTicks = 0;
                if (nudged) return;
            }
            if (human.getNavigation().isDone()) {
                if (++navigationStalledTicks >= RESOURCE_PATH_TIMEOUT_TICKS) {
                    failedResourceUntil.put(targetPos.immutable(), human.tickCount + 150);
                    abortAction(); return;
                }
                if (navigationStalledTicks == 1 || actionTicks % 10 == 0) moveToTarget();
            } else navigationStalledTicks = 0;
            return;
        }
        human.getNavigation().stop();
        controller.ensureActing();
        human.getLookControl().setLookAt(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D);
        if (breaker == null) breaker = new ProgressiveBlockBreaker(human, targetPos);
        WorldActionResult result = breaker.tick();
        if (result != WorldActionResult.RUNNING) {
            SurvivalClaimManager.releaseResource(human, targetPos);
            SquadNeedsEvaluator.invalidate(human);
            breaker = null;
            targetPos = null;
            if (result == WorldActionResult.SUCCESS) {
                controller.completeTask();
                // Keep gathering the same resource need before hand-crafting
                // bootstrap materials or moving to a station. This lets a
                // vertical tree be harvested as one action instead of stopping
                // after the first log and leaving upper logs behind.
                resourceBatchNeed = need;
                resourceBatchActions++;
            } else {
                controller.fail(SurvivalFailureReason.TARGET_CHANGED, 10);
                mode = null;
                nextDecisionTick = nextDecisionTick();
            }
        }
    }

    /** Keep walking to the selected interaction cell instead of mining from the scan radius edge. */
    private boolean withinResourceMiningRange() {
        // The breaker validates reach from the actual entity eye. Do not use
        // the distance to a hypothetical interaction cell as a shortcut: that
        // can start the breaker one or two blocks too far away and turn a
        // normal navigation delay into a TARGET_CHANGED failure.
        return ProgressiveBlockBreaker.withinReach(human, targetPos);
    }

    private void tickStation() {
        if (targetPos == null || !(human.level().getBlockEntity(targetPos) instanceof AbstractFurnaceBlockEntity)) {
            if (targetPos != null) SurvivalClaimManager.releaseStation(human, targetPos);
            targetPos = null;
            mode = null;
            controller.fail(SurvivalFailureReason.STATION_UNAVAILABLE, 20);
            nextDecisionTick = nextDecisionTick();
            return;
        }
        controller.ensureActing();
        if (!SurvivalClaimManager.claimStation(human, targetPos)) {
            abortAction();
            controller.fail(SurvivalFailureReason.CLAIM_UNAVAILABLE, 20);
            return;
        }
        if (!withinStationInteractionRange(targetPos)) {
            stationInteractionCooldownTicks = FURNACE_INTERACTION_PACE_TICKS;
            if (human.getNavigation().isDone()) {
                if (++navigationStalledTicks >= STATION_PATH_TIMEOUT_TICKS) { abortAction(); return; }
                if (navigationStalledTicks == 1 || actionTicks % 10 == 0) moveToTarget();
            } else navigationStalledTicks = 0;
            return;
        }
        human.getNavigation().stop();
        controller.ensureActing();
        lookAtStation();
        if (stationInteractionCooldownTicks > 0) {
            stationInteractionCooldownTicks--;
            return;
        }
        FurnaceOperation.Result result = FurnaceOperation.tick(human, targetPos);
        if (result == FurnaceOperation.Result.WAITING) {
            if (controller.snapshot().state() == SurvivalState.ACT) controller.waitForWorld();
        } else {
            controller.ensureActing();
            SurvivalClaimManager.releaseStation(human, targetPos);
            targetPos = null;
            if (result == FurnaceOperation.Result.FAILED) {
                controller.fail(SurvivalFailureReason.STATION_UNAVAILABLE, 20);
                mode = null;
                nextDecisionTick = nextDecisionTick();
            } else {
                controller.completeTask();
            }
        }
    }

    private void tickCrafting() {
        if (targetPos == null || !human.level().getBlockState(targetPos).is(Blocks.CRAFTING_TABLE)) {
            if (targetPos != null) SurvivalClaimManager.releaseStation(human, targetPos);
            targetPos = null;
            mode = null;
            controller.fail(SurvivalFailureReason.STATION_UNAVAILABLE, 20);
            nextDecisionTick = nextDecisionTick();
            return;
        }
        controller.ensureActing();
        if (!SurvivalClaimManager.claimStation(human, targetPos)) {
            abortAction();
            controller.fail(SurvivalFailureReason.CLAIM_UNAVAILABLE, 20);
            return;
        }
        if (!withinStationInteractionRange(targetPos)) {
            stationInteractionCooldownTicks = STATION_ENTRY_DELAY_TICKS;
            if (human.getNavigation().isDone()) {
                if (++navigationStalledTicks >= STATION_PATH_TIMEOUT_TICKS) { abortAction(); return; }
                if (navigationStalledTicks == 1 || actionTicks % 10 == 0) moveToTarget();
            } else navigationStalledTicks = 0;
            return;
        }
        human.getNavigation().stop();
        controller.ensureActing();
        lookAtStation();
        if (stationInteractionCooldownTicks > 0) {
            stationInteractionCooldownTicks--;
            return;
        }
        if (craftNeededGear()) {
            // Keep the claim and the station look while the worker pauses
            // between individual pieces of gear.
            stationInteractionCooldownTicks = CRAFTING_PACE_TICKS;
            return;
        }
        SurvivalClaimManager.releaseStation(human, targetPos);
        targetPos = null;
        controller.completeTask();
    }

    /** Performs only hand-crafting/bootstrap preparation during planning. */
    private boolean prepareImmediateCraftingMaterials() {
        boolean changed = false;
        int shieldPlankReserve = reservedShieldPlanks();
        for (int craft = 0; craft < 6; craft++) {
            int planks = SurvivalInventory.count(human, stack -> stack.is(ItemTags.PLANKS));
            int sticks = SurvivalInventory.count(human, Items.STICK);
            boolean hasPickaxe = SurvivalInventory.contains(human, stack -> stack.getItem() instanceof PickaxeItem);
            // Reserve planks for the first pickaxe before overproducing sticks.
            int planksThreshold = hasPickaxe ? 8 : 3;
            int sticksThreshold = hasPickaxe ? 5 : 2;
            boolean step = planks < planksThreshold && SurvivalInventory.count(human, stack -> stack.is(ItemTags.LOGS)) > 0
                    && SurvivalRecipeService.craft(human, stack -> stack.is(ItemTags.PLANKS), false).isPresent();
            if (!step) {
                boolean canMakeSticks = hasPickaxe ? planks > 0 : planks > 2;
                step = sticks < sticksThreshold && canMakeSticks
                        // A shield is mandatory once iron progression starts.
                        // Never turn its six planks into sticks while preparing
                        // the next tool.
                        && planks - 2 >= shieldPlankReserve
                        && SurvivalRecipeService.craft(human, stack -> stack.is(Items.STICK), false).isPresent();
            }
            boolean knownTable = hasNearbyStation(Blocks.CRAFTING_TABLE)
                    || findStation(Blocks.CRAFTING_TABLE).isPresent();
            if (!knownTable && SurvivalInventory.count(human, Items.CRAFTING_TABLE) == 0)
                step |= SurvivalRecipeService.craft(human, stack -> stack.is(Items.CRAFTING_TABLE), false).isPresent();
            if (!knownTable && placeStation(Items.CRAFTING_TABLE, Blocks.CRAFTING_TABLE)) step = true;
            if (!step) break;
            changed = true;
        }
        if (changed && controller.snapshot().state() == SurvivalState.PLANNING) {
            controller.completeImmediate(new SurvivalIntent(SurvivalTask.CRAFT, SurvivalObjective.IRON_GEAR));
        }
        return changed;
    }

    private boolean planBatchedResource(SquadNeeds needs) {
        if (resourceBatchNeed == null) return false;
        if (resourceBatchActions >= MAX_WOOD_BATCH_ACTIONS) {
            resourceBatchNeed = null;
            return false;
        }

        // The first block in a vertical tree can hide the next one from the
        // path probe. Select the nearest exposed block here and let the normal
        // resource tick validate its interaction cell and recover on failure.
        Optional<LocalResourceScanner.ResourceTarget> resource = LocalResourceScanner.findReachableFirst(
                human, List.of(resourceBatchNeed),
                pos -> failedResourceUntil.getOrDefault(pos, 0) > human.tickCount);
        if (resource.isEmpty()) {
            // The batch is complete for the currently reachable area. Normal
            // planning may now craft with the materials that were collected.
            resourceBatchNeed = null;
            return false;
        }

        LocalResourceScanner.ResourceTarget selected = resource.get();
        if (!SurvivalClaimManager.claimResource(human, selected.pos())) {
            return false;
        }
        need = selected.need();
        targetPos = selected.pos();
        mode = Mode.RESOURCE;
        // A batch contains several bounded block actions. Reset the per-block
        // navigation timeout so the goal cannot expire halfway through a tree
        // simply because the first log consumed the whole action budget.
        actionTicks = 0;
        navigationStalledTicks = 0;
        resetResourceProgress();
        controller.plan(new SurvivalIntent(SurvivalTask.GATHER, objectiveFor(selected.need())));
        return true;
    }

    private boolean craftNeededGear() {
        if (SurvivalRecipeService.craft(human, stack -> stack.getItem() instanceof PickaxeItem
                && allowedToolUpgrade(stack, PickaxeItem.class), true).isPresent()) return true;
        // Diamond combat gear is more useful than an optional axe. Keep the
        // pickaxe first, then the sword and every available diamond armor
        // piece, before spending the reserve on a diamond axe.
        if (SurvivalRecipeService.craft(human, stack -> stack.getItem() instanceof SwordItem
                && allowedToolUpgrade(stack, SwordItem.class), true).isPresent()) return true;
        if (SurvivalRecipeService.craft(human, this::allowedDiamondArmor, true).isPresent()) return true;
        // The shield is mandatory combat gear and remains ahead of optional
        // axes whenever its recipe is available.
        if (!SurvivalInventory.contains(human, stack -> stack.is(Items.SHIELD))
                && SurvivalRecipeService.craft(human, stack -> stack.is(Items.SHIELD), true).isPresent()) return true;
        if (SurvivalRecipeService.craft(human, stack -> stack.getItem() instanceof AxeItem
                && allowedToolUpgrade(stack, AxeItem.class), true).isPresent()) return true;
        // Iron armor is an opportunistic upgrade. Keep the iron needed by the
        // mandatory iron pickaxe and shield before spending any on armor.
        if (SurvivalInventory.count(human, Items.IRON_INGOT) > mandatoryIronReserve()
                && SurvivalRecipeService.craft(human, stack -> stack.getItem() instanceof net.minecraft.world.item.ArmorItem
                && allowedArmorUpgrade(stack)
                && GearUpgradePolicy.usefulUpgrade(human, stack), true).isPresent()) return true;
        return (SurvivalInventory.count(human, Items.IRON_INGOT) > mandatoryIronReserve()
                || SurvivalInventory.count(human, Items.DIAMOND) >= 4)
                && SurvivalRecipeService.craft(human, stack -> stack.getItem() instanceof net.minecraft.world.item.ArmorItem
                && allowedArmorUpgrade(stack)
                && GearUpgradePolicy.usefulUpgrade(human, stack), true).isPresent();
    }

    private boolean allowedDiamondArmor(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor
                && armor.getMaterial() == net.minecraft.world.item.ArmorMaterials.DIAMOND
                && allowedArmorUpgrade(stack)
                && GearUpgradePolicy.usefulUpgrade(human, stack);
    }

    private int mandatoryIronReserve() {
        int reserve = 0;
        if (!SurvivalInventory.contains(human, stack -> stack.getItem() instanceof PickaxeItem
                && stack.getItem() instanceof net.minecraft.world.item.TieredItem tiered
                && tiered.getTier().getLevel() >= 2)) reserve += 3;
        if (!SurvivalInventory.contains(human, stack -> stack.is(Items.SHIELD))) reserve += 1;
        return reserve;
    }

    private boolean allowedToolUpgrade(ItemStack candidate, Class<?> type) {
        if (!(candidate.getItem() instanceof net.minecraft.world.item.TieredItem tiered)) return false;
        int level = tiered.getTier().getLevel();
        // A tiered item is unique progression gear. Do not craft another copy
        // when an equal-or-better one is already in storage or equipped,
        // including a damaged tool that is still usable.
        if (hasToolAtLeast(type, level)
                || SurvivalInventory.contains(human, stack -> stack.is(candidate.getItem()))) return false;
        if (type == PickaxeItem.class) {
            if (level >= 3) return hasIronPick();
            if (level == 2) return hasStoneUsefulTools();
            return true;
        }
        if (level <= 1) return true;
        return level >= 3 && hasIronPick();
    }

    private boolean hasToolAtLeast(Class<?> type, int level) {
        return SurvivalInventory.contains(human, stack -> type.isInstance(stack.getItem())
                && stack.getItem() instanceof net.minecraft.world.item.TieredItem tiered
                && tiered.getTier().getLevel() >= level);
    }

    private int reservedShieldPlanks() {
        boolean hasShieldIron = SurvivalInventory.contains(human,
                stack -> stack.is(Items.IRON_INGOT) || stack.is(Items.RAW_IRON));
        // The iron pickaxe itself needs two sticks. Reserve shield planks only
        // after that prerequisite exists; reserving them earlier can leave a
        // four-plank worker unable to craft the sticks needed for the pickaxe.
        return !SurvivalInventory.contains(human, stack -> stack.is(Items.SHIELD))
                && hasIronPick() && hasShieldIron ? 6 : 0;
    }

    private boolean hasIronPick() {
        // Diamond gear only needs the iron pick that harvested its materials.
        // The shield remains a separate combat progression need and must not
        // block crafting tools or armor once diamonds are available.
        return SurvivalInventory.contains(human, stack -> stack.getItem() instanceof PickaxeItem
                        && stack.getItem() instanceof net.minecraft.world.item.TieredItem tiered
                        && tiered.getTier().getLevel() >= 2);
    }

    private boolean hasStoneUsefulTools() {
        return SurvivalInventory.contains(human, stack -> stack.getItem() instanceof PickaxeItem
                        && stack.getItem() instanceof net.minecraft.world.item.TieredItem tiered
                        && tiered.getTier().getLevel() >= 1)
                && SurvivalInventory.contains(human, stack -> stack.getItem() instanceof AxeItem
                        && stack.getItem() instanceof net.minecraft.world.item.TieredItem tiered
                        && tiered.getTier().getLevel() >= 1)
                && SurvivalInventory.contains(human, stack -> stack.getItem() instanceof SwordItem
                        && stack.getItem() instanceof net.minecraft.world.item.TieredItem tiered
                        && tiered.getTier().getLevel() >= 1);
    }

    private boolean allowedArmorUpgrade(ItemStack candidate) {
        if (!(candidate.getItem() instanceof net.minecraft.world.item.ArmorItem armor)) return false;
        if (armor.getMaterial() == net.minecraft.world.item.ArmorMaterials.DIAMOND) return hasIronPick();
        return armor.getMaterial() == net.minecraft.world.item.ArmorMaterials.IRON;
    }

    private boolean tryShare(SquadNeeds needs) {
        for (Human member : SquadManager.nearbyMembers(human)) {
            if (SurvivalInventory.count(human, stack -> stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS)) < 4
                    && SquadMaterialSharing.transfer(member, human,
                    stack -> stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS), 4, 4) > 0) return true;
            if (SurvivalInventory.count(human, stack -> stack.getFoodProperties(human) != null) < 4
                    && SquadMaterialSharing.transfer(member, human,
                    stack -> stack.getFoodProperties(member) != null, 4, 4) > 0) return true;
            if (needs.needs(SquadNeed.FUEL) && SquadMaterialSharing.transfer(member, human,
                    stack -> stack.is(Items.COAL) || stack.is(Items.CHARCOAL), 2, 1) > 0) return true;
            if (!SurvivalInventory.contains(human, stack -> stack.getItem() instanceof PickaxeItem)
                    && SquadMaterialSharing.transfer(member, human,
                    stack -> stack.is(Items.IRON_INGOT) || stack.is(Items.DIAMOND), 3, 3) > 0) return true;
        }
        return false;
    }

    private boolean selectAnimal() {
        // Never start a hunt with a pickaxe or empty hand. The combat goal may
        // otherwise inherit the current mining tool and leave the human
        // staring at the animal drops after the first hit.
        if (!equipHuntingWeapon()) return false;
        AABB area = human.getBoundingBox().inflate(Config.resourceScanRadius.get());
        List<net.minecraft.world.entity.animal.Animal> animals = human.level().getEntitiesOfClass(
                net.minecraft.world.entity.animal.Animal.class, area,
                candidate -> candidate instanceof Cow || candidate instanceof Pig || candidate instanceof Sheep || candidate instanceof Chicken);
        animal = animals.stream().min(Comparator.comparingDouble(human::distanceToSqr)).orElse(null);
        if (animal == null) return false;
        if (!SurvivalQueryBudget.tryPath(human)) {
            animal = null;
            return false;
        }
        var path = human.getNavigation().createPath(animal, 1);
        if (path == null || !path.canReach()) {
            animal = null;
            return false;
        }
        mode = Mode.HUNT;
        return true;
    }

    private boolean equipHuntingWeapon() {
        if (human.getMainHandItem().getItem() instanceof SwordItem) {
            human.preserveActionWeaponSelection();
            return true;
        }
        if (human.getData() == null) return false;
        for (int slot = 0; slot < human.getData().getInventoryItemsSize(); slot++) {
            ItemStack candidate = human.getData().getInventoryItem(slot);
            if (!(candidate.getItem() instanceof SwordItem)) continue;
            ItemStack previous = human.getMainHandItem().copy();
            human.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, candidate.copy());
            human.getData().setInventoryItem(slot, previous);
            human.preserveActionWeaponSelection();
            return true;
        }
        return false;
    }

    private boolean hasNearbyPriorityResource() {
        SquadNeeds needs = SquadNeedsEvaluator.evaluate(human);
        List<SquadNeed> priorityNeeds = List.of(SquadNeed.FUEL, SquadNeed.IRON, SquadNeed.GOLD, SquadNeed.DIAMOND)
                .stream().filter(needs::needs).toList();
        return LocalResourceScanner.findReachableFirst(human, priorityNeeds,
                pos -> failedResourceUntil.getOrDefault(pos, 0) > human.tickCount).isPresent();
    }

    private boolean selectFurnace(SquadNeeds needs) {
        int smeltable = SurvivalInventory.count(human, stack -> stack.is(Items.RAW_IRON) || stack.is(Items.RAW_GOLD)
                || stack.is(Items.BEEF) || stack.is(Items.PORKCHOP) || stack.is(Items.CHICKEN) || stack.is(Items.MUTTON)
                || stack.is(Items.RABBIT) || stack.is(Items.COD) || stack.is(Items.SALMON));
        Optional<BlockPos> furnace = findStation(Blocks.FURNACE);
        if (furnace.isPresent() && human.level().getBlockEntity(furnace.get()) instanceof AbstractFurnaceBlockEntity furnaceEntity) {
            if (!furnaceEntity.getItem(2).isEmpty()) {
                // The input has already been consumed, so smeltable may be
                // zero. The output still belongs to this survival action.
            } else if (!furnaceEntity.getItem(0).isEmpty()) {
                // A batch is already cooking and there is nothing to retrieve.
                // Do not claim MOVE just to stand beside the furnace.
                return false;
            } else if (smeltable == 0 || !FurnaceOperation.hasAvailableFuel(human)) {
                return false;
            }
        } else if (smeltable == 0 || !FurnaceOperation.hasAvailableFuel(human)) {
            return false;
        }
        if (furnace.isEmpty() && SurvivalInventory.count(human, Items.FURNACE) == 0) {
            boolean table = findStation(Blocks.CRAFTING_TABLE).isPresent();
            if (table) SurvivalRecipeService.craft(human, stack -> stack.is(Items.FURNACE), true);
        }
        if (furnace.isEmpty() && placeStation(Items.FURNACE, Blocks.FURNACE)) return false;
        furnace = findStation(Blocks.FURNACE);
        if (furnace.isEmpty() || !SurvivalClaimManager.claimStation(human, furnace.get())) return false;
        targetPos = furnace.get();
        mode = Mode.STATION;
        return true;
    }

    private boolean selectCraftingTable() {
        if (!canCraftNeededGear()) return false;
        Optional<BlockPos> table = findStation(Blocks.CRAFTING_TABLE);
        if (table.isEmpty() || !SurvivalClaimManager.claimStation(human, table.get())) return false;
        targetPos = table.get();
        mode = Mode.CRAFTING;
        return true;
    }

    private boolean canCraftNeededGear() {
        if (SurvivalRecipeService.canCraft(human, stack -> stack.getItem() instanceof PickaxeItem
                && allowedToolUpgrade(stack, PickaxeItem.class), true)) return true;
        if (SurvivalRecipeService.canCraft(human, stack -> stack.getItem() instanceof SwordItem
                && allowedToolUpgrade(stack, SwordItem.class), true)) return true;
        if (SurvivalRecipeService.canCraft(human, this::allowedDiamondArmor, true)) return true;
        if (!SurvivalInventory.contains(human, stack -> stack.is(Items.SHIELD))
                && SurvivalRecipeService.canCraft(human, stack -> stack.is(Items.SHIELD), true)) return true;
        if (SurvivalRecipeService.canCraft(human, stack -> stack.getItem() instanceof AxeItem
                && allowedToolUpgrade(stack, AxeItem.class), true)) return true;
        if (SurvivalInventory.count(human, Items.IRON_INGOT) > mandatoryIronReserve()
                && SurvivalRecipeService.canCraft(human, stack -> stack.getItem() instanceof net.minecraft.world.item.ArmorItem
                && allowedArmorUpgrade(stack)
                && GearUpgradePolicy.usefulUpgrade(human, stack), true)) return true;
        return (SurvivalInventory.count(human, Items.IRON_INGOT) > mandatoryIronReserve()
                || SurvivalInventory.count(human, Items.DIAMOND) >= 4)
                && SurvivalRecipeService.canCraft(human, stack -> stack.getItem() instanceof net.minecraft.world.item.ArmorItem
                && allowedArmorUpgrade(stack)
                && GearUpgradePolicy.usefulUpgrade(human, stack), true);
    }

    private Optional<BlockPos> findStation(net.minecraft.world.level.block.Block block) {
        if (stationCacheTick != human.tickCount) {
            stationCache.clear();
            nearbyStationCache.clear();
            stationCacheTick = human.tickCount;
        }
        Optional<BlockPos> cached = stationCache.get(block);
        if (cached != null) return cached;
        int radius = Config.resourceScanRadius.get();
        BlockPos origin = human.blockPosition();
        Optional<BlockPos> result = BlockPos.betweenClosedStream(origin.offset(-radius, -4, -radius), origin.offset(radius, 4, radius))
                .filter(pos -> human.level().getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null)
                .filter(pos -> human.level().getBlockState(pos).is(block))
                .filter(pos -> !SurvivalClaimManager.stationClaimedByOther(human, pos))
                .filter(pos -> stationReachable(pos))
                .map(BlockPos::immutable)
                .min(Comparator.comparingDouble(pos -> pos.distSqr(origin)));
        stationCache.put(block, result);
        return result;
    }

    private boolean hasNearbyStation(net.minecraft.world.level.block.Block block) {
        if (stationCacheTick != human.tickCount) {
            stationCache.clear();
            nearbyStationCache.clear();
            stationCacheTick = human.tickCount;
        }
        Boolean cached = nearbyStationCache.get(block);
        if (cached != null) return cached;
        int radius = Config.resourceScanRadius.get();
        BlockPos origin = human.blockPosition();
        boolean result = BlockPos.betweenClosedStream(origin.offset(-radius, -4, -radius), origin.offset(radius, 4, radius))
                .filter(pos -> human.level().getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null)
                .filter(pos -> human.level().getBlockState(pos).is(block))
                .filter(pos -> !SurvivalClaimManager.stationClaimedByOther(human, pos))
                .anyMatch(this::withinStationInteractionRange);
        nearbyStationCache.put(block, result);
        return result;
    }

    private boolean stationReachable(BlockPos station) {
        if (withinStationInteractionRange(station)) return true;
        return SurvivalPathing.createPath(human, stationInteractionPositions(station), 0).isPresent();
    }

    private boolean withinStationInteractionRange(BlockPos station) {
        return stationInteractionPositions(station).stream().anyMatch(interaction ->
                human.distanceToSqr(interaction.getX() + 0.5D, interaction.getY(), interaction.getZ() + 0.5D)
                        <= RESOURCE_INTERACTION_TOLERANCE_SQR);
    }

    private List<BlockPos> stationInteractionPositions(BlockPos station) {
        return BlockPos.betweenClosedStream(station.offset(-1, -1, -1), station.offset(1, 1, 1))
                .filter(pos -> Math.abs(pos.getX() - station.getX()) + Math.abs(pos.getZ() - station.getZ()) == 1)
                .filter(pos -> human.level().getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null)
                .filter(pos -> human.level().getBlockState(pos).getCollisionShape(human.level(), pos).isEmpty())
                .filter(pos -> human.level().getBlockState(pos.above()).getCollisionShape(human.level(), pos.above()).isEmpty())
                .filter(pos -> !human.level().getBlockState(pos.below())
                        .getCollisionShape(human.level(), pos.below()).isEmpty())
                .map(BlockPos::immutable)
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(human.blockPosition())))
                .toList();
    }

    private boolean placeStation(net.minecraft.world.item.Item item, net.minecraft.world.level.block.Block block) {
        if (!WorldActionSupport.permitted(human)) return false;
        Optional<ItemStack> station = WorldActionSupport.findInventoryStack(human, stack -> stack.is(item));
        if (station.isEmpty()) return false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos pos = human.blockPosition().relative(direction);
            if (WorldActionSupport.place(human, pos, station.get(), block.defaultBlockState(), direction.getOpposite())) {
                stationCache.clear();
                SquadNeedsEvaluator.invalidate(human);
                return true;
            }
        }
        return false;
    }

    private boolean selectExplorationTarget() {
        int radius = Config.explorationRadius.get();
        for (int attempt = 0; attempt < 24; attempt++) {
            int x = Mth.nextInt(human.getRandom(), -radius, radius);
            int z = Mth.nextInt(human.getRandom(), -radius, radius);
            int y = human.blockPosition().getY() + Mth.nextInt(human.getRandom(), -4, 4);
            BlockPos candidate = new BlockPos(human.blockPosition().getX() + x, y, human.blockPosition().getZ() + z);
            if (Math.abs(x) + Math.abs(z) + Math.abs(y - human.blockPosition().getY()) <= 2
                    || !human.level().hasChunkAt(candidate)) continue;
            if (!SurvivalQueryBudget.tryPath(human)) break;
            var path = human.getNavigation().createPath(candidate, 2);
            if (path == null || !path.canReach()) continue;
            targetPos = candidate;
            mode = Mode.EXPLORE;
            return true;
        }
        // An empty decision must not claim MOVE. Let lower-priority vanilla
        // strolling continue when no useful exploration route exists.
        return false;
    }

    private void moveToTarget() {
        if (targetPos == null) return;
        if (mode == Mode.RESOURCE) {
            List<BlockPos> interactions = LocalResourceScanner.interactionPositions(human, targetPos, need == SquadNeed.WOOD);
            if (!interactions.isEmpty()) SurvivalPathing.moveTo(human, interactions, 0, 0.8D);
            return;
        }
        if (mode == Mode.STATION || mode == Mode.CRAFTING) {
            SurvivalPathing.moveTo(human, stationInteractionPositions(targetPos), 0, 0.8D);
            return;
        }
        SurvivalPathing.moveTo(human, List.of(targetPos), 2, 0.8D);
    }

    private void moveToAnimal() {
        if (animal != null) human.getNavigation().moveTo(animal, 1.0D);
    }

    private void abortAction() {
        if (targetPos != null) {
            SurvivalClaimManager.releaseResource(human, targetPos);
            SurvivalClaimManager.releaseStation(human, targetPos);
        }
        clearOwnedMovementRequest();
        targetPos = null;
        mode = null;
        resetResourceProgress();
        stationInteractionCooldownTicks = 0;
        nextDecisionTick = nextDecisionTick();
    }

    private boolean equipMiningTool() {
        if (targetPos == null) return false;
        return MiningToolSelector.select(human, human.level().getBlockState(targetPos))
                .map(selected -> MiningToolSelector.equip(human, selected))
                .orElse(false);
    }

    private void lookAtStation() {
        if (targetPos == null) return;
        human.getLookControl().setLookAt(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D,
                targetPos.getZ() + 0.5D, 30.0F, 30.0F);
    }

    private boolean hasNearbyUsefulLoot() {
        AABB area = human.getBoundingBox().inflate(12.0D, 6.0D, 12.0D);
        return human.level().getEntitiesOfClass(ItemEntity.class, area,
                        item -> item.isAlive()
                                && HumanLootPolicy.isUseful(human, item.getItem())
                                && SurvivalInventory.canStore(human, item.getItem()))
                .stream().findAny().isPresent();
    }

    private boolean resumableAction() {
        SurvivalState state = controller.snapshot().state();
        return mode != null && mode != Mode.EXPLORE
                && (targetPos != null || animal != null)
                && actionTicks < (mode == Mode.CRAFTING ? CRAFTING_ACTION_TIMEOUT_TICKS : 160)
                && state != SurvivalState.DORMANT
                && state != SurvivalState.BACKOFF
                && state != SurvivalState.PLANNING;
    }

    private void clearAction() {
        targetPos = null;
        animal = null;
        mode = null;
        need = null;
        resetResourceProgress();
        stationInteractionCooldownTicks = 0;
    }

    private void resetResourceProgress() {
        resourceProgressTarget = null;
        lastResourcePosition = null;
        resourceNoProgressTicks = 0;
        resourceNudgeTarget = null;
        resourceNudgeTicks = 0;
    }

    private void clearOwnedMovementRequest() {
        human.getNavigation().stop();
        if (human.getMoveControl() instanceof HumanEntityWalkControl moveControl) {
            if (moveControl.isSurvivalNudgeActive()) moveControl.cancelSurvivalNudge();
            else moveControl.stopMovement();
        }
        resourceNudgeTarget = null;
        resourceNudgeTicks = 0;
    }

    private boolean combatMovementActive() {
        return human.getTarget() != null || human.isFleeing || human.toAvoid != null
                || human.healingAfterFleeTicks > 0 || human.isUnderMeleePressure();
    }

    private void trackResourceProgress() {
        if (targetPos == null || !targetPos.equals(resourceProgressTarget)) {
            resourceProgressTarget = targetPos == null ? null : targetPos.immutable();
            lastResourcePosition = human.position();
            resourceNoProgressTicks = 0;
            return;
        }

        Vec3 current = human.position();
        if (lastResourcePosition == null
                || current.distanceToSqr(lastResourcePosition) > RESOURCE_MOVEMENT_EPSILON_SQR) {
            resourceNoProgressTicks = 0;
        } else {
            resourceNoProgressTicks++;
        }
        lastResourcePosition = current;
    }

    /** Gives a stalled resource action a bounded physics-safe advance before timeout recovery. */
    private boolean nudgeTowardLookDirection() {
        if (!canUseResourceNudge()) return false;
        Vec3 look = human.getViewVector(1.0F);
        double dx = look.x;
        double dz = look.z;
        double horizontalLengthSqr = dx * dx + dz * dz;
        if (horizontalLengthSqr < 1.0E-4D && targetPos != null) {
            dx = targetPos.getX() + 0.5D - human.getX();
            dz = targetPos.getZ() + 0.5D - human.getZ();
            horizontalLengthSqr = dx * dx + dz * dz;
        }
        if (horizontalLengthSqr < 1.0E-4D) return false;

        double scale = RESOURCE_FORWARD_NUDGE_DISTANCE / Math.sqrt(horizontalLengthSqr);
        resourceNudgeTarget = new Vec3(
                human.getX() + dx * scale,
                human.getY(),
                human.getZ() + dz * scale);
        resourceNudgeTicks = RESOURCE_FORWARD_NUDGE_DURATION_TICKS;
        continueResourceNudge();
        return true;
    }

    private void continueResourceNudge() {
        if (resourceNudgeTarget == null || resourceNudgeTicks <= 0) return;
        if (!canUseResourceNudge()) {
            clearOwnedMovementRequest();
            return;
        }
        human.getNavigation().stop();
        if (human.getMoveControl() instanceof HumanEntityWalkControl moveControl) {
            moveControl.setSurvivalNudgeWantedPosition(
                    resourceNudgeTarget.x,
                    resourceNudgeTarget.y,
                    resourceNudgeTarget.z,
                    RESOURCE_FORWARD_NUDGE_SPEED);
        }
        resourceNudgeTicks--;
    }

    private boolean canUseResourceNudge() {
        return mode == Mode.RESOURCE && targetPos != null && breaker == null
                && activeActionEligible() && !combatMovementActive()
                && LocalResourceScanner.matches(human.level().getBlockState(targetPos), need);
    }

    private int nextDecisionTick() {
        return human.tickCount + 5 + Math.floorMod(human.getUUID().hashCode(), 5);
    }

    private boolean eligible() {
        return activeActionEligible() && !human.isInvestigatingSound();
    }

    private boolean activeActionEligible() {
        return Config.enableSurvivalProgression.get() && !human.level().isClientSide && human.isAlive()
                && human.getTarget() == null && !WorldActionSupport.critical(human)
                && !human.hasFreshSquadThreatMemory();
    }

    /** Publishes bounded diagnostics for explicitly tagged debug workers. */
    public void publishDebugState() {
        if (human.level().isClientSide) return;
        if (!human.getTags().contains(DEBUG_TAG)) {
            restoreDebugName();
            return;
        }

        SurvivalSnapshot snapshot = controller.snapshot();
        String target = targetPos == null ? "-" : targetPos.toShortString();
        String intent = snapshot.intent() == null ? "-"
                : snapshot.intent().task() + "/" + snapshot.intent().objective();
        String signature = snapshot.state() + "|" + intent + "|" + mode + "|" + need + "|" + target
                + "|" + snapshot.failureReason() + "|" + snapshot.retryAtTick();
        boolean transition = !signature.equals(lastDebugSignature);
        boolean heartbeat = human.tickCount >= nextDebugHeartbeatTick;

        if (transition) {
            applyDebugName(debugLabel(snapshot, intent, target));
            lastDebugSignature = signature;
        }
        if (transition || heartbeat) {
            SquadNeeds needs = SquadNeedsEvaluator.calculate(List.of(human));
            HostileHumans.LOGGER.info(
                    "[SurvivalTrace] event={} entity={} uuid={} tick={} pos={} state={} intent={} mode={} need={} target={} failure={} retryAt={} actionTicks={} navDone={} investigating={} threat={} hand={} inventory={} needs={}",
                    transition ? "transition" : "heartbeat", human.getId(), human.getUUID(), human.tickCount,
                    human.blockPosition().toShortString(), snapshot.state(), intent, mode, need, target,
                    snapshot.failureReason(), snapshot.retryAtTick(), actionTicks, human.getNavigation().isDone(),
                    human.isInvestigatingSound(), human.hasFreshSquadThreatMemory(), debugStack(human.getMainHandItem()),
                    debugInventory(), needs.deficits());
            nextDebugHeartbeatTick = human.tickCount + DEBUG_HEARTBEAT_TICKS;
        }
    }

    private String debugLabel(SurvivalSnapshot snapshot, String intent, String target) {
        StringBuilder label = new StringBuilder("[HH ").append(snapshot.state()).append("] ");
        label.append(snapshot.intent() == null ? "IDLE" : intent);
        if (need != null) label.append(" need=").append(need);
        if (targetPos != null) label.append(" -> ").append(target);
        if (snapshot.failureReason() != null) label.append(" !").append(snapshot.failureReason());
        return label.toString();
    }

    private void applyDebugName(String label) {
        if (!debugNameApplied) {
            Component current = human.getCustomName();
            if (current != null) {
                String text = current.getString();
                int previousStatus = text.indexOf(" | [HH ");
                debugOriginalName = Component.literal(previousStatus < 0 ? text : text.substring(0, previousStatus));
            }
            debugOriginalNameVisible = human.isCustomNameVisible();
            debugNameApplied = true;
        }
        String prefix = debugOriginalName == null ? "" : debugOriginalName.getString() + " | ";
        human.setCustomName(Component.literal(prefix + label));
        human.setCustomNameVisible(true);
    }

    private void restoreDebugName() {
        if (!debugNameApplied) return;
        human.setCustomName(debugOriginalName);
        human.setCustomNameVisible(debugOriginalNameVisible);
        debugOriginalName = null;
        debugNameApplied = false;
        lastDebugSignature = null;
        nextDebugHeartbeatTick = 0;
    }

    private String debugInventory() {
        List<String> items = new ArrayList<>();
        if (human.getData() != null) {
            for (int slot = 0; slot < human.getData().getInventoryItemsSize(); slot++) {
                ItemStack stack = human.getData().getInventoryItem(slot);
                if (!stack.isEmpty()) items.add(slot + ":" + debugStack(stack));
            }
        }
        return items.toString();
    }

    private static String debugStack(ItemStack stack) {
        if (stack.isEmpty()) return "empty";
        return stack.getCount() + "x" + BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

}
