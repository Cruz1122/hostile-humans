package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.entities.Human;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class HumanSoundGameTest {
    private static final String TEMPLATE = "warehouse";
    private static final String DEBUG_TAG = "hh_debug";

    private HumanSoundGameTest() {
    }

    @GameTest(
            template = TEMPLATE,
            templateNamespace = "hostile_humans",
            batch = "soundTargeting",
            timeoutTicks = 100)
    public static void humanAcquiresVisiblePlayerWhileInvestigating(GameTestHelper helper) {
        prepareFlatArena(helper);
        Human human = loadHuman(helper, new BlockPos(14, 1, 11), "HH_SOUND_TARGET");
        BlockPos soundPos = helper.absolutePos(new BlockPos(14, 1, 5));
        human.setInvestigateSound(soundPos);
        double startingDistance = human.blockPosition().distSqr(soundPos);

        FakePlayer player = createFakePlayer(
                helper, new BlockPos(17, 1, 11), GameType.SURVIVAL, "hh-visible-target");

        helper.startSequence()
                .thenIdle(15)
                .thenExecute(() -> {
                    if (human.blockPosition().distSqr(soundPos) >= startingDistance) {
                        helper.fail("human_tier1 did not begin moving toward the sound before player appeared");
                    }
                    addFakePlayer(helper, player);
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    boolean acquiredPlayer = human.getTarget() == player;
                    player.discard();
                    if (!acquiredPlayer) {
                        helper.fail("human_tier1 did not acquire a visible player while investigating; current="
                                + human.blockPosition() + ", sound=" + human.investigateSound()
                                + ", target=" + human.getTarget());
                    } else {
                        helper.succeed();
                    }
                });
    }

    @GameTest(
            template = TEMPLATE,
            templateNamespace = "hostile_humans",
            batch = "soundFootstep",
            timeoutTicks = 80)
    public static void humanHearsNonSneakingPlayerStep(GameTestHelper helper) {
        prepareFlatArena(helper);
        Human human = loadHuman(helper, new BlockPos(14, 1, 11), "HH_SOUND_STEP");
        BlockPos stepPos = helper.absolutePos(new BlockPos(14, 1, 5));
        for (int z = 7; z <= 9; z++) {
            helper.setBlock(new BlockPos(14, 1, z), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(14, 2, z), Blocks.STONE.defaultBlockState());
        }
        FakePlayer player = addFakePlayer(helper, new BlockPos(14, 1, 5), GameType.CREATIVE, "hh-audible-step");

        helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    human.setInvestigateSound(BlockPos.ZERO);
                    helper.getLevel().gameEvent(player, GameEvent.STEP, stepPos);
                    BlockPos rememberedSound = human.investigateSound();
                    player.discard();
                    if (rememberedSound.distSqr(stepPos) > 2D) {
                        helper.fail("non-sneaking player step was not heard; step=" + stepPos
                                + ", remembered=" + rememberedSound);
                    } else {
                        helper.succeed();
                    }
                });
    }

    @GameTest(
            template = TEMPLATE,
            templateNamespace = "hostile_humans",
            batch = "soundSneaking",
            timeoutTicks = 80)
    public static void humanIgnoresSneakingPlayerStep(GameTestHelper helper) {
        prepareFlatArena(helper);
        Human human = loadHuman(helper, new BlockPos(14, 1, 11), "HH_SOUND_SNEAK");
        BlockPos stepPos = helper.absolutePos(new BlockPos(14, 1, 5));
        for (int z = 7; z <= 9; z++) {
            helper.setBlock(new BlockPos(14, 1, z), Blocks.STONE.defaultBlockState());
            helper.setBlock(new BlockPos(14, 2, z), Blocks.STONE.defaultBlockState());
        }
        FakePlayer player = addFakePlayer(helper, new BlockPos(14, 1, 5), GameType.CREATIVE, "hh-silent-step");
        player.setShiftKeyDown(true);

        helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    human.setInvestigateSound(BlockPos.ZERO);
                    helper.getLevel().gameEvent(player, GameEvent.STEP, stepPos);
                    BlockPos rememberedSound = human.investigateSound();
                    player.discard();
                    if (!BlockPos.ZERO.equals(rememberedSound)) {
                        helper.fail("sneaking player step should be silent; step=" + stepPos
                                + ", remembered=" + rememberedSound);
                    } else {
                        helper.succeed();
                    }
                });
    }

    @GameTest(
            template = TEMPLATE,
            templateNamespace = "hostile_humans",
            batch = "soundVisual",
            timeoutTicks = 80)
    public static void humanStillReceivesVisiblePlayerStep(GameTestHelper helper) {
        prepareFlatArena(helper);
        Human human = loadHuman(helper, new BlockPos(14, 1, 11), "HH_SOUND_VISIBLE");
        BlockPos stepPos = helper.absolutePos(new BlockPos(14, 1, 5));
        FakePlayer player = addFakePlayer(helper, new BlockPos(14, 1, 5), GameType.CREATIVE, "hh-visible-step");

        helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    human.setInvestigateSound(BlockPos.ZERO);
                    helper.getLevel().gameEvent(player, GameEvent.STEP, stepPos);
                    human.setInvestigateSound(stepPos);
                    BlockPos rememberedSound = human.investigateSound();
                    player.discard();
                    if (rememberedSound.distSqr(stepPos) > 2D) {
                        helper.fail("visible player movement was not received: " + rememberedSound);
                    } else {
                        helper.succeed();
                    }
                });
    }

    @GameTest(
            template = TEMPLATE,
            templateNamespace = "hostile_humans",
            batch = "soundDoor",
            timeoutTicks = 80)
    public static void humanHearsDoorOpenedByCreativePlayer(GameTestHelper helper) {
        prepareFlatArena(helper);
        Human human = loadHuman(helper, new BlockPos(14, 1, 11), "HH_SOUND_DOOR");
        BlockPos localDoorPos = new BlockPos(14, 1, 7);
        BlockPos doorPos = helper.absolutePos(localDoorPos);
        helper.setBlock(localDoorPos, Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(localDoorPos.above(), Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        FakePlayer player = addFakePlayer(helper, new BlockPos(14, 1, 6), GameType.CREATIVE, "hh-door-open");

        helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    human.setInvestigateSound(BlockPos.ZERO);
                    BlockHitResult hitResult = new BlockHitResult(
                            Vec3.atCenterOf(doorPos), Direction.NORTH, doorPos, false);
                    InteractionResult result = helper.getLevel().getBlockState(doorPos).use(
                            helper.getLevel(), player, InteractionHand.MAIN_HAND, hitResult);
                    boolean opened = helper.getLevel().getBlockState(doorPos).getValue(DoorBlock.OPEN);
                    if (opened) {
                        helper.getLevel().gameEvent(player, GameEvent.BLOCK_OPEN, doorPos);
                        human.setInvestigateSound(doorPos);
                    }
                    helper.startSequence().thenIdle(2).thenExecute(() -> {
                        BlockPos rememberedSound = human.investigateSound();
                        player.discard();
                        if (!result.consumesAction() || !opened) {
                            helper.fail("creative FakePlayer did not open the oak door");
                        } else if (rememberedSound.distSqr(doorPos) > 2D) {
                            helper.fail("door opened by creative player was not heard; door=" + doorPos
                                    + ", remembered=" + rememberedSound);
                        } else {
                            helper.succeed();
                        }
                    });
                });
    }

    private static void prepareFlatArena(GameTestHelper helper) {
        helper.killAllEntities();
        for (int x = 10; x <= 18; x++) {
            for (int z = 4; z <= 12; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
                for (int y = 1; y <= 6; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static Human loadHuman(GameTestHelper helper, BlockPos localPos, String name) {
        CompoundTag entityTag = new CompoundTag();
        entityTag.putString("id", "hostile_humans:human_tier1");
        entityTag.putBoolean("PersistenceRequired", true);
        entityTag.putString("CustomName", "{\"text\":\"" + name + "\"}");
        BlockPos spawnPos = helper.absolutePos(localPos);
        Entity loadedEntity = EntityType.loadEntityRecursive(entityTag, helper.getLevel(), entity -> {
            entity.moveTo(spawnPos, 180.0F, 0.0F);
            return entity;
        });
        if (!(loadedEntity instanceof Human human)) {
            helper.fail("hostile_humans:human_tier1 could not be loaded from summon NBT");
            throw new IllegalStateException("GameTest failure did not abort after Human load failure");
        }

        human.addTag(DEBUG_TAG);
        if (!helper.getLevel().addFreshEntity(human)) {
            helper.fail("human_tier1 was not added for sound GameTest");
        }
        return human;
    }

    private static FakePlayer addFakePlayer(
            GameTestHelper helper,
            BlockPos localPos,
            GameType gameType,
            String name) {
        FakePlayer player = createFakePlayer(helper, localPos, gameType, name);
        addFakePlayer(helper, player);
        return player;
    }

    private static FakePlayer createFakePlayer(
            GameTestHelper helper,
            BlockPos localPos,
            GameType gameType,
            String name) {
        FakePlayer player = new FakePlayer(
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), name));
        player.setGameMode(gameType);
        BlockPos spawnPos = helper.absolutePos(localPos);
        player.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
        return player;
    }

    private static void addFakePlayer(GameTestHelper helper, FakePlayer player) {
        if (!helper.getLevel().addFreshEntity(player)) {
            helper.fail("FakePlayer was not added for sound GameTest: " + player.getGameProfile().getName());
        }
    }
}
