package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import com.craftix.hostile_humans.entity.ai.goal.ChestLootGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class HumanChestLootGameTest {
    private static final String TEMPLATE = "human_smoke";

    private HumanChestLootGameTest() {}

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "chestLooting", timeoutTicks = 180)
    public static void visible_chest_gets_looted(GameTestHelper helper) {
        Human human = createHuman(helper);
        ChestBlockEntity chest = createChest(helper, new BlockPos(3, 1, 2));
        chest.setItem(0, new ItemStack(Items.IRON_SWORD));
        ChestLootGoal goal = new ChestLootGoal(human, 0.8D);
        helper.assertTrue(goal.canUse(), "Visible chest was not selected");
        goal.start();
        helper.startSequence().thenExecute(goal::tick).thenExecute(() ->
                helper.assertTrue(chest.getItem(0).is(Items.IRON_SWORD), "Chest was looted instantly"))
                .thenExecute(() -> tickGoal(goal, 70)).thenExecute(() -> {
            helper.assertTrue(chest.getItem(0).isEmpty(), "Visible chest item remained in chest");
            helper.assertTrue(contains(human, Items.IRON_SWORD), "Useful chest item did not reach human inventory");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "chestLooting", timeoutTicks = 120)
    public static void hidden_chest_is_ignored(GameTestHelper helper) {
        Human human = createHuman(helper);
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.STONE.defaultBlockState());
        ChestBlockEntity chest = createChest(helper, new BlockPos(4, 1, 2));
        chest.setItem(0, new ItemStack(Items.IRON_SWORD));
        ChestLootGoal goal = new ChestLootGoal(human, 0.8D);
        helper.assertTrue(!goal.canUse(), "Human selected a chest behind a solid wall");
        helper.startSequence().thenIdle(5).thenExecute(() -> {
            helper.assertTrue(chest.getItem(0).is(Items.IRON_SWORD), "Human detected a chest behind a solid wall");
            helper.assertTrue(human.distanceToSqr(helper.absolutePos(new BlockPos(4, 1, 2)).getCenter()) > 2.0D,
                    "Human navigated toward the hidden chest");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "chestLooting", timeoutTicks = 180)
    public static void useful_items_are_taken(GameTestHelper helper) {
        Human human = createHuman(helper);
        ChestBlockEntity chest = createChest(helper, new BlockPos(3, 1, 2));
        chest.setItem(0, new ItemStack(Items.DIAMOND_SWORD));
        chest.setItem(1, new ItemStack(Items.BREAD));
        chest.setItem(2, new ItemStack(Items.IRON_INGOT));
        chest.setItem(3, new ItemStack(Items.DIAMOND_CHESTPLATE));
        chest.setItem(4, new ItemStack(Items.TOTEM_OF_UNDYING));
        ChestLootGoal goal = new ChestLootGoal(human, 0.8D);
        helper.assertTrue(goal.canUse(), "Visible chest was not selected");
        goal.start();
        helper.startSequence().thenExecute(() -> tickGoal(goal, 90)).thenIdle(1).thenExecute(() -> {
            helper.assertTrue(chest.getItem(0).isEmpty(), "Useful weapon remained in chest");
            helper.assertTrue(chest.getItem(1).isEmpty(), "Useful food remained in chest");
            helper.assertTrue(chest.getItem(2).isEmpty(), "Progression material remained in chest");
            helper.assertTrue(chest.getItem(3).isEmpty(), "Useful armor remained in chest");
            helper.assertTrue(chest.getItem(4).isEmpty(), "Useful totem remained in chest");
            helper.assertTrue(contains(human, Items.DIAMOND_SWORD) && contains(human, Items.BREAD),
                    "Useful items did not reach human inventory");
            helper.assertTrue(human.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE),
                    "Looted armor was not equipped immediately");
            helper.assertTrue(contains(human, Items.TOTEM_OF_UNDYING), "Looted totem was not retained");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "chestLooting", timeoutTicks = 200)
    public static void netherite_gear_is_taken_and_equipped(GameTestHelper helper) {
        Human human = createHuman(helper);
        ChestBlockEntity chest = createChest(helper, new BlockPos(3, 1, 2));
        chest.setItem(0, new ItemStack(Items.NETHERITE_HELMET));
        chest.setItem(1, new ItemStack(Items.NETHERITE_CHESTPLATE));
        chest.setItem(2, new ItemStack(Items.NETHERITE_LEGGINGS));
        chest.setItem(3, new ItemStack(Items.NETHERITE_BOOTS));
        chest.setItem(4, new ItemStack(Items.NETHERITE_SWORD));
        chest.setItem(5, new ItemStack(Items.NETHERITE_AXE));
        chest.setItem(6, new ItemStack(Items.NETHERITE_PICKAXE));
        chest.setItem(7, new ItemStack(Items.NETHERITE_SHOVEL));
        chest.setItem(8, new ItemStack(Items.NETHERITE_HOE));
        ChestLootGoal goal = new ChestLootGoal(human, 0.8D);
        helper.assertTrue(goal.canUse(), "Visible chest with netherite gear was not selected");
        goal.start();
        helper.startSequence().thenExecute(() -> tickGoal(goal, 130)).thenIdle(1).thenExecute(() -> {
            for (int slot = 0; slot <= 8; slot++) {
                helper.assertTrue(chest.getItem(slot).isEmpty(), "Netherite item remained in chest slot " + slot);
            }
            helper.assertTrue(human.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(Items.NETHERITE_HELMET),
                    "Looted netherite helmet was not equipped");
            helper.assertTrue(human.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE),
                    "Looted netherite chestplate was not equipped");
            helper.assertTrue(human.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).is(Items.NETHERITE_LEGGINGS),
                    "Looted netherite leggings were not equipped");
            helper.assertTrue(human.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(Items.NETHERITE_BOOTS),
                    "Looted netherite boots were not equipped");
            helper.assertTrue(contains(human, Items.NETHERITE_SWORD), "Looted netherite sword was not retained");
            helper.assertTrue(contains(human, Items.NETHERITE_AXE), "Looted netherite axe was not retained");
            helper.assertTrue(contains(human, Items.NETHERITE_PICKAXE), "Looted netherite pickaxe was not retained");
            helper.assertTrue(contains(human, Items.NETHERITE_SHOVEL), "Looted netherite shovel was not retained");
            helper.assertTrue(contains(human, Items.NETHERITE_HOE), "Looted netherite hoe was not retained");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "chestLooting", timeoutTicks = 160)
    public static void full_inventory_does_not_delete_items(GameTestHelper helper) {
        Human human = createHuman(helper);
        for (int slot = 20; slot < 30; slot++) human.getData().setInventoryItem(slot, new ItemStack(Items.STONE));
        ChestBlockEntity chest = createChest(helper, new BlockPos(3, 1, 2));
        chest.setItem(0, new ItemStack(Items.IRON_SWORD));
        ChestLootGoal goal = new ChestLootGoal(human, 0.8D);
        helper.assertTrue(goal.canUse(), "Visible chest was not selected");
        goal.start();
        helper.startSequence().thenExecute(() -> tickGoal(goal, 70)).thenExecute(() -> {
            helper.assertTrue(chest.getItem(0).is(Items.IRON_SWORD), "Full inventory deleted the chest item");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "chestLooting", timeoutTicks = 120)
    public static void combat_interrupts_chest_looting(GameTestHelper helper) {
        Human human = createHuman(helper);
        ChestBlockEntity chest = createChest(helper, new BlockPos(3, 1, 2));
        chest.setItem(0, new ItemStack(Items.IRON_SWORD));
        Mob enemy = EntityType.ZOMBIE.create(helper.getLevel());
        if (enemy == null) {
            helper.fail("Could not create combat target");
            return;
        }
        enemy.moveTo(helper.absolutePos(new BlockPos(2, 1, 4)), 0.0F, 0.0F);
        enemy.setInvulnerable(true);
        helper.getLevel().addFreshEntity(enemy);
        ChestLootGoal goal = new ChestLootGoal(human, 0.8D);
        helper.assertTrue(goal.canUse(), "Visible chest was not selected");
        human.setTarget(enemy);
        helper.assertTrue(!goal.canContinueToUse(), "Chest goal continued after combat started");
        helper.startSequence().thenIdle(5).thenExecute(() -> {
            helper.assertTrue(chest.getItem(0).is(Items.IRON_SWORD), "Combat did not interrupt chest looting");
            helper.assertTrue(human.getTarget() == enemy, "Human did not retain combat target");
            helper.succeed();
        });
    }

    private static Human createHuman(GameTestHelper helper) {
        helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, helper.getLevel().getServer());
        Human human = ModEntityType.HUMAN1.get().create(helper.getLevel());
        if (human == null) throw new IllegalStateException("Could not create human_tier1");
        human.moveTo(helper.absolutePos(new BlockPos(2, 1, 2)), 0.0F, 0.0F);
        human.setNoAi(true);
        human.setInvulnerable(true);
        human.lookForChestCooldown = 0;
        helper.getLevel().addFreshEntity(human);
        return human;
    }

    private static ChestBlockEntity createChest(GameTestHelper helper, BlockPos localPos) {
        helper.setBlock(localPos, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(localPos));
        if (chest == null) throw new IllegalStateException("Could not create chest block entity");
        return chest;
    }

    private static boolean contains(Human human, net.minecraft.world.item.Item item) {
        return human.getMainHandItem().is(item)
                || human.getOffhandItem().is(item)
                || human.getData().getInventoryItems().stream().anyMatch(stack -> stack.is(item));
    }

    private static void tickGoal(ChestLootGoal goal, int ticks) {
        for (int tick = 0; tick < ticks && goal.canContinueToUse(); tick++) goal.tick();
        goal.stop();
    }
}
