package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.ai.squad.SquadAlertReason;
import com.craftix.hostile_humans.entity.ai.squad.SquadManager;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class HumanSquadGameTest {
    private static final String TEMPLATE = "human_smoke";

    private HumanSquadGameTest() {
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "squadTactics", timeoutTicks = 40)
    public static void sameSquadSharesTarget(GameTestHelper helper) {
        UUID squadId = UUID.randomUUID();
        Human scout = createHuman(helper, new BlockPos(1, 1, 1), "amilcar", squadId);
        Human member = createHuman(helper, new BlockPos(3, 1, 1), "aquino", squadId);
        Human enemy = createHuman(helper, new BlockPos(5, 1, 1), "flowtives", null);

        scout.setTarget(enemy);

        helper.assertTrue(member.getTarget() == enemy, "Same-squad member did not acquire the shared target");
        cleanup(scout, member, enemy);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "squadTactics", timeoutTicks = 40)
    public static void sameFactionDifferentSquadDoesNotInstantShare(GameTestHelper helper) {
        Human scout = createHuman(helper, new BlockPos(1, 1, 1), "carola", UUID.randomUUID());
        Human bystander = createHuman(helper, new BlockPos(3, 1, 1), "arigameplays", UUID.randomUUID());
        Human enemy = createHuman(helper, new BlockPos(5, 1, 1), "dream", null);

        scout.setTarget(enemy);

        helper.assertTrue(bystander.getTarget() == null,
                "Same-faction Human in another squad received a magical target");
        cleanup(scout, bystander, enemy);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "squadTactics", timeoutTicks = 40)
    public static void differentFactionNeverJoinsSquad(GameTestHelper helper) {
        UUID corruptedId = UUID.randomUUID();
        Human hispanic = createHuman(helper, new BlockPos(1, 1, 1), "elrichmc", corruptedId);
        Human international = createHuman(helper, new BlockPos(3, 1, 1), "sapnap", corruptedId);

        helper.assertTrue(!SquadManager.canShareSquad(hispanic, international),
                "Different-faction Humans were accepted as cooperative squad members");
        helper.assertTrue(!SquadManager.nearbyMembers(hispanic).contains(international),
                "Corrupt mixed-faction squad member was not ignored");
        cleanup(hispanic, international);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "squadTactics", timeoutTicks = 40)
    public static void hurtMemberAlertsSquad(GameTestHelper helper) {
        UUID squadId = UUID.randomUUID();
        Human victim = createHuman(helper, new BlockPos(1, 1, 1), "spreendmc", squadId);
        Human defender = createHuman(helper, new BlockPos(3, 1, 1), "elrubius", squadId);
        Human attacker = createHuman(helper, new BlockPos(5, 1, 1), "purpled", null);

        victim.hurt(helper.getLevel().damageSources().mobAttack(attacker), 1.0F);

        helper.assertTrue(defender.getTarget() == attacker, "Damage to a squad member did not alert its defender");
        cleanup(victim, defender, attacker);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "squadTactics", timeoutTicks = 40)
    public static void sharedAggroCreatesAnAttackOpportunity(GameTestHelper helper) {
        UUID squadId = UUID.randomUUID();
        Human victim = createHuman(helper, new BlockPos(1, 1, 1), "coldified", squadId);
        Human defender = createHuman(helper, new BlockPos(3, 1, 1), "juanclean", squadId);
        Human attacker = createHuman(helper, new BlockPos(5, 1, 1), "technoblade", null);
        defender.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        defender.startUsingItem(InteractionHand.OFF_HAND);

        victim.hurt(helper.getLevel().damageSources().mobAttack(attacker), 1.0F);

        helper.assertTrue(defender.getTarget() == attacker,
                "Squad defender did not acquire the enemy attacking its companion");
        helper.assertTrue(!defender.isUsingItem(),
                "Squad defender kept covering with a shield after receiving shared aggro");
        var intent = defender.getCombatTacticsController().evaluate();
        helper.assertTrue(intent.action() == com.craftix.hostile_humans.entity.ai.combat.CombatAction.ATTACK
                        && intent.allowMeleeAttack(),
                "Squad defender did not exploit the shared aggro as an attack opportunity");
        cleanup(victim, defender, attacker);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "squadTactics", timeoutTicks = 40)
    public static void retreatingLowAllyIsProtected(GameTestHelper helper) {
        UUID squadId = UUID.randomUUID();
        Human retreating = createHuman(helper, new BlockPos(1, 1, 1), "shadoune666", squadId);
        Human defender = createHuman(helper, new BlockPos(3, 1, 1), "farfadox", squadId);
        Human attacker = createHuman(helper, new BlockPos(5, 1, 1), "tommyinnit", null);
        retreating.setHealth(1.0F);
        retreating.isFleeing = true;
        retreating.toAvoid = attacker;

        SquadManager.alertRetreatingAlly(retreating, attacker);

        helper.assertTrue(defender.getTarget() == attacker, "Healthy squad member did not protect retreating ally");
        helper.assertTrue(!defender.isFleeing, "Healthy defender was forced into squad-wide retreat");
        helper.assertTrue(retreating.isFleeing, "Protection alert cancelled the wounded ally's retreat");
        helper.assertTrue(retreating.getTarget() == null, "Retreating ally reacquired a combat target");
        cleanup(retreating, defender, attacker);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "squadTactics", timeoutTicks = 40)
    public static void squadIdPersistsThroughNbt(GameTestHelper helper) {
        UUID squadId = UUID.randomUUID();
        Human original = createHuman(helper, new BlockPos(1, 1, 1), "serpias", squadId);
        CompoundTag tag = new CompoundTag();
        original.save(tag);
        original.discard();

        Entity loaded = EntityType.loadEntityRecursive(tag, helper.getLevel(), entity -> entity);
        helper.assertTrue(loaded instanceof Human, "Saved squad Human could not be loaded");
        Human restored = (Human) loaded;
        helper.assertTrue(squadId.equals(restored.getSquadId()), "SquadId changed during NBT round trip");
        helper.assertTrue(helper.getLevel().addFreshEntity(restored), "Restored squad Human could not be added");
        restored.kill();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "squadTactics", timeoutTicks = 40)
    public static void sharedTargetCommitmentPreventsPingPong(GameTestHelper helper) {
        Human member = createHuman(helper, new BlockPos(1, 1, 1), "silithur", UUID.randomUUID());
        Human first = createHuman(helper, new BlockPos(3, 1, 1), "boosfer", null);
        Human second = createHuman(helper, new BlockPos(5, 1, 1), "branzycraft", null);

        member.receiveSquadAlert(first, first.blockPosition(), helper.getLevel().getGameTime(), SquadAlertReason.SHARED_AGGRO);
        member.receiveSquadAlert(second, second.blockPosition(), helper.getLevel().getGameTime(), SquadAlertReason.SHARED_AGGRO);

        helper.assertTrue(member.getTarget() == first, "Equal-priority squad alerts caused immediate target ping-pong");
        cleanup(member, first, second);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "squadTactics", timeoutTicks = 40)
    public static void lastKnownTargetPositionExpires(GameTestHelper helper) {
        Human member = createHuman(helper, new BlockPos(1, 1, 1), "soypan", UUID.randomUUID());
        long staleTick = helper.getLevel().getGameTime() - SquadManager.MEMORY_TICKS - 1L;
        member.rememberSquadThreat(UUID.randomUUID(), helper.absolutePos(new BlockPos(4, 1, 1)), staleTick);

        helper.assertTrue(!member.hasFreshSquadThreatMemory(), "Squad target position did not expire");
        helper.assertTrue(member.getLastKnownSquadTargetPos() == null, "Expired squad position remained actionable");
        cleanup(member);
        helper.succeed();
    }

    private static Human createHuman(GameTestHelper helper, BlockPos relativePos, String personaId, UUID squadId) {
        Human human = ModEntityType.HUMAN1.get().create(helper.getLevel());
        if (human == null) throw new IllegalStateException("human_tier1 could not be created");
        human.moveTo(helper.absolutePos(relativePos), 0.0F, 0.0F);
        human.setNoAi(true);
        human.addTag("hh_squad_test");
        if (!helper.getLevel().addFreshEntity(human)) throw new IllegalStateException("Human could not be added");
        if (!human.setPersonaId(personaId)) throw new IllegalStateException("Persona could not be reserved: " + personaId);
        if (squadId != null && !human.setSquadId(squadId)) throw new IllegalStateException("SquadId could not be assigned");
        return human;
    }

    private static void cleanup(Human... humans) {
        for (Human human : humans) human.kill();
    }
}
