package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.HostileHumansCommands;
import com.craftix.hostile_humans.entity.ai.survival.FurnaceOperation;
import com.craftix.hostile_humans.entity.ai.survival.LocalResourceScanner;
import com.craftix.hostile_humans.entity.ai.survival.ProgressiveBlockBreaker;
import com.craftix.hostile_humans.entity.ai.survival.SquadMaterialSharing;
import com.craftix.hostile_humans.entity.ai.survival.SquadNeed;
import com.craftix.hostile_humans.entity.ai.survival.SquadNeeds;
import com.craftix.hostile_humans.entity.ai.survival.SquadNeedsEvaluator;
import com.craftix.hostile_humans.entity.ai.survival.SurvivalClaimManager;
import com.craftix.hostile_humans.entity.ai.survival.SurvivalInventory;
import com.craftix.hostile_humans.entity.ai.survival.SurvivalProgressionGoal;
import com.craftix.hostile_humans.entity.ai.survival.SurvivalRecipeService;
import com.craftix.hostile_humans.entity.ai.survival.SurvivalState;
import com.craftix.hostile_humans.entity.ai.survival.SurvivalTask;
import com.craftix.hostile_humans.entity.ai.survival.SurvivalObjective;
import com.craftix.hostile_humans.entity.ai.action.MiningToolSelector;
import com.craftix.hostile_humans.entity.ai.control.HumanEntityWalkControl;
import com.craftix.hostile_humans.entity.ai.goal.ItemLootGoal;
import com.craftix.hostile_humans.entity.ai.goal.InvestigateSoundGoal;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class HumanSurvivalProgressionGameTest {
    private static final String TEMPLATE = "human_smoke";

    private HumanSurvivalProgressionGameTest() {}

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void needsWoodWhenMissingBasicTools(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        SquadNeeds needs = SquadNeedsEvaluator.calculate(List.of(human));
        helper.assertTrue(needs.needs(SquadNeed.WOOD), "Missing basic tools did not activate WOOD");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void missingStoneUsefulToolsRequiresEnoughStone(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        SquadNeeds needs = SquadNeedsEvaluator.calculate(List.of(human));
        helper.assertTrue(needs.deficit(SquadNeed.STONE) == 9,
                "Missing stone pickaxe, axe and sword did not require nine stone: " + needs.deficit(SquadNeed.STONE));
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void postWoodenPickaxeInventoryNeedsStoneNotWood(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_PICKAXE));
        human.getData().setInventoryItem(20, new ItemStack(Items.OAK_PLANKS));
        human.getData().setInventoryItem(21, new ItemStack(Items.STICK, 6));
        human.getData().setInventoryItem(22, new ItemStack(Items.STICK));
        human.getData().setInventoryItem(23, new ItemStack(Items.COBBLESTONE, 3));

        SquadNeeds needs = SquadNeedsEvaluator.calculate(List.of(human));
        helper.assertTrue(!needs.needs(SquadNeed.WOOD),
                "A fixed wood reserve pulled the worker away from stone: " + needs.deficits());
        helper.assertTrue(needs.deficit(SquadNeed.STONE) == 6,
                "Post-pickaxe stone deficit did not match the six remaining blocks: " + needs.deficits());
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.OAK_LOG.defaultBlockState());
        helper.setBlock(new BlockPos(5, 1, 2), Blocks.STONE.defaultBlockState());
        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        helper.assertTrue(goal.canUse() && goal.snapshot().intent() != null
                        && goal.snapshot().intent().objective() == com.craftix.hostile_humans.entity.ai.survival.SurvivalObjective.STONE_TOOLS,
                "Post-pickaxe planner returned to wood instead of selecting stone: state="
                        + goal.snapshot() + ", needs=" + needs.deficits());
        goal.stop();
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void debugWorkerPublishesAndRestoresVisibleSurvivalState(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setCustomName(net.minecraft.network.chat.Component.literal("Trace Worker"));
        human.setCustomNameVisible(false);
        human.addTag(SurvivalProgressionGoal.DEBUG_TAG);
        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);

        goal.publishDebugState();
        helper.assertTrue(human.isCustomNameVisible() && human.getCustomName() != null
                        && human.getCustomName().getString().contains("[HH DORMANT]"),
                "Tagged worker did not publish its survival state overhead");

        human.removeTag(SurvivalProgressionGoal.DEBUG_TAG);
        goal.publishDebugState();
        helper.assertTrue(!human.isCustomNameVisible() && human.getCustomName() != null
                        && human.getCustomName().getString().equals("Trace Worker"),
                "Removing the debug tag did not restore the worker name");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void doesNotFarmWoodWhenStocked(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.OAK_LOG, 16));
        human.getData().setInventoryItem(21, new ItemStack(Items.IRON_PICKAXE));
        human.getData().setInventoryItem(22, new ItemStack(Items.IRON_AXE));
        human.getData().setInventoryItem(23, new ItemStack(Items.IRON_SWORD));
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.OAK_LOG.defaultBlockState());
        SquadNeeds needs = SquadNeedsEvaluator.calculate(List.of(human));
        helper.assertTrue(!needs.needs(SquadNeed.WOOD), "Stocked human still needed WOOD");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(4, 1, 2))).is(Blocks.OAK_LOG),
                "Stocked state modified the tree target");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 240)
    public static void nearbyFoodDoesNotBlockWoodGathering(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        BlockPos log = helper.absolutePos(new BlockPos(4, 1, 2));
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.OAK_LOG.defaultBlockState());
        Cow cow = EntityType.COW.create(helper.getLevel());
        if (cow == null) throw new IllegalStateException("Cow could not be created");
        cow.moveTo(helper.absolutePos(new BlockPos(2, 1, 4)), 0.0F, 0.0F);
        cow.setNoAi(true);
        helper.getLevel().addFreshEntity(cow);
        helper.assertTrue(LocalResourceScanner.find(human, SquadNeed.WOOD).filter(log::equals).isPresent(),
                "WOOD fixture was not an actionable local resource");
        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        human.tickCount = Math.floorMod(-human.getId(), 20);
        helper.assertTrue(goal.canUse(), "Nearby FOOD blocked an actionable WOOD need");
        goal.start();
        human.setOnGround(true);
        human.setNoGravity(true);
        for (int tick = 0; tick < 120 && helper.getLevel().getBlockState(log).is(Blocks.OAK_LOG); tick++) goal.tick();
        helper.assertTrue(!helper.getLevel().getBlockState(log).is(Blocks.OAK_LOG),
                "Selected WOOD target was not progressively broken");
        goal.stop();
        cleanup(human); cow.kill(); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void exposedOreCanBeSelected(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        BlockPos ore = helper.absolutePos(new BlockPos(4, 1, 2));
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.IRON_ORE.defaultBlockState());
        helper.assertTrue(LocalResourceScanner.find(human, SquadNeed.IRON).filter(ore::equals).isPresent(), "Exposed iron was not selected");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalResourceIsolation", timeoutTicks = 40)
    public static void hiddenOreIsDetectedDirectly(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        BlockPos relative = new BlockPos(4, 2, 2);
        BlockPos ore = helper.absolutePos(relative);
        helper.setBlock(relative, Blocks.IRON_ORE.defaultBlockState());
        for (var direction : net.minecraft.core.Direction.values()) helper.setBlock(relative.relative(direction), Blocks.STONE.defaultBlockState());
        helper.assertTrue(LocalResourceScanner.find(human, SquadNeed.IRON).filter(ore::equals).isPresent(),
                "Loaded hidden iron was not detected directly");
        helper.assertTrue(helper.getLevel().getBlockState(ore.relative(net.minecraft.core.Direction.NORTH)).is(Blocks.STONE),
                "Detecting hidden ore modified a blocking block");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void deepslateOreIsDetectedDirectly(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        BlockPos ore = helper.absolutePos(new BlockPos(4, 2, 2));
        helper.setBlock(new BlockPos(4, 2, 2), Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState());
        for (var direction : net.minecraft.core.Direction.values()) helper.setBlock(new BlockPos(4, 2, 2).relative(direction), Blocks.DEEPSLATE.defaultBlockState());
        helper.assertTrue(LocalResourceScanner.find(human, SquadNeed.DIAMOND).filter(ore::equals).isPresent(),
                "Hidden deepslate diamond was not detected");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void ironStageRequiresPickaxeAndShield(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        SquadNeeds needs = SquadNeedsEvaluator.calculate(List.of(human));
        helper.assertTrue(needs.needs(SquadNeed.IRON), "Iron stage did not require a shield");
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        needs = SquadNeedsEvaluator.calculate(List.of(human));
        helper.assertTrue(!needs.needs(SquadNeed.IRON), "Iron stage still required gear after pickaxe and shield");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void ironStagePreemptivelyRequiresFuel(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
        human.getData().setInventoryItem(20, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.STONE_SWORD));
        SquadNeeds needs = SquadNeedsEvaluator.calculate(List.of(human));
        helper.assertTrue(needs.needs(SquadNeed.IRON) && needs.needs(SquadNeed.FUEL),
                "Stone-equipped iron stage did not reserve furnace fuel before raw ore existed");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 60)
    public static void nearbyOreDoesNotInterruptActiveHunt(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
        human.getData().setInventoryItem(20, new ItemStack(Items.STONE_PICKAXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(23, new ItemStack(Items.STICK, 8));
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        human.getData().setInventoryItem(24, new ItemStack(Items.IRON_INGOT, 3));
        human.getData().setInventoryItem(25, new ItemStack(Items.GOLDEN_APPLE));
        Cow cow = EntityType.COW.create(helper.getLevel());
        if (cow == null) throw new IllegalStateException("Cow could not be created");
        cow.moveTo(helper.absolutePos(new BlockPos(3, 1, 2)), 0.0F, 0.0F);
        cow.setNoAi(true);
        helper.getLevel().addFreshEntity(cow);
        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        human.tickCount = Math.floorMod(-human.getId(), 20);
        human.setInvestigateSound(BlockPos.ZERO);
        helper.assertTrue(goal.canUse() && goal.snapshot().intent() != null
                        && goal.snapshot().intent().task() == SurvivalTask.HUNT,
                "Fallback hunt did not start without a visible ore: " + goal.snapshot());
        goal.start();
        helper.setBlock(new BlockPos(5, 1, 2), Blocks.IRON_ORE.defaultBlockState());
        SquadNeedsEvaluator.invalidate(human);
        helper.assertTrue(goal.canContinueToUse(), "Active hunt was abandoned when a new ore appeared: " + goal.snapshot());
        goal.stop();
        cleanup(human); cow.kill(); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 60)
    public static void huntRestoresSwordAfterRangedWeaponSelection(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
        human.getData().setInventoryItem(0, new ItemStack(Items.STONE_SWORD));
        human.getData().setInventoryItem(1, new ItemStack(Items.STONE_PICKAXE));
        human.getData().setInventoryItem(2, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(4, new ItemStack(Items.STICK, 8));
        human.getData().setInventoryItem(5, new ItemStack(Items.BOW));
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        human.getData().setInventoryItem(6, new ItemStack(Items.IRON_INGOT, 3));
        human.getData().setInventoryItem(7, new ItemStack(Items.GOLDEN_APPLE));
        Cow cow = EntityType.COW.create(helper.getLevel());
        if (cow == null) throw new IllegalStateException("Cow could not be created");
        cow.moveTo(helper.absolutePos(new BlockPos(3, 1, 2)), 0.0F, 0.0F);
        cow.setNoAi(true);
        helper.getLevel().addFreshEntity(cow);
        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        human.tickCount = Math.floorMod(-human.getId(), 20);
        human.setInvestigateSound(BlockPos.ZERO);
        helper.assertTrue(goal.canUse() && goal.snapshot().intent() != null
                        && goal.snapshot().intent().task() == SurvivalTask.HUNT,
                "Hunt did not start with a sword available in inventory: " + goal.snapshot());
        goal.start();
        helper.assertTrue(human.equipWeapon(stack -> stack.getItem() instanceof net.minecraft.world.item.BowItem),
                "Fixture could not simulate ranged weapon selection during the hunt");
        goal.tick();
        helper.assertTrue(human.getMainHandItem().getItem() instanceof net.minecraft.world.item.SwordItem,
                "Hunt attacked with a bow instead of restoring the available sword");
        goal.stop();
        cleanup(human); cow.kill(); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void elevatedResourceUsesWalkableLowerInteractionCell(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(1, 1, 2));
        BlockPos elevatedLog = helper.absolutePos(new BlockPos(5, 3, 2));
        helper.setBlock(new BlockPos(5, 3, 2), Blocks.OAK_LOG.defaultBlockState());

        helper.assertTrue(LocalResourceScanner.interactionPosition(human, elevatedLog).isPresent(),
                "Scanner did not find a walkable cell below an elevated resource");
        helper.assertTrue(LocalResourceScanner.findReachableFirst(human, List.of(SquadNeed.WOOD), pos -> false)
                        .map(LocalResourceScanner.ResourceTarget::pos).filter(elevatedLog::equals).isPresent(),
                "Elevated resource was rejected despite a reachable lower interaction cell");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void resourceMiningChoosesClosestInteractionCell(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(1, 1, 2));
        BlockPos log = helper.absolutePos(new BlockPos(7, 1, 2));
        helper.setBlock(new BlockPos(7, 1, 2), Blocks.OAK_LOG.defaultBlockState());

        var interaction = LocalResourceScanner.interactionPosition(human, log);
        helper.assertTrue(interaction.isPresent(), "Scanner did not find an interaction cell for the log");
        helper.assertTrue(interaction.get().distManhattan(log) == 1,
                "Resource approach stopped too far from the block: resource=" + log
                        + ", interaction=" + interaction.get());
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void largeDiamondVeinSkipsInaccessibleInnerBlocks(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        for (int x = 4; x <= 6; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 1; z <= 4; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
                }
            }
        }
        List<BlockPos> innerVein = List.of(
                new BlockPos(5, 1, 2), new BlockPos(5, 1, 3), new BlockPos(5, 2, 2));
        innerVein.forEach(pos -> helper.setBlock(pos, Blocks.DIAMOND_ORE.defaultBlockState()));
        BlockPos surfaceRelative = new BlockPos(5, 1, 4);
        helper.setBlock(surfaceRelative, Blocks.DIAMOND_ORE.defaultBlockState());
        helper.setBlock(new BlockPos(5, 2, 4), Blocks.AIR.defaultBlockState());
        helper.setBlock(new BlockPos(5, 0, 5), Blocks.STONE.defaultBlockState());
        BlockPos surface = helper.absolutePos(surfaceRelative);

        helper.assertTrue(LocalResourceScanner.interactionPosition(human, helper.absolutePos(innerVein.get(0))).isEmpty(),
                "Inner diamond block incorrectly received a non-adjacent interaction cell");
        var surfaceInteraction = LocalResourceScanner.interactionPosition(human, surface);
        helper.assertTrue(surfaceInteraction.isPresent(),
                "Surface diamond did not receive an interaction cell: " + surfaceInteraction);
        var selected = LocalResourceScanner.findReachableFirst(human, List.of(SquadNeed.DIAMOND), pos -> false);
        helper.assertTrue(selected.map(LocalResourceScanner.ResourceTarget::pos).filter(surface::equals).isPresent(),
                "Large diamond vein did not select its reachable surface block: " + selected);
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 80)
    public static void elevatedResourceUsesEyeReachFromGround(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        BlockPos log = helper.absolutePos(new BlockPos(3, 4, 2));
        helper.setBlock(new BlockPos(3, 4, 2), Blocks.OAK_LOG.defaultBlockState());

        ProgressiveBlockBreaker breaker = new ProgressiveBlockBreaker(human, log);
        for (int tick = 0; tick < 80 && helper.getLevel().getBlockState(log).is(Blocks.OAK_LOG); tick++) {
            breaker.tick();
        }
        helper.assertTrue(helper.getLevel().getBlockState(log).isAir(),
                "Ground-level human could not mine an elevated log within eye reach");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void insufficientPickDoesNotBreakValuableOre(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
        BlockPos ore = helper.absolutePos(new BlockPos(3, 1, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.DIAMOND_ORE.defaultBlockState());
        new ProgressiveBlockBreaker(human, ore).tick();
        helper.assertTrue(helper.getLevel().getBlockState(ore).is(Blocks.DIAMOND_ORE), "Insufficient pick broke diamond ore");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 80)
    public static void ironPickaxeUnlocksDiamondMiningWithoutShield(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        human.getData().setInventoryItem(20, new ItemStack(Items.STONE_PICKAXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(22, new ItemStack(Items.STONE_SWORD));
        human.getData().setInventoryItem(23, new ItemStack(Items.COOKED_BEEF, 8));
        human.getData().setInventoryItem(24, new ItemStack(Items.GOLDEN_APPLE));
        human.getData().setInventoryItem(25, new ItemStack(Items.COAL, 2));
        BlockPos ore = helper.absolutePos(new BlockPos(3, 1, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.DIAMOND_ORE.defaultBlockState());

        SquadNeeds needs = SquadNeedsEvaluator.calculate(List.of(human));
        helper.assertTrue(needs.needs(SquadNeed.DIAMOND),
                "An iron pickaxe did not unlock the diamond need without a shield: " + needs.deficits());
        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        helper.assertTrue(goal.canUse(), "Diamond ore was not selected with an iron pickaxe");
        goal.start();
        for (int tick = 0; tick < 40 && helper.getLevel().getBlockState(ore).is(Blocks.DIAMOND_ORE); tick++) {
            goal.tick();
        }
        helper.assertTrue(helper.getLevel().getBlockState(ore).isAir(),
                "Human with an iron pickaxe never mined the diamond ore");
        goal.stop(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void diamondMaterialsCraftToolsAndArmorWithoutShield(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        human.getData().setInventoryItem(20, new ItemStack(Items.DIAMOND, 33));
        human.getData().setInventoryItem(21, new ItemStack(Items.STICK, 5));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());

        SurvivalProgressionGoal goal = startCraftingGoal(helper, human);
        int firstSwordTick = -1;
        int fullArmorTick = -1;
        int firstAxeTick = -1;
        for (int tick = 0; tick < 260 && goal.canContinueToUse(); tick++) {
            goal.tick();
            if (firstSwordTick < 0 && SurvivalInventory.contains(human, stack -> stack.is(Items.DIAMOND_SWORD))) {
                firstSwordTick = tick;
            }
            long armorPieces = SurvivalInventory.count(human,
                    stack -> stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor
                            && armor.getMaterial() == net.minecraft.world.item.ArmorMaterials.DIAMOND);
            if (fullArmorTick < 0 && armorPieces == 4) fullArmorTick = tick;
            if (firstAxeTick < 0 && SurvivalInventory.contains(human, stack -> stack.is(Items.DIAMOND_AXE))) {
                firstAxeTick = tick;
            }
        }

        helper.assertTrue(SurvivalInventory.contains(human, stack -> stack.is(Items.DIAMOND_PICKAXE))
                        && SurvivalInventory.contains(human, stack -> stack.is(Items.DIAMOND_AXE))
                        && SurvivalInventory.contains(human, stack -> stack.is(Items.DIAMOND_SWORD)),
                "Diamond materials did not produce all diamond tools: " + human.getData().getInventoryItems());
        helper.assertTrue(SurvivalInventory.contains(human, stack -> stack.is(Items.DIAMOND_HELMET))
                        && SurvivalInventory.contains(human, stack -> stack.is(Items.DIAMOND_CHESTPLATE))
                        && SurvivalInventory.contains(human, stack -> stack.is(Items.DIAMOND_LEGGINGS))
                        && SurvivalInventory.contains(human, stack -> stack.is(Items.DIAMOND_BOOTS)),
                "Diamond materials did not produce full diamond armor: " + human.getData().getInventoryItems());
                helper.assertTrue(!SurvivalInventory.contains(human, stack -> stack.is(Items.SHIELD)),
                "The regression fixture unexpectedly supplied a shield");
        helper.assertTrue(firstSwordTick >= 0 && fullArmorTick >= 0 && firstAxeTick > fullArmorTick
                        && firstSwordTick < firstAxeTick,
                "Diamond axe was crafted before the diamond sword and full armor: sword=" + firstSwordTick
                        + ", armor=" + fullArmorTick + ", axe=" + firstAxeTick);
        goal.stop(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 80)
    public static void existingDiamondPickaxeIsNotCraftedAgain(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.DIAMOND_PICKAXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.DIAMOND, 30));
        human.getData().setInventoryItem(22, new ItemStack(Items.STICK, 5));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());

        SurvivalProgressionGoal goal = startCraftingGoal(helper, human);
        tickGoal(goal, 220);

        helper.assertTrue(SurvivalInventory.count(human, Items.DIAMOND_PICKAXE) == 1,
                "Progression crafted a duplicate diamond pickaxe: " + human.getData().getInventoryItems());
        goal.stop(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void missingShieldRequestsWoodBeforeDiamondMining(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        human.getData().setInventoryItem(20, new ItemStack(Items.STONE_PICKAXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(22, new ItemStack(Items.STONE_SWORD));
        human.getData().setInventoryItem(23, new ItemStack(Items.COOKED_BEEF, 8));
        human.getData().setInventoryItem(24, new ItemStack(Items.GOLDEN_APPLE));
        human.getData().setInventoryItem(25, new ItemStack(Items.COAL, 2));
        BlockPos log = helper.absolutePos(new BlockPos(3, 1, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.OAK_LOG.defaultBlockState());

        SquadNeeds needs = SquadNeedsEvaluator.calculate(List.of(human));
        helper.assertTrue(needs.needs(SquadNeed.WOOD) && needs.needs(SquadNeed.IRON),
                "Missing shield did not request both wood and iron: " + needs.deficits());
        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        helper.assertTrue(goal.canUse(), "Human did not select wood needed for the shield");
        helper.assertTrue(goal.snapshot().intent() != null
                        && goal.snapshot().intent().objective() == com.craftix.hostile_humans.entity.ai.survival.SurvivalObjective.WOOD_BOOTSTRAP,
                "Human selected a later objective instead of gathering shield wood: " + goal.snapshot());
        helper.assertTrue(LocalResourceScanner.find(human, SquadNeed.WOOD).filter(log::equals).isPresent(),
                "Shield wood was not exposed as an actionable resource");
        goal.stop(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void selectedMiningToolSurvivesEquipmentReevaluation(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(20, new ItemStack(Items.STONE_PICKAXE));
        BlockPos ore = helper.absolutePos(new BlockPos(3, 1, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.IRON_ORE.defaultBlockState());

        helper.assertTrue(MiningToolSelector.equip(human, human.getData().getInventoryItem(20)), "Mining tool was not equipped");
        helper.assertTrue(human.getMainHandItem().is(Items.STONE_PICKAXE),
                "Mining selector did not put the correct pickaxe in the main hand");
        human.tick();
        helper.assertTrue(human.getMainHandItem().is(Items.STONE_PICKAXE),
                "Combat reevaluation replaced the selected mining tool");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void progressiveBreakingSwingsMainHand(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        BlockPos log = helper.absolutePos(new BlockPos(3, 1, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.OAK_LOG.defaultBlockState());
        new ProgressiveBlockBreaker(human, log).tick();
        helper.assertTrue(human.swinging, "Progressive breaking did not animate the main hand");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void hiddenOreDropsBesideMiner(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
        BlockPos ore = helper.absolutePos(new BlockPos(4, 2, 2));
        for (BlockPos relative : List.of(
                new BlockPos(4, 2, 2), new BlockPos(3, 2, 2), new BlockPos(5, 2, 2),
                new BlockPos(4, 1, 2), new BlockPos(4, 3, 2), new BlockPos(4, 2, 1), new BlockPos(4, 2, 3))) {
            helper.setBlock(relative, Blocks.DEEPSLATE.defaultBlockState());
        }
        helper.setBlock(new BlockPos(4, 2, 2), Blocks.DEEPSLATE_IRON_ORE.defaultBlockState());
        ProgressiveBlockBreaker breaker = new ProgressiveBlockBreaker(human, ore);
        for (int tick = 0; tick < 400 && helper.getLevel().getBlockState(ore).is(Blocks.DEEPSLATE_IRON_ORE); tick++) {
            breaker.tick();
        }
        boolean nearbyDrop = helper.getLevel().getEntitiesOfClass(ItemEntity.class, human.getBoundingBox().inflate(2.0D),
                item -> item.getItem().is(Items.RAW_IRON)).stream().findAny().isPresent();
        helper.assertTrue(nearbyDrop, "Hidden ore loot was left inside the solid ore cell instead of beside the miner");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 80)
    public static void progressionImmediatelySelectsNextResource(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        BlockPos first = helper.absolutePos(new BlockPos(3, 1, 2));
        BlockPos second = helper.absolutePos(new BlockPos(4, 1, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.OAK_LOG.defaultBlockState());
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.OAK_LOG.defaultBlockState());
        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        helper.assertTrue(goal.canUse(), "First nearby resource was not selected");
        goal.start();
        human.setOnGround(true);
        human.setNoGravity(true);
        for (int tick = 0; tick < 80 && helper.getLevel().getBlockState(first).is(Blocks.OAK_LOG); tick++) goal.tick();
        helper.assertTrue(!helper.getLevel().getBlockState(first).is(Blocks.OAK_LOG), "First resource was not broken");
        goal.stop();
        human.tickCount += 10;
        helper.assertTrue(goal.canUse(), "Progression paused instead of immediately selecting the next resource");
        goal.start();
        for (int tick = 0; tick < 80 && helper.getLevel().getBlockState(second).is(Blocks.OAK_LOG); tick++) goal.tick();
        helper.assertTrue(!helper.getLevel().getBlockState(second).is(Blocks.OAK_LOG), "Second resource was not broken continuously");
        goal.stop(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void unreachableExplorationDoesNotClaimMovement(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
        human.getData().setInventoryItem(20, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.STONE_SWORD));
        human.getData().setInventoryItem(22, new ItemStack(Items.STICK, 16));
        human.getData().setInventoryItem(23, new ItemStack(Items.IRON_INGOT, 16));
        human.getData().setInventoryItem(24, new ItemStack(Items.COOKED_BEEF, 16));
        human.getData().setInventoryItem(25, new ItemStack(Items.GOLDEN_APPLE));
        human.getData().setInventoryItem(26, new ItemStack(Items.COAL, 8));
        for (int x = 1; x <= 3; x++) for (int z = 1; z <= 3; z++) {
            if (x == 2 && z == 2) continue;
            for (int y = 1; y <= 2; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
        }

        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        helper.assertTrue(!goal.canUse(), "Progression claimed MOVE without an actionable exploration route");
        helper.assertTrue(human.getNavigation().isDone(), "A failed exploration decision left navigation active");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void uncollectableDropIsIgnored(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        for (int slot = 0; slot < human.getData().getInventoryItemsSize(); slot++) {
            human.getData().setInventoryItem(slot, new ItemStack(Items.STONE, 64));
        }
        ItemEntity drop = new ItemEntity(helper.getLevel(), human.getX() + 1.0D, human.getY(), human.getZ(),
                new ItemStack(Items.OAK_LOG));
        helper.getLevel().addFreshEntity(drop);
        helper.assertTrue(!new ItemLootGoal(human, 1.0D).canUse(),
                "Human selected a useful drop that could not fit in its inventory");
        drop.kill(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalLootIsolation", timeoutTicks = 40)
    public static void elevatedDropWithoutPickupReachIsIgnored(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                human.getBoundingBox().inflate(12.0D, 6.0D, 12.0D)).forEach(item -> item.discard());
        ItemEntity drop = new ItemEntity(helper.getLevel(), human.getX(), human.getY() + 4.0D, human.getZ(),
                new ItemStack(Items.OAK_LOG));
        drop.setNoGravity(true);
        helper.getLevel().addFreshEntity(drop);
        helper.assertTrue(!new ItemLootGoal(human, 1.0D).canUse(),
                "Human selected an elevated drop whose path endpoint was outside pickup reach");
        drop.kill(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void lootGoalCollectsAndStacksReachedDrop(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.OAK_LOG, 3));
        ItemEntity drop = new ItemEntity(helper.getLevel(), human.getX() + 0.5D, human.getY(), human.getZ(),
                new ItemStack(Items.OAK_LOG));
        drop.setPickUpDelay(0);
        helper.getLevel().addFreshEntity(drop);
        int inserted = com.craftix.hostile_humans.entity.ai.survival.LootCollector.collect(human, drop);
        helper.assertTrue(inserted == 1 && drop.isRemoved(), "Reached drop remained in the world");
        helper.assertTrue(human.getData().getInventoryItem(20).getCount() == 4,
                "Reached drop did not stack into the existing inventory slot");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void lootGoalDoesNotStareAtDelayedDrop(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                human.getBoundingBox().inflate(12.0D, 6.0D, 12.0D)).forEach(ItemEntity::discard);
        ItemEntity drop = new ItemEntity(helper.getLevel(), human.getX() + 0.5D, human.getY(), human.getZ(),
                new ItemStack(Items.RAW_IRON));
        drop.setPickUpDelay(10);
        helper.getLevel().addFreshEntity(drop);
        ItemLootGoal goal = new ItemLootGoal(human, 1.0D);
        helper.assertTrue(goal.canUse(), "Delayed survival drop was not reserved for eventual pickup");
        helper.assertTrue(!goal.getFlags().contains(net.minecraft.world.entity.ai.goal.Goal.Flag.LOOK),
                "Loot goal still owns LOOK and can stare at drops");
        drop.kill(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void lootGoalPrioritizesNeededFoodOverNearestUsefulDrop(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.COOKED_BEEF, 3));
        ItemEntity leather = new ItemEntity(helper.getLevel(), human.getX() + 0.5D, human.getY(), human.getZ(),
                new ItemStack(Items.LEATHER));
        leather.setPickUpDelay(0);
        helper.getLevel().addFreshEntity(leather);
        ItemEntity beef = new ItemEntity(helper.getLevel(), human.getX() + 1.0D, human.getY(), human.getZ(),
                new ItemStack(Items.BEEF));
        beef.setPickUpDelay(0);
        helper.getLevel().addFreshEntity(beef);
        ItemLootGoal goal = new ItemLootGoal(human, 1.0D);
        helper.assertTrue(goal.canUse(), "Needed food was not selected from useful nearby drops");
        goal.start();
        goal.tick();
        helper.assertTrue(SurvivalInventory.count(human, Items.BEEF) == 1,
                "Needed food was not prioritized over the nearest useful non-food drop");
        helper.assertTrue(leather.isAlive(), "Nearest non-food drop was selected ahead of needed food");
        goal.stop(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalPickupContinuity", timeoutTicks = 300)
    public static void miningCollectsItsDropBeforeFallbackActions(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, human.getBoundingBox().inflate(30.0D))
                .forEach(ItemEntity::discard);
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
        human.getData().setInventoryItem(0, new ItemStack(Items.STONE_SWORD));
        human.getData().setInventoryItem(1, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(2, new ItemStack(Items.OAK_PLANKS, 16));
        human.getData().setInventoryItem(3, new ItemStack(Items.STICK, 8));
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.CRAFTING_TABLE.defaultBlockState());
        BlockPos ore = helper.absolutePos(new BlockPos(4, 1, 2));
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.IRON_ORE.defaultBlockState());
        helper.assertTrue(LocalResourceScanner.findReachableFirst(human,
                        List.of(SquadNeed.FUEL, SquadNeed.IRON), pos -> false)
                        .map(LocalResourceScanner.ResourceTarget::pos).filter(ore::equals).isPresent(),
                "Continuity fixture did not expose actionable iron ore");
        human.targetSelector.removeAllGoals(goal -> true);
        // Keep the normal sound investigation goal. Removing it leaves any
        // ambient vibration latched forever, which makes survival ineligible
        // and turns this integration test into a batch-order-dependent fixture.
        human.goalSelector.removeAllGoals(goal -> true);
        human.goalSelector.addGoal(6, new ItemLootGoal(human, 1.0D));
        human.goalSelector.addGoal(5, new SurvivalProgressionGoal(human));
        human.setHasDecidedToSleepTonight(true);
        human.setSleepingThisNight(false);
        human.setNoAi(false);

        helper.startSequence().thenExecuteFor(240, () -> human.setOnGround(true)).thenExecute(() -> {
            helper.assertTrue(!helper.getLevel().getBlockState(ore).is(Blocks.IRON_ORE),
                    "Normal AI ticks did not finish mining the selected iron ore: position=" + human.blockPosition()
                            + ", hand=" + human.getMainHandItem() + ", needs="
                            + SquadNeedsEvaluator.calculate(List.of(human)).deficits() + ", investigating="
                            + human.investigateSound() + ", running="
                            + human.goalSelector.getRunningGoals()
                            .map(goal -> goal.getPriority() + ":" + goal.getGoal().getClass().getSimpleName()).toList());
            helper.assertTrue(SurvivalInventory.count(human, Items.RAW_IRON) > 0,
                    "Human broke iron ore but did not collect its drop before another action");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                            human.getBoundingBox().inflate(12.0D), item -> item.getItem().is(Items.RAW_IRON)).isEmpty(),
                    "Mined raw iron remained abandoned in the arena");
            cleanup(human); helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalWoodBootstrapIntegration", timeoutTicks = 800)
    public static void emptyHumanHarvestsTreeAndCraftsPickaxeThroughNormalAiTicks(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        // GameTest templates share a server world. Remove leftover human
        // fixtures in the neighboring arenas before their death drops can
        // become unrelated loot for this bootstrap scenario.
        helper.getLevel().getEntitiesOfClass(Human.class, human.getBoundingBox().inflate(30.0D))
                .stream().filter(other -> other != human).forEach(Human::discard);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, human.getBoundingBox().inflate(30.0D))
                .forEach(ItemEntity::discard);
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.CRAFTING_TABLE.defaultBlockState());
        List<BlockPos> tree = List.of(
                helper.absolutePos(new BlockPos(4, 1, 2)),
                helper.absolutePos(new BlockPos(4, 2, 2)),
                helper.absolutePos(new BlockPos(4, 3, 2)));
        tree.forEach(pos -> helper.getLevel().setBlock(pos, Blocks.OAK_LOG.defaultBlockState(), 3));
        BlockPos treeOrigin = human.blockPosition();
        BlockPos.betweenClosedStream(treeOrigin.offset(-12, -4, -12), treeOrigin.offset(12, 4, 12))
                .filter(pos -> helper.getLevel().getBlockState(pos).is(BlockTags.LOGS))
                .filter(pos -> !tree.contains(pos))
                .forEach(pos -> helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3));
        helper.assertTrue(LocalResourceScanner.find(human, SquadNeed.WOOD)
                        .filter(tree::contains).isPresent(),
                "Wood integration fixture did not expose its tree to the survival scanner");
        human.targetSelector.removeAllGoals(goal -> true);
        // This fixture only exercises survival progression. Nearby GameTest
        // arenas can emit block-break events, so sound investigation would
        // otherwise preempt the tree-harvesting action nondeterministically.
        human.goalSelector.removeAllGoals(goal -> true);
        human.goalSelector.addGoal(6, new ItemLootGoal(human, 1.0D));
        SurvivalProgressionGoal progressionGoal = new SurvivalProgressionGoal(human);
        human.goalSelector.addGoal(5, progressionGoal);
        human.setNoAi(false);

        helper.startSequence()
                .thenExecuteFor(20, () -> {
                    human.setInvestigateSound(BlockPos.ZERO);
                    human.setOnGround(true);
                })
                .thenExecuteFor(700, () -> {
                    human.setInvestigateSound(BlockPos.ZERO);
                    human.setOnGround(true);
                })
                .thenExecute(() -> {
            long remainingLogs = tree.stream()
                    .filter(pos -> helper.getLevel().getBlockState(pos).is(Blocks.OAK_LOG))
                    .count();
                    helper.assertTrue(remainingLogs == 0,
                            "Normal survival AI abandoned part of the tree: remaining=" + remainingLogs
                            + ", position=" + human.blockPosition() + ", hand=" + human.getMainHandItem()
                            + ", needs=" + SquadNeedsEvaluator.calculate(List.of(human)).deficits()
                            + ", investigating=" + human.investigateSound() + ", running="
                            + human.goalSelector.getRunningGoals()
                            .map(goal -> goal.getPriority() + ":" + goal.getGoal().getClass().getSimpleName()).toList());
                    helper.assertTrue(SurvivalInventory.contains(human, stack -> stack.is(Items.WOODEN_PICKAXE)),
                    "Normal survival AI harvested wood but did not complete the wooden-pickaxe bootstrap: hand="
                            + human.getMainHandItem() + ", inventory=" + human.getData().getInventoryItems());
                    cleanup(human); helper.succeed();
                });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalFullFlowIntegration", timeoutTicks = 2000)
    public static void emptyWorkerContinuesFromWoodenPickaxeIntoStone(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        helper.getLevel().getEntitiesOfClass(Human.class, human.getBoundingBox().inflate(40.0D))
                .stream().filter(other -> other != human).forEach(Human::discard);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, human.getBoundingBox().inflate(1_000.0D))
                .forEach(ItemEntity::discard);

        helper.setBlock(new BlockPos(3, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());
        List<BlockPos> logs = List.of(
                helper.absolutePos(new BlockPos(5, 1, 2)),
                helper.absolutePos(new BlockPos(5, 2, 2)),
                helper.absolutePos(new BlockPos(5, 3, 2)),
                helper.absolutePos(new BlockPos(5, 4, 2)));
        List<BlockPos> stone = List.of(
                helper.absolutePos(new BlockPos(4, 1, 4)), helper.absolutePos(new BlockPos(5, 1, 4)),
                helper.absolutePos(new BlockPos(6, 1, 4)), helper.absolutePos(new BlockPos(4, 1, 5)),
                helper.absolutePos(new BlockPos(5, 1, 5)), helper.absolutePos(new BlockPos(6, 1, 5)),
                helper.absolutePos(new BlockPos(4, 1, 6)), helper.absolutePos(new BlockPos(5, 1, 6)),
                helper.absolutePos(new BlockPos(6, 1, 6)));
        Set<BlockPos> fixtureResources = new HashSet<>(logs);
        fixtureResources.addAll(stone);
        BlockPos scanOrigin = human.blockPosition();
        List<SquadNeed> scannedResources = List.of(SquadNeed.WOOD, SquadNeed.FUEL, SquadNeed.STONE,
                SquadNeed.IRON, SquadNeed.GOLD, SquadNeed.DIAMOND);
        BlockPos.betweenClosedStream(scanOrigin.offset(-12, 0, -12), scanOrigin.offset(12, 4, 12))
                .filter(pos -> !fixtureResources.contains(pos))
                .filter(pos -> scannedResources.stream()
                        .anyMatch(need -> LocalResourceScanner.matches(human.level().getBlockState(pos), need)))
                .forEach(pos -> human.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3));
        logs.forEach(pos -> helper.getLevel().setBlock(pos, Blocks.OAK_LOG.defaultBlockState(), 3));
        stone.forEach(pos -> helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3));

        human.targetSelector.removeAllGoals(goal -> true);
        human.setNoGravity(false);
        human.setNoAi(false);

        helper.startSequence().thenExecuteFor(1800, () -> {
            human.setOnGround(true);
            // Other full-suite fixtures can emit vibrations in this shared
            // world. This scenario verifies uninterrupted survival progression.
            human.setInvestigateSound(BlockPos.ZERO);
        }).thenExecute(() -> {
            long remainingLogs = logs.stream().filter(pos -> helper.getLevel().getBlockState(pos).is(Blocks.OAK_LOG)).count();
            long remainingStone = stone.stream()
                    .map(helper::absolutePos)
                    .filter(pos -> helper.getLevel().getBlockState(pos).is(Blocks.STONE))
                    .count();
            String diagnostics = "remainingLogs=" + remainingLogs + ", remainingStone=" + remainingStone
                    + ", position=" + human.blockPosition() + ", hand=" + human.getMainHandItem()
                    + ", inventory=" + human.getData().getInventoryItems() + ", snapshot=" + human.getSurvivalSnapshot()
                    + ", needs=" + SquadNeedsEvaluator.calculate(List.of(human)).deficits()
                    + ", investigating=" + human.investigateSound() + ", target=" + human.getTarget()
                    + ", threat=" + human.hasFreshSquadThreatMemory() + ", navigationDone=" + human.getNavigation().isDone()
                    + ", running=" + human.goalSelector.getRunningGoals()
                    .map(goal -> goal.getPriority() + ":" + goal.getGoal().getClass().getSimpleName()).toList();
            helper.assertTrue(remainingLogs <= 1,
                    "Normal flow stopped after its wooden bootstrap: " + diagnostics);
            helper.assertTrue(remainingStone < stone.size(),
                    "Normal flow stopped after crafting its wooden pickaxe: " + diagnostics);
            helper.assertTrue(SurvivalInventory.contains(human, stack -> stack.is(Items.COBBLESTONE))
                            || SurvivalInventory.contains(human, stack -> stack.is(Items.STONE_PICKAXE)),
                    "Normal flow did not complete its stone pickaxe: " + diagnostics);
            helper.assertTrue(!SurvivalInventory.contains(human, stack -> stack.is(Items.BOW) || stack.is(Items.ARROW)),
                    "Survival progression created ranged equipment: " + diagnostics);
            cleanup(human); helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void humanInventoryMatchesPlayerInventorySize(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        helper.assertTrue(human.getData().getInventoryItemsSize() == 36,
                "Human inventory does not match the player's 36 storage slots");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void ironMaterialsCraftMandatoryGear(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
        human.getData().setInventoryItem(20, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.STONE_SWORD));
        human.getData().setInventoryItem(22, new ItemStack(Items.STICK, 16));
        human.getData().setInventoryItem(23, new ItemStack(Items.OAK_PLANKS, 16));
        human.getData().setInventoryItem(24, new ItemStack(Items.IRON_INGOT, 4));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());
        SurvivalProgressionGoal goal = startCraftingGoal(helper, human);
        tickGoal(goal, 60);
        helper.assertTrue(SurvivalInventory.contains(human, stack -> stack.is(Items.IRON_PICKAXE))
                        && SurvivalInventory.contains(human, stack -> stack.is(Items.SHIELD)),
                "Four iron ingots plus wood did not slowly craft the mandatory pickaxe and shield");
        goal.stop();
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 80)
    public static void ironPickPreparationDoesNotDeadlockOnShieldReserve(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
        human.getData().setInventoryItem(20, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.STONE_SWORD));
        human.getData().setInventoryItem(22, new ItemStack(Items.OAK_PLANKS, 4));
        human.getData().setInventoryItem(23, new ItemStack(Items.IRON_INGOT, 4));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());

        SurvivalProgressionGoal preparation = new SurvivalProgressionGoal(human);
        preparation.canUse();
        helper.assertTrue(SurvivalInventory.count(human, Items.STICK) >= 2,
                "Four planks were held for the shield instead of making pickaxe sticks");
        preparation.stop();

        SurvivalProgressionGoal crafting = startCraftingGoal(helper, human);
        tickGoal(crafting, 60);
        helper.assertTrue(SurvivalInventory.count(human, Items.IRON_PICKAXE) == 1,
                "Iron pickaxe could not be crafted after preparing sticks: "
                        + human.getData().getInventoryItems());
        crafting.stop();
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 80)
    public static void shieldCraftingReservesMinimalPlanks(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        human.getData().setInventoryItem(20, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.STONE_SWORD));
        human.getData().setInventoryItem(22, new ItemStack(Items.OAK_PLANKS, 6));
        human.getData().setInventoryItem(23, new ItemStack(Items.IRON_INGOT));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());

        SurvivalProgressionGoal goal = startCraftingGoal(helper, human);
        tickGoal(goal, 80);

        helper.assertTrue(SurvivalInventory.count(human, Items.SHIELD) == 1,
                "Six planks and one iron ingot did not produce the mandatory shield: "
                        + human.getData().getInventoryItems());
        helper.assertTrue(SurvivalInventory.count(human, Items.OAK_PLANKS) == 0
                        && SurvivalInventory.count(human, Items.IRON_INGOT) == 0
                        && SurvivalInventory.count(human, Items.STICK) == 0,
                "Shield preparation consumed or retained the wrong materials: "
                        + human.getData().getInventoryItems());
        goal.stop(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalCraftingIsolation", timeoutTicks = 40)
    public static void humanReturnsToKnownTableToCraftIronGear(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
        human.getData().setInventoryItem(20, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.STONE_SWORD));
        human.getData().setInventoryItem(22, new ItemStack(Items.STICK, 16));
        human.getData().setInventoryItem(23, new ItemStack(Items.OAK_PLANKS, 16));
        human.getData().setInventoryItem(24, new ItemStack(Items.IRON_INGOT, 4));
        helper.setBlock(new BlockPos(8, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());
        SurvivalProgressionGoal goal = startCraftingGoal(helper, human);
        human.teleportTo(helper.absolutePos(new BlockPos(7, 1, 2)).getX() + 0.5D,
                helper.absolutePos(new BlockPos(7, 1, 2)).getY(),
                helper.absolutePos(new BlockPos(7, 1, 2)).getZ() + 0.5D);
        tickGoal(goal, 60);
        helper.assertTrue(SurvivalInventory.contains(human, stack -> stack.is(Items.IRON_PICKAXE))
                        && SurvivalInventory.contains(human, stack -> stack.is(Items.SHIELD)),
                "Human reached the crafting table but did not slowly craft mandatory iron gear");
        goal.stop(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalNavigation", timeoutTicks = 40)
    public static void interruptedSurvivalActionResumesInsteadOfResetting(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        prepareDiamondNavigator(human);
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.DIAMOND_ORE.defaultBlockState());
        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);

        helper.assertTrue(goal.canUse(), "Diamond gathering action was not selected for interruption regression");
        goal.start();
        goal.stop();
        helper.assertTrue(goal.snapshot().state() == SurvivalState.SUSPENDED,
                "Stopping an active action discarded its resumable state: " + goal.snapshot());
        helper.assertTrue(goal.canUse(), "Suspended survival action did not become eligible after preemption");
        goal.start();
        helper.assertTrue(goal.snapshot().state() == SurvivalState.NAVIGATE,
                "Suspended survival action did not resume navigation: " + goal.snapshot());
        goal.stop(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalNavigation", timeoutTicks = 40)
    public static void resumedSurvivalActionAbortsWhenClaimIsLost(GameTestHelper helper) {
        Human first = human(helper, new BlockPos(2, 1, 2));
        Human second = human(helper, new BlockPos(6, 1, 4));
        expandGround(helper);
        prepareDiamondNavigator(first);
        BlockPos ore = helper.absolutePos(new BlockPos(7, 1, 2));
        helper.setBlock(new BlockPos(7, 1, 2), Blocks.DIAMOND_ORE.defaultBlockState());
        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(first);

        helper.assertTrue(goal.canUse(), "Diamond gathering action was not selected for claim interruption regression");
        goal.start();
        goal.stop();
        helper.assertTrue(goal.snapshot().state() == SurvivalState.SUSPENDED,
                "Stopping an active action did not leave resumable state: " + goal.snapshot());
        helper.assertTrue(SurvivalClaimManager.claimResource(second, ore),
                "Second human could not acquire the released resource claim");

        goal.start();
        goal.tick();
        helper.assertTrue(goal.snapshot().state() == SurvivalState.BACKOFF
                        && goal.snapshot().failureReason() == com.craftix.hostile_humans.entity.ai.survival.SurvivalFailureReason.CLAIM_UNAVAILABLE,
                "Resumed action ignored a lost resource claim: " + goal.snapshot());
        goal.stop(); cleanup(first, second); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalNavigation", timeoutTicks = 180)
    public static void registeredSurvivalGoalFollowsPathToDiamond(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        isolateTemplateFixtures(human);
        expandGround(helper);
        prepareDiamondNavigator(human);
        BlockPos ore = helper.absolutePos(new BlockPos(7, 1, 2));
        helper.setBlock(new BlockPos(7, 1, 2), Blocks.DIAMOND_ORE.defaultBlockState());
        human.targetSelector.removeAllGoals(ignored -> true);
        human.setNoAi(false);
        human.setNoGravity(false);
        human.setOnGround(true);
        human.setInvestigateSound(BlockPos.ZERO);

        helper.startSequence().thenExecuteFor(150, () -> {
            human.setInvestigateSound(BlockPos.ZERO);
            human.setOnGround(true);
        }).thenExecute(() -> {
            helper.assertTrue(helper.getLevel().getBlockState(ore).isAir(),
                    "Registered survival goal did not follow its path: human=" + human.blockPosition()
                            + ", snapshot=" + human.getSurvivalSnapshot() + ", target=" + human.getTarget()
                            + ", investigating=" + human.isInvestigatingSound() + ", swimming=" + human.isSwimming()
                            + ", items=" + helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                            human.getBoundingBox().inflate(20.0D)).stream()
                            .map(item -> item.getItem() + "@" + item.blockPosition()).toList()
                            + ", running=" + human.goalSelector.getRunningGoals()
                            .map(goal -> goal.getPriority() + ":" + goal.getGoal().getClass().getSimpleName()).toList());
            cleanup(human); helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalNavigation", timeoutTicks = 180)
    public static void registeredSurvivalGoalWalksToCraftingTableBeforeCrafting(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        isolateTemplateFixtures(human);
        expandGround(helper);
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
        human.getData().setInventoryItem(20, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.STONE_SWORD));
        human.getData().setInventoryItem(22, new ItemStack(Items.STICK, 16));
        human.getData().setInventoryItem(23, new ItemStack(Items.OAK_PLANKS, 16));
        human.getData().setInventoryItem(24, new ItemStack(Items.IRON_INGOT, 4));
        human.getData().setInventoryItem(25, new ItemStack(Items.COOKED_BEEF, 8));
        BlockPos table = helper.absolutePos(new BlockPos(6, 1, 2));
        helper.setBlock(new BlockPos(6, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());

        human.targetSelector.removeAllGoals(ignored -> true);
        human.setNoAi(false);
        human.setNoGravity(false);
        human.setOnGround(true);
        human.setInvestigateSound(BlockPos.ZERO);
        boolean[] crafted = {false};
        BlockPos[] craftPosition = {BlockPos.ZERO};

        helper.startSequence().thenExecuteFor(150, () -> {
            human.setInvestigateSound(BlockPos.ZERO);
            if (!crafted[0] && SurvivalInventory.contains(human, stack -> stack.is(Items.IRON_PICKAXE))
                    && SurvivalInventory.contains(human, stack -> stack.is(Items.SHIELD))) {
                crafted[0] = true;
                craftPosition[0] = human.blockPosition();
            }
            human.setOnGround(true);
        }).thenExecute(() -> {
            helper.assertTrue(crafted[0], "Live survival goal never crafted mandatory iron gear at the table: human="
                    + human.blockPosition() + ", snapshot=" + human.getSurvivalSnapshot() + ", running="
                    + human.goalSelector.getRunningGoals()
                    .map(goal -> goal.getPriority() + ":" + goal.getGoal().getClass().getSimpleName()).toList()
                    + ", items=" + helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    human.getBoundingBox().inflate(20.0D)).stream()
                    .map(item -> item.getItem() + "@" + item.blockPosition()).toList());
            helper.assertTrue(craftPosition[0].distManhattan(table) <= 2,
                    "Human crafted before reaching the table interaction cell: position=" + craftPosition[0]
                            + ", table=" + table);
            cleanup(human); helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalNavigation", timeoutTicks = 80)
    public static void craftingPacesIndividualGearAndFacesTable(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        human.getData().setInventoryItem(20, new ItemStack(Items.DIAMOND, 33));
        human.getData().setInventoryItem(21, new ItemStack(Items.STICK, 5));
        BlockPos table = helper.absolutePos(new BlockPos(3, 1, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());

        SurvivalProgressionGoal goal = startCraftingGoal(helper, human);
        int previousArmor = 0;
        int firstArmorTick = -1;
        int secondArmorTick = -1;
        for (int tick = 0; tick < 220 && secondArmorTick < 0; tick++) {
            goal.tick();
            human.getLookControl().tick();
            int armor = SurvivalInventory.count(human,
                    stack -> stack.getItem() instanceof net.minecraft.world.item.ArmorItem);
            if (armor > previousArmor) {
                if (firstArmorTick < 0) firstArmorTick = tick;
                else if (secondArmorTick < 0) secondArmorTick = tick;
                previousArmor = armor;
            }
        }

        Vec3 toTable = Vec3.atCenterOf(table).subtract(human.getEyePosition()).normalize();
        helper.assertTrue(firstArmorTick >= 0 && secondArmorTick >= firstArmorTick + 20,
                "Crafting produced armor pieces too quickly: first=" + firstArmorTick
                        + ", second=" + secondArmorTick);
        helper.assertTrue(human.getViewVector(1.0F).dot(toTable) > 0.75D,
                "Human did not look at the crafting table while crafting: view=" + human.getViewVector(1.0F)
                        + ", target=" + toTable);
        goal.stop(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalNavigation", timeoutTicks = 80)
    public static void stalledResourceAdvancesAlongLookDirection(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        expandGround(helper);
        prepareDiamondNavigator(human);
        helper.setBlock(new BlockPos(2, 1, 5), Blocks.DIAMOND_ORE.defaultBlockState());
        human.goalSelector.removeAllGoals(ignored -> true);
        human.targetSelector.removeAllGoals(ignored -> true);
        human.setYRot(0.0F);
        human.setXRot(0.0F);

        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        helper.assertTrue(goal.canUse(), "Diamond gathering action was not selected for the stalled-navigation regression");
        goal.start();
        human.setNoAi(true);
        Vec3 before = human.position();
        for (int tick = 0; tick < 25; tick++) {
            human.getNavigation().stop();
            goal.tick();
        }
        human.setNoAi(false);

        helper.startSequence().thenExecuteFor(20, () -> {
            human.setOnGround(true);
            goal.tick();
        }).thenExecute(() -> {
            helper.assertTrue(human.getZ() > before.z + 1.0D,
                    "Stalled resource action did not nudge the human along its look direction: before="
                            + before + ", after=" + human.position());
            goal.stop(); cleanup(human); helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalNavigation", timeoutTicks = 100)
    public static void resourceNudgeDoesNotLeakIntoCombatMovement(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        expandGround(helper);
        prepareDiamondNavigator(human);
        helper.setBlock(new BlockPos(2, 1, 5), Blocks.DIAMOND_ORE.defaultBlockState());
        human.goalSelector.removeAllGoals(ignored -> true);
        human.targetSelector.removeAllGoals(ignored -> true);
        human.setYRot(0.0F);
        human.setXRot(0.0F);

        SurvivalProgressionGoal goal = human.getSurvivalProgressionGoal();
        helper.assertTrue(goal.canUse(), "Diamond gathering action was not selected for movement ownership regression");
        goal.start();
        human.setNoAi(true);
        for (int tick = 0; tick < 25; tick++) {
            human.getNavigation().stop();
            goal.tick();
        }

        helper.assertTrue(human.getMoveControl() instanceof HumanEntityWalkControl,
                "Human did not use its movement ownership controller");
        HumanEntityWalkControl moveControl = (HumanEntityWalkControl) human.getMoveControl();
        helper.assertTrue(moveControl.isSurvivalNudgeActive(),
                "Resource fixture did not activate the direct movement recovery");
        var target = EntityType.COW.create(helper.getLevel());
        helper.assertTrue(target != null, "Combat target could not be created");
        target.moveTo(human.getX(), human.getY(), human.getZ() + 8.0D, 180.0F, 0.0F);
        target.setNoAi(true);
        helper.getLevel().addFreshEntity(target);
        human.setTarget(target);
        helper.assertTrue(!moveControl.isSurvivalNudgeActive(),
                "Combat target acquisition left the resource nudge active");
        helper.assertTrue(Math.abs(human.getSpeed()) < 0.0001F,
                "Combat target acquisition retained the resource nudge speed: " + human.getSpeed());
        helper.assertTrue(human.getDeltaMovement().horizontalDistanceSqr() < 0.0001D,
                "Combat target acquisition retained resource nudge momentum: " + human.getDeltaMovement());
        goal.tick();
        helper.assertTrue(!moveControl.isSurvivalNudgeActive(),
                "Resource nudge restarted while a combat target was active");
        goal.stop();
        target.discard();
        cleanup(human);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalNavigation", timeoutTicks = 100)
    public static void resourceNudgeStopsWhenFleeingWithoutTarget(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        expandGround(helper);
        prepareDiamondNavigator(human);
        helper.setBlock(new BlockPos(2, 1, 5), Blocks.DIAMOND_ORE.defaultBlockState());
        human.goalSelector.removeAllGoals(ignored -> true);
        human.targetSelector.removeAllGoals(ignored -> true);
        human.setYRot(0.0F);
        human.setXRot(0.0F);

        SurvivalProgressionGoal goal = human.getSurvivalProgressionGoal();
        helper.assertTrue(goal.canUse(), "Diamond gathering action was not selected for retreat ownership regression");
        goal.start();
        human.setNoAi(true);
        for (int tick = 0; tick < 25; tick++) {
            human.getNavigation().stop();
            goal.tick();
        }

        HumanEntityWalkControl moveControl = (HumanEntityWalkControl) human.getMoveControl();
        helper.assertTrue(moveControl.isSurvivalNudgeActive(),
                "Resource fixture did not activate before retreat");
        var threat = EntityType.COW.create(helper.getLevel());
        helper.assertTrue(threat != null, "Retreat threat could not be created");
        threat.moveTo(human.getX(), human.getY(), human.getZ() + 4.0D, 180.0F, 0.0F);
        threat.setNoAi(true);
        helper.getLevel().addFreshEntity(threat);
        human.toAvoid = threat;
        human.isFleeing = true;
        goal.tick();

        helper.assertTrue(!moveControl.isSurvivalNudgeActive(),
                "Resource nudge remained active after retreat started");
        helper.assertTrue(Math.abs(human.getSpeed()) < 0.0001F,
                "Retreat retained the resource nudge speed: " + human.getSpeed());
        goal.stop();
        threat.discard();
        cleanup(human);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalNavigation", timeoutTicks = 80)
    public static void resourceNavigationEquipsRequiredToolBeforeArrival(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        expandGround(helper);
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        human.getData().setInventoryItem(20, new ItemStack(Items.IRON_PICKAXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(22, new ItemStack(Items.STONE_SWORD));
        human.getData().setInventoryItem(23, new ItemStack(Items.STICK, 5));
        human.getData().setInventoryItem(24, new ItemStack(Items.COOKED_BEEF, 8));
        human.getData().setInventoryItem(25, new ItemStack(Items.COAL, 2));
        BlockPos ore = helper.absolutePos(new BlockPos(7, 1, 2));
        helper.setBlock(new BlockPos(7, 1, 2), Blocks.DIAMOND_ORE.defaultBlockState());

        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        helper.assertTrue(goal.canUse(), "Diamond resource was not selected for tool-switch regression");
        goal.start();
        human.setNoAi(true);
        goal.tick();
        helper.assertTrue(human.getMainHandItem().is(Items.IRON_PICKAXE),
                "Human kept an unsuitable tool while navigating to diamond ore: " + human.getMainHandItem());
        helper.assertTrue(helper.getLevel().getBlockState(ore).is(Blocks.DIAMOND_ORE),
                "Tool-switch fixture changed the resource before arrival");
        goal.stop(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void soundInvestigationBlocksNewSurvivalPlan(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        prepareDiamondNavigator(human);
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.DIAMOND_ORE.defaultBlockState());
        human.setInvestigateSound(helper.absolutePos(new BlockPos(8, 1, 8)));
        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);

        helper.assertTrue(!goal.canUse(), "Survival progression preempted an active sound investigation");
        goal.stop(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void soundInvestigationHasHardTimeout(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setInvestigateSound(helper.absolutePos(new BlockPos(8, 1, 8)));
        InvestigateSoundGoal goal = new InvestigateSoundGoal(human, 1.0D);
        helper.assertTrue(goal.canUse(), "Sound investigation fixture did not start");
        goal.start();
        for (int tick = 0; tick < 110; tick++) goal.tick();
        helper.assertTrue(!goal.canContinueToUse() && !human.isInvestigatingSound(),
                "Unreachable sound investigation did not time out");
        goal.stop(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void craftingUsesLoadedRecipe(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.OAK_LOG));
        ItemStack output = SurvivalRecipeService.craft(human, stack -> stack.is(Items.OAK_PLANKS), false).orElse(ItemStack.EMPTY);
        helper.assertTrue(output.is(Items.OAK_PLANKS) && output.getCount() == 4, "Loaded oak planks recipe was not assembled");
        helper.assertTrue(SurvivalInventory.count(human, Items.OAK_LOG) == 0 && SurvivalInventory.count(human, Items.OAK_PLANKS) == 4,
                "Craft did not conserve the loaded recipe inputs/output");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void craftingDoesNotDuplicateItems(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.OAK_LOG));
        SurvivalRecipeService.craft(human, stack -> stack.is(Items.OAK_PLANKS), false);
        boolean second = SurvivalRecipeService.craft(human, stack -> stack.is(Items.OAK_PLANKS), false).isPresent();
        helper.assertTrue(!second && SurvivalInventory.count(human, Items.OAK_PLANKS) == 4, "Crafting duplicated output without a second input");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void craftingProbeDoesNotConsumeIngredients(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.IRON_INGOT, 3));
        human.getData().setInventoryItem(21, new ItemStack(Items.STICK, 2));

        helper.assertTrue(SurvivalRecipeService.canCraft(human, stack -> stack.is(Items.IRON_PICKAXE), true),
                "Crafting probe did not detect an available iron pickaxe recipe");
        helper.assertTrue(SurvivalInventory.count(human, Items.IRON_INGOT) == 3
                        && SurvivalInventory.count(human, Items.STICK) == 2
                        && SurvivalInventory.count(human, Items.IRON_PICKAXE) == 0,
                "Crafting probe consumed ingredients or inserted its result");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void foundationalCraftingReachesCraftingTable(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.DARK_OAK_LOG, 2));
        helper.assertTrue(SurvivalRecipeService.craft(human, stack -> stack.is(ItemTags.PLANKS), false).isPresent(),
                "Loaded progression recipe could not turn a non-oak log into planks");
        helper.assertTrue(SurvivalRecipeService.craft(human, stack -> stack.is(Items.STICK), false).isPresent(),
                "Loaded progression recipe could not craft sticks");
        helper.assertTrue(SurvivalRecipeService.craft(human, stack -> stack.is(ItemTags.PLANKS), false).isPresent(),
                "Second log could not supply crafting-table planks");
        helper.assertTrue(SurvivalRecipeService.craft(human, stack -> stack.is(Items.CRAFTING_TABLE), false).isPresent(),
                "Loaded progression recipes did not reach a crafting table");
        helper.assertTrue(SurvivalInventory.count(human, Items.CRAFTING_TABLE) == 1,
                "Crafting table output was not retained");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void progressionCraftsWoodenPickaxeBeforeMining(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.OAK_LOG, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());
        SurvivalProgressionGoal goal = startCraftingGoal(helper, human);
        tickGoal(goal, 80);
        helper.assertTrue(SurvivalInventory.contains(human, stack -> stack.is(Items.WOODEN_PICKAXE)),
                "Progression stopped after gathering wood instead of slowly crafting a wooden pickaxe");
        goal.stop();
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void progressionDoesNotCraftRangedEquipment(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.OAK_PLANKS, 12));
        human.getData().setInventoryItem(21, new ItemStack(Items.STICK, 9));
        human.getData().setInventoryItem(22, new ItemStack(Items.STRING, 3));
        human.getData().setInventoryItem(23, new ItemStack(Items.FEATHER, 6));
        human.getData().setInventoryItem(24, new ItemStack(Items.FLINT, 6));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());
        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        goal.canUse();
        helper.assertTrue(!SurvivalInventory.contains(human, stack -> stack.is(Items.BOW) || stack.is(Items.ARROW)),
                "Survival progression crafted ranged equipment");
        helper.assertTrue(SurvivalInventory.count(human, Items.STRING) == 3
                        && SurvivalInventory.count(human, Items.FEATHER) == 6
                        && SurvivalInventory.count(human, Items.FLINT) == 6,
                "Survival progression consumed ranged crafting materials");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void progressionCraftsStoneToolsWhenCobblestoneIsStocked(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.COBBLESTONE, 9));
        human.getData().setInventoryItem(21, new ItemStack(Items.STICK, 5));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());
        SurvivalProgressionGoal goal = startCraftingGoal(helper, human);
        tickGoal(goal, 100);
        helper.assertTrue(SurvivalInventory.contains(human, stack -> stack.is(Items.STONE_PICKAXE))
                        && SurvivalInventory.contains(human, stack -> stack.is(Items.STONE_AXE))
                        && SurvivalInventory.contains(human, stack -> stack.is(Items.STONE_SWORD)),
                "Stocked cobblestone did not produce the stone pickaxe, axe and sword");
        goal.stop();
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void shapedStickRecipeUsesCorrectLayout(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.OAK_PLANKS, 2));
        ItemStack output = SurvivalRecipeService.craft(human, stack -> stack.is(Items.STICK), false).orElse(ItemStack.EMPTY);
        helper.assertTrue(output.is(Items.STICK) && output.getCount() == 4, "Vertical shaped stick recipe did not match");
        helper.assertTrue(SurvivalInventory.count(human, Items.OAK_PLANKS) == 0
                && SurvivalInventory.count(human, Items.STICK) == 4, "Stick recipe consumed or created the wrong amount");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void shapedPickaxeRecipeUsesCorrectLayout(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.COBBLESTONE, 3));
        human.getData().setInventoryItem(21, new ItemStack(Items.STICK, 2));
        ItemStack output = SurvivalRecipeService.craft(human, stack -> stack.is(Items.STONE_PICKAXE), true).orElse(ItemStack.EMPTY);
        helper.assertTrue(output.is(Items.STONE_PICKAXE), "Stone pickaxe shaped recipe did not match");
        helper.assertTrue(SurvivalInventory.count(human, Items.COBBLESTONE) == 0
                && SurvivalInventory.count(human, Items.STICK) == 0
                && SurvivalInventory.count(human, Items.STONE_PICKAXE) == 1, "Pickaxe crafting was not conserved");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void craftingServiceRejectsDuplicateTieredTools(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
        human.getData().setInventoryItem(20, new ItemStack(Items.COBBLESTONE, 3));
        human.getData().setInventoryItem(21, new ItemStack(Items.STICK, 2));
        human.getData().setInventoryItem(22, new ItemStack(Items.STONE_PICKAXE));

        helper.assertTrue(SurvivalRecipeService.craft(human, stack -> stack.is(Items.STONE_PICKAXE), true).isEmpty(),
                "Crafting service created a duplicate stone pickaxe");
        helper.assertTrue(SurvivalInventory.count(human, Items.STONE_PICKAXE) == 1,
                "Duplicate stone pickaxe was inserted despite the crafting guard");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void shapedSwordRecipeUsesCorrectLayout(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.COBBLESTONE, 2));
        human.getData().setInventoryItem(21, new ItemStack(Items.STICK));
        ItemStack output = SurvivalRecipeService.craft(human, stack -> stack.is(Items.STONE_SWORD), true).orElse(ItemStack.EMPTY);
        helper.assertTrue(output.is(Items.STONE_SWORD), "Stone sword shaped recipe did not match");
        helper.assertTrue(SurvivalInventory.count(human, Items.COBBLESTONE) == 0
                && SurvivalInventory.count(human, Items.STICK) == 0
                && SurvivalInventory.count(human, Items.STONE_SWORD) == 1, "Sword crafting was not conserved");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void shapedArmorRecipeUsesCorrectLayout(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.IRON_INGOT, 5));
        ItemStack output = SurvivalRecipeService.craft(human, stack -> stack.is(Items.IRON_HELMET), true).orElse(ItemStack.EMPTY);
        helper.assertTrue(output.is(Items.IRON_HELMET), "Iron helmet shaped recipe did not match");
        helper.assertTrue(SurvivalInventory.count(human, Items.IRON_INGOT) == 0
                && SurvivalInventory.count(human, Items.IRON_HELMET) == 1, "Armor crafting was not conserved");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalExplorationIsolation", timeoutTicks = 40)
    public static void exploreFinishesWhenNavigationDone(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        human.getData().setInventoryItem(20, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.STONE_SWORD));
        human.getData().setInventoryItem(22, new ItemStack(Items.STICK, 16));
        human.getData().setInventoryItem(24, new ItemStack(Items.COOKED_BEEF, 16));
        human.getData().setInventoryItem(25, new ItemStack(Items.GOLDEN_APPLE));
        human.getData().setInventoryItem(26, new ItemStack(Items.COAL, 8));
        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        helper.assertTrue(goal.canUse(), "Exploration action was not selected in an empty local area");
        goal.start();
        human.getNavigation().stop();
        goal.tick();
        helper.assertTrue(!goal.canContinueToUse(), "Explore remained active after navigation completed");
        goal.stop(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 80)
    public static void huntingDamagesReachableAnimal(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        prepareHunter(human);
        Cow cow = EntityType.COW.create(helper.getLevel());
        if (cow == null) throw new IllegalStateException("Cow could not be created");
        cow.moveTo(helper.absolutePos(new BlockPos(3, 1, 2)), 0.0F, 0.0F);
        cow.setNoAi(true);
        helper.getLevel().addFreshEntity(cow);
        float initialHealth = cow.getHealth();
        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        human.tickCount = Math.floorMod(-human.getId(), 20);
        helper.assertTrue(goal.canUse(), "Reachable animal was not selected for hunting");
        goal.start();
        for (int tick = 0; tick < 80 && cow.getHealth() == initialHealth; tick++) {
            goal.tick();
            cow.invulnerableTime = 0;
        }
        helper.assertTrue(cow.getHealth() < initialHealth, "Hunting selected an animal but never attacked it");
        goal.stop();
        cleanup(human); cow.kill(); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void storedFoodDoesNotTriggerFallbackHunt(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        prepareHunter(human);
        human.getData().setInventoryItem(25, new ItemStack(Items.COOKED_BEEF, 4));
        Cow cow = EntityType.COW.create(helper.getLevel());
        if (cow == null) throw new IllegalStateException("Cow could not be created");
        cow.moveTo(helper.absolutePos(new BlockPos(3, 1, 2)), 0.0F, 0.0F);
        cow.setNoAi(true);
        helper.getLevel().addFreshEntity(cow);

        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        human.tickCount = Math.floorMod(-human.getId(), 20);
        boolean selected = goal.canUse();
        helper.assertTrue(!selected || goal.snapshot().intent() == null
                        || goal.snapshot().intent().task() != SurvivalTask.HUNT,
                "Human selected a hunt despite having edible food in inventory");

        goal.stop(); cleanup(human); cow.kill(); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void foodIsLastProgressionPriority(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        BlockPos log = helper.absolutePos(new BlockPos(4, 1, 2));
        helper.getLevel().setBlock(log, Blocks.OAK_LOG.defaultBlockState(), 3);
        Cow cow = EntityType.COW.create(helper.getLevel());
        if (cow == null) throw new IllegalStateException("Cow could not be created");
        cow.moveTo(helper.absolutePos(new BlockPos(3, 1, 2)), 0.0F, 0.0F);
        cow.setNoAi(true);
        helper.getLevel().addFreshEntity(cow);

        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        human.tickCount = Math.floorMod(-human.getId(), 20);
        helper.assertTrue(goal.canUse() && goal.snapshot().intent() != null
                        && goal.snapshot().intent().objective() == SurvivalObjective.WOOD_BOOTSTRAP,
                "Food interrupted wood progression: " + goal.snapshot());
        goal.stop(); cleanup(human); cow.kill(); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalHuntContinuity", timeoutTicks = 100)
    public static void huntingContinuesAfterFirstHitThroughNormalEntityTicks(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        prepareHunter(human);
        Cow cow = EntityType.COW.create(helper.getLevel());
        if (cow == null) throw new IllegalStateException("Cow could not be created");
        cow.moveTo(helper.absolutePos(new BlockPos(3, 1, 2)), 0.0F, 0.0F);
        cow.setNoAi(true);
        helper.getLevel().addFreshEntity(cow);
        float initialHealth = cow.getHealth();
        float[] firstHitHealth = {Float.NaN};
        int[] firstHitTick = {-1};
        int[] firstHitInvulnerableTime = {-1};
        BlockPos[] firstHitInvestigation = {BlockPos.ZERO};
        SurvivalProgressionGoal huntGoal = new SurvivalProgressionGoal(human);
        human.targetSelector.removeAllGoals(goal -> true);
        human.goalSelector.addGoal(-20, huntGoal);
        human.setNoAi(false);

        helper.startSequence()
                .thenExecuteFor(50, () -> {
                    if (Float.isNaN(firstHitHealth[0]) && cow.getHealth() < initialHealth) {
                        firstHitHealth[0] = cow.getHealth();
                        firstHitTick[0] = human.tickCount;
                        firstHitInvulnerableTime[0] = cow.invulnerableTime;
                        firstHitInvestigation[0] = human.investigateSound();
                    }
                })
                .thenExecute(() -> {
                    helper.assertTrue(!Float.isNaN(firstHitHealth[0]),
                            "Normal entity ticks never produced the first hunting hit: position=" + human.blockPosition()
                                    + ", investigating=" + human.investigateSound() + ", target=" + human.getTarget()
                                    + ", running=" + human.goalSelector.getRunningGoals()
                                            .map(goal -> goal.getPriority() + ":" + goal.getGoal().getClass().getSimpleName()).toList());
                    helper.assertTrue(firstHitInvulnerableTime[0] > 0,
                            "First hit bypassed normal damage invulnerability: " + firstHitInvulnerableTime[0]);
                    helper.assertTrue(BlockPos.ZERO.equals(firstHitInvestigation[0]),
                            "Human interrupted its own hunt to investigate the damage it caused: "
                                    + firstHitInvestigation[0]);
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertTrue(!cow.isAlive() || cow.getHealth() < firstHitHealth[0],
                            "Hunting stopped after first hit: firstHealth=" + firstHitHealth[0]
                                    + ", currentHealth=" + cow.getHealth() + ", firstTick=" + firstHitTick[0]
                                    + ", currentTick=" + human.tickCount + ", investigating=" + human.investigateSound());
                     human.goalSelector.removeGoal(huntGoal);
                     cleanup(human); cow.kill(); helper.succeed();
                 });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalAnimalProgression", timeoutTicks = 360)
    public static void huntingCollectsVanillaMeatAndResumesProgression(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        // Other GameTest fixtures may run in adjacent templates. Keep this
        // progression-only fixture from acquiring their hostile targets.
        human.targetSelector.removeAllGoals(goal -> true);
        human.goalSelector.removeAllGoals(goal -> true);
        human.setTarget(null);
        human.clearSquadThreatMemory();
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        human.getData().setInventoryItem(20, new ItemStack(Items.STONE_PICKAXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(22, new ItemStack(Items.COOKED_BEEF, 7));
        helper.getLevel().getEntities(EntityType.ITEM, human.getBoundingBox().inflate(16.0D), entity -> true)
                .forEach(ItemEntity::discard);

        boolean previousDoMobLoot = helper.getLevel().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
        helper.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(true, helper.getLevel().getServer());
        Cow cow;
        BlockPos nextResource = helper.absolutePos(new BlockPos(4, 1, 2));
        try {
            cow = EntityType.COW.create(helper.getLevel());
            if (cow == null) throw new IllegalStateException("Cow could not be created");
            cow.moveTo(helper.absolutePos(new BlockPos(3, 1, 2)), 0.0F, 0.0F);
            cow.setNoAi(true);
            helper.getLevel().addFreshEntity(cow);
            helper.assertTrue(cow.hurt(human.damageSources().mobAttack(human), cow.getHealth()),
                    "Vanilla animal could not be killed for the loot regression");
            helper.assertTrue(!cow.isAlive(), "Vanilla animal death did not complete immediately");
        } catch (RuntimeException | Error failure) {
            helper.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT)
                    .set(previousDoMobLoot, helper.getLevel().getServer());
            throw failure;
        }

        // Keep this test focused on the vanilla drop -> survival inventory
        // handoff. Pickup delay and navigation reach are covered independently;
        // pinning the real vanilla beef drop beside the human avoids a flaky
        // dependency on entity spawn physics between GameTest batches.
        ItemEntity beefDrop = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                        human.getBoundingBox().inflate(16.0D), item -> item.getItem().is(Items.BEEF))
                .stream().findFirst().orElseThrow(() -> new AssertionError("Cow produced no vanilla beef drop"));
        beefDrop.setPickUpDelay(0);
        beefDrop.setPos(human.getX() + 0.5D, human.getY(), human.getZ());

        ItemLootGoal lootGoal = new ItemLootGoal(human, 1.0D);
        helper.assertTrue(lootGoal.canUse(), "Vanilla animal meat was not offered as useful loot");
        lootGoal.start();
        SurvivalProgressionGoal progressionGoal = new SurvivalProgressionGoal(human);
        helper.startSequence()
                .thenExecuteFor(40, lootGoal::tick)
                .thenExecute(() -> {
                    helper.assertTrue(SurvivalInventory.count(human, Items.BEEF) > 0,
                            "Vanilla animal meat was not collected into survival inventory: items="
                                    + helper.getLevel().getEntitiesOfClass(ItemEntity.class, human.getBoundingBox().inflate(16.0D))
                                    .stream().map(item -> item.getItem().toString() + "@" + item.position()
                                            + "/delayed=" + item.hasPickUpDelay()).toList()
                                    + ", inventory=" + human.getData().getInventoryItems());
                    lootGoal.stop();
                    human.setInvestigateSound(BlockPos.ZERO);
                    helper.setBlock(new BlockPos(4, 1, 2), Blocks.OAK_LOG.defaultBlockState());
                    human.tickCount += 10;
                    helper.assertTrue(progressionGoal.canUse(),
                            "Progression did not select a new resource after collecting animal meat");
                    progressionGoal.start();
                })
                .thenExecuteFor(100, () -> {
                    human.setInvestigateSound(BlockPos.ZERO);
                    human.clearSquadThreatMemory();
                    progressionGoal.tick();
                })
                .thenExecute(() -> {
                    try {
                        helper.assertTrue(!helper.getLevel().getBlockState(nextResource).is(Blocks.OAK_LOG),
                                "Progression did not resume with a new resource after collecting animal meat");
                        progressionGoal.stop();
                        cleanup(human); cow.kill(); helper.succeed();
                    } finally {
                        helper.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT)
                                .set(previousDoMobLoot, helper.getLevel().getServer());
                    }
                });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void furnaceSmeltingIsNotInstant(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.RAW_IRON));
        human.getData().setInventoryItem(21, new ItemStack(Items.COAL));
        BlockPos furnacePos = helper.absolutePos(new BlockPos(3, 1, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.FURNACE.defaultBlockState());
        helper.assertTrue(FurnaceOperation.tick(human, furnacePos) == FurnaceOperation.Result.INSERTED, "Furnace operation did not insert input/fuel");
        helper.assertTrue(SurvivalInventory.count(human, Items.IRON_INGOT) == 0
                && ((AbstractFurnaceBlockEntity) helper.getLevel().getBlockEntity(furnacePos)).getItem(2).isEmpty(), "Smelting completed instantly");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void furnaceInsertsBatch(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.BEEF, 8));
        human.getData().setInventoryItem(21, new ItemStack(Items.COAL));
        BlockPos furnacePos = helper.absolutePos(new BlockPos(3, 1, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.FURNACE.defaultBlockState());
        helper.assertTrue(FurnaceOperation.tick(human, furnacePos) == FurnaceOperation.Result.INSERTED, "Furnace batch was not inserted");
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) helper.getLevel().getBlockEntity(furnacePos);
        helper.assertTrue(furnace.getItem(0).getCount() == 8 && furnace.getItem(1).getCount() == 1,
                "Furnace did not receive the expected input batch and fuel");
        helper.assertTrue(SurvivalInventory.count(human, Items.BEEF) == 0, "Furnace batch left duplicate input");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void cookedFoodIsRetrieved(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        BlockPos furnacePos = helper.absolutePos(new BlockPos(3, 1, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.FURNACE.defaultBlockState());
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) helper.getLevel().getBlockEntity(furnacePos);
        furnace.setItem(2, new ItemStack(Items.COOKED_BEEF));
        helper.assertTrue(FurnaceOperation.tick(human, furnacePos) == FurnaceOperation.Result.RETRIEVED,
                "Available furnace output was not retrieved");
        helper.assertTrue(SurvivalInventory.count(human, Items.COOKED_BEEF) == 1,
                "Retrieved cooked food did not reach human inventory");
        helper.assertTrue(furnace.getItem(2).isEmpty(), "Retrieved cooked food remained duplicated in the furnace");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalNavigation", timeoutTicks = 100)
    public static void progressionPacesFurnaceOutputRetrievalAndFacesFurnace(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_PICKAXE));
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        human.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        human.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        human.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        human.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        human.getData().setInventoryItem(20, new ItemStack(Items.DIAMOND_AXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.DIAMOND_SWORD));
        human.getData().setInventoryItem(22, new ItemStack(Items.COOKED_BEEF, 8));
        human.getData().setInventoryItem(23, new ItemStack(Items.GOLDEN_APPLE));
        BlockPos furnacePos = helper.absolutePos(new BlockPos(6, 1, 2));
        helper.setBlock(new BlockPos(6, 1, 2), Blocks.FURNACE.defaultBlockState());
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) helper.getLevel().getBlockEntity(furnacePos);
        furnace.setItem(2, new ItemStack(Items.COOKED_BEEF));
        furnace.setChanged();

        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        helper.assertTrue(goal.canUse() && goal.snapshot().intent() != null
                        && goal.snapshot().intent().task() == SurvivalTask.SMELT,
                "Ready furnace output did not select a smelting action: " + goal.snapshot());
        goal.start();
        human.teleportTo(furnacePos.getX() - 0.5D, furnacePos.getY(), furnacePos.getZ() + 0.5D);
        int initialCookedFood = SurvivalInventory.count(human, Items.COOKED_BEEF);
        helper.assertTrue(initialCookedFood == 8,
                "Furnace fixture did not retain its initial food reserve");
        goal.tick();
        helper.assertTrue(!furnace.getItem(2).isEmpty()
                        && SurvivalInventory.count(human, Items.COOKED_BEEF) == initialCookedFood,
                "Furnace output was retrieved without the station interaction delay");

        for (int tick = 0; tick < 20 && !furnace.getItem(2).isEmpty(); tick++) {
            goal.tick();
            human.getLookControl().tick();
        }

        Vec3 toFurnace = Vec3.atCenterOf(furnacePos).subtract(human.getEyePosition()).normalize();
        helper.assertTrue(SurvivalInventory.count(human, Items.COOKED_BEEF) == initialCookedFood + 1
                        && furnace.getItem(2).isEmpty(),
                "Paced furnace interaction did not retrieve exactly one output");
        helper.assertTrue(human.getViewVector(1.0F).dot(toFurnace) > 0.75D,
                "Human did not look at the furnace while retrieving output: view=" + human.getViewVector(1.0F)
                        + ", target=" + toFurnace);
        goal.stop(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void smeltingDoesNotStartWithoutFuel(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_PICKAXE));
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        human.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        human.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        human.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        human.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        human.getData().setInventoryItem(20, new ItemStack(Items.DIAMOND_AXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.DIAMOND_SWORD));
        human.getData().setInventoryItem(22, new ItemStack(Items.COOKED_BEEF, 8));
        human.getData().setInventoryItem(23, new ItemStack(Items.RAW_GOLD));
        BlockPos furnacePos = helper.absolutePos(new BlockPos(3, 1, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.FURNACE.defaultBlockState());

        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        goal.canUse();
        helper.assertTrue(goal.snapshot().intent() == null
                        || goal.snapshot().intent().task() != SurvivalTask.SMELT,
                "Survival selected a smelting loop without fuel: " + goal.snapshot());
        helper.assertTrue(FurnaceOperation.tick(human, furnacePos) == FurnaceOperation.Result.FAILED,
                "Furnace operation did not fail cleanly without fuel");

        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) helper.getLevel().getBlockEntity(furnacePos);
        furnace.setItem(0, new ItemStack(Items.RAW_GOLD));
        furnace.setChanged();
        helper.assertTrue(FurnaceOperation.tick(human, furnacePos) == FurnaceOperation.Result.FAILED,
                "Dead furnace input remained stuck in WAITING without fuel");
        cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void squadMemberCanShareMaterial(GameTestHelper helper) {
        UUID squad = UUID.randomUUID();
        Human donor = squadHuman(helper, new BlockPos(2, 1, 2), "coldified", squad);
        Human receiver = squadHuman(helper, new BlockPos(4, 1, 2), "elrichmc", squad);
        donor.getData().setInventoryItem(20, new ItemStack(Items.IRON_INGOT, 8));
        int before = SurvivalInventory.count(donor, Items.IRON_INGOT) + SurvivalInventory.count(receiver, Items.IRON_INGOT);
        int moved = SquadMaterialSharing.transfer(donor, receiver, stack -> stack.is(Items.IRON_INGOT), 3, 2);
        int after = SurvivalInventory.count(donor, Items.IRON_INGOT) + SurvivalInventory.count(receiver, Items.IRON_INGOT);
        helper.assertTrue(moved == 3 && before == after && SurvivalInventory.count(receiver, Items.IRON_INGOT) == 3,
                "Sharing failed conservation or exact transfer");
        cleanup(donor, receiver); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void resourceClaimPreventsDoubleTarget(GameTestHelper helper) {
        Human first = human(helper, new BlockPos(2, 1, 2));
        Human second = human(helper, new BlockPos(4, 1, 2));
        BlockPos ore = helper.absolutePos(new BlockPos(3, 1, 2));
        helper.assertTrue(SurvivalClaimManager.claimResource(first, ore), "First resource claim failed");
        helper.assertTrue(!SurvivalClaimManager.claimResource(second, ore), "Second human claimed the same resource");
        cleanup(first, second); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void combatInterruptsProgression(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        Mob zombie = EntityType.ZOMBIE.create(helper.getLevel());
        if (zombie == null) throw new IllegalStateException("Zombie could not be created");
        zombie.moveTo(helper.absolutePos(new BlockPos(4, 1, 2)), 0.0F, 0.0F);
        zombie.setNoAi(true); helper.getLevel().addFreshEntity(zombie);
        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        human.setTarget(zombie);
        helper.assertTrue(!goal.canContinueToUse(), "Combat did not interrupt progression eligibility");
        cleanup(human); zombie.kill(); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalCommandIsolation", timeoutTicks = 40)
    public static void inventoryCommandSelectsNearestHuman(GameTestHelper helper) {
        Human near = human(helper, new BlockPos(2, 1, 2));
        Human far = human(helper, new BlockPos(6, 1, 2));
        near.getData().setInventoryItem(20, new ItemStack(Items.OAK_LOG, 2));
        far.getData().setInventoryItem(20, new ItemStack(Items.COBBLESTONE, 2));

        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 1, 2)));
        helper.assertTrue(HostileHumansCommands.findNearest(helper.getLevel(), origin, 32).orElse(null) == near,
                "Inventory command did not select the nearest Human");
        cleanup(near, far); helper.succeed();
    }

    private static Human human(GameTestHelper helper, BlockPos relativePos) {
        helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, helper.getLevel().getServer());
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 5; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
            for (int y = 1; y <= 5; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
        Human human = ModEntityType.HUMAN1.get().create(helper.getLevel());
        if (human == null) throw new IllegalStateException("Human could not be created");
        human.moveTo(helper.absolutePos(relativePos), 0.0F, 0.0F);
        human.setNoAi(true);
        human.setOnGround(true);
        human.setNoGravity(true);
        human.setHasDecidedToSleepTonight(true);
        human.setSleepingThisNight(false);
        if (!helper.getLevel().addFreshEntity(human)) throw new IllegalStateException("Human could not be added");
        human.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        human.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        for (int slot = 0; slot < human.getData().getInventoryItemsSize(); slot++) human.getData().setInventoryItem(slot, ItemStack.EMPTY);
        return human;
    }

    private static SurvivalProgressionGoal startCraftingGoal(GameTestHelper helper, Human human) {
        SurvivalProgressionGoal goal = new SurvivalProgressionGoal(human);
        for (int attempt = 0; attempt < 12; attempt++) {
            if (goal.canUse()) {
                helper.assertTrue(goal.snapshot().intent() != null
                                && goal.snapshot().intent().task() == SurvivalTask.CRAFT,
                        "Fixture selected a non-crafting action: " + goal.snapshot());
                goal.start();
                return goal;
            }
            human.tickCount += 10;
        }
        throw new AssertionError("Crafting fixture never selected a table action: " + goal.snapshot());
    }

    private static void tickGoal(SurvivalProgressionGoal goal, int ticks) {
        for (int tick = 0; tick < ticks && goal.canContinueToUse(); tick++) goal.tick();
    }

    private static Human squadHuman(GameTestHelper helper, BlockPos pos, String persona, UUID squad) {
        Human human = human(helper, pos);
        // A previous test may still own this fixed persona for one tick while
        // its entity removal is processed. Sharing only needs a valid persona,
        // so use the normal available-persona fallback in that case.
        boolean assigned = human.setPersonaId(persona)
                // Keep the sharing assertion in one faction if the requested
                // unique persona is still reserved by a previous test.
                || human.setPersonaId("juanclean")
                || human.setPersonaId("ymiau")
                || human.assignRandomPersona();
        if (!assigned || !human.setSquadId(squad)) throw new IllegalStateException("Could not configure squad human");
        return human;
    }

    private static void prepareHunter(Human human) {
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        human.getData().setInventoryItem(20, new ItemStack(Items.STONE_PICKAXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(22, new ItemStack(Items.STICK, 16));
        human.getData().setInventoryItem(23, new ItemStack(Items.IRON_INGOT, 32));
        human.getData().setInventoryItem(24, new ItemStack(Items.GOLDEN_APPLE));
    }

    private static void prepareDiamondNavigator(Human human) {
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        human.getData().setInventoryItem(20, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.STONE_SWORD));
        human.getData().setInventoryItem(22, new ItemStack(Items.STICK, 5));
        human.getData().setInventoryItem(23, new ItemStack(Items.COOKED_BEEF, 8));
        human.getData().setInventoryItem(24, new ItemStack(Items.GOLDEN_APPLE));
        human.getData().setInventoryItem(25, new ItemStack(Items.COAL, 2));
    }

    private static void expandGround(GameTestHelper helper) {
        for (int x = -2; x <= 10; x++) for (int z = -2; z <= 8; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
            for (int y = 1; y <= 5; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
    }

    private static void isolateTemplateFixtures(Human human) {
        human.level().getEntitiesOfClass(Human.class, human.getBoundingBox().inflate(8.0D)).stream()
                .filter(other -> other != human)
                .forEach(other -> {
                    SurvivalClaimManager.releaseAll(other);
                    other.discard();
                });
        human.level().getEntitiesOfClass(ItemEntity.class, human.getBoundingBox().inflate(8.0D))
                .forEach(ItemEntity::discard);
    }

    private static void cleanup(Human... humans) {
        // Killing equipped fixtures creates untagged item drops that nearby
        // integration batches can pursue. Discard preserves batch isolation.
        for (Human human : humans) human.discard();
    }
}
