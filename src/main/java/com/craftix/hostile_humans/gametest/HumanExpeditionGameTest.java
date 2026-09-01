package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.ai.mission.CampMissionController;
import com.craftix.hostile_humans.entity.ai.mission.MissionType;
import com.craftix.hostile_humans.entity.ai.mission.SquadMissionSavedData;
import com.craftix.hostile_humans.entity.ai.camp.Camp;
import com.craftix.hostile_humans.entity.ai.camp.CampService;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class HumanExpeditionGameTest {
    private static final String TEMPLATE = "human_smoke";
    private HumanExpeditionGameTest() {}

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "expeditions", timeoutTicks = 40)
    public static void camp_squad_starts_one_expedition(GameTestHelper helper) {
        UUID squad = UUID.randomUUID();
        Human first = human(helper, new BlockPos(2, 1, 2), "amilcar", squad);
        Human second = human(helper, new BlockPos(3, 1, 2), "aquino", squad);
        Camp camp = CampService.createDebugCamp(helper.getLevel(), helper.absolutePos(new BlockPos(8, 1, 2)), first.getPersonaDefinition().orElseThrow().faction());
        helper.assertTrue(camp != null, "Debug camp could not be created for expedition fixture");
        first.setCampId(camp.id()); second.setCampId(camp.id());
        helper.assertTrue(CampMissionController.forceExpedition(first), "Camp squad could not start expedition");
        var state = SquadMissionSavedData.get(helper.getLevel()).state(squad);
        helper.assertTrue(state.type() == MissionType.EXPEDITION, "Expedition did not use central mission state");
        helper.assertTrue(state.primaryNeed() != null, "Expedition has no primary need");
        helper.assertTrue(!CampMissionController.forceExpedition(second), "Second member created a competing mission");
        cleanup(first, second);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "expeditions", timeoutTicks = 80)
    public static void squad_without_camp_does_not_start_expedition(GameTestHelper helper) {
        Human human = human(helper, new BlockPos(2, 1, 2), "carola", UUID.randomUUID());
        helper.assertTrue(!CampMissionController.forceExpedition(human), "Camp expedition started without camp membership");
        cleanup(human);
        helper.succeed();
    }

    private static Human human(GameTestHelper helper, BlockPos pos, String persona, UUID squad) {
        Human human = ModEntityType.HUMAN1.get().create(helper.getLevel());
        if (human == null) throw new IllegalStateException("Could not create expedition Human");
        human.moveTo(helper.absolutePos(pos), 0.0F, 0.0F); human.setNoAi(true); helper.getLevel().addFreshEntity(human);
        if (!human.setPersonaId(persona) || !human.setSquadId(squad)) throw new IllegalStateException("Could not assign expedition Human");
        human.getData().setInventoryItem(0, Items.COOKED_BEEF.getDefaultInstance());
        return human;
    }
    private static void cleanup(Human... humans) { for (Human human : humans) human.kill(); }
}
