package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.AggressionMode;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import com.craftix.hostile_humans.entity.equipment.MeleeWeaponSelector;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class HumanEquipmentGameTest {
    private static final String TEMPLATE = "human_smoke";

    private HumanEquipmentGameTest() {
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalEquipment", timeoutTicks = 80)
    public static void primaryMeleeWinsOverFallback(GameTestHelper helper) {
        Human human = createHuman(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        human.getData().setInventoryItem(0, new ItemStack(Items.IRON_SWORD));
        human.getData().setInventoryItem(1, new ItemStack(Items.DIAMOND_PICKAXE));
        human.reevaluateEquipment();
        helper.assertTrue(human.getMainHandItem().is(Items.IRON_SWORD), "Primary sword did not beat fallback pickaxe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalEquipment", timeoutTicks = 80)
    public static void fallbackToolIsUsedWithoutPrimary(GameTestHelper helper) {
        Human human = createHuman(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        human.getData().setInventoryItem(0, new ItemStack(Items.IRON_PICKAXE));
        human.reevaluateEquipment();
        helper.assertTrue(human.getMainHandItem().is(Items.IRON_PICKAXE), "Fallback pickaxe was not equipped");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalEquipment", timeoutTicks = 80)
    public static void pickupTriggersWeaponUpgrade(GameTestHelper helper) {
        Human human = createHuman(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        ItemStack loot = new ItemStack(Items.DIAMOND_SWORD);
        helper.assertTrue(human.getData().storeInventoryItem(loot), "Loot could not enter HumanData inventory");
        human.markEquipmentDirty();
        human.reevaluateEquipment();
        helper.assertTrue(human.getMainHandItem().is(Items.DIAMOND_SWORD), "Human did not equip picked-up primary weapon");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalEquipment", timeoutTicks = 80)
    public static void sameToolQualityPrefersDiamond(GameTestHelper helper) {
        Human human = createHuman(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        human.getData().setInventoryItem(0, new ItemStack(Items.DIAMOND_PICKAXE));
        human.reevaluateEquipment();
        helper.assertTrue(human.getMainHandItem().is(Items.DIAMOND_PICKAXE),
                "Higher-quality pickaxe did not replace the iron pickaxe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalEquipment", timeoutTicks = 80)
    public static void invalidItemsRemainUnequipped(GameTestHelper helper) {
        Human human = createHuman(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        human.getData().setInventoryItem(0, new ItemStack(Items.BREAD));
        human.getData().setInventoryItem(1, new ItemStack(Items.IRON_INGOT));
        human.getData().setInventoryItem(2, new ItemStack(Blocks.STONE.asItem()));
        human.reevaluateEquipment();
        helper.assertTrue(human.getMainHandItem().isEmpty(), "Invalid item was equipped as a melee weapon");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalEquipment", timeoutTicks = 120)
    public static void tiedCandidatesRemainStable(GameTestHelper helper) {
        Human human = createHuman(helper, new BlockPos(2, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        human.getData().setInventoryItem(0, new ItemStack(Items.IRON_PICKAXE));
        human.getData().setInventoryItem(1, new ItemStack(Items.IRON_SHOVEL));
        human.reevaluateEquipment();
        ItemStack selected = human.getMainHandItem().copy();
        helper.startSequence().thenIdle(80).thenExecute(() -> {
            helper.assertTrue(ItemStack.isSameItem(human.getMainHandItem(), selected), "Weapon selection oscillated during idle ticks");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalEquipment", timeoutTicks = 80)
    public static void partialPickupLeavesWorldRemainder(GameTestHelper helper) {
        Human human = createHuman(helper, new BlockPos(2, 1, 2));
        human.getData().setInventoryItem(20, new ItemStack(Items.COBWEB, 60));
        for (int slot = 21; slot < 30; slot++) {
            human.getData().setInventoryItem(slot, new ItemStack(Items.STONE));
        }
        ItemStack incoming = new ItemStack(Items.COBWEB, 8);
        helper.assertTrue(human.getData().storeInventoryItem(incoming), "Partial stack was not accepted");
        helper.assertTrue(human.getData().getInventoryItem(20).getCount() == 64, "Existing stack did not fill to its limit");
        helper.assertTrue(incoming.getCount() == 4, "Unaccepted item remainder was not preserved");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalEquipment", timeoutTicks = 40)
    public static void pickupUsesVanillaMobReach(GameTestHelper helper) {
        Human human = createHuman(helper, new BlockPos(2, 1, 2));
        ItemEntity near = new ItemEntity(helper.getLevel(),
                helper.absolutePos(new BlockPos(3, 1, 2)).getX(),
                helper.absolutePos(new BlockPos(3, 1, 2)).getY(),
                helper.absolutePos(new BlockPos(3, 1, 2)).getZ(),
                new ItemStack(Items.COBBLESTONE));
        ItemEntity far = new ItemEntity(helper.getLevel(),
                helper.absolutePos(new BlockPos(4, 1, 2)).getX(),
                helper.absolutePos(new BlockPos(4, 1, 2)).getY(),
                helper.absolutePos(new BlockPos(4, 1, 2)).getZ(),
                new ItemStack(Items.COBBLESTONE));
        near.setPickUpDelay(0);
        far.setPickUpDelay(0);
        helper.getLevel().addFreshEntity(near);
        helper.getLevel().addFreshEntity(far);

        helper.startSequence().thenIdle(5).thenExecute(() -> {
            helper.assertTrue(near.isRemoved(), "Nearby loot was not picked up");
            helper.assertTrue(far.isAlive(), "Loot outside vanilla mob reach was pulled in");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalEquipment", timeoutTicks = 160)
    public static void humanWalksToNearbyUsefulDrop(GameTestHelper helper) {
        Human human = createHuman(helper, new BlockPos(1, 1, 2));
        human.setNoAi(false);
        human.setAggressionLevel(AggressionMode.PASSIVE);
        ItemEntity droppedSword = new ItemEntity(helper.getLevel(),
                helper.absolutePos(new BlockPos(4, 1, 2)).getX() + 0.5D,
                helper.absolutePos(new BlockPos(4, 1, 2)).getY(),
                helper.absolutePos(new BlockPos(4, 1, 2)).getZ() + 0.5D,
                new ItemStack(Items.DIAMOND_SWORD));
        droppedSword.setPickUpDelay(0);
        helper.getLevel().addFreshEntity(droppedSword);

        helper.startSequence().thenExecuteAfter(120, () -> {
            helper.assertTrue(droppedSword.isRemoved(), "Human did not walk to and pick up nearby useful loot");
            helper.assertTrue(human.getMainHandItem().is(Items.DIAMOND_SWORD),
                    "Human did not equip the useful item it looted");
            helper.succeed();
        });
    }

    private static Human createHuman(GameTestHelper helper, BlockPos localPos) {
        for (int x = 0; x <= 5; x++) {
            for (int z = 0; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(x, 1, z), Blocks.AIR.defaultBlockState());
            }
        }
        Human human = ModEntityType.HUMAN1.get().create(helper.getLevel());
        if (human == null) {
            helper.fail("Could not create human_tier1");
            throw new IllegalStateException("Human creation failed");
        }
        BlockPos pos = helper.absolutePos(localPos);
        human.moveTo(pos, 0.0F, 0.0F);
        human.setNoAi(true);
        helper.getLevel().addFreshEntity(human);
        return human;
    }
}
