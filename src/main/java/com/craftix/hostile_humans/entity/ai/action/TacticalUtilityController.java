package com.craftix.hostile_humans.entity.ai.action;

import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.entity.LivingEntity;

/** Coordinates high-priority utility actions without competing with combat goals. */
public final class TacticalUtilityController {
    private final Human human;

    public TacticalUtilityController(Human human) {
        this.human = human;
    }

    public void tick() {
        if (human.level().isClientSide || !human.isAlive()) return;
        if (WaterBucketAction.tryExtinguish(human)) return;
        if (WaterBucketAction.tryRecover(human)) return;
        if (!Config.enableEnderPearlTactics.get()) return;

        LivingEntity target = human.getTarget();
        boolean defensive = human.isFleeing && human.toAvoid != null
                || target != null && HumanUtil.isLowHp(human) && human.distanceTo(target) <= 10.0D;
        if (defensive && EnderPearlAction.tryThrowDefensively(human)) return;
        if (!defensive) EnderPearlAction.tryThrowOffensively(human);
    }
}
