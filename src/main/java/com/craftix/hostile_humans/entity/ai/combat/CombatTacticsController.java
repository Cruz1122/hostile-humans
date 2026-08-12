package com.craftix.hostile_humans.entity.ai.combat;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolActions;

/** Server-only tactical policy. It consumes only currently observable combat signals. */
public final class CombatTacticsController {
    private static final double MELEE_THREAT_DISTANCE = 4.5D;

    private final Human human;

    private ShieldState shieldState = ShieldState.UNAVAILABLE;
    private int reactionUntil = -1;
    private int minimumBlockUntil;
    private int blockDeadline;
    private int reblockAllowedAt;
    private int counterAttackUntil;
    private int comboEscapeUntil;
    private int nextProjectileScan;
    private CombatAction comboEscapeAction = CombatAction.STRAFE_LEFT;
    private boolean projectileThreat;
    private boolean visibleDisabler;
    private boolean targetBlocking;
    private CombatIntent intent = CombatIntent.idle(ShieldState.UNAVAILABLE);

    public CombatTacticsController(Human human) {
        this.human = human;
    }

    public CombatIntent evaluate() {
        if (human.level().isClientSide || !Config.enableShieldTactics.get()) {
            return intent = CombatIntent.idle(currentShieldState());
        }

        LivingEntity target = human.getTarget();
        boolean shieldAvailable = human.getOffhandItem().canPerformAction(ToolActions.SHIELD_BLOCK);

        if (human.isFleeing || human.healingAfterFleeTicks > 0) {
            lowerShield(false);
            return intent = new CombatIntent(CombatTactic.DISENGAGE, CombatAction.HOLD_POSITION,
                    shieldState, false, false, false, false, human.tickCount + 2);
        }

        if (human.tickCount >= nextProjectileScan) {
            projectileThreat = Config.enableProjectileBlocking.get() && hasIncomingProjectile();
            nextProjectileScan = human.tickCount + 2 + Math.floorMod(human.getId(), 3);
        }

        boolean visible = target != null && human.hasLineOfSight(target);
        double targetDistance = visible ? human.distanceTo(target) : Double.MAX_VALUE;
        targetBlocking = visible && target.isBlocking();
        visibleDisabler = visible && targetHoldingShieldDisabler(target);
        boolean comboPressure = visible && human.isUnderMeleePressure()
                && human.consecutiveReceivedCombatHits >= 2;
        boolean closeMeleeThreat = visible && targetDistance <= MELEE_THREAT_DISTANCE;
        boolean meleePressure = closeMeleeThreat
                && (human.isUnderMeleePressure() || target.swinging || comboPressure);

        if (!shieldAvailable) {
            shieldState = ShieldState.UNAVAILABLE;
            if (comboPressure) return comboEscapeIntent(false);
            return intent = new CombatIntent(CombatTactic.PRESSURE, CombatAction.ATTACK,
                    shieldState, true, projectileThreat, visibleDisabler, targetBlocking, human.tickCount + 1);
        }

        if (human.shieldDisabledUntilTick > human.tickCount) {
            shieldState = ShieldState.DISABLED;
            lowerShield(false);
            if (comboPressure) return comboEscapeIntent(false);
            return intent = new CombatIntent(CombatTactic.PRESSURE, CombatAction.ATTACK,
                    shieldState, true, projectileThreat, visibleDisabler, targetBlocking,
                    human.shieldDisabledUntilTick);
        }

        if (shieldState == ShieldState.UNAVAILABLE || shieldState == ShieldState.DISABLED) {
            shieldState = ShieldState.READY;
        }

        if (targetBlocking && !human.isUsingItem() && Config.enableShieldBreaking.get()
                && hasShieldDisablerInInventory(target) && targetDistance <= 4.0D) {
            return intent = new CombatIntent(CombatTactic.BREAK_SHIELD,
                    CombatAction.SWITCH_TO_SHIELD_DISABLER, shieldState, false,
                    projectileThreat, false, true, human.tickCount + 3);
        }

        if (human.isUsingItem() && human.getUsedItemHand() == InteractionHand.OFF_HAND
                && human.getUseItem().canPerformAction(ToolActions.SHIELD_BLOCK)) {
            if (!human.isBlocking()) {
                shieldState = ShieldState.RAISING;
                return intent = new CombatIntent(CombatTactic.DEFEND, CombatAction.RAISE_SHIELD,
                        shieldState, false, projectileThreat, visibleDisabler, targetBlocking,
                        Math.max(minimumBlockUntil, human.tickCount + 1));
            }

            shieldState = ShieldState.BLOCKING;
            boolean maximumReached = human.tickCount >= blockDeadline;
            boolean passiveWindowEnded = !projectileThreat && !meleePressure
                    && human.tickCount >= minimumBlockUntil;

            if (maximumReached || passiveWindowEnded) {
                lowerShield(true);
                return intent = new CombatIntent(CombatTactic.PUNISH,
                        CombatAction.ATTACK, shieldState, true, projectileThreat, visibleDisabler,
                        targetBlocking, counterAttackUntil);
            }

            return intent = new CombatIntent(CombatTactic.DEFEND, CombatAction.HOLD_POSITION, shieldState,
                    false, projectileThreat, visibleDisabler, targetBlocking, blockDeadline);
        }

        if (human.tickCount < counterAttackUntil) {
            return intent = new CombatIntent(CombatTactic.PUNISH, CombatAction.ATTACK,
                    ShieldState.LOWERING_TO_ATTACK, true, projectileThreat, visibleDisabler,
                    targetBlocking, counterAttackUntil);
        }

        if (comboPressure && human.tickCount < reblockAllowedAt) {
            return comboEscapeIntent(true);
        }

        if (human.tickCount < reblockAllowedAt) {
            return intent = new CombatIntent(CombatTactic.PRESSURE,
                    comboPressure ? chooseLateralMovement() : CombatAction.ATTACK,
                    ShieldState.RECOVERING, true, projectileThreat, visibleDisabler,
                    targetBlocking, reblockAllowedAt);
        }

        boolean threat = projectileThreat || meleePressure || comboPressure || closeMeleeThreat;

        if (!threat) {
            reactionUntil = -1;
            return intent = new CombatIntent(CombatTactic.PRESSURE, CombatAction.APPROACH,
                    shieldState, true, projectileThreat, visibleDisabler, targetBlocking,
                    human.tickCount + 1);
        }

        if (reactionUntil < 0) {
            reactionUntil = human.tickCount + skillTier().reactionTicks(stableDecisionRoll());
        }

        if (human.tickCount >= reactionUntil && !human.isUsingItem()) {
            startShieldUse();
            return intent = new CombatIntent(CombatTactic.DEFEND, CombatAction.RAISE_SHIELD,
                    shieldState, false, projectileThreat, visibleDisabler, targetBlocking,
                    minimumBlockUntil);
        }

        return intent = new CombatIntent(CombatTactic.PRESSURE,
                comboPressure ? chooseLateralMovement() : CombatAction.APPROACH,
                shieldState, true, projectileThreat, visibleDisabler, targetBlocking,
                reactionUntil);
    }

