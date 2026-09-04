package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.ai.camp.Camp;
import com.craftix.hostile_humans.entity.ai.camp.CampSavedData;
import com.craftix.hostile_humans.entity.ai.settlement.GeneratedSettlementManager;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class GeneratedSettlementGameTest {
    private static final String TEMPLATE = "human_smoke";

    private GeneratedSettlementGameTest() {
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "settlements_placed", timeoutTicks = 40)
    public static void placed_settlement_initializes_camp_squad_once(GameTestHelper helper) {
        int previousDimensionCap = Config.dimensionHumanCap.get();
        int previousMinCampSpacing = Config.minCampSpacing.get();
        int previousMaxCampsPerDimension = Config.maxCampsPerDimension.get();
        boolean previousCampsEnabled = Config.enableCamps.get();
        boolean previousSettlementsEnabled = Config.enableGeneratedSettlements.get();
        GameRules.BooleanValue mobSpawning = helper.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING);
        boolean previousMobSpawning = mobSpawning.get();
        Config.dimensionHumanCap.set(Math.max(previousDimensionCap, 256));
        Config.minCampSpacing.set(16);
        Config.maxCampsPerDimension.set(256);
        Config.enableCamps.set(true);
        Config.enableGeneratedSettlements.set(true);
        mobSpawning.set(false, helper.getLevel().getServer());
        CampSavedData camps = CampSavedData.get(helper.getLevel());
        Camp camp = null;
        List<Human> humans = List.of();
        try {
            BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
            String initializationIdentity = UUID.randomUUID().toString();
            BlockPos campfire = origin.offset(2, 0, 2);
            BlockPos craftingTable = origin.offset(4, 0, 2);
            BlockPos furnace = origin.offset(6, 0, 2);
            BlockPos chest = origin.offset(8, 0, 2);
            List<BlockPos> markers = List.of(
                    origin.offset(2, 0, 6), origin.offset(4, 0, 6),
                    origin.offset(6, 0, 6), origin.offset(8, 0, 6));

            for (BlockPos position : List.of(campfire, craftingTable, furnace, chest)) {
                helper.getLevel().setBlock(position.below(), Blocks.STONE.defaultBlockState(), 3);
            }
            markers.forEach(position -> helper.getLevel().setBlock(position.below(), Blocks.STONE.defaultBlockState(), 3));
            helper.getLevel().setBlock(campfire, Blocks.CAMPFIRE.defaultBlockState(), 3);
            helper.getLevel().setBlock(craftingTable, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
            helper.getLevel().setBlock(furnace, Blocks.FURNACE.defaultBlockState(), 3);
            helper.getLevel().setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
            markers.forEach(position -> helper.getLevel().setBlock(position, Blocks.STRUCTURE_VOID.defaultBlockState(), 3));

            camps.all().forEach(candidate -> camps.remove(candidate.id()));
            helper.assertTrue(GeneratedSettlementManager.initializePlaced(helper.getLevel(), origin, origin.offset(10, 13, 10), initializationIdentity),
                    "Placed settlement did not initialize");

            Camp generatedCamp = camps.all().stream()
                    .filter(candidate -> candidate.center().equals(campfire))
                    .findFirst().orElse(null);
            camp = generatedCamp;
            helper.assertTrue(camp != null, "Generated settlement did not register its camp");
            if (camp == null) {
                helper.fail("Generated settlement camp was not found");
                return;
            }

            humans = settlementHumans(helper, camp, markers);
            helper.assertTrue(humans.size() == 4, "Generated settlement spawned " + humans.size() + " Humans instead of four");
            UUID squad = humans.isEmpty() ? null : humans.get(0).getSquadId();
            var faction = humans.isEmpty() ? null : humans.get(0).getPersonaDefinition().map(definition -> definition.faction()).orElse(null);
            helper.assertTrue(squad != null && faction != null, "Generated settlement squad has incomplete identity");
            helper.assertTrue(humans.stream().allMatch(human -> generatedCamp.id().equals(human.getCampId())),
                    "Generated settlement Humans do not share the generated camp");
            helper.assertTrue(humans.stream().allMatch(human -> squad.equals(human.getSquadId())),
                    "Generated settlement Humans do not share a squad");
            helper.assertTrue(humans.stream().allMatch(human -> human.getPersonaDefinition()
                    .map(definition -> definition.faction() == faction).orElse(false)),
                    "Generated settlement Humans do not share a faction");
            helper.assertTrue(markers.stream().allMatch(position -> helper.getLevel().isEmptyBlock(position)),
                    "Settlement spawn markers were not consumed");

            int humansAfterFirstInitialization = humans.size();
            helper.assertTrue(GeneratedSettlementManager.initializePlaced(helper.getLevel(), origin, origin.offset(10, 13, 10), initializationIdentity),
                    "Repeated settlement initialization was not accepted");
            int humansAfterSecondInitialization = settlementHumans(helper, camp, markers).size();
            helper.assertTrue(camps.get(camp.id()) == camp
                            && humansAfterSecondInitialization == humansAfterFirstInitialization,
                    "Repeated settlement initialization created duplicate camp members");
            helper.succeed();
        } finally {
            humans.forEach(Human::discard);
            if (camp != null) camps.remove(camp.id());
            Config.dimensionHumanCap.set(previousDimensionCap);
            Config.minCampSpacing.set(previousMinCampSpacing);
            Config.maxCampsPerDimension.set(previousMaxCampsPerDimension);
            Config.enableCamps.set(previousCampsEnabled);
            Config.enableGeneratedSettlements.set(previousSettlementsEnabled);
            mobSpawning.set(previousMobSpawning, helper.getLevel().getServer());
        }
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "settlements_command", timeoutTicks = 40)
    public static void debug_command_places_and_initializes_settlement(GameTestHelper helper) {
        boolean previousCampsEnabled = Config.enableCamps.get();
        boolean previousSettlementsEnabled = Config.enableGeneratedSettlements.get();
        int previousDimensionCap = Config.dimensionHumanCap.get();
        int previousMinCampSpacing = Config.minCampSpacing.get();
        int previousMaxCampsPerDimension = Config.maxCampsPerDimension.get();
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 1, 0));
        Config.enableCamps.set(true);
        Config.enableGeneratedSettlements.set(true);
        Config.dimensionHumanCap.set(Math.max(previousDimensionCap, 256));
        Config.minCampSpacing.set(16);
        Config.maxCampsPerDimension.set(256);
        CampSavedData camps = CampSavedData.get(level);
        camps.all().forEach(camp -> camps.remove(camp.id()));
        try {
            helper.assertTrue(GeneratedSettlementManager.placeDebugSettlement(level, origin, UUID.randomUUID().toString()),
                    "Debug settlement command path did not place and initialize a settlement");
            helper.assertTrue(camps.all().stream()
                            .anyMatch(camp -> camp.center().equals(origin.offset(24, 2, 24))),
                    "Debug settlement command path did not register its generated camp");
        } finally {
            level.getEntitiesOfClass(Human.class, new net.minecraft.world.phys.AABB(origin).inflate(48))
                    .forEach(Human::discard);
            camps.all().forEach(camp -> camps.remove(camp.id()));
            Config.enableCamps.set(previousCampsEnabled);
            Config.enableGeneratedSettlements.set(previousSettlementsEnabled);
            Config.dimensionHumanCap.set(previousDimensionCap);
            Config.minCampSpacing.set(previousMinCampSpacing);
            Config.maxCampsPerDimension.set(previousMaxCampsPerDimension);
        }
        helper.succeed();
    }

    private static List<Human> settlementHumans(GameTestHelper helper, Camp camp, List<BlockPos> markers) {
        return helper.getLevel().getEntitiesOfClass(Human.class,
                new net.minecraft.world.phys.AABB(camp.center()).inflate(12.0D),
                human -> markers.stream().anyMatch(marker -> human.distanceToSqr(marker.getX() + .5D,
                        marker.getY(), marker.getZ() + .5D) < 4.0D));
    }
}
