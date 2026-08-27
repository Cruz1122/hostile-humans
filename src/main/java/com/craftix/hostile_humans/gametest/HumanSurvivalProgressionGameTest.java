package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.ai.survival.FurnaceOperation;
import com.craftix.hostile_humans.entity.ai.survival.LocalResourceScanner;
import com.craftix.hostile_humans.entity.ai.survival.ProgressionCraftingPolicy;
import com.craftix.hostile_humans.entity.ai.survival.ProgressiveBlockBreaker;
import com.craftix.hostile_humans.entity.ai.survival.SquadMaterialSharing;
import com.craftix.hostile_humans.entity.ai.survival.SquadNeed;
import com.craftix.hostile_humans.entity.ai.survival.SquadNeeds;
import com.craftix.hostile_humans.entity.ai.survival.SquadNeedsEvaluator;
import com.craftix.hostile_humans.entity.ai.survival.SurvivalClaimManager;
import com.craftix.hostile_humans.entity.ai.survival.SurvivalInventory;
import com.craftix.hostile_humans.entity.ai.survival.SurvivalProgressionGoal;
import com.craftix.hostile_humans.entity.ai.survival.SurvivalRecipeService;
import com.craftix.hostile_humans.entity.ai.goal.ItemLootGoal;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
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
    public static void doesNotFarmWoodWhenStocked(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.OAK_LOG, 16));
        human.getData().setInventoryItem(21, new ItemStack(Items.IRON_PICKAXE));
        human.getData().setInventoryItem(22, new ItemStack(Items.IRON_AXE));
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

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void hiddenOreIsNotDetected(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        BlockPos relative = new BlockPos(4, 2, 2);
        helper.setBlock(relative, Blocks.IRON_ORE.defaultBlockState());
        for (var direction : net.minecraft.core.Direction.values()) helper.setBlock(relative.relative(direction), Blocks.STONE.defaultBlockState());
        helper.assertTrue(LocalResourceScanner.find(human, SquadNeed.IRON).isEmpty(), "Completely hidden iron was detected");
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

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void progressiveBreakingSwingsMainHand(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        BlockPos log = helper.absolutePos(new BlockPos(3, 1, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.OAK_LOG.defaultBlockState());
        new ProgressiveBlockBreaker(human, log).tick();
        helper.assertTrue(human.swinging, "Progressive breaking did not animate the main hand");
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
        human.tickCount++;
        helper.assertTrue(goal.canUse(), "Progression paused instead of immediately selecting the next resource");
        goal.start();
        for (int tick = 0; tick < 80 && helper.getLevel().getBlockState(second).is(Blocks.OAK_LOG); tick++) goal.tick();
        helper.assertTrue(!helper.getLevel().getBlockState(second).is(Blocks.OAK_LOG), "Second resource was not broken continuously");
        goal.stop(); cleanup(human); helper.succeed();
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

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void elevatedDropWithoutPickupReachIsIgnored(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
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
        ItemLootGoal goal = new ItemLootGoal(human, 1.0D);
        helper.assertTrue(goal.canUse(), "Reached useful drop was not selected");
        goal.start();
        goal.tick();
        helper.assertTrue(drop.isRemoved(), "Reached drop remained in the world");
        helper.assertTrue(human.getData().getInventoryItem(20).getCount() == 4,
                "Reached drop did not stack into the existing inventory slot");
        goal.stop(); cleanup(human); helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void humanInventoryMatchesPlayerInventorySize(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        helper.assertTrue(human.getData().getInventoryItemsSize() == 36,
                "Human inventory does not match the player's 36 storage slots");
        cleanup(human); helper.succeed();
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

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void exploreFinishesWhenNavigationDone(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
        human.getData().setInventoryItem(20, new ItemStack(Items.STONE_AXE));
        human.getData().setInventoryItem(21, new ItemStack(Items.STONE_SWORD));
        human.getData().setInventoryItem(22, new ItemStack(Items.STICK, 16));
        human.getData().setInventoryItem(23, new ItemStack(Items.IRON_INGOT, 16));
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

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 240)
    public static void squadHuntingKillsAnimalWithoutSynchronizedStalls(GameTestHelper helper) {
        Human first = human(helper, new BlockPos(2, 1, 2));
        Human second = human(helper, new BlockPos(2, 1, 3));
        Human third = human(helper, new BlockPos(3, 1, 2));
        prepareHunter(first);
        prepareHunter(second);
        prepareHunter(third);
        Cow cow = EntityType.COW.create(helper.getLevel());
        if (cow == null) throw new IllegalStateException("Cow could not be created");
        cow.moveTo(helper.absolutePos(new BlockPos(3, 1, 3)), 0.0F, 0.0F);
        cow.setNoAi(true);
        helper.getLevel().addFreshEntity(cow);
        cow.setHealth(4.0F);
        List<SurvivalProgressionGoal> goals = List.of(new SurvivalProgressionGoal(first),
                new SurvivalProgressionGoal(second), new SurvivalProgressionGoal(third));
        for (SurvivalProgressionGoal goal : goals) {
            helper.assertTrue(goal.canUse(), "Squad hunter did not select the reachable animal");
            goal.start();
        }
        for (int tick = 0; tick < 40 && cow.isAlive(); tick++) {
            for (Human hunter : List.of(first, second, third)) {
                cow.invulnerableTime = 0;
                cow.hurtTime = 0;
                hunter.doHurtTarget(cow);
            }
        }
        helper.assertTrue(!cow.isAlive(), "Three staggered hunters did not kill one passive animal promptly");
        goals.forEach(SurvivalProgressionGoal::stop);
        cleanup(first); cleanup(second); cleanup(third); cow.kill(); helper.succeed();
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

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 260)
    public static void cookedFoodIsRetrieved(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.BEEF));
        human.getData().setInventoryItem(21, new ItemStack(Items.COAL));
        BlockPos furnacePos = helper.absolutePos(new BlockPos(3, 1, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.FURNACE.defaultBlockState());
        FurnaceOperation.tick(human, furnacePos);
        helper.startSequence().thenIdle(220).thenExecute(() -> {
            FurnaceOperation.tick(human, furnacePos);
            helper.assertTrue(SurvivalInventory.count(human, Items.COOKED_BEEF) == 1, "Cooked food was not retrieved");
            cleanup(human); helper.succeed();
        });
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

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "survivalProgression", timeoutTicks = 40)
    public static void goldenAppleNotOvercrafted(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.GOLDEN_APPLE));
        SquadNeeds covered = new SquadNeeds(java.util.Map.of());
        helper.assertTrue(!ProgressionCraftingPolicy.shouldCraftGoldenApple(human, covered), "Stock target allowed another golden apple");
        cleanup(human); helper.succeed();
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
        if (!helper.getLevel().addFreshEntity(human)) throw new IllegalStateException("Human could not be added");
        human.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        human.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        for (int slot = 0; slot < human.getData().getInventoryItemsSize(); slot++) human.getData().setInventoryItem(slot, ItemStack.EMPTY);
        return human;
    }

    private static Human squadHuman(GameTestHelper helper, BlockPos pos, String persona, UUID squad) {
        Human human = human(helper, pos);
        if (!human.setPersonaId(persona) || !human.setSquadId(squad)) throw new IllegalStateException("Could not configure squad human");
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

    private static void cleanup(Human... humans) {
        for (Human human : humans) human.kill();
    }
}
