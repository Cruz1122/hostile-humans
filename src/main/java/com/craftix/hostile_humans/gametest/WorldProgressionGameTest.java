package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.progression.WorldGearProgressionSavedData;
import com.craftix.hostile_humans.progression.WorldGearProgressionTracker;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class WorldProgressionGameTest {
    private WorldProgressionGameTest() {
    }

    @GameTest(template = "human_smoke", templateNamespace = "hostile_humans", batch = "worldProgression", timeoutTicks = 40)
    public static void gearUnlocksMaterial(GameTestHelper helper) {
        WorldGearProgressionSavedData data = new WorldGearProgressionSavedData();
        WorldGearProgressionTracker.observe(data, new ItemStack(Items.IRON_PICKAXE));
        WorldGearProgressionTracker.observe(data, new ItemStack(Items.DIAMOND_BOOTS));
        WorldGearProgressionTracker.observe(data, new ItemStack(Items.NETHERITE_AXE));
        helper.assertTrue(data.isIronUnlocked(), "Iron gear did not unlock iron");
        helper.assertTrue(data.isDiamondUnlocked(), "Diamond armor did not unlock diamond");
        helper.assertTrue(data.isNetheriteUnlocked(), "Netherite gear did not unlock netherite");
        helper.assertTrue(!data.isGoldUnlocked(), "Gold unlocked without gold gear or dimension entry");
        helper.succeed();
    }

    @GameTest(template = "human_smoke", templateNamespace = "hostile_humans", batch = "worldProgression", timeoutTicks = 40)
    public static void rawMaterialDoesNotUnlock(GameTestHelper helper) {
        WorldGearProgressionSavedData data = new WorldGearProgressionSavedData();
        WorldGearProgressionTracker.observe(data, new ItemStack(Items.DIAMOND));
        WorldGearProgressionTracker.observe(data, new ItemStack(Items.NETHERITE_INGOT));
        helper.assertTrue(!data.isDiamondUnlocked(), "Raw diamond unlocked diamond gear");
        helper.assertTrue(!data.isNetheriteUnlocked(), "Netherite ingot unlocked netherite gear");
        helper.succeed();
    }

    @GameTest(template = "human_smoke", templateNamespace = "hostile_humans", batch = "worldProgression", timeoutTicks = 40)
    public static void enteringNetherUnlocksOverworldGear(GameTestHelper helper) {
        WorldGearProgressionSavedData data = new WorldGearProgressionSavedData();
        WorldGearProgressionSavedData.unlockForDimension(data, Level.NETHER);
        helper.assertTrue(data.isIronUnlocked() && data.isGoldUnlocked() && data.isDiamondUnlocked(),
                "Nether entry did not unlock iron, gold and diamond");
        helper.assertTrue(!data.isNetheriteUnlocked(), "Nether entry unlocked netherite");
        helper.succeed();
    }

    @GameTest(template = "human_smoke", templateNamespace = "hostile_humans", batch = "worldProgression", timeoutTicks = 40)
    public static void enteringEndUnlocksOverworldGear(GameTestHelper helper) {
        WorldGearProgressionSavedData data = new WorldGearProgressionSavedData();
        WorldGearProgressionSavedData.unlockForDimension(data, Level.END);
        helper.assertTrue(data.isIronUnlocked() && data.isGoldUnlocked() && data.isDiamondUnlocked(),
                "End entry did not unlock iron, gold and diamond");
        helper.assertTrue(!data.isNetheriteUnlocked(), "End entry unlocked netherite");
        helper.succeed();
    }

    @GameTest(template = "human_smoke", templateNamespace = "hostile_humans", batch = "worldProgression", timeoutTicks = 40)
    public static void progressionIsIrreversible(GameTestHelper helper) {
        WorldGearProgressionSavedData data = new WorldGearProgressionSavedData();
        data.unlockIron();
        data.unlockGold();
        data.unlockDiamond();
        data.unlockNetherite();
        helper.assertTrue(data.isIronUnlocked() && data.isGoldUnlocked() && data.isDiamondUnlocked()
                        && data.isNetheriteUnlocked(), "A progression flag was lost");
        helper.succeed();
    }

    @GameTest(template = "human_smoke", templateNamespace = "hostile_humans", batch = "worldProgression", timeoutTicks = 40)
    public static void progressionPersists(GameTestHelper helper) {
        WorldGearProgressionSavedData original = new WorldGearProgressionSavedData();
        original.unlockIron();
        original.unlockDiamond();
        CompoundTag saved = original.save(new CompoundTag());
        WorldGearProgressionSavedData restored = WorldGearProgressionSavedData.load(saved);
        helper.assertTrue(restored.isIronUnlocked(), "Iron flag did not survive serialization");
        helper.assertTrue(restored.isDiamondUnlocked(), "Diamond flag did not survive serialization");
        helper.assertTrue(!restored.isGoldUnlocked() && !restored.isNetheriteUnlocked(),
                "Serialization invented progression flags");
        helper.succeed();
    }
}
