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
        int before = setup.human.getData().getInventoryItem(0).getCount();
        helper.assertTrue(PlaceCobwebAction.tryPlace(setup.human), "Valid tactical cobweb placement was rejected");
        helper.assertTrue(setup.hasCobwebNearHuman(), "Cobweb was not placed at the deterministic candidate");
        helper.assertTrue(setup.human.getData().getInventoryItem(0).getCount() == before - 1, "Placement did not consume exactly one cobweb");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalCobweb", timeoutTicks = 80)
    public static void doesNotPlaceWithoutInventory(GameTestHelper helper) {
        Setup setup = setup(helper, false, true);
        helper.assertTrue(!PlaceCobwebAction.tryPlace(setup.human), "Placement unexpectedly succeeded without cobweb inventory");
        helper.assertTrue(!setup.hasCobwebNearHuman(), "A cobweb appeared without inventory");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalCobweb", timeoutTicks = 80)
    public static void respectsMobGriefing(GameTestHelper helper) {
        Setup setup = setup(helper, true, false);
        int before = setup.human.getData().getInventoryItem(0).getCount();
        helper.assertTrue(!PlaceCobwebAction.tryPlace(setup.human), "Placement ignored mobGriefing=false");
        helper.assertTrue(!setup.hasCobwebNearHuman(), "Cobweb appeared with mobGriefing=false");
        helper.assertTrue(setup.human.getData().getInventoryItem(0).getCount() == before, "Inventory changed while placement was rejected");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalCobweb", timeoutTicks = 80)
    public static void respectsCooldown(GameTestHelper helper) {
        Setup setup = setup(helper, true, true);
        helper.assertTrue(PlaceCobwebAction.tryPlace(setup.human), "First cobweb placement failed");
        setup.human.getData().getInventoryItem(0).setCount(2);
        int placed = setup.human.cobwebsPlacedThisCombat;
        helper.assertTrue(!PlaceCobwebAction.tryPlace(setup.human), "Second placement ignored cooldown or nearby web");
        helper.assertTrue(setup.human.cobwebsPlacedThisCombat == placed, "Cooldown attempt incremented combat counter");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalCobweb", timeoutTicks = 80)
    public static void rejectsSelfIntersectingCandidate(GameTestHelper helper) {
        Setup setup = setup(helper, true, true);
        helper.getLevel().setBlock(setup.candidate(), Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue(!PlaceCobwebAction.tryPlace(setup.human), "Placement succeeded at an occupied candidate");
        helper.assertTrue(!setup.hasCobwebNearHuman(), "Invalid candidate was modified");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "tacticalCobweb", timeoutTicks = 80)
    public static void respectsCombatLimit(GameTestHelper helper) {
        Setup setup = setup(helper, true, true);
        setup.human.cobwebsPlacedThisCombat = Config.maxCobwebsPerCombat.get();
        int before = setup.human.getData().getInventoryItem(0).getCount();
        helper.assertTrue(!PlaceCobwebAction.tryPlace(setup.human), "Placement exceeded the configured combat limit");
        helper.assertTrue(setup.human.getData().getInventoryItem(0).getCount() == before, "Combat-limit rejection consumed a cobweb");
        helper.succeed();
    }

    private static Setup setup(GameTestHelper helper, boolean withCobweb, boolean mobGriefing) {
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(x, 1, z), Blocks.AIR.defaultBlockState());
            }
        }
        helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(mobGriefing, helper.getLevel().getServer());
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
        return new Setup(helper, human, threat);
    }

    private record Setup(GameTestHelper helper, Human human, LivingEntity threat) {
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
