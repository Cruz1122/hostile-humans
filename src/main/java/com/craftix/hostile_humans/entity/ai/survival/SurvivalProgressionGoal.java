package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.ai.action.WorldActionResult;
import com.craftix.hostile_humans.entity.ai.action.WorldActionSupport;
import com.craftix.hostile_humans.entity.ai.squad.SquadManager;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
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

/** Small needs -> opportunity -> action loop. Combat and critical existing goals preempt it. */
public final class SurvivalProgressionGoal extends Goal {
    private enum Mode { RESOURCE, STATION, CRAFTING, HUNT, EXPLORE }
    private static final int HUNT_PATH_TIMEOUT_TICKS = 80;
    private static final int RESOURCE_PATH_TIMEOUT_TICKS = 35;
    private static final int STATION_PATH_TIMEOUT_TICKS = 35;
    private static final int EXPLORE_TIMEOUT_TICKS = 140;

    private final Human human;
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

    public SurvivalProgressionGoal(Human human) {
        this.human = human;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Survival decisions are deliberately less urgent than combat and
        // should not perform world scans every time GoalSelector checks us.
        if (!eligible() || human.tickCount < nextDecisionTick) return false;
        nextDecisionTick = nextDecisionTick();
        SquadNeeds needs = SquadNeedsEvaluator.evaluate(human);
        if (tryShare(needs)) return false;
        if (tryImmediateCraft()) return false;
        List<SquadNeed> neededResources = Arrays.stream(SquadNeed.values()).filter(needs::needs).toList();
        Optional<LocalResourceScanner.ResourceTarget> resource = LocalResourceScanner.findReachableFirst(human, neededResources,
                pos -> failedResourceUntil.getOrDefault(pos, 0) > human.tickCount);
        if (resource.isPresent()) {
            var selected = resource.get();
            if (SurvivalClaimManager.claimResource(human, selected.pos())) {
                need = selected.need();
                targetPos = selected.pos();
                mode = Mode.RESOURCE;
                return true;
            }
        }
        if (needs.needs(SquadNeed.FOOD) && selectAnimal(needs)) return true;
        if (selectFurnace(needs)) return true;
        if (selectCraftingTable()) return true;
        need = needs.highestPriority().orElse(null);
        if (need == null) return false;
        return selectExplorationTarget();
    }

    @Override
    public boolean canContinueToUse() {
        if (mode == Mode.HUNT) {
            return eligible() && actionTicks++ < 160 && animal != null && animal.isAlive();
        }
        return eligible() && actionTicks++ < (mode == Mode.EXPLORE ? EXPLORE_TIMEOUT_TICKS : 160) && mode != null;
    }

    @Override
    public void start() {
        actionTicks = 0;
        navigationStalledTicks = 0;
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
        if (mode == Mode.HUNT) {
            tickHunt();
            return;
        }
        if (!eligible()) return;
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
    }

