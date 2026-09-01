package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import com.craftix.hostile_humans.entity.spawner.NaturalHumanSpawner;
import com.craftix.hostile_humans.entity.spawner.SpawnContext;
import com.craftix.hostile_humans.entity.spawner.SpawnContextClassifier;
import com.craftix.hostile_humans.persona.PersonaFaction;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.util.FakePlayer;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class NaturalHumanSpawningGameTest {
    private static final String TEMPLATE = "human_smoke";

    private NaturalHumanSpawningGameTest() {
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "naturalHumanSpawning", timeoutTicks = 40)
    public static void overworldSurfaceContext(GameTestHelper helper) {
        helper.assertTrue(SpawnContextClassifier.classify(Level.OVERWORLD,
                        new SpawnContextClassifier.StructureFlags(false, false, false, false, false), false)
                        == SpawnContext.OVERWORLD_SURFACE, "Overworld surface context was misclassified");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "naturalHumanSpawning", timeoutTicks = 40)
    public static void caveContext(GameTestHelper helper) {
        helper.assertTrue(SpawnContextClassifier.classify(Level.OVERWORLD,
                        new SpawnContextClassifier.StructureFlags(false, false, false, false, false), true)
                        == SpawnContext.OVERWORLD_CAVE, "Cave context was misclassified");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "naturalHumanSpawning", timeoutTicks = 40)
    public static void netherWildsContext(GameTestHelper helper) {
        helper.assertTrue(SpawnContextClassifier.classify(Level.NETHER,
                        new SpawnContextClassifier.StructureFlags(false, false, false, false, false), false)
                        == SpawnContext.NETHER_WILDS, "Nether wilds did not classify without a structure");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "naturalHumanSpawning", timeoutTicks = 40)
    public static void structureContextsHavePriority(GameTestHelper helper) {
        SpawnContextClassifier.StructureFlags fortress = new SpawnContextClassifier.StructureFlags(false, false, true, false, false);
        SpawnContextClassifier.StructureFlags bastion = new SpawnContextClassifier.StructureFlags(false, false, true, true, false);
        SpawnContextClassifier.StructureFlags endCity = new SpawnContextClassifier.StructureFlags(false, false, false, false, true);
        SpawnContextClassifier.StructureFlags village = new SpawnContextClassifier.StructureFlags(true, true, false, false, false);
        helper.assertTrue(SpawnContextClassifier.classify(Level.NETHER, fortress, false) == SpawnContext.NETHER_FORTRESS,
                "Fortress context was not selected");
        helper.assertTrue(SpawnContextClassifier.classify(Level.NETHER, bastion, false) == SpawnContext.BASTION,
                "Bastion did not outrank fortress");
        helper.assertTrue(SpawnContextClassifier.classify(Level.END, endCity, false) == SpawnContext.END_CITY,
                "End City context was not selected");
        helper.assertTrue(SpawnContextClassifier.classify(Level.OVERWORLD, village, true) == SpawnContext.VILLAGE,
                "Village did not outrank cave");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "naturalHumanSpawning", timeoutTicks = 80)
    public static void squadInitializesFactionSquadAndUniquePersonas(GameTestHelper helper) {
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 1, 1));
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                BlockPos floor = new BlockPos(1 + x, 0, 1 + z);
                helper.setBlock(floor, Blocks.STONE.defaultBlockState());
                helper.setBlock(floor.above(), Blocks.AIR.defaultBlockState());
                helper.setBlock(floor.above(2), Blocks.AIR.defaultBlockState());
            }
        }
        helper.assertTrue(NaturalHumanSpawner.isValidNaturalPosition(helper.getLevel(), anchor),
                "Natural squad anchor is not a valid natural position");
        clearExistingHumans(helper, anchor);
        List<Human> spawned = List.of();
        try {
            spawned = NaturalHumanSpawner.spawnGroup(helper.getLevel(), anchor,
                    SpawnContext.OVERWORLD_SURFACE, 4, RandomSource.create(1234L));
            helper.assertTrue(spawned.size() == 4, "Natural squad did not create four Humans");
            UUID squadId = spawned.get(0).getSquadId();
            PersonaFaction faction = spawned.get(0).getPersonaDefinition().orElseThrow().faction();
            Set<String> personas = new HashSet<>();
            for (Human human : spawned) {
                helper.assertTrue(squadId != null && squadId.equals(human.getSquadId()), "Squad IDs differ");
                helper.assertTrue(human.getPersonaDefinition().orElseThrow().faction() == faction,
                        "Natural squad factions differ");
                personas.add(human.getPersonaId());
            }
            helper.assertTrue(personas.size() == spawned.size(), "Natural squad reused a living persona");
        } finally {
            cleanup(spawned);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "naturalHumanSpawning", timeoutTicks = 80)
    public static void naturalSpawnRespectsDistanceAndLoadedChunk(GameTestHelper helper) {
        int configuredDistance = Math.max(Config.minSpawnDistance.get(),
                Math.min(Config.maxSpawnDistance.get(), 28));
        BlockPos player = helper.absolutePos(new BlockPos(0, 1, 4));
        BlockPos candidate = player.offset(configuredDistance, 0, 0);
        helper.assertTrue(NaturalHumanSpawner.isLoaded(helper.getLevel(), candidate), "Test candidate chunk is not loaded");
        helper.assertTrue(NaturalHumanSpawner.isWithinSpawnDistance(Vec3.atCenterOf(player), candidate),
                "Valid natural candidate failed distance check");
        helper.assertTrue(NaturalHumanSpawner.isWithinSpawnDistance(
                        Vec3.atCenterOf(player).add(0.0D, 160.0D, 0.0D), candidate),
                "Vertical separation incorrectly changed horizontal spawn distance");
        BlockPos far = new BlockPos(4096, 1, 4096);
        helper.assertTrue(!NaturalHumanSpawner.isLoaded(helper.getLevel(), far), "Far chunk unexpectedly loaded");
        helper.assertTrue(!NaturalHumanSpawner.isLoaded(helper.getLevel(), far), "Chunk probe changed loaded state");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "naturalHumanSpawning", timeoutTicks = 40)
    public static void initialEndIslandUsesBoundedCentralRadius(GameTestHelper helper) {
        helper.assertTrue(NaturalHumanSpawner.isWithinInitialEndIsland(new BlockPos(256, 80, 0)),
                "Initial End island excluded its radius boundary");
        helper.assertTrue(!NaturalHumanSpawner.isWithinInitialEndIsland(new BlockPos(257, 80, 0)),
                "Initial End island included a position beyond its radius");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "naturalHumanSpawning", timeoutTicks = 80)
    public static void naturalSpawnUsesProceduralLoadout(GameTestHelper helper) {
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 1, 1));
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                helper.setBlock(new BlockPos(1 + x, 0, 1 + z), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(1 + x, 1, 1 + z), Blocks.AIR.defaultBlockState());
                helper.setBlock(new BlockPos(1 + x, 2, 1 + z), Blocks.AIR.defaultBlockState());
            }
        }
        List<Human> spawned = NaturalHumanSpawner.spawnGroup(helper.getLevel(), anchor,
                SpawnContext.OVERWORLD_SURFACE, 1, RandomSource.create(0x22L));
        try {
            helper.assertTrue(spawned.size() == 1, "Natural procedural Human did not spawn");
            Human human = spawned.get(0);
            helper.assertTrue(!human.getMainHandItem().isEmpty(), "Natural Human has no primary weapon");
            helper.assertTrue(human.getData().getInventoryItems().stream().anyMatch(human::isFood),
                    "Natural Human has no food in the generated inventory");
        } finally {
            cleanup(spawned);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "naturalHumanSpawning", timeoutTicks = 80)
    public static void legacyRangedHumansKeepImplicitArrows(GameTestHelper helper) {
        Human human = ModEntityType.HUMAN1.get().create(helper.getLevel());
        helper.assertTrue(human != null, "Could not create legacy Human fixture");
        if (human == null) return;
        human.moveTo(helper.absolutePos(new BlockPos(2, 1, 2)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(human);
        human.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, Items.BOW.getDefaultInstance());
        helper.assertTrue(human.hasProjectileForWeapon(human.getMainHandItem()),
                "Legacy ranged Human lost implicit projectile compatibility");
        helper.assertTrue(human.getProjectile(human.getMainHandItem()).is(Items.ARROW),
                "Legacy ranged Human no longer receives the vanilla fallback arrow");
        human.kill();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "naturalHumanSpawning", timeoutTicks = 80)
    public static void naturalRangedHumansConsumeStoredArrows(GameTestHelper helper) {
        Human human = ModEntityType.HUMAN1.get().create(helper.getLevel());
        helper.assertTrue(human != null, "Could not create natural ranged Human fixture");
        if (human == null) return;
        human.moveTo(helper.absolutePos(new BlockPos(2, 1, 2)), 0.0F, 0.0F);
        human.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(human.blockPosition()), MobSpawnType.NATURAL, null, null);
        helper.assertTrue(human.getData() != null, "Natural ranged Human fixture has no durable inventory");
        if (human.getData() == null) return;
        for (int i = 0; i < human.getData().getInventoryItemsSize(); i++) {
            human.getData().setInventoryItem(i, ItemStack.EMPTY);
        }
        human.getData().setInventoryItem(0, new ItemStack(Items.ARROW));
        human.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, Items.BOW.getDefaultInstance());
        helper.assertTrue(human.getProjectile(human.getMainHandItem()).is(Items.ARROW),
                "Natural ranged Human did not consume stored arrow");
        helper.assertTrue(human.getProjectile(human.getMainHandItem()).isEmpty(),
                "Natural ranged Human retained exhausted ammunition");
        human.kill();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "naturalHumanSpawning", timeoutTicks = 40)
    public static void finalContextChancesKeepStructuresSpecial(GameTestHelper helper) {
        helper.assertTrue(NaturalHumanSpawner.contextSpawnChance(SpawnContext.OVERWORLD_SURFACE)
                        < NaturalHumanSpawner.contextSpawnChance(SpawnContext.OVERWORLD_STRUCTURE),
                "Overworld structures did not receive a density bonus");
        helper.assertTrue(NaturalHumanSpawner.contextSpawnChance(SpawnContext.NETHER_WILDS)
                        < NaturalHumanSpawner.contextSpawnChance(SpawnContext.BASTION),
                "Bastions did not receive a density bonus");
        helper.assertTrue(NaturalHumanSpawner.contextSpawnChance(SpawnContext.END_WILDS)
                        < NaturalHumanSpawner.contextSpawnChance(SpawnContext.END_CITY),
                "End Cities did not receive a density bonus");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "naturalHumanSpawning", timeoutTicks = 40)
    public static void naturalDefaultsAllowRegularEncounters(GameTestHelper helper) {
        helper.assertTrue(Config.enableNaturalHumanSpawning.getDefault(),
                "Natural Human spawning is disabled by default");
        helper.assertTrue(Config.spawnAttemptIntervalTicks.getDefault() <= 120,
                "Natural spawn attempts are too infrequent by default");
        helper.assertTrue(Config.spawnSurfaceChance.getDefault() >= 0.30D,
                "Natural surface encounters are too rare by default");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "naturalHumanSpawnerTick", timeoutTicks = 80)
    public static void serverTickCreatesNaturalEncounter(GameTestHelper helper) {
        BlockPos playerColumn = helper.absolutePos(new BlockPos(2, 1, 2));
        LevelChunk playerChunk = helper.getLevel().getChunkSource().getChunkNow(
                playerColumn.getX() >> 4, playerColumn.getZ() >> 4);
        helper.assertTrue(playerChunk != null, "Natural-spawn player chunk is not loaded");
        if (playerChunk == null) return;
        int localX = playerColumn.getX() & 15;
        int localZ = playerColumn.getZ() & 15;
        BlockPos playerPos = new BlockPos(playerColumn.getX(),
                playerChunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, localX, localZ) + 1,
                playerColumn.getZ());
        clearNearbyHumans(helper, playerPos, 32.0D);

        AABB encounterArea = new AABB(playerPos.getX() - 32.0D, helper.getLevel().getMinBuildHeight(),
                playerPos.getZ() - 32.0D, playerPos.getX() + 32.0D,
                helper.getLevel().getMaxBuildHeight(), playerPos.getZ() + 32.0D);
        Set<UUID> initialHumanIds = new HashSet<>();
        helper.getLevel().getEntitiesOfClass(Human.class, encounterArea)
                .forEach(human -> initialHumanIds.add(human.getUUID()));

        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "natural-spawn-tick"));
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D, 0.0F, 0.0F);

        int previousInterval = Config.spawnAttemptIntervalTicks.get();
        int previousMinimum = Config.minSpawnDistance.get();
        int previousMaximum = Config.maxSpawnDistance.get();
        int previousNearbyCap = Config.nearbyHumanCapPerPlayer.get();
        int previousDimensionCap = Config.dimensionHumanCap.get();
        double previousSurfaceChance = Config.spawnSurfaceChance.get();
        double previousCaveChance = Config.spawnCaveChance.get();
        double previousStructureChance = Config.spawnStructureChance.get();
        double previousSquadChance = Config.squadChance.get();
        boolean previousEnabled = Config.enableNaturalHumanSpawning.get();
        var mobSpawningRule = helper.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING);
        boolean previousMobSpawning = mobSpawningRule.get();
        Map<ServerPlayer, GameType> previousPlayerModes = new IdentityHashMap<>();
        Runnable restoreState = () -> {
            Config.spawnAttemptIntervalTicks.set(previousInterval);
            Config.minSpawnDistance.set(previousMinimum);
            Config.maxSpawnDistance.set(previousMaximum);
            Config.nearbyHumanCapPerPlayer.set(previousNearbyCap);
            Config.dimensionHumanCap.set(previousDimensionCap);
            Config.spawnSurfaceChance.set(previousSurfaceChance);
            Config.spawnCaveChance.set(previousCaveChance);
            Config.spawnStructureChance.set(previousStructureChance);
            Config.squadChance.set(previousSquadChance);
            Config.enableNaturalHumanSpawning.set(previousEnabled);
            mobSpawningRule.set(previousMobSpawning, helper.getLevel().getServer());
            previousPlayerModes.forEach((other, gameType) -> {
                if (!other.isRemoved()) other.setGameMode(gameType);
            });
            player.discard();
        };

        try {
            for (ServerPlayer other : List.copyOf(helper.getLevel().players())) {
                if (!other.isSpectator()) {
                    previousPlayerModes.put(other, other.gameMode.getGameModeForPlayer());
                    other.setGameMode(GameType.SPECTATOR);
                }
            }
            mobSpawningRule.set(true, helper.getLevel().getServer());
            helper.assertTrue(helper.getLevel().addFreshEntity(player), "Natural-spawn FakePlayer could not be added");
            helper.assertTrue(helper.getLevel().players().contains(player), "Natural-spawn FakePlayer is not eligible in level.players()");
            helper.assertTrue(helper.getLevel().players().stream().filter(other -> !other.isSpectator()).count() == 1L,
                    "Natural-spawn FakePlayer was not the only eligible player");
            Config.enableNaturalHumanSpawning.set(true);
            Config.spawnAttemptIntervalTicks.set(20);
            Config.minSpawnDistance.set(8);
            Config.maxSpawnDistance.set(24);
            Config.nearbyHumanCapPerPlayer.set(64);
            Config.dimensionHumanCap.set(256);
            Config.spawnSurfaceChance.set(1.0D);
            Config.spawnCaveChance.set(1.0D);
            Config.spawnStructureChance.set(1.0D);
            Config.squadChance.set(0.0D);
        } catch (RuntimeException | Error failure) {
            restoreState.run();
            throw failure;
        }

        helper.runAfterDelay(65, () -> {
            List<Human> spawned = List.of();
            try {
                spawned = helper.getLevel().getEntitiesOfClass(Human.class,
                        encounterArea,
                        human -> !initialHumanIds.contains(human.getUUID())
                                && human.getType() == ModEntityType.ROAMER.get()
                                && human.getSpawnContext() != SpawnContext.UNKNOWN);
                helper.assertTrue(!spawned.isEmpty(), "Registered server tick never created a new natural Roamer");
            } finally {
                spawned.forEach(Human::discard);
                restoreState.run();
            }
            helper.succeed();
        });
    }

    private static void cleanup(List<Human> humans) {
        for (Human human : humans) human.kill();
    }

    private static void clearExistingHumans(GameTestHelper helper, BlockPos origin) {
        clearNearbyHumans(helper, origin, 10_000.0D);
    }

    private static void clearNearbyHumans(GameTestHelper helper, BlockPos origin, double radius) {
        for (Human human : helper.getLevel().getEntitiesOfClass(Human.class,
                new AABB(origin).inflate(radius))) human.discard();
    }

}
