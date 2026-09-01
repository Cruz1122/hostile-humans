package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.ai.action.EnderPearlAction;
import com.craftix.hostile_humans.entity.ai.action.WaterBucketAction;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class HumanUtilityGameTest {
    private static final String TEMPLATE = "human_smoke";

    private HumanUtilityGameTest() {}

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalUtility", timeoutTicks = 40)
    public static void waterBucketPlacesAndRecoversOwnSource(GameTestHelper helper) {
        Human human = createHuman(helper, new BlockPos(8, 1, 2));
        human.getData().setInventoryItem(0, new ItemStack(Items.WATER_BUCKET));
        human.setRemainingFireTicks(100);

        helper.assertTrue(human.getTacticalUtilityController() != null, "Utility controller was not initialized");
        human.getTacticalUtilityController().tick();
        BlockPos source = human.getWaterSourcePos();
        helper.assertTrue(source != null && helper.getLevel().getBlockState(source).is(Blocks.WATER),
                "Water source was not placed");
        helper.assertTrue(human.getData().getInventoryItem(0).is(Items.BUCKET),
                "Water bucket did not become an empty bucket");
        human.waterRecoveryCooldown = 0;

        helper.assertTrue(WaterBucketAction.tryRecover(human), "Own water source was not recovered");
        helper.assertTrue(helper.getLevel().getBlockState(source).isAir(), "Recovered source remained in the world");
        helper.assertTrue(human.getData().getInventoryItem(0).is(Items.WATER_BUCKET),
                "Recovery did not restore a water bucket");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalUtilityGameRule", timeoutTicks = 40)
    public static void waterBucketRespectsMobGriefing(GameTestHelper helper) {
        boolean previous = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).get();
        Human human = createHuman(helper, new BlockPos(8, 1, 2));
        human.getData().setInventoryItem(0, new ItemStack(Items.WATER_BUCKET));
        human.setRemainingFireTicks(100);
        try {
            helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING)
                    .set(false, helper.getLevel().getServer());
            helper.assertTrue(!WaterBucketAction.tryExtinguish(human), "Water placement ignored mobGriefing=false");
            helper.assertTrue(human.getData().getInventoryItem(0).is(Items.WATER_BUCKET),
                    "Rejected water placement consumed the bucket");
        } finally {
            helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING)
                    .set(previous, helper.getLevel().getServer());
            human.discard();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalUtility", timeoutTicks = 40)
    public static void waterRecoveryOnlyUsesTrackedSource(GameTestHelper helper) {
        Human human = createHuman(helper, new BlockPos(8, 1, 2));
        BlockPos unrelated = helper.absolutePos(new BlockPos(9, 1, 2));
        helper.setBlock(new BlockPos(9, 1, 2), Blocks.WATER.defaultBlockState());
        human.getData().setInventoryItem(0, new ItemStack(Items.BUCKET));

        helper.assertTrue(!WaterBucketAction.tryRecover(human), "Untracked water source was recovered");
        helper.assertTrue(helper.getLevel().getBlockState(unrelated).is(Blocks.WATER),
                "Untracked water source was removed");
        helper.assertTrue(human.getData().getInventoryItem(0).is(Items.BUCKET),
                "Untracked recovery changed the inventory");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalUtility", timeoutTicks = 40)
    public static void offensivePearlConsumesExactlyOnePearl(GameTestHelper helper) {
        Human human = createHuman(helper, new BlockPos(2, 1, 2));
        Mob target = EntityType.ZOMBIE.create(helper.getLevel());
        if (target == null) throw new IllegalStateException("Could not create pearl target");
        target.moveTo(helper.absolutePos(new BlockPos(14, 1, 2)), 0.0F, 0.0F);
        target.setNoAi(true);
        target.setInvulnerable(true);
        helper.getLevel().addFreshEntity(target);
        human.getData().setInventoryItem(0, new ItemStack(Items.ENDER_PEARL, 2));
        human.setTarget(target);

        human.getTacticalUtilityController().tick();
        helper.assertTrue(human.getData().getInventoryItem(0).getCount() == 1,
                "Offensive throw did not consume exactly one pearl");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ThrownEnderpearl.class, human.getBoundingBox().inflate(2.0D)).size() == 1,
                "Offensive throw did not spawn one pearl projectile");
        target.discard();
        human.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalUtility", timeoutTicks = 40)
    public static void defensivePearlMovesAwayFromThreat(GameTestHelper helper) {
        Human human = createHuman(helper, new BlockPos(10, 1, 2));
        Mob threat = EntityType.ZOMBIE.create(helper.getLevel());
        if (threat == null) throw new IllegalStateException("Could not create defensive pearl threat");
        threat.moveTo(helper.absolutePos(new BlockPos(7, 1, 2)), 0.0F, 0.0F);
        threat.setNoAi(true);
        threat.setInvulnerable(true);
        helper.getLevel().addFreshEntity(threat);
        human.getData().setInventoryItem(0, new ItemStack(Items.ENDER_PEARL));
        human.isFleeing = true;
        human.toAvoid = threat;

        human.getTacticalUtilityController().tick();
        helper.assertTrue(human.getData().getInventoryItem(0).isEmpty(),
                "Defensive throw did not consume the pearl");
        helper.assertTrue(human.enderPearlCooldown > 0, "Defensive throw did not set a cooldown");
        threat.discard();
        human.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalUtility", timeoutTicks = 40)
    public static void utilityStatePersistsThroughNbt(GameTestHelper helper) {
        Human human = createHuman(helper, new BlockPos(8, 1, 2));
        BlockPos source = helper.absolutePos(new BlockPos(9, 1, 2));
        helper.setBlock(new BlockPos(9, 1, 2), Blocks.WATER.defaultBlockState());
        human.setWaterSourcePos(source);
        human.enderPearlCooldown = 37;
        human.waterRecoveryCooldown = 19;

        net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
        human.addAdditionalSaveData(saved);
        human.clearWaterSourcePos();
        human.enderPearlCooldown = 0;
        human.waterRecoveryCooldown = 0;
        human.readAdditionalSaveData(saved);

        helper.assertTrue(source.equals(human.getWaterSourcePos()), "Tracked water source did not persist");
        helper.assertTrue(human.enderPearlCooldown == 37 && human.waterRecoveryCooldown == 19,
                "Utility cooldowns did not persist");
        helper.succeed();
    }

    private static Human createHuman(GameTestHelper helper, BlockPos localPosition) {
        for (int x = 0; x <= 20; x++) {
            for (int z = 0; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(x, 1, z), Blocks.AIR.defaultBlockState());
                helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR.defaultBlockState());
            }
        }
        Human human = ModEntityType.HUMAN1.get().create(helper.getLevel());
        if (human == null) throw new IllegalStateException("Could not create utility Human");
        human.moveTo(helper.absolutePos(localPosition), 0.0F, 0.0F);
        human.setNoAi(true);
        human.setOnGround(true);
        helper.getLevel().addFreshEntity(human);
        return human;
    }
}
