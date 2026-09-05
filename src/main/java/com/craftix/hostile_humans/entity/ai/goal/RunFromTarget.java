package com.craftix.hostile_humans.entity.ai.goal;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.ai.squad.SquadManager;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.function.Predicate;

public class RunFromTarget extends Goal {
    protected final Human human;
    protected final float maxDist;
    protected final Predicate<LivingEntity> avoidPredicate;
    protected final Predicate<LivingEntity> predicateOnAvoidEntity;
    private final double walkSpeedModifier;
    private final double sprintSpeedModifier;

    @Nullable
    protected Path path;
    Vec3 targetPos = null;
    boolean jump;

    public RunFromTarget(Human p_25027_, float p_25029_, double p_25030_, double p_25031_) {
        this(p_25027_, (p_25052_) -> {
            return true;
        }, p_25029_, p_25030_, p_25031_, EntitySelector.NO_CREATIVE_OR_SPECTATOR::test);
    }

    public RunFromTarget(Human p_25040_, Predicate<LivingEntity> p_25042_, float p_25043_, double p_25044_, double p_25045_, Predicate<LivingEntity> p_25046_) {
        this.human = p_25040_;
        this.avoidPredicate = p_25042_;
        this.maxDist = p_25043_;
        this.walkSpeedModifier = p_25044_;
        this.sprintSpeedModifier = p_25045_;
        this.predicateOnAvoidEntity = p_25046_;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    public boolean canUse() {
        if (human.getOffhandItem().is(Items.TOTEM_OF_UNDYING))
            return false;

        if (!HumanUtil.isLowHp(human))
            return false;

        if (!human.shouldStartFleeingThisCombat())
            return false;

        if (human.getTarget() != null || human.toAvoid == null || !(human.toAvoid.distanceTo(human) < 15)) {
            human.toAvoid = human.getTarget();
        }

        if (human.toAvoid == null) {
            return false;
        } else {
            return generatePathAwayFromAttacker();
        }
    }

    private boolean generatePathAwayFromAttacker() {

        Vec3 vec3 = null;
        for (int i = 0; i < 10; ++i) {
            vec3 = DefaultRandomPos.getPosAway(this.human, 64, 7, human.toAvoid.position());
            if (vec3 != null) {
                break;
            }
        }
        if (vec3 == null) {
            return false;
        } else if (human.distanceToSqr(vec3.x, vec3.y, vec3.z) < human.toAvoid.distanceToSqr(this.human)) {
            return false;
        } else {
            this.path = this.human.getNavigation().createPath(vec3.x, vec3.y, vec3.z, 0);
            if (path != null) targetPos = vec3;
            return this.path != null;
        }
    }

    public boolean canContinueToUse() {
        if (!HumanUtil.isLowHp(human))
            return false;
        if (human.toAvoid instanceof Player player && (player.isSpectator() || player.isCreative())) {
            return false;
        }
        if (human.toAvoid == null) {
            return false;
        }

        if (this.human.distanceToSqr(human.toAvoid) > 16 * 16) {
            return false;
        }

        if (!human.onGround()) {
            jump = true;
        } else if (jump) {
            jump = false;
            this.human.getNavigation().stop();
            generatePathAwayFromAttacker();
            this.human.getNavigation().moveTo(this.path, this.walkSpeedModifier);
        }

        return !this.human.getNavigation().isDone();
    }

    public void start() {
        LivingEntity attacker = human.toAvoid;
        human.interruptSurvivalMovementForCombat();
        human.isFleeing = true;
        human.setTarget(null);
        SquadManager.alertRetreatingAlly(human, attacker);
        this.human.getNavigation().moveTo(this.path, this.walkSpeedModifier);
    }

    public void stop() {
        //System.out.println("stop flee");
        boolean stillNeedsHealing = HumanUtil.isLowHp(human);
        human.isFleeing = false;
        human.onPlayerJumpCoolDown = 20;
        human.toAvoid = null;
        if (stillNeedsHealing) {
            human.healingAfterFleeTicks = 20 * 10;
            human.setTarget(null);
            human.getNavigation().stop();
        }
    }

    public void tick() {

        if (human.toAvoid == null) return;
        human.interruptSurvivalMovementForCombat();
        //System.out.println("tick flee");
        human.isFleeing = true;
        if (this.human.distanceToSqr(human.toAvoid) < 7 * 7) {
            this.human.getNavigation().setSpeedModifier(this.sprintSpeedModifier);
        } else {
            this.human.getNavigation().setSpeedModifier(this.walkSpeedModifier);
        }
    }
}
