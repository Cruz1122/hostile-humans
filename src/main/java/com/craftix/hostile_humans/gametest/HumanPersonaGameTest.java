package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import com.craftix.hostile_humans.persona.ActivePersonaSavedData;
import com.craftix.hostile_humans.persona.PersonaFaction;
import com.craftix.hostile_humans.persona.PersonaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class HumanPersonaGameTest {
    private static final String TEMPLATE = "human_smoke";

    private HumanPersonaGameTest() {
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "persona", timeoutTicks = 40)
    public static void personaDatasetLoads(GameTestHelper helper) {
        PersonaRegistry registry = PersonaRegistry.get();
        helper.assertTrue(registry.size() == 731, "Expected 731 personas");
        helper.assertTrue(registry.forFaction(PersonaFaction.HISPANIC_CREATORS).size() == 308,
                "Expected 308 Hispanic personas");
        helper.assertTrue(registry.forFaction(PersonaFaction.INTERNATIONAL_CREATORS).size() == 397,
                "Expected 397 International personas");
        helper.assertTrue(registry.forFaction(PersonaFaction.MINECRAFT_LEGENDS).size() == 26,
                "Expected 26 Legend personas");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "persona", timeoutTicks = 40)
    public static void personaMatchesEntityTier(GameTestHelper helper) {
        Human tierOne = createHuman(helper, new BlockPos(1, 1, 1));
        tierOne.setCombatSkillTierOverride(CombatSkillTier.T1);
        helper.assertTrue(tierOne.assignRandomPersona(), "T1 human did not receive a persona");
        helper.assertTrue(tierOne.getPersonaDefinition().orElseThrow().tier() == 1,
                "T1 human received a persona from another tier");

        Human tierFive = createHuman(helper, new BlockPos(3, 1, 1));
        tierFive.setCombatSkillTierOverride(CombatSkillTier.T5);
        helper.assertTrue(tierFive.assignRandomPersona(), "T5 human did not receive a persona");
        helper.assertTrue(tierFive.getPersonaDefinition().orElseThrow().tier() == 5,
                "T5 human received a persona from another tier");
        tierOne.kill();
        tierFive.kill();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "persona", timeoutTicks = 40)
    public static void sameFactionIsFriendly(GameTestHelper helper) {
        Human first = createHuman(helper, new BlockPos(1, 1, 1));
        Human second = createHuman(helper, new BlockPos(3, 1, 1));
        helper.assertTrue(first.setPersonaId("amilcar"), "Amilcar could not be reserved");
        helper.assertTrue(second.setPersonaId("aquino"), "Aquino could not be reserved");
        first.setTarget(second);
        helper.assertTrue(Human.areAllies(first, second), "Same-faction humans are not allies");
        helper.assertTrue(first.getTarget() == null, "Same-faction human became a target");
        first.kill();
        second.kill();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "persona", timeoutTicks = 40)
    public static void differentFactionsAreHostile(GameTestHelper helper) {
        Human first = createHuman(helper, new BlockPos(1, 1, 1));
        Human second = createHuman(helper, new BlockPos(3, 1, 1));
        helper.assertTrue(first.setPersonaId("ymiau"), "yMiau could not be reserved");
        helper.assertTrue(second.setPersonaId("flowtives"), "Flowtives could not be reserved");
        first.setTarget(second);
        helper.assertTrue(Human.areEnemies(first, second), "Different-faction humans are not enemies");
        helper.assertTrue(first.getTarget() == second, "Enemy human could not be targeted");
        first.kill();
        second.kill();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "persona", timeoutTicks = 40)
    public static void personaCannotBeReservedTwice(GameTestHelper helper) {
        Human first = createHuman(helper, new BlockPos(1, 1, 1));
        Human second = createHuman(helper, new BlockPos(3, 1, 1));
        helper.assertTrue(first.setPersonaId("technoblade"), "First Technoblade reservation failed");
        helper.assertTrue(!second.setPersonaId("technoblade"), "Second Technoblade reservation was accepted");
        helper.assertTrue(second.getPersonaId().isEmpty(), "Rejected human retained duplicate persona ID");
        first.kill();
        second.kill();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "persona", timeoutTicks = 40)
    public static void personaReleasedOnDeath(GameTestHelper helper) {
        Human human = createHuman(helper, new BlockPos(1, 1, 1));
        helper.assertTrue(human.setPersonaId("minemanner"), "Minemanner could not be reserved");
        ActivePersonaSavedData reservations = ActivePersonaSavedData.get(helper.getLevel());
        helper.assertTrue(reservations.isReserved("minemanner"), "Minemanner was not reserved");
        human.kill();
        helper.startSequence().thenIdle(2).thenExecute(() -> {
            helper.assertTrue(!reservations.isReserved("minemanner"), "Minemanner was not released on death");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "persona", timeoutTicks = 40)
    public static void personaPersistsThroughNbt(GameTestHelper helper) {
        Human original = createHuman(helper, new BlockPos(1, 1, 1));
        helper.assertTrue(original.setPersonaId("swight"), "Swight could not be reserved");
        CompoundTag tag = new CompoundTag();
        original.save(tag);
        original.discard();

        Entity loaded = EntityType.loadEntityRecursive(tag, helper.getLevel(), entity -> entity);
        helper.assertTrue(loaded instanceof Human, "Saved Human could not be loaded");
        Human restored = (Human) loaded;
        helper.assertTrue("swight".equals(restored.getPersonaId()), "PersonaId changed during NBT round trip");
        helper.assertTrue(helper.getLevel().addFreshEntity(restored), "Restored Human could not be added");
        restored.kill();
        helper.succeed();
    }

    private static Human createHuman(GameTestHelper helper, BlockPos relativePos) {
        Human human = ModEntityType.HUMAN1.get().create(helper.getLevel());
        if (human == null) throw new IllegalStateException("human_tier1 could not be created");
        BlockPos position = helper.absolutePos(relativePos);
        human.moveTo(position, 0.0F, 0.0F);
        human.setNoAi(true);
        human.addTag("hh_persona_test");
        if (!helper.getLevel().addFreshEntity(human)) {
            throw new IllegalStateException("human_tier1 could not be added");
        }
        return human;
    }
}
