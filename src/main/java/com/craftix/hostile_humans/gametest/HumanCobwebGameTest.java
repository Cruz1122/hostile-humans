package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.ai.action.PlaceCobwebAction;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import com.mojang.authlib.GameProfile;
import com.craftix.hostile_humans.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class HumanCobwebGameTest {
    private static final String TEMPLATE = "human_smoke";

    private HumanCobwebGameTest() {
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalCobweb", timeoutTicks = 80)
    public static void placesAndConsumesOneCobweb(GameTestHelper helper) {
        Setup setup = setup(helper, true, true);
        try {
            int before = setup.human.getData().getInventoryItem(0).getCount();
            helper.assertTrue(PlaceCobwebAction.tryPlace(setup.human), "Valid tactical cobweb placement was rejected");
            helper.assertTrue(setup.hasCobwebNearHuman(), "Cobweb was not placed at the deterministic candidate");
            helper.assertTrue(setup.human.getData().getInventoryItem(0).getCount() == before - 1, "Placement did not consume exactly one cobweb");
        } finally {
            setup.cleanup();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalCobweb", timeoutTicks = 80)
    public static void doesNotPlaceWithoutInventory(GameTestHelper helper) {
        Setup setup = setup(helper, false, true);
        try {
            helper.assertTrue(!PlaceCobwebAction.tryPlace(setup.human), "Placement unexpectedly succeeded without cobweb inventory");
            helper.assertTrue(!setup.hasCobwebNearHuman(), "A cobweb appeared without inventory");
        } finally {
            setup.cleanup();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalCobwebGameRule", timeoutTicks = 80)
    public static void respectsMobGriefing(GameTestHelper helper) {
        var gameRule = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean previousValue = gameRule.get();
        Setup setup = null;
        try {
            setup = setup(helper, true, false);
            int before = setup.human.getData().getInventoryItem(0).getCount();
            helper.assertTrue(!PlaceCobwebAction.tryPlace(setup.human), "Placement ignored mobGriefing=false");
            helper.assertTrue(!setup.hasCobwebNearHuman(), "Cobweb appeared with mobGriefing=false");
            helper.assertTrue(setup.human.getData().getInventoryItem(0).getCount() == before, "Inventory changed while placement was rejected");
            helper.succeed();
        } finally {
            if (setup != null) setup.cleanup();
            gameRule.set(previousValue, helper.getLevel().getServer());
        }
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalCobweb", timeoutTicks = 80)
    public static void respectsCooldown(GameTestHelper helper) {
        Setup setup = setup(helper, true, true);
        try {
            helper.assertTrue(PlaceCobwebAction.tryPlace(setup.human), "First cobweb placement failed");
            setup.human.getData().getInventoryItem(0).setCount(2);
            int placed = setup.human.cobwebsPlacedThisCombat;
            helper.assertTrue(!PlaceCobwebAction.tryPlace(setup.human), "Second placement ignored cooldown or nearby web");
            helper.assertTrue(setup.human.cobwebsPlacedThisCombat == placed, "Cooldown attempt incremented combat counter");
        } finally {
            setup.cleanup();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalCobweb", timeoutTicks = 80)
    public static void rejectsSelfIntersectingCandidate(GameTestHelper helper) {
        Setup setup = setup(helper, true, true);
        try {
            helper.getLevel().setBlock(setup.candidate(), Blocks.STONE.defaultBlockState(), 3);
            helper.getLevel().setBlock(setup.candidate().north(), Blocks.STONE.defaultBlockState(), 3);
            helper.getLevel().setBlock(setup.candidate().south(), Blocks.STONE.defaultBlockState(), 3);
            helper.getLevel().setBlock(setup.candidate().east(), Blocks.STONE.defaultBlockState(), 3);
            helper.getLevel().setBlock(setup.candidate().west(), Blocks.STONE.defaultBlockState(), 3);
            helper.assertTrue(!PlaceCobwebAction.tryPlace(setup.human), "Placement succeeded at an occupied candidate");
            helper.assertTrue(!setup.hasCobwebNearHuman(), "Invalid candidate was modified");
        } finally {
            setup.cleanup();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalCobweb", timeoutTicks = 80)
    public static void respectsCombatLimit(GameTestHelper helper) {
        Setup setup = setup(helper, true, true);
        try {
            setup.human.cobwebsPlacedThisCombat = Config.maxCobwebsPerCombat.get();
            int before = setup.human.getData().getInventoryItem(0).getCount();
            helper.assertTrue(!PlaceCobwebAction.tryPlace(setup.human), "Placement exceeded the configured combat limit");
            helper.assertTrue(setup.human.getData().getInventoryItem(0).getCount() == before, "Combat-limit rejection consumed a cobweb");
        } finally {
            setup.cleanup();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalCobweb", timeoutTicks = 100)
    public static void breaksCobwebBlockingCombatMovement(GameTestHelper helper) {
        Human human = ModEntityType.HUMAN1.get().create(helper.getLevel());
        Mob target = net.minecraft.world.entity.EntityType.ZOMBIE.create(helper.getLevel());
        if (human == null || target == null) throw new IllegalStateException("Could not create cobweb combat fixture");

        BlockPos humanPos = helper.absolutePos(new BlockPos(2, 1, 2));
        human.moveTo(humanPos, 0.0F, 0.0F);
        human.setNoAi(true);
        human.setOnGround(true);
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        helper.getLevel().addFreshEntity(human);
        target.moveTo(helper.absolutePos(new BlockPos(8, 1, 2)), 180.0F, 0.0F);
        target.setNoAi(true);
        helper.getLevel().addFreshEntity(target);
        human.setTarget(target);
        helper.getLevel().setBlock(humanPos, Blocks.COBWEB.defaultBlockState(), 3);

        for (int tick = 0; tick < 60; tick++) {
            human.tickCount = tick;
            human.getTacticalWorldActionController().tick();
        }

        helper.assertTrue(helper.getLevel().getBlockState(humanPos).isAir(),
                "Human remained trapped in a cobweb instead of breaking it");
        helper.assertTrue(human.getTacticalWorldActionController().brokenThisPursuit() > 0,
                "Human did not record breaking the cobweb as combat navigation progress");
        target.discard();
        human.discard();
        helper.succeed();
    }

    private static Setup setup(GameTestHelper helper, boolean withCobweb, boolean mobGriefing) {
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(x, 1, z), Blocks.AIR.defaultBlockState());
            }
        }
        var mobGriefingRule = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean previousMobGriefing = mobGriefingRule.get();
        mobGriefingRule.set(mobGriefing, helper.getLevel().getServer());
        Human human = ModEntityType.HUMAN1.get().create(helper.getLevel());
        if (human == null) {
            helper.fail("Could not create human_tier1");
            throw new IllegalStateException("Human creation failed");
        }
        BlockPos humanPos = helper.absolutePos(new BlockPos(2, 1, 2));
        human.moveTo(humanPos, 0.0F, 0.0F);
        human.setNoAi(true);
        helper.getLevel().addFreshEntity(human);
        FakePlayer threat = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "hh-cobweb-threat"));
        threat.setGameMode(GameType.SURVIVAL);
        threat.moveTo(human.getX() + 2.0D, human.getY(), human.getZ(), 180.0F, 0.0F);
        helper.getLevel().addFreshEntity(threat);
        human.isFleeing = true;
        human.toAvoid = threat;
        if (withCobweb) {
            human.getData().setInventoryItem(0, new ItemStack(Items.COBWEB, 2));
        }
        return new Setup(helper, human, threat, previousMobGriefing);
    }

    private record Setup(GameTestHelper helper, Human human, LivingEntity threat, boolean previousMobGriefing) {
        private void cleanup() {
            threat.discard();
            human.discard();
            helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING)
                    .set(previousMobGriefing, helper.getLevel().getServer());
        }

        private BlockPos candidate() {
            return PlaceCobwebAction.candidateFor(human);
        }

        private net.minecraft.world.level.block.state.BlockState levelBlock(BlockPos pos) {
            return helper.getLevel().getBlockState(pos);
        }

        private boolean hasCobwebNearHuman() {
            BlockPos center = human.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -1, -3), center.offset(3, 2, 3))) {
                if (levelBlock(pos).is(Blocks.COBWEB)) {
                    return true;
                }
            }
            return false;
        }
    }
}
