package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.ai.action.BreakObstacleAction;
import com.craftix.hostile_humans.entity.ai.action.BridgeGapAction;
import com.craftix.hostile_humans.entity.ai.action.MiningSpeedCalculator;
import com.craftix.hostile_humans.entity.ai.action.MiningToolSelector;
import com.craftix.hostile_humans.entity.ai.action.PillarUpAction;
import com.craftix.hostile_humans.entity.ai.action.WorldActionContext;
import com.craftix.hostile_humans.entity.ai.action.WorldActionResult;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class HumanWorldNavigationGameTest {
    private static final String TEMPLATE = "human_smoke";

    private HumanWorldNavigationGameTest() {}

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "worldNavigation", timeoutTicks = 40)
    public static void pillarRequiresInventory(GameTestHelper helper) {
        Setup setup = setup(helper, new BlockPos(2, 1, 2), new BlockPos(2, 4, 2));
        setup.human.setTarget(setup.target);
        helper.assertTrue(!PillarUpAction.tryPlace(setup.human), "Pillar started without an inventory block");
        helper.assertTrue(helper.getLevel().getBlockState(setup.human.blockPosition()).isAir(), "Pillar modified the world without inventory");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "worldNavigationGameRule", timeoutTicks = 40)
    public static void pillarRespectsMobGriefing(GameTestHelper helper) {
        var gameRule = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean previousValue = gameRule.get();
        Setup setup = setup(helper, new BlockPos(2, 1, 2), new BlockPos(2, 4, 2));
        setup.human.getData().setInventoryItem(0, new ItemStack(Items.COBBLESTONE, 2));
        try {
            gameRule.set(false, helper.getLevel().getServer());
            setup.human.setTarget(setup.target);
            helper.assertTrue(!PillarUpAction.tryPlace(setup.human), "Pillar ignored mobGriefing=false");
            helper.assertTrue(setup.human.getData().getInventoryItem(0).getCount() == 2, "Rejected pillar consumed a block");
            helper.succeed();
        } finally {
            gameRule.set(previousValue, helper.getLevel().getServer());
        }
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "worldNavigation", timeoutTicks = 40)
    public static void bridgeRequiresInventory(GameTestHelper helper) {
        Setup setup = setup(helper, new BlockPos(1, 1, 2), new BlockPos(5, 1, 2));
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.AIR.defaultBlockState());
        helper.setBlock(new BlockPos(3, 0, 2), Blocks.AIR.defaultBlockState());
        setup.human.setTarget(setup.target);
        helper.assertTrue(!BridgeGapAction.tryPlace(setup.human), "Bridge started without inventory");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(2, 0, 2))).isAir(), "Bridge changed the world without blocks");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "worldNavigationGameRule", timeoutTicks = 40)
    public static void bridgeRespectsMobGriefing(GameTestHelper helper) {
        var gameRule = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean previousValue = gameRule.get();
        Setup setup = setup(helper, new BlockPos(1, 1, 2), new BlockPos(5, 1, 2));
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.AIR.defaultBlockState());
        setup.human.getData().setInventoryItem(0, new ItemStack(Items.COBBLESTONE, 2));
        try {
            gameRule.set(false, helper.getLevel().getServer());
            setup.human.setTarget(setup.target);
            helper.assertTrue(!BridgeGapAction.tryPlace(setup.human), "Bridge ignored mobGriefing=false");
            helper.assertTrue(setup.human.getData().getInventoryItem(0).getCount() == 2, "Rejected bridge consumed a block");
            helper.succeed();
        } finally {
            gameRule.set(previousValue, helper.getLevel().getServer());
        }
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "worldNavigation", timeoutTicks = 40)
    public static void bridgeCoversFullGapWhenFeetAreOverFirstAirCell(GameTestHelper helper) {
        Setup setup = setup(helper, new BlockPos(2, 1, 2), new BlockPos(6, 1, 2));
        for (int x = 2; x <= 4; x++) helper.setBlock(new BlockPos(x, 0, 2), Blocks.AIR.defaultBlockState());
        setup.human.getData().setInventoryItem(0, new ItemStack(Items.COBBLESTONE, 3));
        setup.human.setTarget(setup.target);
        BridgeGapAction action = new BridgeGapAction();
        WorldActionContext context = new WorldActionContext(setup.human);
        helper.assertTrue(action.canStart(context), "Bridge did not detect the gap under the human's feet");

        WorldActionResult result = WorldActionResult.RUNNING;
        for (int tick = 0; result == WorldActionResult.RUNNING && tick < 30; tick++) {
            result = action.tick(context);
        }

        helper.assertTrue(result == WorldActionResult.SUCCESS, "Bridge did not finish covering the full gap");
        for (int x = 2; x <= 4; x++) {
            BlockPos position = helper.absolutePos(new BlockPos(x, 0, 2));
            helper.assertTrue(helper.getLevel().getBlockState(position).is(Blocks.COBBLESTONE),
                    "Bridge left gap cell " + x + " uncovered");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "worldNavigation", timeoutTicks = 40)
    public static void bridgeIgnoresItemEntityInGap(GameTestHelper helper) {
        Setup setup = setup(helper, new BlockPos(1, 1, 2), new BlockPos(6, 1, 2));
        for (int x = 2; x <= 4; x++) helper.setBlock(new BlockPos(x, 0, 2), Blocks.AIR.defaultBlockState());
        BlockPos itemPosition = helper.absolutePos(new BlockPos(3, 0, 2));
        ItemEntity droppedItem = new ItemEntity(helper.getLevel(), itemPosition.getX() + 0.5D,
                itemPosition.getY() + 0.25D, itemPosition.getZ() + 0.5D,
                new ItemStack(Items.CHAINMAIL_LEGGINGS));
        droppedItem.setUnlimitedLifetime();
        helper.getLevel().addFreshEntity(droppedItem);
        setup.human.getData().setInventoryItem(0, new ItemStack(Items.COBBLESTONE, 3));
        setup.human.setTarget(setup.target);

        WorldActionResult result = runBridgeAction(setup.human);

        helper.assertTrue(result == WorldActionResult.SUCCESS,
                "Item entity incorrectly blocked bridge placement; result=" + result);
        for (int x = 2; x <= 4; x++) {
            BlockPos position = helper.absolutePos(new BlockPos(x, 0, 2));
            helper.assertTrue(helper.getLevel().getBlockState(position).is(Blocks.COBBLESTONE),
                    "Bridge left gap cell " + x + " uncovered around an item entity");
        }
        helper.assertTrue(droppedItem.isAlive(), "Bridge placement removed the item entity");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "worldNavigation", timeoutTicks = 40)
    public static void bridgeRespectsLivingEntityInGap(GameTestHelper helper) {
        Setup setup = setup(helper, new BlockPos(1, 1, 2), new BlockPos(6, 1, 2));
        for (int x = 2; x <= 4; x++) helper.setBlock(new BlockPos(x, 0, 2), Blocks.AIR.defaultBlockState());
        BlockPos blockerPosition = helper.absolutePos(new BlockPos(3, 0, 2));
        Mob blocker = EntityType.ZOMBIE.create(helper.getLevel());
        if (blocker == null) throw new IllegalStateException("Could not create bridge blocker");
        blocker.moveTo(blockerPosition.getX() + 0.5D, blockerPosition.getY(), blockerPosition.getZ() + 0.5D,
                0.0F, 0.0F);
        blocker.setNoAi(true);
        blocker.setPersistenceRequired();
        helper.getLevel().addFreshEntity(blocker);
        setup.human.getData().setInventoryItem(0, new ItemStack(Items.COBBLESTONE, 3));
        setup.human.setTarget(setup.target);

        BridgeGapAction action = new BridgeGapAction();
        WorldActionResult result = runBridgeAction(action, setup.human);

        helper.assertTrue(result == WorldActionResult.FAILED,
                "Bridge did not reject a living entity in the gap; result=" + result);
        helper.assertTrue(action.placedBlockCount() == 1,
                "Bridge did not report its partial placement before failing");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(2, 0, 2))).is(Blocks.COBBLESTONE),
                "Bridge did not place the unobstructed first block");
        helper.assertTrue(helper.getLevel().getBlockState(blockerPosition).isAir(),
                "Bridge placed a block inside a living entity");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "worldNavigation", timeoutTicks = 40)
    public static void miningRequiresCorrectTool(GameTestHelper helper) {
        Setup setup = setup(helper, new BlockPos(2, 1, 2), new BlockPos(5, 1, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.STONE.defaultBlockState());
        setup.human.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        setup.human.setTarget(setup.target);
        helper.assertTrue(!MiningToolSelector.select(setup.human, Blocks.STONE.defaultBlockState()).isPresent(), "Sword was selected for stone mining");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(3, 1, 2))).is(Blocks.STONE), "Stone disappeared without a tool");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "worldNavigation", timeoutTicks = 40)
    public static void miningSelectsToolForBlockMaterial(GameTestHelper helper) {
        Setup setup = setup(helper, new BlockPos(2, 1, 2), new BlockPos(5, 1, 2));
        setup.human.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        setup.human.getData().setInventoryItem(0, new ItemStack(Items.IRON_PICKAXE));
        setup.human.getData().setInventoryItem(1, new ItemStack(Items.IRON_SHOVEL));

        helper.assertTrue(MiningToolSelector.select(setup.human, Blocks.STONE.defaultBlockState())
                        .orElse(ItemStack.EMPTY).is(Items.IRON_PICKAXE),
                "Stone did not select the pickaxe");
        helper.assertTrue(MiningToolSelector.select(setup.human, Blocks.DIRT.defaultBlockState())
                        .orElse(ItemStack.EMPTY).is(Items.IRON_SHOVEL),
                "Dirt did not select the shovel");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "worldNavigation", timeoutTicks = 40)
    public static void miningSpeedRespectsBlockHardness(GameTestHelper helper) {
        Setup setup = setup(helper, new BlockPos(2, 1, 2), new BlockPos(5, 1, 2));
        setup.human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        BlockPos stone = helper.absolutePos(new BlockPos(3, 1, 2));
        BlockPos obsidian = helper.absolutePos(new BlockPos(4, 1, 2));
        float stoneProgress = MiningSpeedCalculator.progressPerTick(setup.human,
                Blocks.STONE.defaultBlockState(), stone, setup.human.getMainHandItem(), true);
        float obsidianProgress = MiningSpeedCalculator.progressPerTick(setup.human,
                Blocks.OBSIDIAN.defaultBlockState(), obsidian, setup.human.getMainHandItem(), true);
        helper.assertTrue(stoneProgress > 0.0F && obsidianProgress > 0.0F,
                "A correct pickaxe produced no mining progress");
        helper.assertTrue(stoneProgress > obsidianProgress,
                "A harder block was not slower to mine");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "worldNavigation", timeoutTicks = 80)
    public static void miningIsProgressiveNotInstant(GameTestHelper helper) {
        Setup setup = setup(helper, new BlockPos(2, 1, 2), new BlockPos(5, 1, 2));
        BlockPos obstacle = helper.absolutePos(new BlockPos(3, 1, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.STONE.defaultBlockState());
        setup.human.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        setup.human.setTarget(setup.target);
        BreakObstacleAction action = new BreakObstacleAction();
        action.canStart(new com.craftix.hostile_humans.entity.ai.action.WorldActionContext(setup.human));
        action.tick(new com.craftix.hostile_humans.entity.ai.action.WorldActionContext(setup.human));
        helper.assertTrue(!helper.getLevel().getBlockState(obstacle).isAir(), "Stone was broken instantly on the first mining tick");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "worldNavigationGameRule", timeoutTicks = 40)
    public static void miningRespectsMobGriefing(GameTestHelper helper) {
        var gameRule = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean previousValue = gameRule.get();
        Setup setup = setup(helper, new BlockPos(2, 1, 2), new BlockPos(5, 1, 2));
        BlockPos obstacle = helper.absolutePos(new BlockPos(3, 1, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.STONE.defaultBlockState());
        setup.human.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        try {
            gameRule.set(false, helper.getLevel().getServer());
            setup.human.setTarget(setup.target);
            helper.assertTrue(!BreakObstacleAction.tryBreak(setup.human), "Mining ignored mobGriefing=false");
            helper.assertTrue(helper.getLevel().getBlockState(obstacle).is(Blocks.STONE), "Mining removed a block with mobGriefing=false");
            helper.succeed();
        } finally {
            gameRule.set(previousValue, helper.getLevel().getServer());
        }
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "worldNavigationMiningIntegration", timeoutTicks = 120)
    public static void controllerBreaksAdjacentObstacleDuringRealCombat(GameTestHelper helper) {
        Setup setup = liveSetup(helper, new BlockPos(2, 1, 2), new BlockPos(5, 1, 2));
        BlockPos obstacle = helper.absolutePos(new BlockPos(3, 1, 2));
        for (int x = 0; x <= 7; x++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(new BlockPos(x, y, 1), Blocks.BEDROCK.defaultBlockState());
                helper.setBlock(new BlockPos(x, y, 3), Blocks.BEDROCK.defaultBlockState());
            }
        }
        for (int y = 1; y <= 3; y++) {
            helper.setBlock(new BlockPos(0, y, 2), Blocks.BEDROCK.defaultBlockState());
            helper.setBlock(new BlockPos(7, y, 2), Blocks.BEDROCK.defaultBlockState());
        }
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.STONE.defaultBlockState());
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.STONE.defaultBlockState());
        setup.human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        setup.human.setTarget(setup.target);
        helper.assertTrue(new BreakObstacleAction().canStart(
                        new com.craftix.hostile_humans.entity.ai.action.WorldActionContext(setup.human)),
                "Mining regression setup does not satisfy BreakObstacleAction preconditions");

        helper.startSequence()
                .thenIdle(80)
                .thenExecute(() -> {
                    if (!helper.getLevel().getBlockState(obstacle).isAir()) {
                        helper.fail("Controller did not mine the adjacent obstacle during real combat; human="
                                + setup.human.blockPosition() + ", target=" + setup.human.getTarget()
                                + ", active=" + setup.human.getTacticalWorldActionController().activeAction()
                                + ", broken=" + setup.human.getTacticalWorldActionController().brokenThisPursuit());
                    } else {
                        helper.succeed();
                    }
                });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "worldNavigationPillarIntegration", timeoutTicks = 140)
    public static void controllerPillarsAtUnreachableRaisedTarget(GameTestHelper helper) {
        Setup setup = liveSetup(helper, new BlockPos(2, 1, 2), new BlockPos(5, 4, 2));
        for (int x = 0; x <= 7; x++) {
            for (int y = 1; y <= 5; y++) {
                helper.setBlock(new BlockPos(x, y, 1), Blocks.BEDROCK.defaultBlockState());
                helper.setBlock(new BlockPos(x, y, 3), Blocks.BEDROCK.defaultBlockState());
            }
        }
        for (int y = 1; y <= 5; y++) {
            helper.setBlock(new BlockPos(0, y, 2), Blocks.BEDROCK.defaultBlockState());
        }
        for (int x = 4; x <= 7; x++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(new BlockPos(x, y, 2), Blocks.STONE.defaultBlockState());
            }
        }
        setup.human.getData().setInventoryItem(0, new ItemStack(Items.COBBLESTONE, 6));
        setup.human.setTarget(setup.target);
        helper.assertTrue(new PillarUpAction().canStart(
                        new com.craftix.hostile_humans.entity.ai.action.WorldActionContext(setup.human)),
                "Pillar regression setup does not satisfy PillarUpAction preconditions");

        helper.startSequence()
                .thenIdle(100)
                .thenExecute(() -> {
                    boolean placedPillar = setup.human.getTacticalWorldActionController().placedThisPursuit() > 0;
                    if (!placedPillar) {
                        helper.fail("Controller did not place a pillar for an unreachable raised target; human="
                                + setup.human.blockPosition() + ", target=" + setup.human.getTarget()
                                + ", active=" + setup.human.getTacticalWorldActionController().activeAction()
                                + ", blocks=" + setup.human.getData().getInventoryItem(0).getCount());
                    } else {
                        helper.succeed();
                    }
                });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "worldNavigationBridgeIntegration", timeoutTicks = 140)
    public static void controllerBridgesUnreachableGapDuringRealCombat(GameTestHelper helper) {
        Setup setup = liveSetup(helper, new BlockPos(1, 1, 2), new BlockPos(6, 1, 2));
        for (int x = 0; x <= 7; x++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(new BlockPos(x, y, 1), Blocks.BEDROCK.defaultBlockState());
                helper.setBlock(new BlockPos(x, y, 3), Blocks.BEDROCK.defaultBlockState());
            }
        }
        for (int y = 1; y <= 3; y++) {
            helper.setBlock(new BlockPos(0, y, 2), Blocks.BEDROCK.defaultBlockState());
            helper.setBlock(new BlockPos(7, y, 2), Blocks.BEDROCK.defaultBlockState());
        }
        // Two blocks of headroom keep the corridor traversable after bridging,
        // while the ceiling prevents the live combat navigation from jumping
        // the entire fixture before the world-action controller can detect a stall.
        for (int x = 1; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 3, 2), Blocks.BEDROCK.defaultBlockState());
        }
        for (int x = 2; x <= 4; x++) helper.setBlock(new BlockPos(x, 0, 2), Blocks.AIR.defaultBlockState());
        setup.human.getData().setInventoryItem(0, new ItemStack(Items.COBBLESTONE, 6));
        setup.human.setTarget(setup.target);
        helper.assertTrue(new BridgeGapAction().canStart(
                        new com.craftix.hostile_humans.entity.ai.action.WorldActionContext(setup.human)),
                "Bridge regression setup does not satisfy BridgeGapAction preconditions");

        helper.startSequence()
                .thenIdle(100)
                .thenExecute(() -> {
                    boolean completeBridge = true;
                    for (int x = 2; x <= 4; x++) {
                        completeBridge &= helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, 0, 2))).is(Blocks.COBBLESTONE);
                    }
                    int placedBlocks = setup.human.getTacticalWorldActionController().placedThisPursuit();
                    if (!completeBridge || placedBlocks != 3) {
                        helper.fail("Controller did not complete and count the three-block bridge; human="
                                + setup.human.blockPosition() + ", target=" + setup.human.getTarget()
                                + ", active=" + setup.human.getTacticalWorldActionController().activeAction()
                                + ", blocks=" + setup.human.getData().getInventoryItem(0).getCount()
                                + ", placed=" + placedBlocks);
                    } else {
                        helper.succeed();
                    }
                });
    }

    private static Setup setup(GameTestHelper helper, BlockPos humanLocal, BlockPos targetLocal) {
        helper.killAllEntities();
        helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, helper.getLevel().getServer());
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 5; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
            for (int y = 1; y <= 6; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
        Human human = ModEntityType.HUMAN1.get().create(helper.getLevel());
        LivingEntity target = EntityType.ZOMBIE.create(helper.getLevel());
        if (human == null || target == null) throw new IllegalStateException("Could not create navigation test entities");
        human.moveTo(helper.absolutePos(humanLocal), 0.0F, 0.0F);
        target.moveTo(helper.absolutePos(targetLocal), 0.0F, 0.0F);
        human.setNoAi(true);
        if (target instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setPersistenceRequired();
        }
        helper.getLevel().addFreshEntity(human);
        helper.getLevel().addFreshEntity(target);
        return new Setup(human, target);
    }

    private static Setup liveSetup(GameTestHelper helper, BlockPos humanLocal, BlockPos targetLocal) {
        Setup setup = setup(helper, humanLocal, targetLocal);
        setup.human.setNoAi(false);
        setup.human.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        setup.human.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        for (int slot = 0; slot < setup.human.getData().getInventoryItemsSize(); slot++) {
            setup.human.getData().setInventoryItem(slot, ItemStack.EMPTY);
        }
        setup.target.setInvulnerable(true);
        return setup;
    }

    private static WorldActionResult runBridgeAction(Human human) {
        return runBridgeAction(new BridgeGapAction(), human);
    }

    private static WorldActionResult runBridgeAction(BridgeGapAction action, Human human) {
        WorldActionContext context = new WorldActionContext(human);
        if (!action.canStart(context)) return WorldActionResult.FAILED;
        WorldActionResult result = WorldActionResult.RUNNING;
        for (int tick = 0; result == WorldActionResult.RUNNING && tick < 30; tick++) {
            result = action.tick(context);
        }
        return result;
    }

    private record Setup(Human human, LivingEntity target) {}
}
