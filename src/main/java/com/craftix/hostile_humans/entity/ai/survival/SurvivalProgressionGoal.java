package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.HostileHumans;
import com.craftix.hostile_humans.entity.ai.action.WorldActionResult;
import com.craftix.hostile_humans.entity.ai.action.WorldActionSupport;
import com.craftix.hostile_humans.entity.ai.squad.SquadManager;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.AABB;

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
    private static final double RESOURCE_INTERACTION_TOLERANCE_SQR = 2.25D;

    private final Human human;
    private final SurvivalController controller;
    private SquadNeed need;
    private Mode mode;
    private BlockPos targetPos;
    private Entity animal;
    private ProgressiveBlockBreaker breaker;
    private int actionTicks;
    private int huntStalledTicks;
    private int huntAttackCooldown;
    private double closestAnimalDistanceSqr;
    private int nextDecisionTick;
    private int navigationStalledTicks;
    private final Map<BlockPos, Integer> failedResourceUntil = new HashMap<>();
    private final Map<net.minecraft.world.level.block.Block, Optional<BlockPos>> stationCache = new HashMap<>();
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
        if (!eligible() || human.tickCount < nextDecisionTick || !controller.requestAssessment()) return false;
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
        SquadNeeds needs = SquadNeedsEvaluator.evaluate(human);
        if (tryShare(needs)) {
            controller.completeImmediate(new SurvivalIntent(SurvivalTask.SHARE, SurvivalObjective.FOOD));
            return false;
        }
        if (tryImmediateCraft()) return false;

        List<SquadNeed> neededResources = Arrays.stream(SquadNeed.values()).filter(needs::needs).toList();
        Optional<LocalResourceScanner.ResourceTarget> resource = LocalResourceScanner.findReachableFirst(human, neededResources,
                pos -> failedResourceUntil.getOrDefault(pos, 0) > human.tickCount);
        if (resource.isPresent()) {
            LocalResourceScanner.ResourceTarget selected = resource.get();
            if (SurvivalClaimManager.claimResource(human, selected.pos())) {
                need = selected.need();
                targetPos = selected.pos();
                mode = Mode.RESOURCE;
                controller.plan(new SurvivalIntent(SurvivalTask.GATHER, objectiveFor(selected.need())));
                return true;
            }
        }
        if (needs.needs(SquadNeed.FOOD) && selectAnimal()) {
            controller.plan(new SurvivalIntent(SurvivalTask.HUNT, SurvivalObjective.FOOD));
            return true;
        }
        if (selectFurnace(needs)) {
            controller.plan(new SurvivalIntent(SurvivalTask.SMELT, SurvivalObjective.IRON_GEAR));
            return true;
        }
        if (selectCraftingTable()) {
            controller.plan(new SurvivalIntent(SurvivalTask.CRAFT, SurvivalObjective.IRON_GEAR));
            return true;
        }
        need = needs.highestPriority().orElse(null);
        if (need == null || !selectExplorationTarget()) return false;
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
        boolean actionEligible = mode == Mode.EXPLORE ? eligible() : activeActionEligible();
        return actionEligible && actionTicks < (mode == Mode.EXPLORE ? EXPLORE_TIMEOUT_TICKS : 160);
    }

    @Override
    public void start() {
        actionTicks = 0;
        navigationStalledTicks = 0;
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
        if (breaker != null) breaker.abort();
        if (targetPos != null) {
            SurvivalClaimManager.releaseResource(human, targetPos);
            SurvivalClaimManager.releaseStation(human, targetPos);
        }
        human.getNavigation().stop();
        breaker = null;
        targetPos = null;
        animal = null;
        mode = null;
        need = null;
        controller.stop(SurvivalFailureReason.INTERRUPTED);
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
        if (controller.snapshot().state() == SurvivalState.ACQUIRE) controller.acquired();
        SurvivalClaimManager.claimResource(human, targetPos);
        if (!withinResourceMiningRange()) {
            if (human.getNavigation().isDone()) {
                if (++navigationStalledTicks >= RESOURCE_PATH_TIMEOUT_TICKS) {
                    failedResourceUntil.put(targetPos.immutable(), human.tickCount + 150);
                    abortAction(); return;
                }
                moveToTarget();
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
                // Keep the goal alive in PLANNING so the next resource is
                // selected by the same controller on the next server tick.
            } else {
                controller.fail(SurvivalFailureReason.TARGET_CHANGED, 10);
                mode = null;
                nextDecisionTick = nextDecisionTick();
            }
        }
    }

    /** Keep walking to the selected interaction cell instead of mining from the scan radius edge. */
    private boolean withinResourceMiningRange() {
        if (!ProgressiveBlockBreaker.withinReach(human, targetPos)) {
            return false;
        }
        return LocalResourceScanner.interactionPosition(human, targetPos)
                .map(interaction -> human.distanceToSqr(interaction.getX() + 0.5D,
                        interaction.getY(), interaction.getZ() + 0.5D) <= RESOURCE_INTERACTION_TOLERANCE_SQR)
                .orElse(true);
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
        SurvivalClaimManager.claimStation(human, targetPos);
        if (human.distanceToSqr(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D) > 25.0D) {
            if (human.getNavigation().isDone()) {
                if (++navigationStalledTicks >= STATION_PATH_TIMEOUT_TICKS) { abortAction(); return; }
                moveToTarget();
            } else navigationStalledTicks = 0;
            return;
        }
        human.getNavigation().stop();
        controller.ensureActing();
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
        SurvivalClaimManager.claimStation(human, targetPos);
        if (human.distanceToSqr(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D) > 25.0D) {
            if (human.getNavigation().isDone()) {
                if (++navigationStalledTicks >= STATION_PATH_TIMEOUT_TICKS) { abortAction(); return; }
                moveToTarget();
            } else navigationStalledTicks = 0;
            return;
        }
        human.getNavigation().stop();
        controller.ensureActing();
        tryImmediateCraft();
        SurvivalClaimManager.releaseStation(human, targetPos);
        targetPos = null;
        controller.completeTask();
    }

    private boolean tryImmediateCraft() {
        boolean changed = false;
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
                        && SurvivalRecipeService.craft(human, stack -> stack.is(Items.STICK), false).isPresent();
            }
            boolean table = hasNearbyStation(Blocks.CRAFTING_TABLE);
            boolean knownTable = table || findStation(Blocks.CRAFTING_TABLE).isPresent();
            if (!knownTable && SurvivalInventory.count(human, Items.CRAFTING_TABLE) == 0)
                step |= SurvivalRecipeService.craft(human, stack -> stack.is(Items.CRAFTING_TABLE), false).isPresent();
            if (!knownTable && placeStation(Items.CRAFTING_TABLE, Blocks.CRAFTING_TABLE)) step = true;
            table = hasNearbyStation(Blocks.CRAFTING_TABLE);
            if (table) step |= craftNeededGear();
            if (!step) break;
            changed = true;
        }
        if (changed && controller.snapshot().state() == SurvivalState.PLANNING) {
            controller.completeImmediate(new SurvivalIntent(SurvivalTask.CRAFT, SurvivalObjective.IRON_GEAR));
        }
        return changed;
    }

    private boolean craftNeededGear() {
        if (SurvivalRecipeService.craft(human, stack -> stack.getItem() instanceof PickaxeItem
                && allowedToolUpgrade(stack, PickaxeItem.class), true).isPresent()) return true;
        if (SurvivalRecipeService.craft(human, stack -> stack.getItem() instanceof AxeItem
                && allowedToolUpgrade(stack, AxeItem.class), true).isPresent()) return true;
        // Mining tools take precedence over combat upgrades so the human can
        // immediately continue gathering stone after the first wood stage.
        if (SurvivalRecipeService.craft(human, stack -> stack.getItem() instanceof SwordItem
                && allowedToolUpgrade(stack, SwordItem.class), true).isPresent()) return true;
        if (!SurvivalInventory.contains(human, stack -> stack.is(Items.SHIELD))
                && SurvivalRecipeService.craft(human, stack -> stack.is(Items.SHIELD), true).isPresent()) return true;
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
        int current = SurvivalInventory.count(human, stack -> type.isInstance(stack.getItem())
                && stack.getItem() instanceof net.minecraft.world.item.TieredItem owned
                && owned.getTier().getLevel() >= level) > 0 ? level : -1;
        if (type == PickaxeItem.class) {
            if (level >= 3) return hasIronPickAndShield();
            if (level == 2) return hasStoneUsefulTools();
            return level == 0 && current < 0 || level == 1 && current < 1 || level == 2 && current < 2;
        }
        if (level <= 1) return current < level;
        return level >= 3 && hasIronPickAndShield();
    }

    private boolean hasIronPickAndShield() {
        return SurvivalInventory.contains(human, stack -> stack.getItem() instanceof PickaxeItem
                        && stack.getItem() instanceof net.minecraft.world.item.TieredItem tiered
                        && tiered.getTier().getLevel() >= 2)
                && SurvivalInventory.contains(human, stack -> stack.is(Items.SHIELD));
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
        if (armor.getMaterial() == net.minecraft.world.item.ArmorMaterials.DIAMOND) return hasIronPickAndShield();
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
            } else if (smeltable == 0) {
                return false;
            }
        } else if (smeltable == 0) {
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
        if (SurvivalRecipeService.canCraft(human, stack -> stack.getItem() instanceof AxeItem
                && allowedToolUpgrade(stack, AxeItem.class), true)) return true;
        if (SurvivalRecipeService.canCraft(human, stack -> stack.getItem() instanceof SwordItem
                && allowedToolUpgrade(stack, SwordItem.class), true)) return true;
        if (!SurvivalInventory.contains(human, stack -> stack.is(Items.SHIELD))
                && SurvivalRecipeService.canCraft(human, stack -> stack.is(Items.SHIELD), true)) return true;
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
            stationCacheTick = human.tickCount;
        }
        Optional<BlockPos> cached = stationCache.get(block);
        if (cached != null) return cached;
        int radius = Config.resourceScanRadius.get();
        BlockPos origin = human.blockPosition();
        Optional<BlockPos> result = BlockPos.betweenClosedStream(origin.offset(-radius, -4, -radius), origin.offset(radius, 4, radius))
                .filter(human.level()::hasChunkAt)
                .filter(pos -> human.level().getBlockState(pos).is(block))
                .filter(pos -> !SurvivalClaimManager.stationClaimedByOther(human, pos))
                .filter(pos -> stationReachable(pos))
                .map(BlockPos::immutable)
                .min(Comparator.comparingDouble(pos -> pos.distSqr(origin)));
        stationCache.put(block, result);
        return result;
    }

    private boolean hasNearbyStation(net.minecraft.world.level.block.Block block) {
        int radius = Config.resourceScanRadius.get();
        BlockPos origin = human.blockPosition();
        return BlockPos.betweenClosedStream(origin.offset(-radius, -4, -radius), origin.offset(radius, 4, radius))
                .filter(human.level()::hasChunkAt)
                .filter(pos -> human.level().getBlockState(pos).is(block))
                .filter(pos -> !SurvivalClaimManager.stationClaimedByOther(human, pos))
                .anyMatch(pos -> human.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 25.0D);
    }

    private boolean stationReachable(BlockPos station) {
        if (human.distanceToSqr(station.getX() + 0.5D, station.getY() + 0.5D, station.getZ() + 0.5D) <= 9.0D) {
            return true;
        }
        return Direction.Plane.HORIZONTAL.stream()
                .map(station::relative)
                .filter(pos -> SurvivalQueryBudget.tryPath(human))
                .map(pos -> human.getNavigation().createPath(pos, 0))
                .anyMatch(path -> path != null && path.canReach());
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
        BlockPos destination = mode == Mode.RESOURCE
                ? LocalResourceScanner.interactionPosition(human, targetPos).orElse(targetPos) : targetPos;
        human.getNavigation().moveTo(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D, 0.8D);
    }

    private void moveToAnimal() {
        if (animal != null) human.getNavigation().moveTo(animal, 1.0D);
    }

    private void abortAction() {
        if (targetPos != null) {
            SurvivalClaimManager.releaseResource(human, targetPos);
            SurvivalClaimManager.releaseStation(human, targetPos);
        }
        human.getNavigation().stop();
        targetPos = null;
        mode = null;
        nextDecisionTick = nextDecisionTick();
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
