package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import com.craftix.hostile_humans.entity.loadout.HumanDeathRewardCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.util.FakePlayer;
import com.mojang.authlib.GameProfile;

import java.util.Map;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class HumanDeathLootGameTest {
    private static final String TEMPLATE = "human_smoke";

    private HumanDeathLootGameTest() {
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanDeathLoot", timeoutTicks = 40)
    public static void currentInventoryAndEquipmentDropExactlyOnce(GameTestHelper helper) {
        Human human = createHuman(helper);
        human.getData().setInventoryItem(0, new ItemStack(Items.IRON_INGOT, 3));
        human.getData().setInventoryItem(1, new ItemStack(Items.SHIELD));
        human.setItemSlot(EquipmentSlot.MAINHAND, enchantedSword());
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        human.setItemSlot(EquipmentSlot.HEAD, damaged(new ItemStack(Items.DIAMOND_HELMET), 17));

        // Other death-loot fixtures can run in nearby templates. Keep the
        // assertion area tight enough to exclude their drops.
        AABB area = human.getBoundingBox().inflate(1.5D);
        itemEntities(helper, area).forEach(ItemEntity::discard);
        human.kill();
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(countItem(helper, area, Items.IRON_INGOT) == 3, "Inventory iron was not conserved");
            int shields = countItem(helper, area, Items.SHIELD);
            helper.assertTrue(shields == 2, "Identical genuine shields were deduplicated or duplicated; observed=" + shields);
            helper.assertTrue(countItem(helper, area, Items.DIAMOND_HELMET) == 1, "Equipment helmet was not dropped");
            helper.assertTrue(countEnchantedSwords(helper, area) == 1, "Enchanted equipment was not dropped exactly once");
            ItemEntity sword = itemEntities(helper, area).stream().filter(entity -> entity.getItem().is(Items.DIAMOND_SWORD))
                    .findFirst().orElseThrow();
            helper.assertTrue(sword.getItem().getDamageValue() == 23, "Equipment durability changed during death drop");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, sword.getItem()) == 3,
                    "Equipment enchantment level was not preserved");
            helper.assertTrue(human.getData().getInventoryItems().stream().allMatch(ItemStack::isEmpty),
                    "Inventory source still retained a dropped stack");
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                helper.assertTrue(human.getItemBySlot(slot).isEmpty(), "Equipment source still retained " + slot);
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanDeathLoot", timeoutTicks = 40)
    public static void acquiredItemsDropAndConsumedItemsDoNotReappear(GameTestHelper helper) {
        Human human = createHuman(helper);
        human.getData().setInventoryItem(0, new ItemStack(Items.COOKED_BEEF, 2));
        human.getData().getInventoryItem(0).shrink(1);
        human.getData().setInventoryItem(1, new ItemStack(Items.ENDER_PEARL));
        AABB area = new AABB(human.blockPosition()).inflate(4.0D);
        human.kill();
        helper.runAfterDelay(2, () -> {
            int beef = countItem(helper, area, Items.COOKED_BEEF);
            int pearls = countItem(helper, area, Items.ENDER_PEARL);
            helper.assertTrue(beef == 1, "Consumed food reappeared or remaining food was lost; observed=" + beef);
            helper.assertTrue(pearls == 1, "Acquired item was not dropped; observed=" + pearls);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanDeathLoot", timeoutTicks = 40)
    public static void vanishingCurseStillRemovesTheItem(GameTestHelper helper) {
        Human human = createHuman(helper);
        ItemStack vanishing = new ItemStack(Items.DIAMOND_SWORD);
        EnchantmentHelper.setEnchantments(Map.of(Enchantments.VANISHING_CURSE, 1), vanishing);
        human.setItemSlot(EquipmentSlot.MAINHAND, vanishing);
        AABB area = new AABB(human.blockPosition()).inflate(4.0D);
        human.kill();
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(countItem(helper, area, Items.DIAMOND_SWORD) == 0, "Vanishing item was dropped");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanDeathLoot", timeoutTicks = 40)
    public static void xpIsTieredAndLoadoutBounded(GameTestHelper helper) {
        Human strong = createHuman(helper);
        strong.setCombatSkillTierOverride(CombatSkillTier.T1);
        strong.setItemSlot(EquipmentSlot.MAINHAND, enchantedSword());
        strong.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        strong.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        strong.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        strong.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        strong.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));

        Human weak = createHuman(helper, new BlockPos(4, 1, 2));
        weak.setCombatSkillTierOverride(CombatSkillTier.T5);
        int strongXp = HumanDeathRewardCalculator.calculate(strong);
        int weakXp = HumanDeathRewardCalculator.calculate(weak);
        helper.assertTrue(strongXp > weakXp, "T1 did not reward more XP than T5");
        helper.assertTrue(strongXp <= 60 && weakXp <= 10, "Human XP exceeded its configured tier cap");
        helper.assertTrue(strongXp == strong.getExperienceReward(), "XP reward did not use the loadout score");
        strong.kill();
        weak.kill();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanDeathLootPlayerXp", timeoutTicks = 60)
    public static void playerKillProducesHumanExperience(GameTestHelper helper) {
        Human human = createHuman(helper);
        AABB isolationArea = human.getBoundingBox().inflate(8.0D);
        helper.getLevel().getEntitiesOfClass(Human.class, isolationArea, other -> other != human)
                .forEach(Human::discard);
        helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, isolationArea).forEach(ExperienceOrb::discard);
        human.setCombatSkillTierOverride(CombatSkillTier.T1);
        human.setItemSlot(EquipmentSlot.MAINHAND, enchantedSword());
        FakePlayer killer = new FakePlayer(helper.getLevel(), new GameProfile(java.util.UUID.randomUUID(), "human-xp-killer"));
        killer.setPos(human.position().add(16.0D, 0.0D, 0.0D));
        helper.getLevel().addFreshEntity(killer);
        AABB area = human.getBoundingBox().inflate(1.5D);
        int expected = human.getExperienceReward();
        human.setHealth(1.0F);
        human.hurt(helper.getLevel().damageSources().playerAttack(killer), 100.0F);
        helper.runAfterDelay(2, () -> {
            try {
                int actual = helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, area).stream()
                        .mapToInt(ExperienceOrb::getValue).sum();
                helper.assertTrue(actual == expected, "Player kill XP was " + actual + ", expected " + expected);
            } finally {
                killer.discard();
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanDeathLoot", timeoutTicks = 60)
    public static void humanCollectsExperienceOrb(GameTestHelper helper) {
        Human human = createHuman(helper);
        helper.getLevel().getEntitiesOfClass(Human.class, human.getBoundingBox().inflate(8.0D), other -> other != human)
                .forEach(Human::discard);
        ExperienceOrb orb = new ExperienceOrb(helper.getLevel(), human.getX() + 0.5D,
                human.getY(), human.getZ(), 7);
        helper.getLevel().addFreshEntity(orb);
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(orb.isRemoved(), "Human did not collect the nearby experience orb");
            helper.assertTrue(human.getExperiencePoints() == 7,
                    "Human stored " + human.getExperiencePoints() + " XP instead of 7");
            helper.assertTrue(human.getExperienceLevel() == 1,
                    "Human XP did not advance to the expected player-style level");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "humanDeathLoot", timeoutTicks = 40)
    public static void humanExperiencePersistsThroughNbt(GameTestHelper helper) {
        Human human = createHuman(helper);
        human.giveExperiencePoints(37);
        CompoundTag saved = new CompoundTag();
        human.addAdditionalSaveData(saved);

        human.giveExperiencePoints(5);
        human.readAdditionalSaveData(saved);
        helper.assertTrue(human.getExperiencePoints() == 37,
                "Human XP did not survive NBT round trip: " + human.getExperiencePoints());
        helper.succeed();
    }

    private static Human createHuman(GameTestHelper helper) {
        return createHuman(helper, new BlockPos(2, 1, 2));
    }

    private static Human createHuman(GameTestHelper helper, BlockPos localPosition) {
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(x, 1, z), Blocks.AIR.defaultBlockState());
                helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR.defaultBlockState());
            }
        }
        Human human = ModEntityType.HUMAN1.get().create(helper.getLevel());
        if (human == null) throw new IllegalStateException("Could not create Human fixture");
        human.moveTo(helper.absolutePos(localPosition), 0.0F, 0.0F);
        human.setNoAi(true);
        human.setOnGround(true);
        helper.getLevel().addFreshEntity(human);
        return human;
    }

    private static ItemStack enchantedSword() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        EnchantmentHelper.setEnchantments(Map.of(Enchantments.SHARPNESS, 3), sword);
        CompoundTag tag = sword.getOrCreateTag();
        tag.putString("HumanDeathTest", "preserve");
        return damaged(sword, 23);
    }

    private static ItemStack damaged(ItemStack stack, int damage) {
        stack.setDamageValue(damage);
        return stack;
    }

    private static int countItem(GameTestHelper helper, AABB area, net.minecraft.world.item.Item item) {
        return itemEntities(helper, area).stream().filter(entity -> entity.getItem().is(item))
                .mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    private static long countEnchantedSwords(GameTestHelper helper, AABB area) {
        return itemEntities(helper, area).stream().filter(entity -> entity.getItem().is(Items.DIAMOND_SWORD)
                && entity.getItem().isEnchanted() && entity.getItem().getTag() != null
                && "preserve".equals(entity.getItem().getTag().getString("HumanDeathTest"))).count();
    }

    private static java.util.List<ItemEntity> itemEntities(GameTestHelper helper, AABB area) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area);
    }
}