    private void tickHunt() {
        if (!eligible() || animal == null || !animal.isAlive()) {
            mode = null;
            return;
        }
        var target = (net.minecraft.world.entity.LivingEntity) animal;
        human.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distanceSqr = human.distanceToSqr(target);
        double attackRangeSqr = Math.max(9.0D, human.getMeleeAttackRangeSqr(target));
        if (distanceSqr <= attackRangeSqr) {
            human.getNavigation().stop();
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
            nextDecisionTick = nextDecisionTick();
            return;
        }
        SurvivalClaimManager.claimResource(human, targetPos);
        if (human.distanceToSqr(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D) > 9.0D) {
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
        human.getLookControl().setLookAt(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D);
        if (breaker == null) breaker = new ProgressiveBlockBreaker(human, targetPos);
        WorldActionResult result = breaker.tick();
        if (result != WorldActionResult.RUNNING) {
            SurvivalClaimManager.releaseResource(human, targetPos);
            SquadNeedsEvaluator.invalidate(human);
            breaker = null;
            targetPos = null;
            mode = null;
            nextDecisionTick = nextDecisionTick();
        }
    }

    private void tickStation() {
        if (targetPos == null || !(human.level().getBlockEntity(targetPos) instanceof AbstractFurnaceBlockEntity)) {
            if (targetPos != null) SurvivalClaimManager.releaseStation(human, targetPos);
            targetPos = null;
            mode = null;
            nextDecisionTick = nextDecisionTick();
            return;
        }
        SurvivalClaimManager.claimStation(human, targetPos);
        if (human.distanceToSqr(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D) > 9.0D) {
            if (human.getNavigation().isDone()) {
                if (++navigationStalledTicks >= STATION_PATH_TIMEOUT_TICKS) { abortAction(); return; }
                moveToTarget();
            } else navigationStalledTicks = 0;
            return;
        }
        human.getNavigation().stop();
        FurnaceOperation.Result result = FurnaceOperation.tick(human, targetPos);
        if (result != FurnaceOperation.Result.WAITING) {
            SurvivalClaimManager.releaseStation(human, targetPos);
            targetPos = null;
            mode = null;
            nextDecisionTick = nextDecisionTick();
        }
    }

    private void tickCrafting() {
        if (targetPos == null || !human.level().getBlockState(targetPos).is(Blocks.CRAFTING_TABLE)) {
            if (targetPos != null) SurvivalClaimManager.releaseStation(human, targetPos);
            targetPos = null;
            mode = null;
            nextDecisionTick = nextDecisionTick();
            return;
        }
        SurvivalClaimManager.claimStation(human, targetPos);
        if (human.distanceToSqr(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D) > 9.0D) {
            if (human.getNavigation().isDone()) {
                if (++navigationStalledTicks >= STATION_PATH_TIMEOUT_TICKS) { abortAction(); return; }
                moveToTarget();
            } else navigationStalledTicks = 0;
            return;
        }
        human.getNavigation().stop();
        tryImmediateCraft();
        SurvivalClaimManager.releaseStation(human, targetPos);
        targetPos = null;
        mode = null;
        nextDecisionTick = nextDecisionTick();
    }

    private boolean tryImmediateCraft() {
        boolean changed = false;
        for (int craft = 0; craft < 6; craft++) {
            int planks = SurvivalInventory.count(human, stack -> stack.is(ItemTags.PLANKS));
            int sticks = SurvivalInventory.count(human, Items.STICK);
            boolean step = planks < 8 && SurvivalInventory.count(human, stack -> stack.is(ItemTags.LOGS)) > 0
                    && SurvivalRecipeService.craft(human, stack -> stack.is(ItemTags.PLANKS), false).isPresent();
            if (!step) step = sticks < 4 && planks > 0
                    && SurvivalRecipeService.craft(human, stack -> stack.is(Items.STICK), false).isPresent();
            boolean table = hasNearbyStation(Blocks.CRAFTING_TABLE);
            boolean knownTable = table || findStation(Blocks.CRAFTING_TABLE).isPresent();
            if (!knownTable && SurvivalInventory.count(human, Items.CRAFTING_TABLE) == 0)
                step |= SurvivalRecipeService.craft(human, stack -> stack.is(Items.CRAFTING_TABLE), false).isPresent();
            if (!knownTable && placeStation(Items.CRAFTING_TABLE, Blocks.CRAFTING_TABLE)) step = true;
            table = hasNearbyStation(Blocks.CRAFTING_TABLE);
            if (table) step |= craftNeededGear();
            if (table && !SurvivalInventory.contains(human, stack -> stack.getItem() instanceof BowItem))
                step |= SurvivalRecipeService.craft(human, stack -> stack.getItem() instanceof BowItem, true).isPresent();
            if (!step) break;
            changed = true;
        }
        return changed;
    }

    private boolean craftNeededGear() {
        if (SurvivalRecipeService.craft(human, stack -> stack.getItem() instanceof PickaxeItem
                && allowedToolUpgrade(stack, PickaxeItem.class)
                && GearUpgradePolicy.usefulUpgrade(human, stack), true).isPresent()) return true;
        if (SurvivalRecipeService.craft(human, stack -> stack.getItem() instanceof AxeItem
                && allowedToolUpgrade(stack, AxeItem.class)
                && GearUpgradePolicy.usefulUpgrade(human, stack), true).isPresent()) return true;
        // Mining tools take precedence over combat upgrades so the human can
        // immediately continue gathering stone after the first wood stage.
        if (SurvivalRecipeService.craft(human, stack -> stack.getItem() instanceof SwordItem
                && allowedToolUpgrade(stack, SwordItem.class)
                && GearUpgradePolicy.usefulUpgrade(human, stack), true).isPresent()) return true;
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
            if (!SurvivalInventory.contains(human, stack -> stack.getItem() instanceof BowItem)
                    && SquadMaterialSharing.transfer(member, human, stack -> stack.is(Items.STRING), 3, 3) > 0) return true;
            if (SurvivalInventory.contains(human, stack -> stack.getItem() instanceof BowItem)
                    && SurvivalInventory.count(human, Items.ARROW) < 16
                    && SquadMaterialSharing.transfer(member, human, stack -> stack.is(Items.ARROW), 16, 8) > 0) return true;
        }
        return false;
    }

