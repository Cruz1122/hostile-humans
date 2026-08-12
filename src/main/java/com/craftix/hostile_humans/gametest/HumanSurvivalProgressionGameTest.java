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
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
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
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
            for (int y = 1; y <= 5; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
        Human human = ModEntityType.HUMAN1.get().create(helper.getLevel());
        if (human == null) throw new IllegalStateException("Human could not be created");
        human.moveTo(helper.absolutePos(relativePos), 0.0F, 0.0F);
        human.setNoAi(true);
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

    private static void cleanup(Human... humans) {
        for (Human human : humans) human.kill();
    }
}
