package com.craftix.hostile_humans.entity.ai.control;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.ai.combat.CombatAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HumanEntityWalkControl extends MoveControl {
    private final Human human;

    public HumanEntityWalkControl(Mob mob) {
        super(mob);
        this.human = (Human) mob;
    }

    /** Clears a one-shot movement request before another goal can own movement. */
    public void stopMovement() {
        this.operation = MoveControl.Operation.WAIT;
        this.wantedX = this.mob.getX();
        this.wantedY = this.mob.getY();
        this.wantedZ = this.mob.getZ();
        this.speedModifier = 0.0D;
        this.strafeForwards = 0.0F;
        this.strafeRight = 0.0F;
        this.mob.setZza(0.0F);
        this.mob.setXxa(0.0F);
    }

    @Override
    public void tick() {
        if (!this.mob.level().isClientSide) prepareCombatMovement();
        if (!this.mob.onGround() && this.operation == MoveControl.Operation.MOVE_TO) {
            this.operation = MoveControl.Operation.JUMPING;
        }

        if (this.mob.onGround()) {
            this.updateSprintState();
        }
        float f9;
        if (this.operation == MoveControl.Operation.STRAFE) {
            float f = (float) this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
            float f1 = (float) this.speedModifier * f;
            float f2 = this.strafeForwards;
            float f3 = this.strafeRight;
            float f4 = Mth.sqrt(f2 * f2 + f3 * f3);
            if (f4 < 1.0F) {
                f4 = 1.0F;
            }

            f4 = f1 / f4;
            f2 *= f4;
            f3 *= f4;
            float f5 = Mth.sin(this.mob.getYRot() * 0.017453292F);
            float f6 = Mth.cos(this.mob.getYRot() * 0.017453292F);
            float f7 = f2 * f6 - f3 * f5;
            f9 = f3 * f6 + f2 * f5;
            if (!this.isWalkable(f7, f9)) {
                this.strafeForwards = 1.0F;
                this.strafeRight = 0.0F;
            }

            this.mob.setSpeed(f1);
            this.mob.setZza(this.strafeForwards);
            this.mob.setXxa(this.strafeRight);
            this.operation = MoveControl.Operation.WAIT;
        } else if (this.operation == MoveControl.Operation.MOVE_TO) {
            this.operation = MoveControl.Operation.WAIT;
            double dx = this.wantedX - this.mob.getX();
            double dz = this.wantedZ - this.mob.getZ();
            double dy = this.wantedY - this.mob.getY();
            double distSqr = dx * dx + dy * dy + dz * dz;
            if (distSqr < 2.500000277905201E-7) {
                this.mob.setZza(0.0F);
                return;
            }

            f9 = (float) (Mth.atan2(dz, dx) * 57.2957763671875D) - 90.0F;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), f9, 30.0F));
            this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));

            LivingEntity target = this.human.getTarget();
            if (this.human.shouldUseWaterMovement()) {
                double verticalSpeed = Mth.clamp(dy * 0.1D, -0.08D, 0.08D);
                Vec3 movement = this.mob.getDeltaMovement();
                this.mob.setDeltaMovement(movement.x, verticalSpeed, movement.z);
                return;
            }

            if (this.tryRunJump(target)) {
                return;
            }

            BlockPos blockpos = this.mob.blockPosition();
            BlockState blockstate = this.mob.level().getBlockState(blockpos);
            VoxelShape voxelshape = blockstate.getCollisionShape(this.mob.level(), blockpos);
            double horizontalDistSqr = dx * dx + dz * dz;
            if (dy > (double) this.mob.getStepHeight() && horizontalDistSqr < (double) Math.max(1.0F, this.mob.getBbWidth())
                    || !voxelshape.isEmpty() && this.mob.getY() < voxelshape.max(Direction.Axis.Y) + (double) blockpos.getY()
                    && !blockstate.is(BlockTags.DOORS)
                    && !blockstate.is(BlockTags.FENCES)) {
                this.mob.getJumpControl().jump();
                this.operation = MoveControl.Operation.JUMPING;
            }

            if (this.operation != MoveControl.Operation.JUMPING && Config.attackJump.get()) {
                if (target != null
                        && this.mob.onGround()
                        && !this.mob.isSprinting()
                        && this.human.onPlayerJumpCoolDown <= 0
                        && target.distanceTo(this.human) < 1.35F
                        && HumanUtil.isMeleeWeapon(this.human.getMainHandItem())
                        && this.human.getRandom().nextFloat() < 0.01F) {
                    this.mob.getJumpControl().jump();
                    this.operation = MoveControl.Operation.JUMPING;

                    double x = target.getX() - this.mob.getX();
                    double z = target.getZ() - this.mob.getZ();
                    double len = Math.sqrt(x * x + z * z);
                    if (len > 1.0E-4D) {
                        double boostX = x / len * 0.12D;
                        double boostZ = z / len * 0.12D;
                        this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(boostX, 0.0D, boostZ));
                    }

                    this.human.onPlayerJumpCoolDown = 80;
                }
            }
        } else if (this.operation == MoveControl.Operation.JUMPING) {
            this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            if (this.mob.onGround()) {
                this.operation = MoveControl.Operation.WAIT;
            }
        } else {
            this.mob.setZza(0.0F);
            this.mob.setXxa(0.0F);
        }
    }

    private void prepareCombatMovement() {
        if (human.isFleeing || human.toAvoid != null || human.healingAfterFleeTicks > 0) return;
        CombatAction action = human.getCombatIntent().action();
        if (action != CombatAction.STRAFE_LEFT && action != CombatAction.STRAFE_RIGHT
                && action != CombatAction.BACKSTEP) {
            return;
        }
        LivingEntity target = human.getTarget();
        if (target == null || !human.hasLineOfSight(target)) return;
        human.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (action == CombatAction.BACKSTEP) {
            this.strafeForwards = -0.85F;
            this.strafeRight = 0.0F;
        } else if (human.getCombatIntent().tactic() == com.craftix.hostile_humans.entity.ai.combat.CombatTactic.DISENGAGE) {
            this.strafeForwards = -0.85F;
            this.strafeRight = action == CombatAction.STRAFE_LEFT ? 0.35F : -0.35F;
        } else {
            this.strafeForwards = human.getCombatIntent().allowMeleeAttack() ? 0.55F : -0.45F;
            this.strafeRight = action == CombatAction.STRAFE_LEFT ? 0.35F : -0.35F;
        }
        this.speedModifier = human.getCombatIntent().tactic() == com.craftix.hostile_humans.entity.ai.combat.CombatTactic.DISENGAGE
                ? 1.15D : 0.85D;
        this.operation = MoveControl.Operation.STRAFE;
    }

    private boolean tryRunJump(LivingEntity target) {
        boolean fleeing = this.human.isFleeing;
        if (!Config.runJump.get() || this.human.healingAfterFleeTicks > 0
                || !this.mob.onGround()
                || !this.mob.isSprinting()
                || this.human.onPlayerJumpCoolDown > 0) {
            return false;
        }
        if (!fleeing && (target == null
                || target.distanceTo(this.human) < 7.0F
                || target.distanceTo(this.human) > 14.0F
                || Math.abs(target.getY() - this.human.getY()) > 2.5D
                || this.human.isHolding(HumanUtil::isRangedWeapon)
                || HumanUtil.isTrident(this.human.getMainHandItem())
                || !this.human.getSensing().hasLineOfSight(target))) {
            return false;
        }

        double dx = (target == null ? this.wantedX : target.getX()) - this.mob.getX();
        double dz = (target == null ? this.wantedZ : target.getZ()) - this.mob.getZ();
        double horizontalLength = Math.sqrt(dx * dx + dz * dz);
        if (horizontalLength < 1.0E-4D) {
            return false;
        }

        float targetYaw = (float) (Mth.atan2(dz, dx) * 57.2957763671875D) - 90.0F;
        this.mob.setYRot(this.rotlerp(this.mob.getYRot(), targetYaw, 30.0F));
        this.mob.yBodyRot = this.mob.getYRot();
        this.mob.getJumpControl().jump();
        this.operation = MoveControl.Operation.JUMPING;

        Vec3 current = this.mob.getDeltaMovement();
        double desiredSpeed = Math.max(0.22D, Math.min(0.28D, current.horizontalDistance() + 0.02D));
        this.mob.setDeltaMovement(dx / horizontalLength * desiredSpeed, current.y, dz / horizontalLength * desiredSpeed);
        this.human.onPlayerJumpCoolDown = 40;
        return true;
    }

    private void updateSprintState() {
        LivingEntity target = this.human.getTarget();
        boolean fleeing = this.human.isFleeing;
        boolean shouldSprint = Config.runJump.get()
                && this.human.healingAfterFleeTicks <= 0
                && !this.human.isUsingItem()
                && !this.human.getNavigation().isDone()
                && (fleeing || (target != null
                && !this.human.isHolding(HumanUtil::isRangedWeapon)
                && !HumanUtil.isTrident(this.human.getMainHandItem())
                && this.human.distanceTo(target) >= 7.0F));
        this.human.setSprinting(shouldSprint);
    }
}
