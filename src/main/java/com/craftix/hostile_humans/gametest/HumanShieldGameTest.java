package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;
import com.craftix.hostile_humans.entity.ai.combat.CombatAction;
import com.craftix.hostile_humans.entity.ai.combat.CombatIntent;
import com.craftix.hostile_humans.entity.ai.combat.ShieldState;
import com.craftix.hostile_humans.entity.ai.goal.MeleeAttackGoal;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("hostile_humans")
@PrefixGameTestTemplate(false)
public final class HumanShieldGameTest {
    private static final String TEMPLATE = "human_smoke";

    private HumanShieldGameTest() {
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 40)
    public static void everyCombatTierSupportsAggressiveCombos(GameTestHelper helper) {
        double previousAccuracy = 1.0D;
        for (CombatSkillTier tier : CombatSkillTier.values()) {
            int cooldownMin = tier.attackCooldownMin(7);
            int cooldownMax = tier.attackCooldownMax(14, cooldownMin);
            helper.assertTrue(cooldownMin >= 2 && cooldownMax <= 9,
                    tier + " did not receive an aggressive combo cooldown");
            helper.assertTrue(cooldownMin <= cooldownMax,
                    tier + " produced an invalid cooldown range");
            helper.assertTrue(tier.attackAccuracy() > 0.0D && tier.attackAccuracy() < 1.0D,
                    tier + " must be capable of both hits and misses");
            helper.assertTrue(tier.attackAccuracy() < previousAccuracy,
                    tier + " should be less accurate than the previous skill tier");
            previousAccuracy = tier.attackAccuracy();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 40)
    public static void meleeCooldownRespectsEquippedWeaponSpeed(GameTestHelper helper) {
        int emptyHandCooldown = MeleeAttackGoal.weaponCooldownTicks(ItemStack.EMPTY);
        int swordCooldown = MeleeAttackGoal.weaponCooldownTicks(Items.IRON_SWORD.getDefaultInstance());
        int axeCooldown = MeleeAttackGoal.weaponCooldownTicks(Items.IRON_AXE.getDefaultInstance());

        helper.assertTrue(swordCooldown > emptyHandCooldown,
                "A sword did not add its attack-speed cooldown");
        helper.assertTrue(axeCooldown > swordCooldown,
                "A slower axe did not receive a longer attack cooldown");
        helper.assertTrue(axeCooldown >= 20,
                "The equipped axe can still be spammed before vanilla recovery");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 40)
    public static void shieldRequiresUsableOffhand(GameTestHelper helper) {
        Human human = createHuman(helper);
        helper.assertTrue(human.getCombatTacticsController().shieldState() == ShieldState.UNAVAILABLE,
                "A human without an offhand shield must start unavailable");
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        helper.assertTrue(human.getOffhandItem().canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK),
                "Shield was not recognized through Forge ToolActions");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 40)
    public static void meleePressureRequiresRecentHit(GameTestHelper helper) {
        Human human = createHuman(helper);
        human.tickCount = 100;

        helper.assertTrue(!human.isUnderMeleePressure(),
                "A human that has never been hit was treated as under melee pressure");

        human.lastReceivedCombatHitTick = 99;
        helper.assertTrue(human.isUnderMeleePressure(),
                "A hit from the previous tick was not treated as melee pressure");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 60)
    public static void farTargetSelectsBowWithoutDroppingShield(GameTestHelper helper) {
        Human human = createHuman(helper);
        Zombie target = createZombie(helper, new BlockPos(11, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        human.getData().setInventoryItem(0, new ItemStack(Items.BOW));
        human.setTarget(target);
        human.tickCount = 100;
        human.switchingWeaponCoolDown = 0;

        assertLineOfSight(helper, human, target);
        human.updateCombatWeaponSelection();

        helper.assertTrue(human.getMainHandItem().is(Items.BOW),
                "A distant visible target did not select the owned bow");
        helper.assertTrue(human.getOffhandItem().is(Items.SHIELD),
                "Changing the main-hand weapon removed the offhand shield");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 60)
    public static void closeTargetReturnsToMelee(GameTestHelper helper) {
        Human human = createHuman(helper);
        Zombie target = createZombie(helper, new BlockPos(4, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        human.getData().setInventoryItem(0, new ItemStack(Items.IRON_SWORD));
        human.setTarget(target);
        human.tickCount = 100;
        human.switchingWeaponCoolDown = 0;

        assertLineOfSight(helper, human, target);
        human.updateCombatWeaponSelection();

        helper.assertTrue(human.getMainHandItem().is(Items.IRON_SWORD),
                "A close target did not restore the owned melee weapon");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 60)
    public static void comboEscapeStillAllowsCounterattack(GameTestHelper helper) {
        Human human = createHuman(helper);
        Zombie target = createZombie(helper, new BlockPos(4, 1, 2));
        human.setCombatSkillTierOverride(CombatSkillTier.T1);
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        human.setTarget(target);
        human.tickCount = 100;
        human.lastReceivedCombatHitTick = 99;
        human.consecutiveReceivedCombatHits = 3;

        assertLineOfSight(helper, human, target);
        CombatIntent intent = human.getCombatTacticsController().evaluate();

        helper.assertTrue(intent.action() == CombatAction.STRAFE_LEFT
                        || intent.action() == CombatAction.STRAFE_RIGHT,
                "Combo pressure did not produce lateral escape movement");
        helper.assertTrue(intent.allowMeleeAttack(),
                "Combo escape incorrectly disabled every counterattack");
        helper.assertTrue(intent.commitmentUntilTick() - human.tickCount >= 12,
                "Combo escape commitment is too short to create visible movement");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 80)
    public static void t1HoldsShieldUnderSwordPressure(GameTestHelper helper) {
        Human human = createHuman(helper);
        Zombie target = createZombie(helper, new BlockPos(4, 1, 2));
        target.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        human.setCombatSkillTierOverride(CombatSkillTier.T1);
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        human.setTarget(target);
        human.lastReceivedCombatHitTick = human.tickCount;
        human.consecutiveReceivedCombatHits = 2;
        assertLineOfSight(helper, human, target);

        helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    human.lastReceivedCombatHitTick = human.tickCount;
                    human.consecutiveReceivedCombatHits = 3;
                })
                .thenIdle(6)
                .thenExecute(() -> {
                    helper.assertTrue(human.isUsingItem(),
                            "T1 did not hold its shield through observable sword pressure");
                    ShieldState shieldState = human.getCombatIntent().shieldState();
                    helper.assertTrue(shieldState == ShieldState.RAISING || shieldState == ShieldState.BLOCKING,
                            "T1 left its active shield state during observable sword pressure");
                    helper.assertTrue(human.getCombatIntent().action() == CombatAction.HOLD_POSITION,
                            "Active shield defense requested sustained lateral orbiting");
                    helper.succeed();
                });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 80)
    public static void t1KeepsShieldRaisedAgainstAxeWindup(GameTestHelper helper) {
        Human human = createHuman(helper);
        Zombie target = createZombie(helper, new BlockPos(4, 1, 2));
        target.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        human.setCombatSkillTierOverride(CombatSkillTier.T1);
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        human.setTarget(target);
        human.lastReceivedCombatHitTick = human.tickCount;
        human.consecutiveReceivedCombatHits = 2;
        assertLineOfSight(helper, human, target);

        helper.startSequence()
                .thenIdle(8)
                .thenExecute(() -> {
                    human.lastReceivedCombatHitTick = human.tickCount;
                    human.consecutiveReceivedCombatHits = 3;
                    target.swing(InteractionHand.MAIN_HAND);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(human.shieldDisabledUntilTick <= human.tickCount,
                            "Shield was disabled without an axe hit connecting");
                    helper.assertTrue(human.isUsingItem(),
                            "T1 proactively lowered its shield during a visible axe wind-up");
                    ShieldState shieldState = human.getCombatIntent().shieldState();
                    helper.assertTrue(shieldState == ShieldState.RAISING || shieldState == ShieldState.BLOCKING,
                            "T1 left active shield defense before the axe hit connected");
                    helper.succeed();
                });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 60)
    public static void criticalModifierAmplifiesOneHit(GameTestHelper helper) {
        Human human = createHuman(helper);
        Zombie normalTarget = createZombie(helper, new BlockPos(3, 1, 2));
        Zombie criticalTarget = createZombie(helper, new BlockPos(4, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));

        float normalDamage = 0.0F;
        for (int attempt = 0; attempt < 40 && normalDamage <= 0.0F; attempt++) {
            normalTarget.setHealth(normalTarget.getMaxHealth());
            normalTarget.invulnerableTime = 0;
            human.criticalStrikeReady = false;
            human.doHurtTarget(normalTarget);
            normalDamage = normalTarget.getMaxHealth() - normalTarget.getHealth();
        }
        float criticalDamage = 0.0F;
        for (int attempt = 0; attempt < 40 && criticalDamage <= 0.0F; attempt++) {
            criticalTarget.setHealth(criticalTarget.getMaxHealth());
            criticalTarget.invulnerableTime = 0;
            human.criticalStrikeReady = true;
            human.doHurtTarget(criticalTarget);
            criticalDamage = criticalTarget.getMaxHealth() - criticalTarget.getHealth();
        }

        helper.assertTrue(criticalDamage > normalDamage,
                "Critical strike did not amplify the single melee hit");
        helper.assertTrue(!human.criticalStrikeReady,
                "Critical strike state was not consumed after attacking");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 100)
    public static void humanAttacksHostileMob(GameTestHelper helper) {
        Human human = createHuman(helper);
        helper.getLevel().getEntitiesOfClass(Mob.class, human.getBoundingBox().inflate(6.0D), entity -> entity != human)
                .forEach(Entity::discard);
        Zombie target = createZombie(helper, new BlockPos(4, 1, 2));
        human.setNoAi(false);
        human.setCombatSkillTierOverride(CombatSkillTier.T1);
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        human.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        human.setCombatTask();
        human.setTarget(target);
        float initialHealth = target.getHealth();

        helper.startSequence()
                .thenIdle(80)
                .thenExecute(() -> {
                    helper.assertTrue(target.getHealth() < initialHealth,
                            "Human targeted the hostile mob but never attacked it");
                    helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 40)
    public static void roamerTargetPolicyIncludesHostileMobs(GameTestHelper helper) {
        Human roamer = ModEntityType.ROAMER.get().create(helper.getLevel());
        if (roamer == null) throw new IllegalStateException("Roamer creation failed");
        Zombie target = createZombie(helper, new BlockPos(4, 1, 2));
        helper.assertTrue(roamer.shouldTargetMob(target),
                "Roamer target policy excluded hostile mobs");
        roamer.discard();
        target.discard();
        helper.succeed();
    }

    private static Human createHuman(GameTestHelper helper) {
        for (int x = 0; x <= 12; x++) {
            for (int z = 0; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        Human human = ModEntityType.HUMAN1.get().create(helper.getLevel());
        if (human == null) throw new IllegalStateException("Human creation failed");
        human.moveTo(helper.absolutePos(new BlockPos(2, 1, 2)), 0.0F, 0.0F);
        human.setNoAi(true);
        helper.getLevel().addFreshEntity(human);
        return human;
    }

    private static Zombie createZombie(GameTestHelper helper, BlockPos localPos) {
        Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
        if (zombie == null) throw new IllegalStateException("Zombie creation failed");
        zombie.moveTo(helper.absolutePos(localPos), 180.0F, 0.0F);
        zombie.setNoAi(true);
        helper.getLevel().addFreshEntity(zombie);
        return zombie;
    }

    private static void assertLineOfSight(GameTestHelper helper, Human human, Zombie target) {
        helper.assertTrue(human.hasLineOfSight(target),
                "Test setup must provide line of sight between combatants");
    }
}
