package com.craftix.hostile_humans.entity.ai.goal;

import com.craftix.hostile_humans.entity.HumanEntity;
import com.craftix.hostile_humans.entity.ai.control.HumanEntityWalkControl;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

import static com.craftix.hostile_humans.entity.HumanMobEntityData.DATA_SIT_POS;
import static com.craftix.hostile_humans.entity.ai.goal.CrossbowGoal.CrossbowState.*;

public class CrossbowGoal<T extends HumanEntity & RangedAttackMob & CrossbowAttackMob> extends Goal {
    public static final UniformInt PATHFINDING_DELAY_RANGE = TimeUtil.rangeOfSeconds(1, 2);
    private final T mob;
    private final double speedModifier;
    private final float attackRadiusSqr;
    private CrossbowState crossbowState = UNCHARGED;
    private int seeTime;
    private int attackDelay;
    private int updatePathDelay;

    public CrossbowGoal(T p_25814_, double p_25815_, float p_25816_) {
        this.mob = p_25814_;
        this.speedModifier = p_25815_;
        this.attackRadiusSqr = p_25816_ * p_25816_;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        if (mob instanceof Human human && human.isFleeing)
            return false;
        if (mob instanceof Human human && human.isSleepingOrLyingDown())
            return false;
        return this.isValidTarget() && this.isHoldingCrossbow()
                && (!(mob instanceof Human human) || human.hasProjectileForWeapon(mob.getMainHandItem()));
    }

    private boolean isHoldingCrossbow() {
        return this.mob.isHolding(is -> is.getItem() instanceof CrossbowItem);
    }

    public boolean canContinueToUse() {
        if (mob instanceof Human human && human.isFleeing)
            return false;
        if (mob instanceof Human human && human.isSleepingOrLyingDown())
            return false;
        return this.isValidTarget() && (this.canUse() || !this.mob.getNavigation().isDone()) && this.isHoldingCrossbow()
                && (!(mob instanceof Human human) || human.hasProjectileForWeapon(mob.getMainHandItem()));
    }

    private boolean isValidTarget() {
        return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
    }

    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.seeTime = 0;
        if (this.mob.isUsingItem()) {
            ItemStack weapon = this.mob.getUseItem();
            this.mob.stopUsingItem();
            this.mob.setChargingCrossbow(false);
            if (weapon.getItem() instanceof CrossbowItem) {
                CrossbowItem.setCharged(weapon, false);
            }
        }
        this.crossbowState = UNCHARGED;

        boolean isSit = mob.isOrderedToSit();
        if (isSit) {
            mob.setOrderedToPosition(mob.getEntityData().get(DATA_SIT_POS));
        } else {
            mob.getNavigation().stop();
            if (mob.getMoveControl() instanceof HumanEntityWalkControl moveControl) moveControl.stopMovement();
        }
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        if (this.mob instanceof Human human && human.isSleepingOrLyingDown()) {
            this.mob.stopUsingItem();
            this.mob.setChargingCrossbow(false);
            this.crossbowState = UNCHARGED;
            return;
        }
        LivingEntity livingentity = this.mob.getTarget();
        if (livingentity != null) {
            if (this.mob instanceof Human human && human.handleRangedMeleeFallback(livingentity)) {
                this.crossbowState = UNCHARGED;
                this.attackDelay = 0;
                this.mob.setChargingCrossbow(false);
                return;
            }
            boolean flag = this.mob.getSensing().hasLineOfSight(livingentity);
            boolean flag1 = this.seeTime > 0;
            if (flag != flag1) {
                this.seeTime = 0;
            }

            if (flag) {
                ++this.seeTime;
            } else {
                --this.seeTime;
            }

            double d0 = this.mob.distanceToSqr(livingentity);
            boolean flag2 = (d0 > (double) this.attackRadiusSqr || this.seeTime < 5) && this.attackDelay == 0;
            if (flag2) {
                --this.updatePathDelay;
                if (this.updatePathDelay <= 0) {

                    this.mob.getNavigation().moveTo(livingentity, this.canRun() ? this.speedModifier : this.speedModifier * 0.5D);
                    this.updatePathDelay = PATHFINDING_DELAY_RANGE.sample(this.mob.getRandom());
                }
            } else {
                this.updatePathDelay = 0;
                this.mob.getNavigation().stop();
            }

            this.mob.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
            if (this.crossbowState == UNCHARGED) {
                if (!flag2) {
                    this.mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.mob, item -> item instanceof CrossbowItem));
                    this.crossbowState = CHARGING;
                    this.mob.setChargingCrossbow(true);
                }
            } else if (this.crossbowState == CHARGING) {
                if (!this.mob.isUsingItem() || !(this.mob.getUseItem().getItem() instanceof CrossbowItem)) {
                    this.crossbowState = UNCHARGED;
                    this.mob.setChargingCrossbow(false);
                    return;
                }

                int i = this.mob.getTicksUsingItem();
                ItemStack itemstack = this.mob.getUseItem();
                if (i >= CrossbowItem.getChargeDuration(itemstack)) {
                    this.mob.releaseUsingItem();
                    this.crossbowState = CHARGED;
                    this.attackDelay = 20 + this.mob.getRandom().nextInt(20);
                    this.mob.setChargingCrossbow(false);

                    if (this.mob instanceof Human human) {
                        human.lastCombatTime = human.tickCount;
                    }
                }
            } else if (this.crossbowState == CHARGED) {
                --this.attackDelay;
                if (this.attackDelay == 0) {
                    this.crossbowState = READY_TO_ATTACK;
                }
            } else if (this.crossbowState == CrossbowState.READY_TO_ATTACK && flag) {

                this.mob.performRangedAttack(livingentity, 1.0F);
                ItemStack itemstack1 = this.mob.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this.mob, item -> item instanceof CrossbowItem));
                CrossbowItem.setCharged(itemstack1, false);
                this.crossbowState = UNCHARGED;
            }
        }
    }

    private boolean canRun() {
        return this.crossbowState == UNCHARGED;
    }

    enum CrossbowState {
        UNCHARGED,
        CHARGING,
        CHARGED,
        READY_TO_ATTACK
    }
}
