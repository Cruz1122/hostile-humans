package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import com.craftix.hostile_humans.entity.spawner.NaturalHumanSpawner;
import com.craftix.hostile_humans.entity.spawner.NaturalSpawnSavedData;
import com.craftix.hostile_humans.entity.spawner.SpawnContext;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class NaturalHumanDimensionGameTest {
    private static final String TEMPLATE = "human_smoke";
    private static final int SCENARIO_TICKS = 125;

    private NaturalHumanDimensionGameTest() {
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans",
            batch = "naturalNetherScenario", timeoutTicks = 180)
    public static void longRunningNetherEncounterScenario(GameTestHelper helper) {
        runDimensionScenario(helper, Level.NETHER, new BlockPos(400, 65, 400), Blocks.NETHERRACK,
                SpawnContext.NETHER_WILDS);
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans",
            batch = "naturalEndScenario", timeoutTicks = 180)
    public static void longRunningOuterEndEncounterScenario(GameTestHelper helper) {
        runDimensionScenario(helper, Level.END, new BlockPos(400, 101, 0), Blocks.END_STONE,
                SpawnContext.END_WILDS);
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans",
            batch = "initialEndIslandScenario", timeoutTicks = 240)
    public static void initialEndIslandCreatesOneToThreeSquads(GameTestHelper helper) {
        ServerLevel end = requireLevel(helper, Level.END);
        BlockPos playerPos = new BlockPos(0, 101, 0);
        Set<ChunkPos> forcedChunks = forceArea(end, playerPos, 54);
        Map<BlockPos, BlockState> originalBlocks = preparePlatform(end, playerPos, 54, Blocks.END_STONE);
        FakePlayer player = createPlayer(end, playerPos, "initial-end-island");

        helper.runAfterDelay(5, () -> {
            List<List<Human>> squads = List.of();
            try {
                helper.assertTrue(NaturalHumanSpawner.isValidNaturalPosition(end, playerPos),
                        "Controlled initial-End surface is not a valid natural position");
                NaturalSpawnSavedData data = new NaturalSpawnSavedData();
                squads = NaturalHumanSpawner.populateInitialEndIsland(
                        end, player, data, RandomSource.create(0x1EAD5EEDL));
                helper.assertTrue(squads.size() >= 1 && squads.size() <= 3,
                        "Initial End island created " + squads.size() + " squads instead of 1-3");
                Set<UUID> squadIds = new HashSet<>();
                for (List<Human> squad : squads) {
                    helper.assertTrue(squad.size() == 2, "Initial End squad did not contain two Humans");
                    UUID squadId = squad.get(0).getSquadId();
                    helper.assertTrue(squadId != null && squad.stream().allMatch(human -> squadId.equals(human.getSquadId())),
                            "Initial End squad did not share one non-null squad ID");
                    helper.assertTrue(squad.stream().allMatch(human -> human.getSpawnContext() == SpawnContext.END_WILDS),
                            "Initial End squad received the wrong spawn context");
                    squadIds.add(squadId);
                }
                helper.assertTrue(squadIds.size() == squads.size(), "Initial End squads reused a squad ID");
                helper.assertTrue(data.isInitialEndIslandPopulated(), "Successful End population was not marked complete");
                helper.assertTrue(NaturalHumanSpawner.populateInitialEndIsland(
                        end, player, data, RandomSource.create(7L)).isEmpty(),
                        "Initial End island populated more than once");
            } finally {
                squads.stream().flatMap(List::stream).forEach(Human::discard);
                player.discard();
                restoreBlocks(end, originalBlocks);
                restoreForcedChunks(end, forcedChunks);
            }
            helper.succeed();
        });
    }

    private static void runDimensionScenario(GameTestHelper helper, ResourceKey<Level> dimension,
                                             BlockPos playerPos, Block floor,
                                             SpawnContext expectedContext) {
        ServerLevel level = requireLevel(helper, dimension);
        Set<ChunkPos> forcedChunks = forceArea(level, playerPos, 30);
        Map<BlockPos, BlockState> originalBlocks = preparePlatform(level, playerPos, 30, floor);
        List<Human> spawned = new ArrayList<>();
        FakePlayer observer = createPlayer(level, playerPos, "scenario-" + dimension.location().getPath());
        observer.setGameMode(GameType.SPECTATOR);
        boolean[] restored = {false};

        Runnable restoreState = () -> {
            if (restored[0]) return;
            restored[0] = true;
            spawned.forEach(Human::discard);
            observer.discard();
            restoreBlocks(level, originalBlocks);
            restoreForcedChunks(level, forcedChunks);
        };

        try {
            helper.assertTrue(level.addFreshEntity(observer),
                    "Scenario observer could not be added in " + dimension.location());
        } catch (RuntimeException | Error failure) {
            restoreState.run();
            throw failure;
        }

        helper.runAfterDelay(20, () -> {
            try {
                helper.assertTrue(NaturalHumanSpawner.isValidNaturalPosition(level, playerPos),
                        "Controlled scenario surface is invalid in " + dimension.location());
                spawned.addAll(NaturalHumanSpawner.spawnGroup(
                        level, playerPos, expectedContext, 2, RandomSource.create(0x51CE0000L + dimension.hashCode())));
                helper.assertTrue(spawned.size() == 2,
                        "Controlled scenario did not create a two-member encounter in " + dimension.location());
                spawned.forEach(human -> human.setInvulnerable(true));
            } catch (RuntimeException | Error failure) {
                restoreState.run();
                throw failure;
            }
        });

        helper.runAfterDelay(SCENARIO_TICKS, () -> {
            try {
                helper.assertTrue(spawned.stream().allMatch(Human::isAlive),
                        "A protected scenario Human died in " + dimension.location());
                helper.assertTrue(spawned.stream().allMatch(human -> human.getType() == ModEntityType.ROAMER.get()
                                && human.level() == level && human.getSpawnContext() == expectedContext),
                        "Long-running scenario lost its dimension or context in " + dimension.location());
            } finally {
                restoreState.run();
            }
            helper.succeed();
        });
    }

    private static ServerLevel requireLevel(GameTestHelper helper, ResourceKey<Level> dimension) {
        ServerLevel level = helper.getLevel().getServer().getLevel(dimension);
        helper.assertTrue(level != null, "GameTest server has no level for " + dimension.location());
        if (level == null) throw new IllegalStateException("Missing GameTest dimension " + dimension.location());
        return level;
    }

    private static FakePlayer createPlayer(ServerLevel level, BlockPos position, String name) {
        FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), name));
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, 0.0F, 0.0F);
        return player;
    }

    private static Map<BlockPos, BlockState> preparePlatform(ServerLevel level, BlockPos playerPos,
                                                              int radius, Block floor) {
        Map<BlockPos, BlockState> original = new HashMap<>();
        int floorY = playerPos.getY() - 1;
        for (int x = playerPos.getX() - radius; x <= playerPos.getX() + radius; x++) {
            for (int z = playerPos.getZ() - radius; z <= playerPos.getZ() + radius; z++) {
                rememberAndSet(level, original, new BlockPos(x, floorY, z), floor.defaultBlockState());
                rememberAndSet(level, original, new BlockPos(x, floorY + 1, z), Blocks.AIR.defaultBlockState());
                rememberAndSet(level, original, new BlockPos(x, floorY + 2, z), Blocks.AIR.defaultBlockState());
            }
        }
        return original;
    }

    private static void rememberAndSet(ServerLevel level, Map<BlockPos, BlockState> original,
                                       BlockPos position, BlockState state) {
        BlockPos immutable = position.immutable();
        original.put(immutable, level.getBlockState(immutable));
        level.setBlock(immutable, state, 3);
    }

    private static void restoreBlocks(ServerLevel level, Map<BlockPos, BlockState> original) {
        original.forEach((position, state) -> level.setBlock(position, state, 3));
    }

    private static Set<ChunkPos> forceArea(ServerLevel level, BlockPos center, int radius) {
        Set<ChunkPos> newlyForced = new HashSet<>();
        int minimumChunkX = (center.getX() - radius) >> 4;
        int maximumChunkX = (center.getX() + radius) >> 4;
        int minimumChunkZ = (center.getZ() - radius) >> 4;
        int maximumChunkZ = (center.getZ() + radius) >> 4;
        for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
            for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
                if (!level.getForcedChunks().contains(chunk.toLong())) {
                    level.setChunkForced(chunkX, chunkZ, true);
                    newlyForced.add(chunk);
                }
            }
        }
        return newlyForced;
    }

    private static void restoreForcedChunks(ServerLevel level, Set<ChunkPos> forcedChunks) {
        forcedChunks.forEach(chunk -> level.setChunkForced(chunk.x, chunk.z, false));
    }

}
