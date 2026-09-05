package com.craftix.hostile_humans.entity.ai.goal;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.entity.HumanEntity;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;
import com.craftix.hostile_humans.entity.ai.control.HumanEntityWalkControl;
import com.craftix.hostile_humans.entity.equipment.MeleeWeaponSelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

import static com.craftix.hostile_humans.entity.HumanMobEntityData.DATA_SIT_POS;

public class MeleeAttackGoal extends HumanGoal {
    private static final long COOLDOWN_BETWEEN_CAN_USE_CHECKS = 5L;
    private static final double PLAYER_ATTACK_REACH_SQR = 9.0D;
    private final double speedModifier;
    private final boolean followingTargetEvenIfNotSeen;
    private final boolean canPenalize = false;
    private Path path;
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;
    private int ticksUntilNextPathRecalculation;
    private int ticksUntilNextAttack;
    private long lastCanUseCheck;
    private int failedPathFindingPenalty = 0;

    public MeleeAttackGoal(HumanEntity humanEntity, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(humanEntity);
        this.speedModifier = speedModifier;
        this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob instanceof Human human && !human.getCombatIntent().allowMeleeAttack()) {
            return false;
        }
        if (this.mob instanceof Human human && human.isSleepingOrLyingDown())
            return false;

        if (mob instanceof Human human && human.isFleeing)
            return false;
        long gameTime = this.mob.level().getGameTime();
        if (gameTime - this.lastCanUseCheck < COOLDOWN_BETWEEN_CAN_USE_CHECKS) {
            return false;
        } else {
            this.lastCanUseCheck = gameTime;
            LivingEntity livingEntity = this.mob.getTarget();
            if (livingEntity == null || !this.mob.canAttack(livingEntity)) {
                return false;
            } else {
                if (!this.hasMeleeSlot(livingEntity)) {
                    return false;
                }
                if (canPenalize) {
                    if (--this.ticksUntilNextPathRecalculation <= 0) {
                        this.path = this.mob.getNavigation().createPath(livingEntity, 0);
                        this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);
                        return this.path != null;
                    } else {
                        return true;
                    }
                }
                this.path = this.mob.getNavigation().createPath(livingEntity, 0);
                if (this.path != null) {
                    return true;
                } else {
                    return this.getAttackReachSqr(livingEntity) >= this.mob.distanceToSqr(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
                }
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (mob instanceof Human human && !human.getCombatIntent().allowMeleeAttack()) {
            return false;
        }
        if (this.mob instanceof Human human && human.isSleepingOrLyingDown())
            return false;

        if (mob instanceof Human human && human.isFleeing)
            return false;
        LivingEntity livingEntity = this.mob.getTarget();
        if (livingEntity == null) {
            return false;
        }
        if (!this.hasMeleeSlot(livingEntity)) {
            return false;
        }
        if (!this.mob.canAttack(livingEntity)) {
            return false;
        } else if (!this.followingTargetEvenIfNotSeen) {
            return !this.mob.getNavigation().isDone();
        } else return true;
    }

    @Override
    public void start() {
        boolean isSit = mob.isOrderedToSit();
        if (!isSit) this.mob.getNavigation().moveTo(this.path, this.speedModifier);
        this.mob.setAggressive(true);
        this.ticksUntilNextPathRecalculation = 0;
        this.ticksUntilNextAttack = 0;
    }

    @Override
    public void stop() {
        this.mob.setAggressive(false);

        boolean isSit = mob.isOrderedToSit();

        if (isSit) {
            mob.setOrderedToPosition(mob.getEntityData().get(DATA_SIT_POS));
        } else {
            mob.getNavigation().stop();
            if (mob.getMoveControl() instanceof HumanEntityWalkControl moveControl) moveControl.stopMovement();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity livingEntity = this.mob.getTarget();
        if (livingEntity != null) {
            if (livingEntity instanceof Player player) {
                ItemStack itemStack = player.getItemInHand(player.getUsedItemHand());

                if (itemStack.is(mob.getTameItem()) || mob.isOwnedBy(player)) {
                    stop();
                    return;
                }
            }
            this.mob.getLookControl().setLookAt(livingEntity, 30.0F, 30.0F);
            double distance = this.mob.distanceToSqr(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
            this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
            if ((this.followingTargetEvenIfNotSeen || this.mob.getSensing().hasLineOfSight(livingEntity)) && this.ticksUntilNextPathRecalculation <= 0 && (this.pathedTargetX == 0.0D && this.pathedTargetY == 0.0D && this.pathedTargetZ == 0.0D || livingEntity.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0D || this.mob.getRandom().nextFloat() < 0.05F)) {
                this.pathedTargetX = livingEntity.getX();
                this.pathedTargetY = livingEntity.getY();
                this.pathedTargetZ = livingEntity.getZ();
                this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);
                if (this.canPenalize) {
                    this.ticksUntilNextPathRecalculation += failedPathFindingPenalty;
                    if (this.mob.getNavigation().getPath() != null) {
                        net.minecraft.world.level.pathfinder.Node finalPathPoint = this.mob.getNavigation().getPath().getEndNode();
                        if (finalPathPoint != null && livingEntity.distanceToSqr(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) < 1)
                            failedPathFindingPenalty = 0;
                        else failedPathFindingPenalty += 10;
                    } else {
                        failedPathFindingPenalty += 10;
                    }
                }
                if (distance > 1024.0D) {
                    this.ticksUntilNextPathRecalculation += 10;
                } else if (distance > 256.0D) {
                    this.ticksUntilNextPathRecalculation += 5;
                }

                if (!this.mob.getNavigation().moveTo(livingEntity, this.speedModifier)) {
                    this.ticksUntilNextPathRecalculation += 15;
                }

                this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
            }

            this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
            this.checkAndPerformAttack(livingEntity, distance);
        }
    }

    protected void checkAndPerformAttack(LivingEntity livingEntity, double attackDistance) {
        double distance = this.getAttackReachSqr(livingEntity);
        if (this.mob instanceof Human human && HumanUtil.isRangedWeapon(human.getMainHandItem())) {
            return;
        }
        boolean preparingBuff = this.mob instanceof Human human && human.isPreparingPreAttackBuff();
        boolean lyingDown = this.mob instanceof Human human && human.isSleepingOrLyingDown();
        double extendedSwingRange = distance * 1.35D;
        boolean attackAllowed = !(this.mob instanceof Human human)
                || human.getCombatIntent().allowMeleeAttack();
        if (lyingDown || preparingBuff || !attackAllowed
                || attackDistance > extendedSwingRange || this.ticksUntilNextAttack > 0) {
            return;
        }

        if (this.mob instanceof Human human && attackDistance > distance) {
            this.resetAttackCooldown();
            human.lastCombatTime = human.tickCount;
            // Pressure outside real reach is observable commitment only; it never deals damage.
            human.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            return;
        }

        if (this.mob instanceof Human human) {
            if (human.criticalAttackArmedUntilTick > 0) {
                if (human.tickCount > human.criticalAttackArmedUntilTick) {
                    human.criticalAttackArmedUntilTick = 0;
                } else if (!human.onGround() && human.getDeltaMovement().y < 0.0D
                        && human.fallDistance > 0.0F) {
                    human.criticalStrikeReady = true;
                    human.criticalAttackArmedUntilTick = 0;
                } else if (human.onGround()
                        && human.tickCount > human.criticalAttackArmedUntilTick - 15) {
                    // The jump was obstructed or already landed; do not stall melee for a full
                    // cooldown waiting for a critical that can no longer happen.
                    human.criticalAttackArmedUntilTick = 0;
                } else {
                    return;
                }
            } else if (shouldAttemptCritical(human, livingEntity)
                    && human.onGround() && human.getDeltaMovement().y <= 0.0D) {
                human.getJumpControl().jump();
                human.criticalAttackArmedUntilTick = human.tickCount + 20;
                return;
            }
            human.lastCombatTime = human.tickCount;
        }

        this.resetAttackCooldown();
        if (mob.isBlocking()) {
            mob.stopUsingItem();
        }
        this.mob.doHurtTarget(livingEntity);
    }

    private boolean shouldAttemptCritical(Human human, LivingEntity target) {
        CombatSkillTier tier = human.getCombatTacticsController().skillTier();
        if (tier.criticalChance() <= 0.0D || target.isBlocking()
                || human.getCombatIntent().incomingProjectile() || human.isUnderMeleePressure()) {
            return false;
        }
        long seed = human.getUUID().getLeastSignificantBits()
                ^ target.getUUID().getMostSignificantBits() ^ human.tickCount / 4L;
        return Math.floorMod(seed, 10000) / 10000.0D < tier.criticalChance();
    }

    protected void resetAttackCooldown() {
        int cooldownMin = Math.min(Config.meleeAttackCooldownMin.get(), Config.meleeAttackCooldownMax.get());
        int cooldownMax = Math.max(Config.meleeAttackCooldownMin.get(), Config.meleeAttackCooldownMax.get());
        if (mob instanceof Human human) {
            CombatSkillTier tier = human.getCombatTacticsController().skillTier();
            cooldownMin = tier.attackCooldownMin(cooldownMin);
            cooldownMax = tier.attackCooldownMax(cooldownMax, cooldownMin);
        }
        int weaponCooldown = weaponCooldownTicks(mob.getMainHandItem());
        cooldownMin = Math.max(cooldownMin, weaponCooldown);
        cooldownMax = Math.max(cooldownMax, weaponCooldown);
        this.ticksUntilNextAttack = this.adjustedTickDelay(mob.getRandom().nextInt(cooldownMin, cooldownMax + 1));
    }

    /** Returns the vanilla attack-strength recovery time for the equipped main-hand item. */
    public static int weaponCooldownTicks(ItemStack stack) {
        double attackSpeed = Attributes.ATTACK_SPEED.getDefaultValue();
        var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_SPEED);
        double multiplyBase = 0.0D;
        double multiplyTotal = 0.0D;
        for (AttributeModifier modifier : modifiers) {
            switch (modifier.getOperation()) {
                case ADDITION -> attackSpeed += modifier.getAmount();
                case MULTIPLY_BASE -> multiplyBase += modifier.getAmount();
                case MULTIPLY_TOTAL -> multiplyTotal += modifier.getAmount();
            }
        }
        attackSpeed *= 1.0D + multiplyBase;
        attackSpeed *= 1.0D + multiplyTotal;
        return (int) Math.ceil(20.0D / Math.max(0.1D, attackSpeed));
    }

    protected double getAttackReachSqr(LivingEntity livingEntity) {
        return PLAYER_ATTACK_REACH_SQR;
    }

    private boolean hasMeleeSlot(LivingEntity target) {
        if (!(this.mob instanceof Human human)) {
            return true;
        }
        if (HumanUtil.isRangedWeapon(human.getMainHandItem())) {
            return false;
        }
        if (!(target instanceof Player)) return true;
        if (!MeleeWeaponSelector.isMeleeCandidate(human.getMainHandItem())
                && !HumanUtil.isTrident(human.getMainHandItem())) return true;

        int maxMelee = Config.maxTargeting.get();
        if (maxMelee <= 0) {
            return false;
        }

        double myDistance = human.distanceToSqr(target);
        var closerMeleeHumans = target.level().getEntities(target, target.getBoundingBox().inflate(15), entity -> {
            if (!(entity instanceof Human otherHuman) || otherHuman == human || otherHuman.getTarget() != target) {
                return false;
            }
            if (otherHuman.isFleeing || otherHuman.healingAfterFleeTicks > 0 || otherHuman.isSleepingOrLyingDown()) {
                return false;
            }
            if (!MeleeWeaponSelector.isMeleeCandidate(otherHuman.getMainHandItem()) && !HumanUtil.isTrident(otherHuman.getMainHandItem())) {
                return false;
            }
            return otherHuman.distanceToSqr(target) <= myDistance;
        });
        return closerMeleeHumans.size() < maxMelee;
    }
}