    private void startShieldUse() {
        shieldState = ShieldState.RAISING;
        int vanillaRaiseDelay = 5;
        minimumBlockUntil = human.tickCount + vanillaRaiseDelay + skillTier().minimumBlockTicks();
        blockDeadline = human.tickCount + vanillaRaiseDelay + skillTier().maxBlockTicks();
        reactionUntil = -1;
        human.shieldUpTicks = skillTier().maxBlockTicks();
        human.startUsingItem(InteractionHand.OFF_HAND);
    }

    private void lowerShield(boolean openCounterWindow) {
        if (human.isUsingItem() && human.getUsedItemHand() == InteractionHand.OFF_HAND
                && human.getUseItem().canPerformAction(ToolActions.SHIELD_BLOCK)) {
            human.stopUsingItem();
        }
        reactionUntil = -1;
        shieldState = human.shieldDisabledUntilTick > human.tickCount
                ? ShieldState.DISABLED : ShieldState.RECOVERING;
        if (openCounterWindow) {
            counterAttackUntil = human.tickCount + Math.max(5, skillTier().reblockDelay());
        }
        reblockAllowedAt = human.shieldDisabledUntilTick > human.tickCount
                ? human.shieldDisabledUntilTick
                : Math.max(counterAttackUntil, human.tickCount + skillTier().reblockDelay());
    }

