package com.craftix.hostile_humans.entity.ai.goal;

import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.ai.combat.ShieldState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.item.CrossbowItem;
import net.minecraftforge.common.ToolActions;

public class RaiseShieldGoal extends Goal {

    public final Human human;

    public RaiseShieldGoal(Human guard) {
        this.human = guard;
    }

    @Override
    public boolean canUse() {
        if (Config.enableShieldTactics.get()) {
            ShieldState state = human.getCombatIntent().shieldState();
            return (state == ShieldState.RAISING || state == ShieldState.BLOCKING)
                    && human.shieldDisabledUntilTick <= human.tickCount
                    && human.getOffhandItem().canPerformAction(ToolActions.SHIELD_BLOCK);
        }
        if (!human.getOffhandItem().getItem().canPerformAction(human.getOffhandItem(), ToolActions.SHIELD_BLOCK)
                || human.shieldCoolDown > 0
                || HumanUtil.isRangedWeapon(human.getMainHandItem())) {
            return false;
        }
        if (human.isBlocking()) return human.shieldUpTicks > 0;
    	
        boolean animalAnger = true;
        if (human.getTarget() instanceof Animal animal) {
            if (animal.getTarget() != human) {
                animalAnger = false;
            }
        }

        return !CrossbowItem.isCharged(human.getMainHandItem()) && raiseShield() && animalAnger;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        if (human.getOffhandItem().getItem().canPerformAction(human.getOffhandItem(), net.minecraftforge.common.ToolActions.SHIELD_BLOCK)) {
            if (human.shieldUpTicks <= 0) human.shieldUpTicks = 20;
            if (!human.isUsingItem()) human.startUsingItem(InteractionHand.OFF_HAND);
        }
    }

    @Override
    public void stop() {
        if (human.isUsingItem() && human.getUsedItemHand() == InteractionHand.OFF_HAND
                && human.getUseItem().canPerformAction(ToolActions.SHIELD_BLOCK)) {
            human.stopUsingItem();
        }
        if (human.shieldCoolDown == 0) human.shieldCoolDown = 6;
    }

    protected boolean raiseShield() {
        LivingEntity target = human.getTarget();
        if (target != null && human.shieldCoolDown == 0) {
            boolean isRanged = HumanUtil.isRangedWeapon(human.getMainHandItem());

            return human.distanceTo(target) <= 4.0D || target instanceof Creeper || target instanceof RangedAttackMob && target.distanceTo(human) >= 5.0D && !isRanged || target instanceof Ravager;
        }
        return false;
    }
}