    private boolean selectAnimal(SquadNeeds needs) {
        // Never start a hunt with a pickaxe or empty hand. The combat goal may
        // otherwise inherit the current mining tool and leave the human
        // staring at the animal drops after the first hit.
        if (!equipHuntingWeapon()) return false;
        AABB area = human.getBoundingBox().inflate(Config.resourceScanRadius.get());
        List<net.minecraft.world.entity.animal.Animal> animals = human.level().getEntitiesOfClass(
                net.minecraft.world.entity.animal.Animal.class, area,
                candidate -> candidate instanceof Cow || candidate instanceof Pig || candidate instanceof Sheep || candidate instanceof Chicken);
        animal = animals.stream().min(Comparator.<net.minecraft.world.entity.animal.Animal>comparingInt(candidate -> candidate instanceof Chicken
                        && needs.needs(SquadNeed.FEATHERS) ? 0 : 1)
                .thenComparingDouble(candidate -> human.distanceToSqr(candidate))).orElse(null);
        if (animal == null) return false;
        var path = human.getNavigation().createPath(animal, 1);
        if (path == null || !path.canReach()) {
            animal = null;
            return false;
        }
        mode = Mode.HUNT;
        return true;
    }

    private boolean equipHuntingWeapon() {
        if (human.getMainHandItem().getItem() instanceof SwordItem) return true;
        if (human.getData() == null) return false;
        for (int slot = 0; slot < human.getData().getInventoryItemsSize(); slot++) {
            ItemStack candidate = human.getData().getInventoryItem(slot);
            if (!(candidate.getItem() instanceof SwordItem)) continue;
            ItemStack previous = human.getMainHandItem().copy();
            human.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, candidate.copy());
            human.getData().setInventoryItem(slot, previous);
            human.markEquipmentDirty();
            human.queueEquipmentReevaluation();
            return true;
        }
        return false;
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
        if (!canCraftNeededGear()
                && (SurvivalInventory.contains(human, stack -> stack.getItem() instanceof BowItem)
                || !SurvivalRecipeService.canCraft(human, stack -> stack.getItem() instanceof BowItem, true))) return false;
        Optional<BlockPos> table = findStation(Blocks.CRAFTING_TABLE);
        if (table.isEmpty() || !SurvivalClaimManager.claimStation(human, table.get())) return false;
        targetPos = table.get();
        mode = Mode.CRAFTING;
        return true;
    }

    private boolean canCraftNeededGear() {
        if (SurvivalRecipeService.canCraft(human, stack -> stack.getItem() instanceof PickaxeItem
                && allowedToolUpgrade(stack, PickaxeItem.class)
                && GearUpgradePolicy.usefulUpgrade(human, stack), true)) return true;
        if (SurvivalRecipeService.canCraft(human, stack -> stack.getItem() instanceof AxeItem
                && allowedToolUpgrade(stack, AxeItem.class)
                && GearUpgradePolicy.usefulUpgrade(human, stack), true)) return true;
        if (SurvivalRecipeService.canCraft(human, stack -> stack.getItem() instanceof SwordItem
                && allowedToolUpgrade(stack, SwordItem.class)
                && GearUpgradePolicy.usefulUpgrade(human, stack), true)) return true;
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
        int radius = Config.resourceScanRadius.get();
        BlockPos origin = human.blockPosition();
        return BlockPos.betweenClosedStream(origin.offset(-radius, -4, -radius), origin.offset(radius, 4, radius))
                .filter(human.level()::hasChunkAt)
                .filter(pos -> human.level().getBlockState(pos).is(block))
                .filter(pos -> !SurvivalClaimManager.stationClaimedByOther(human, pos))
                .filter(pos -> stationReachable(pos))
                .map(BlockPos::immutable)
                .min(Comparator.comparingDouble(pos -> pos.distSqr(origin)));
    }

    private boolean hasNearbyStation(net.minecraft.world.level.block.Block block) {
        int radius = Config.resourceScanRadius.get();
        BlockPos origin = human.blockPosition();
        return BlockPos.betweenClosedStream(origin.offset(-radius, -4, -radius), origin.offset(radius, 4, radius))
                .filter(human.level()::hasChunkAt)
                .filter(pos -> human.level().getBlockState(pos).is(block))
                .filter(pos -> !SurvivalClaimManager.stationClaimedByOther(human, pos))
                .anyMatch(pos -> human.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 9.0D);
    }

    private boolean stationReachable(BlockPos station) {
        if (human.distanceToSqr(station.getX() + 0.5D, station.getY() + 0.5D, station.getZ() + 0.5D) <= 9.0D) {
            return true;
        }
        return Direction.Plane.HORIZONTAL.stream()
                .map(station::relative)
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
        return Config.enableSurvivalProgression.get() && !human.level().isClientSide && human.isAlive()
                && human.getTarget() == null && !WorldActionSupport.critical(human)
                && !human.isInvestigatingSound() && !human.hasFreshSquadThreatMemory();
    }

}
