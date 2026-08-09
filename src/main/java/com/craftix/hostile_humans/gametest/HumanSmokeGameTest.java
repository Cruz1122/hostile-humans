package com.craftix.hostile_humans.gametest;

import com.mojang.authlib.GameProfile;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

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

    @GameTest(
            template = TEMPLATE,
            templateNamespace = "hostile_humans",
            batch = "soundInvestigation",
            timeoutTicks = 160)
    public static void humanInvestigatesBrokenBlockBehindClosedDoor(GameTestHelper helper) {
        BlockPos spawnPos = helper.absolutePos(new BlockPos(12, 1, 12));
        BlockPos stimulusPos = helper.absolutePos(new BlockPos(12, 1, 1));
        for (int x = 0; x <= 24; x++) {
            for (int z = 0; z <= 24; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int y = 1; y <= 3; y++) {
            for (int axis = 6; axis <= 18; axis++) {
                helper.setBlock(new BlockPos(axis, y, 6), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(axis, y, 18), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(6, y, axis), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(18, y, axis), Blocks.STONE.defaultBlockState());
            }
        }
        helper.setBlock(new BlockPos(12, 1, 6), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(new BlockPos(12, 2, 6), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        helper.setBlock(new BlockPos(12, 1, 1), Blocks.GOLD_BLOCK.defaultBlockState());

        CompoundTag entityTag = new CompoundTag();
        entityTag.putString("id", "hostile_humans:human_tier1");
        entityTag.putBoolean("PersistenceRequired", true);
        entityTag.putString("CustomName", "{\"text\":\"HH_SOUND_DEBUG\"}");
        Entity loadedEntity = EntityType.loadEntityRecursive(entityTag, helper.getLevel(), entity -> {
            entity.moveTo(spawnPos, 0.0F, 0.0F);
            return entity;
        });
        if (!(loadedEntity instanceof Human human)) {
            helper.fail("hostile_humans:human_tier1 could not be loaded from summon NBT");
            return;
        }
        human.addTag(DEBUG_TAG);
        if (!helper.getLevel().addFreshEntity(human)) {
            helper.fail("human_tier1 was not added for sound investigation");
            return;
        }

        helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    FakePlayer player = new FakePlayer(
                            helper.getLevel(),
                            new GameProfile(UUID.randomUUID(), "hh-sound-move"));
                    player.setGameMode(GameType.SURVIVAL);
                    player.moveTo(stimulusPos.getX() + 0.5D, stimulusPos.getY(),
                            stimulusPos.getZ() - 1.5D, 0.0F, 0.0F);
                    if (!helper.getLevel().addFreshEntity(player)) {
                        helper.fail("survival FakePlayer was not added for sound investigation");
                        return;
                    }
                    boolean destroyed = player.gameMode.destroyBlock(stimulusPos);
                    BlockPos rememberedSound = human.investigateSound();
                    player.discard();
                    if (!destroyed) {
                        helper.fail("survival FakePlayer did not destroy the sound stimulus block");
                    } else if (rememberedSound.distSqr(stimulusPos) > 2D) {
                        helper.fail("block break did not store an approximate sound position; stimulus="
                                + stimulusPos + ", remembered=" + rememberedSound);
                    }
                })
                .thenIdle(120)
                .thenExecute(() -> {
                    if (human.blockPosition().distSqr(stimulusPos) > 9D) {
                        helper.fail("human_tier1 did not leave the room and approach sound within 120 ticks; current="
                                + human.blockPosition() + ", stimulus=" + stimulusPos
                                + ", remembered=" + human.investigateSound()
                                + ", target=" + human.getTarget());
                    } else {
                        helper.succeed();
                    }
                });
    }
}
