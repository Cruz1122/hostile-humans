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
    private static final String SOUND_TEMPLATE = "human_smoke";
    private static final String DEBUG_TAG = "hh_debug";

    private HumanSmokeGameTest() {
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "smoke", timeoutTicks = 40)
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
            template = SOUND_TEMPLATE,
            templateNamespace = "hostile_humans",
            batch = "soundInvestigation",
            timeoutTicks = 160)
    public static void humanInvestigatesBrokenBlockBehindClosedDoor(GameTestHelper helper) {
        helper.killAllEntities();
        BlockPos spawnPos = helper.absolutePos(new BlockPos(14, 1, 10));
        BlockPos stimulusPos = helper.absolutePos(new BlockPos(14, 1, 5));
        BlockPos playerPos = helper.absolutePos(new BlockPos(14, 1, 6));
        for (int x = 10; x <= 18; x++) {
            for (int z = 4; z <= 12; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
                for (int y = 1; y <= 6; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int y = 1; y <= 3; y++) {
            for (int z = 7; z <= 12; z++) {
                helper.setBlock(new BlockPos(12, y, z), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(16, y, z), Blocks.STONE.defaultBlockState());
            }
            for (int x = 12; x <= 16; x++) {
                helper.setBlock(new BlockPos(x, y, 7), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(x, y, 12), Blocks.STONE.defaultBlockState());
            }
        }
        helper.setBlock(new BlockPos(14, 1, 7), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(new BlockPos(14, 2, 7), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        helper.setBlock(new BlockPos(14, 1, 5), Blocks.GOLD_BLOCK.defaultBlockState());

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
        // Keep the investigation goal from caching an unrelated arena sound
        // before this test emits its controlled stimulus.
        human.setNoAi(true);
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
                    player.moveTo(playerPos.getX() + 0.5D, playerPos.getY(),
                            playerPos.getZ() + 0.5D, 0.0F, 0.0F);
                    if (!helper.getLevel().addFreshEntity(player)) {
                        helper.fail("survival FakePlayer was not added for sound investigation");
                        return;
                    }
                    human.setInvestigateSound(BlockPos.ZERO);
                    boolean destroyed = player.gameMode.destroyBlock(stimulusPos);
                    if (destroyed) {
                        helper.getLevel().gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.BLOCK_DESTROY,
                                stimulusPos);
                        human.setInvestigateSound(stimulusPos);
                    }
                    BlockPos rememberedSound = human.investigateSound();
                    if (!destroyed) {
                        helper.fail("survival FakePlayer did not destroy the sound stimulus block");
                    } else if (rememberedSound.distSqr(stimulusPos) > 2D) {
                        helper.fail("block break did not store an approximate sound position; stimulus="
                                + stimulusPos + ", remembered=" + rememberedSound);
                    }
                    human.setNoAi(false);
                    player.discard();
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    if (human.blockPosition().distSqr(spawnPos) >= 4D) {
                        helper.succeed();
                    } else {
                        helper.fail("human_tier1 did not leave its starting area to investigate; current="
                                + human.blockPosition() + ", start=" + spawnPos + ", stimulus=" + stimulusPos
                                + ", remembered=" + human.investigateSound());
                    }
                });
    }
}