    private CombatIntent comboEscapeIntent(boolean shieldUsable) {
        if (human.tickCount >= comboEscapeUntil) {
            comboEscapeAction = chooseLateralMovement();
            comboEscapeUntil = human.tickCount + skillTier().comboEscapeTicks();
        } else if (human.tickCount - human.lastReceivedCombatHitTick <= 2) {
            comboEscapeUntil = Math.max(comboEscapeUntil,
                    human.tickCount + skillTier().comboEscapeTicks() / 2);
        }
        return intent = new CombatIntent(CombatTactic.DISENGAGE, comboEscapeAction,
                shieldUsable ? currentShieldState() : ShieldState.UNAVAILABLE,
                true, projectileThreat, visibleDisabler, targetBlocking, comboEscapeUntil);
    }

    private CombatAction chooseLateralMovement() {
        return (stableDecisionRoll() & 1) == 0
                ? CombatAction.STRAFE_LEFT : CombatAction.STRAFE_RIGHT;
    }

    private int stableDecisionRoll() {
        LivingEntity target = human.getTarget();
        int targetHash = target == null ? 0 : target.getUUID().hashCode();
        return human.getUUID().hashCode() ^ targetHash ^ human.tickCount / 8;
    }

    private ShieldState currentShieldState() {
        if (!human.getOffhandItem().canPerformAction(ToolActions.SHIELD_BLOCK)) {
            return ShieldState.UNAVAILABLE;
        }
        if (human.shieldDisabledUntilTick > human.tickCount) return ShieldState.DISABLED;
        if (human.isBlocking()) return ShieldState.BLOCKING;
        if (human.isUsingItem() && human.getUsedItemHand() == InteractionHand.OFF_HAND) {
            return ShieldState.RAISING;
        }
        return ShieldState.READY;
    }

    private boolean hasIncomingProjectile() {
        for (Projectile projectile : human.level().getEntitiesOfClass(
                Projectile.class, human.getBoundingBox().inflate(10.0D))) {
            if (projectile.getOwner() == human || !human.hasLineOfSight(projectile)) continue;
            double distance = projectile.distanceTo(human);
            if (distance > 10.0D || projectile.getDeltaMovement().lengthSqr() < 0.0001D) continue;
            var toHuman = human.position().add(0, human.getEyeHeight() * 0.5D, 0)
                    .subtract(projectile.position()).normalize();
            if (projectile.getDeltaMovement().normalize().dot(toHuman) > 0.65D) return true;
        }
        return false;
    }

    private boolean targetHoldingShieldDisabler(LivingEntity target) {
        ItemStack shield = human.getOffhandItem();
        ItemStack weapon = target.getMainHandItem();
        return !weapon.isEmpty() && weapon.canDisableShield(shield, human, target);
    }

    private boolean hasShieldDisablerInInventory(LivingEntity target) {
        ItemStack targetShield = target.getUseItem();
        if (!human.getMainHandItem().isEmpty()
                && human.getMainHandItem().canDisableShield(targetShield, target, human)) {
            return true;
        }
        if (human.getData() == null) return false;
        for (ItemStack stack : human.getData().getInventoryItems()) {
            if (!stack.isEmpty() && stack.canDisableShield(targetShield, target, human)) return true;
        }
        return false;
    }

    public CombatIntent intent() { return intent; }
    public ShieldState shieldState() { return shieldState; }
    public CombatSkillTier skillTier() {
        if (human.getPersonaDefinition().isPresent()) {
            return human.getPersonaDefinition().get().combatSkillTier();
        }
        return human.getCombatSkillTierOverride() == null
                ? CombatSkillTier.forHuman(human.getTier(), human.getUUID().hashCode())
                : human.getCombatSkillTierOverride();
    }
    public boolean visibleDisabler() { return visibleDisabler; }
    public boolean targetBlocking() { return targetBlocking; }
}
