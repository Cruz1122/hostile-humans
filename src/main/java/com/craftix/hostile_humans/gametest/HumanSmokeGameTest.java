package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class HumanSmokeGameTest {
    private static final String TEMPLATE = "human_smoke";
    private static final String DEBUG_TAG = "hh_debug";

    private HumanSmokeGameTest() {
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", timeoutTicks = 40)
    public static void humanLifecycleSmoke(GameTestHelper helper) {
        EntityType<Human> humanType = ModEntityType.HUMAN1.get();
        Human human = humanType.create(helper.getLevel());
        if (human == null) {
            helper.fail("hostile_humans:human_tier1 could not be created");
            return;
        }

        BlockPos spawnPos = helper.absolutePos(new BlockPos(2, 1, 2));
        human.moveTo(spawnPos, 0.0F, 0.0F);
        human.addTag(DEBUG_TAG);
        if (!helper.getLevel().addFreshEntity(human)) {
            helper.fail("human_tier1 was not added to the GameTest ServerLevel");
            return;
        }

        helper.startSequence()
                .thenIdle(30)
                .thenExecute(() -> {
                    if (!human.isAddedToWorld()) {
                        helper.fail("human_tier1 is no longer added to the ServerLevel after 30 ticks");
                    } else if (!human.isAlive()) {
                        helper.fail("human_tier1 is not alive after 30 ticks");
                    } else if (human.isRemoved()) {
                        helper.fail("human_tier1 was removed during the smoke test");
                    } else {
                        helper.succeed();
                    }
                });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", timeoutTicks = 140)
    public static void humanInvestigatesSoundPosition(GameTestHelper helper) {
        Human human = ModEntityType.HUMAN1.get().create(helper.getLevel());
        if (human == null) {
            helper.fail("hostile_humans:human_tier1 could not be created for sound investigation");
            return;
        }

        BlockPos spawnPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos stimulusPos = helper.absolutePos(new BlockPos(4, 1, 2));
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
            }
        }
        for (int y = 1; y <= 3; y++) {
            for (int z = 0; z <= 3; z++) {
                helper.setBlock(new BlockPos(4, y, z), Blocks.STONE.defaultBlockState());
            }
        }
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.AIR.defaultBlockState());
        human.moveTo(spawnPos, 0.0F, 0.0F);
        human.addTag(DEBUG_TAG);
        human.setInvestigateSound(stimulusPos);
        if (!helper.getLevel().addFreshEntity(human)) {
            helper.fail("human_tier1 was not added for sound investigation");
            return;
        }

        helper.startSequence()
                .thenIdle(120)
                .thenExecute(() -> {
                    if (human.blockPosition().distSqr(human.investigateSound()) > 9D) {
                        helper.fail("human_tier1 did not approach sound within 120 ticks; current="
                                + human.blockPosition() + ", sound=" + human.investigateSound());
                    } else {
                        helper.succeed();
                    }
                });
    }
}
