package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.spawner.NaturalHumanSpawner;
import com.craftix.hostile_humans.entity.spawner.SpawnContext;
import com.craftix.hostile_humans.entity.spawner.SpawnContextClassifier;
import com.craftix.hostile_humans.persona.PersonaFaction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
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
        BlockPos candidate = helper.absolutePos(new BlockPos(28, 1, 4));
        helper.assertTrue(NaturalHumanSpawner.isLoaded(helper.getLevel(), candidate), "Test candidate chunk is not loaded");
        helper.assertTrue(NaturalHumanSpawner.isWithinSpawnDistance(Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 1, 0))), candidate),
                "Valid natural candidate failed distance check");
        BlockPos far = new BlockPos(4096, 1, 4096);
        helper.assertTrue(!NaturalHumanSpawner.isLoaded(helper.getLevel(), far), "Far chunk unexpectedly loaded");
        helper.assertTrue(!NaturalHumanSpawner.isLoaded(helper.getLevel(), far), "Chunk probe changed loaded state");
        helper.succeed();
    }

    private static void cleanup(List<Human> humans) {
        for (Human human : humans) human.kill();
    }

    private static void clearExistingHumans(GameTestHelper helper, BlockPos origin) {
        for (Human human : helper.getLevel().getEntitiesOfClass(Human.class,
                new AABB(origin).inflate(10_000.0D))) human.kill();
    }

}
