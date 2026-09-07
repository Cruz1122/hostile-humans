package com.craftix.hostile_humans.gametest;

import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;
import com.craftix.hostile_humans.entity.ai.combat.CombatAction;
import com.craftix.hostile_humans.entity.ai.combat.CombatIntent;
import com.craftix.hostile_humans.entity.ai.combat.CombatTactic;
import com.craftix.hostile_humans.entity.ai.combat.ShieldState;
import com.craftix.hostile_humans.entity.ai.goal.MeleeAttackGoal;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import com.mojang.authlib.GameProfile;

import java.util.UUID;

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
    public static void shieldBreakingAxeDisablesEnemyShield(GameTestHelper helper) {
        Human human = createHuman(helper);
        Human target = ModEntityType.HUMAN1.get().create(helper.getLevel());
        if (target == null) throw new IllegalStateException("Could not create shield target");
        helper.assertTrue(human.setPersonaId("coldified"), "Could not reserve attacker persona");
        helper.assertTrue(target.setPersonaId("technoblade"), "Could not reserve shield target persona");
        target.setNoAi(true);
        target.moveTo(helper.absolutePos(new BlockPos(4, 1, 2)), 180.0F, 0.0F);
        target.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        helper.getLevel().addFreshEntity(target);
        target.startUsingItem(InteractionHand.OFF_HAND);
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        human.setCombatSkillTierOverride(CombatSkillTier.T1);
        human.setTarget(target);
        helper.assertTrue(target.isUsingItem(), "Shield test fixture did not start using its shield");

        CombatIntent intent = human.getCombatTacticsController().evaluate();
        helper.assertTrue(intent.action() == CombatAction.ATTACK && intent.allowMeleeAttack(),
                "Equipped axe was kept in shield-switch mode instead of being used to attack");
        target.setHealth(100.0F);
        for (int attempt = 0; attempt < 10 && target.isUsingItem(); attempt++) {
            human.doHurtTarget(target);
        }
        helper.assertTrue(!target.isUsingItem(), "Axe hit did not disable the enemy shield");
        helper.assertTrue(target.shieldDisabledUntilTick > target.tickCount,
                "Axe hit did not put the enemy shield on cooldown");
        target.kill();
        human.kill();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 40)
    public static void rangedAimAppliesMovementPenalty(GameTestHelper helper) {
        Human human = createHuman(helper);
        double normalSpeed = human.getAttributeValue(Attributes.MOVEMENT_SPEED);
        for (ItemStack rangedWeapon : new ItemStack[]{new ItemStack(Items.BOW), new ItemStack(Items.CROSSBOW)}) {
            human.setItemSlot(EquipmentSlot.MAINHAND, rangedWeapon);
            human.startUsingItem(InteractionHand.MAIN_HAND);
            helper.assertTrue(human.getAttributeValue(Attributes.MOVEMENT_SPEED) < normalSpeed,
                    rangedWeapon.getItem() + " did not apply a movement penalty while aiming");
            human.stopUsingItem();
            helper.assertTrue(Math.abs(human.getAttributeValue(Attributes.MOVEMENT_SPEED) - normalSpeed) < 0.0001D,
                    rangedWeapon.getItem() + " left the aiming movement penalty active after release");
        }
        human.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 40)
    public static void rangedLoadoutDoesNotHideBehindShield(GameTestHelper helper) {
        Human human = createHuman(helper);
        Zombie target = createZombie(helper, new BlockPos(4, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        human.setTarget(target);
        human.startUsingItem(InteractionHand.OFF_HAND);

        CombatIntent intent = human.getCombatTacticsController().evaluate();

        helper.assertTrue(intent.allowMeleeAttack(),
                "A ranged loadout with a shield was incorrectly converted into a permanent bunker");
        helper.assertTrue(!human.isUsingItem(),
                "A ranged loadout kept its shield raised instead of remaining attackable");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 40)
    public static void shieldLoadoutSuppressesProjectileKnockback(GameTestHelper helper) {
        Human target = createHuman(helper);
        Human shooter = createHuman(helper);
        target.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        target.startUsingItem(InteractionHand.OFF_HAND);
        target.setDeltaMovement(0.12D, 0.0D, -0.08D);
        Arrow arrow = new Arrow(helper.getLevel(), shooter);
        target.setYRot(0.0F);
        arrow.moveTo(target.getX(), target.getEyeY(), target.getZ() + 2.0D, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(arrow);

        Vec3 expected = target.getDeltaMovement();
        float healthBefore = target.getHealth();
        boolean hurt = target.hurt(helper.getLevel().damageSources().arrow(arrow, shooter), 1.0F);

        Vec3 actual = target.getDeltaMovement();
        helper.assertTrue(!hurt && target.getHealth() == healthBefore,
                "A shielded NPC took projectile damage instead of reflecting the arrow");
        helper.assertTrue(Math.abs(actual.x - expected.x) < 0.0001D
                        && Math.abs(actual.z - expected.z) < 0.0001D,
                "A projectile applied knockback to an NPC carrying a shield");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 40)
    public static void meleeNpcChasesRangedAttackerInsteadOfHoldingShield(GameTestHelper helper) {
        Human melee = createHuman(helper);
        Human ranged = createHuman(helper);
        melee.addTag("hh_team_arena");
        melee.addTag("hh_team_red");
        ranged.addTag("hh_team_arena");
        ranged.addTag("hh_team_blue");
        ranged.moveTo(helper.absolutePos(new BlockPos(10, 1, 2)), 180.0F, 0.0F);
        melee.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        melee.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        ranged.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        ranged.startUsingItem(InteractionHand.MAIN_HAND);
        melee.setTarget(ranged);
        melee.startUsingItem(InteractionHand.OFF_HAND);

        CombatIntent intent = melee.getCombatTacticsController().evaluate();

        helper.assertTrue(intent.action() == CombatAction.ATTACK && intent.allowMeleeAttack(),
                "A melee NPC did not switch to aggressive pursuit of a ranged attacker");
        helper.assertTrue(!melee.isUsingItem(),
                "A melee NPC kept its shield raised while its ranged target was attacking");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 40)
    public static void rangedOnlyNpcMeleesThenJumpsAwayAtCloseRange(GameTestHelper helper) {
        Human human = createHuman(helper);
        Zombie target = createZombie(helper, new BlockPos(4, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        human.setTarget(target);
        float healthBefore = target.getHealth();

        boolean handled = human.handleRangedMeleeFallback(target);

        helper.assertTrue(handled && target.getHealth() < healthBefore,
                "A ranged-only NPC did not perform its close-range melee fallback");
        helper.assertTrue(human.getDeltaMovement().y >= 0.42D,
                "The ranged NPC did not jump away after its close-range hit");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 40)
    public static void unshieldedTargetTriggersImmediateMeleePressure(GameTestHelper helper) {
        Human human = createHuman(helper);
        Zombie target = createZombie(helper, new BlockPos(4, 1, 2));
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        human.setTarget(target);

        CombatIntent intent = human.getCombatTacticsController().evaluate();

        helper.assertTrue(intent.tactic() == CombatTactic.PRESSURE
                        && intent.action() == CombatAction.ATTACK
                        && intent.allowMeleeAttack(),
                "A target without a shield did not trigger immediate melee pressure");
        helper.assertTrue(intent.shieldState() == ShieldState.READY,
                "An available NPC shield was incorrectly consumed by unshielded-target pressure");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 40)
    public static void disabledTargetShieldTriggersImmediateMeleePressure(GameTestHelper helper) {
        Human human = createHuman(helper);
        Human target = createHuman(helper);
        helper.assertTrue(human.setPersonaId("coldified"), "Could not reserve attacker persona");
        helper.assertTrue(target.setPersonaId("technoblade"), "Could not reserve target persona");
        target.moveTo(helper.absolutePos(new BlockPos(4, 1, 2)), 180.0F, 0.0F);
        target.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        target.shieldDisabledUntilTick = target.tickCount + 100;
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        human.setTarget(target);

        CombatIntent intent = human.getCombatTacticsController().evaluate();

        helper.assertTrue(intent.action() == CombatAction.ATTACK && intent.allowMeleeAttack(),
                "A target with an axe-disabled shield did not trigger immediate melee pressure");
        helper.assertTrue(intent.shieldState() == ShieldState.READY,
                "An available NPC shield was incorrectly consumed by disabled-target pressure");
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
    public static void retaliationSurvivesMeleeGoalTransition(GameTestHelper helper) {
        Human human = createHuman(helper);
        Zombie attacker = createZombie(helper, new BlockPos(4, 1, 2));

        human.hurt(helper.getLevel().damageSources().mobAttack(attacker), 1.0F);
        helper.assertTrue(human.getTarget() == attacker,
                "A valid attacker was not selected for immediate retaliation");

        new MeleeAttackGoal(human, 1.0D, true).stop();
        helper.assertTrue(human.getTarget() == attacker,
                "Stopping a temporary melee goal cleared the retaliation target");
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
        target.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
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
        target.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
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
        float ascendingDamage = 0.0F;
        for (int attempt = 0; attempt < 40 && ascendingDamage <= 0.0F; attempt++) {
            criticalTarget.setHealth(criticalTarget.getMaxHealth());
            criticalTarget.invulnerableTime = 0;
            human.criticalStrikeReady = true;
            human.setOnGround(false);
            human.setDeltaMovement(0.0D, 0.1D, 0.0D);
            human.fallDistance = 1.0F;
            helper.assertTrue(!human.isDescendingForCritical(),
                    "Ascending critical fixture was incorrectly considered descending");
            human.doHurtTarget(criticalTarget);
            ascendingDamage = criticalTarget.getMaxHealth() - criticalTarget.getHealth();
        }
        for (int attempt = 0; attempt < 40 && criticalDamage <= 0.0F; attempt++) {
            criticalTarget.setHealth(criticalTarget.getMaxHealth());
            criticalTarget.invulnerableTime = 0;
            human.criticalStrikeReady = true;
            human.setOnGround(false);
            human.setDeltaMovement(0.0D, -0.1D, 0.0D);
            human.fallDistance = 1.0F;
            helper.assertTrue(human.isDescendingForCritical(),
                    "Critical fixture was not descending before the critical attack");
            human.doHurtTarget(criticalTarget);
            criticalDamage = criticalTarget.getMaxHealth() - criticalTarget.getHealth();
        }

        helper.assertTrue(criticalDamage > normalDamage,
                "Critical strike did not amplify the single melee hit");
        helper.assertTrue(ascendingDamage <= normalDamage,
                "Ascending attack received critical damage before the descent");
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
    public static void combatAlwaysFacesActiveTarget(GameTestHelper helper) {
        Human human = createHuman(helper);
        Zombie target = createZombie(helper, new BlockPos(4, 1, 2));
        human.setTarget(target);
        human.setYRot(180.0F);
        human.setXRot(0.0F);
        human.yBodyRot = 180.0F;
        human.yHeadRot = 180.0F;

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> {
                    float expectedYaw = -90.0F;
                    helper.assertTrue(Math.abs(Mth.wrapDegrees(human.getYRot() - expectedYaw)) < 1.0F,
                            "Human finished a combat tick facing away from its target: yaw="
                                    + human.getYRot() + ", target=" + target.blockPosition());
                    helper.assertTrue(Math.abs(Mth.wrapDegrees(human.yBodyRot - expectedYaw)) < 1.0F,
                            "Human body finished a combat tick facing away from its target: bodyYaw="
                                    + human.yBodyRot);
                    helper.succeed();
                });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 40)
    public static void creativePlayersAreIgnoredAsCombatTargets(GameTestHelper helper) {
        Human human = createHuman(helper);
        FakePlayer player = new FakePlayer(
                helper.getLevel(), new GameProfile(UUID.randomUUID(), "hh-creative-combat"));
        player.setGameMode(GameType.SURVIVAL);
        human.setTarget(player);
        helper.assertTrue(human.getTarget() == player,
                "Survival player could not be used to seed the target transition");

        player.setGameMode(GameType.CREATIVE);
        helper.assertTrue(!human.canAttack(player), "Creative player remained attackable");
        human.setTarget(player);
        helper.assertTrue(human.getTarget() == null,
                "Human accepted a creative player as a newly assigned target");

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> {
                    helper.assertTrue(human.getTarget() == null,
                            "Human retained a player after that player switched to creative");
                    helper.succeed();
                });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 140)
    public static void rangedWeaponsActuallyFire(GameTestHelper helper) {
        Human human = createHuman(helper);
        helper.getLevel().getEntitiesOfClass(Mob.class, human.getBoundingBox().inflate(12.0D), entity -> entity != human)
                .forEach(Entity::discard);
        Zombie target = createZombie(helper, new BlockPos(10, 1, 2));
        human.setNoAi(false);
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        human.getData().setInventoryItem(0, new ItemStack(Items.ARROW, 8));
        human.setTarget(target);
        float initialHealth = target.getHealth();

        helper.startSequence()
                .thenIdle(80)
                .thenExecute(() -> {
                    boolean fired = target.getHealth() < initialHealth
                            || !helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.AbstractArrow.class,
                            new AABB(human.blockPosition()).inflate(16.0D)).isEmpty();
                    helper.assertTrue(fired, "Human equipped a bow but never fired an arrow");
                    helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 180)
    public static void crossbowsActuallyFire(GameTestHelper helper) {
        Human human = createHuman(helper);
        helper.getLevel().getEntitiesOfClass(Mob.class, human.getBoundingBox().inflate(12.0D), entity -> entity != human)
                .forEach(Entity::discard);
        Zombie target = createZombie(helper, new BlockPos(10, 1, 2));
        human.setNoAi(false);
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        human.getData().setInventoryItem(0, new ItemStack(Items.ARROW, 8));
        human.setTarget(target);
        float initialHealth = target.getHealth();

        helper.startSequence()
                .thenIdle(120)
                .thenExecute(() -> helper.assertTrue(target.getHealth() < initialHealth,
                        "Human equipped a crossbow but never fired a bolt"))
                .thenExecute(helper::succeed);
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 80)
    public static void goldenApplesApplyVanillaEffects(GameTestHelper helper) {
        Human human = createHuman(helper);
        human.setHealth(human.getMaxHealth() / 2.0F);
        human.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
        human.startUsingItem(InteractionHand.MAIN_HAND);

        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(human.hasEffect(MobEffects.ABSORPTION),
                            "Enchanted golden apple did not apply absorption");
                    helper.assertTrue(human.hasEffect(MobEffects.REGENERATION),
                            "Enchanted golden apple did not apply regeneration");
                    helper.assertTrue(human.hasEffect(MobEffects.DAMAGE_RESISTANCE),
                            "Enchanted golden apple did not apply resistance");
                    helper.succeed();
                });
    }

    @GameTest(template = TEMPLATE, templateNamespace = "hostile_humans", batch = "shieldTactics", timeoutTicks = 40)
    public static void totemPreventsLethalDamage(GameTestHelper helper) {
        Human human = createHuman(helper);
        human.setHealth(1.0F);
        human.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));

        boolean hurt = human.hurt(helper.getLevel().damageSources().generic(), 100.0F);

        helper.assertTrue(!hurt && human.isAlive(), "Totem did not prevent lethal damage");
        helper.assertTrue(human.hasEffect(MobEffects.REGENERATION),
                "Totem did not apply regeneration");
        helper.assertTrue(human.hasEffect(MobEffects.ABSORPTION),
                "Totem did not apply absorption");
        helper.assertTrue(human.getOffhandItem().isEmpty(), "Totem was not consumed");
        human.discard();
        helper.succeed();
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
