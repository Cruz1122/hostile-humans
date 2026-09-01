package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.ai.camp.Camp;
import com.craftix.hostile_humans.entity.ai.camp.CampService;
import com.craftix.hostile_humans.entity.ai.camp.CampStorageService;
import com.craftix.hostile_humans.entity.ai.mission.CampMissionController;
import com.craftix.hostile_humans.entity.ai.mission.MissionType;
import com.craftix.hostile_humans.entity.ai.mission.SquadMissionSavedData;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class HumanRaidGameTest {
    private static final String TEMPLATE = "human_smoke";
    private HumanRaidGameTest() {}

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "raids", timeoutTicks = 40)
    public static void same_faction_is_not_a_raid_target(GameTestHelper helper) {
        Human attacker = human(helper, new BlockPos(2, 1, 2), "amilcar", UUID.randomUUID());
        Camp home = CampService.createDebugCamp(helper.getLevel(), helper.absolutePos(new BlockPos(5, 1, 2)), attacker.getPersonaDefinition().orElseThrow().faction());
        Camp same = CampService.createDebugCamp(helper.getLevel(), helper.absolutePos(new BlockPos(10, 1, 2)), attacker.getPersonaDefinition().orElseThrow().faction());
        attacker.setCampId(home.id());
        helper.assertTrue(!CampMissionController.forceRaid(attacker, same.id()), "Same-faction camp became a raid target");
        cleanup(attacker);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "raids", timeoutTicks = 100)
    public static void enemy_camp_can_be_raided_without_loot_duplication(GameTestHelper helper) {
        UUID squad = UUID.randomUUID();
        Human attacker = human(helper, new BlockPos(2, 1, 2), "amilcar", squad);
        Camp home = CampService.createDebugCamp(helper.getLevel(), helper.absolutePos(new BlockPos(5, 1, 2)), attacker.getPersonaDefinition().orElseThrow().faction());
        Human defender = human(helper, new BlockPos(12, 1, 2), "flowtives", UUID.randomUUID());
        Camp enemy = CampService.createDebugCamp(helper.getLevel(), helper.absolutePos(new BlockPos(14, 1, 2)), defender.getPersonaDefinition().orElseThrow().faction());
        attacker.setCampId(home.id()); defender.setCampId(enemy.id());
        ChestBlockEntity chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(enemy.storagePositions().get(0));
        chest.setItem(0, new ItemStack(Items.IRON_INGOT, 4));
        helper.assertTrue(CampMissionController.forceRaid(attacker, enemy.id()), "Enemy camp raid did not start");
        var state = SquadMissionSavedData.get(helper.getLevel()).state(squad);
        helper.assertTrue(state.type() == MissionType.RAID, "Raid did not use central mission state");
        helper.assertTrue(!CampStorageService.withdrawNeeded(attacker, enemy, stack -> stack.is(Items.IRON_INGOT), 0), "Zero-capacity withdrawal changed loot");
        helper.assertTrue(chest.getItem(0).getCount() == 4, "Raid setup duplicated or removed loot");
        cleanup(attacker, defender);
        helper.succeed();
    }

    private static Human human(GameTestHelper helper, BlockPos pos, String persona, UUID squad) {
        Human human = ModEntityType.HUMAN1.get().create(helper.getLevel());
        if (human == null) throw new IllegalStateException("Could not create raid Human");
        human.moveTo(helper.absolutePos(pos), 0.0F, 0.0F); human.setNoAi(true); helper.getLevel().addFreshEntity(human);
        if (!human.setPersonaId(persona) || !human.setSquadId(squad)) throw new IllegalStateException("Could not assign raid Human");
        return human;
    }
    private static void cleanup(Human... humans) { for (Human human : humans) human.kill(); }
}
