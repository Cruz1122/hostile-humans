package com.craftix.hostile_humans.entity.ai.goal;

import java.util.EnumSet;

import javax.annotation.Nullable;

import com.craftix.hostile_humans.entity.entities.Human;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

public class InvestigateSoundGoal extends Goal {
	protected final Mob mob;
	private final double speedModifier;
	private boolean hasInvestigated = false;
	@Nullable
	protected BlockPos pos = BlockPos.ZERO;
	private int calmDown;
	private int searchTicksRemaining;
	private boolean isRunning;

	public InvestigateSoundGoal(Mob pMob, double pSpeedModifier) {
		this.mob = pMob;
		this.speedModifier = pSpeedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	/**
	 * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
	 * method as well.
	 */
	public boolean canUse() {
		if (this.mob.isSleeping()) return false;
		if (this.mob.getTarget() != null) return false;
		if (this.calmDown > 0) {
			--this.calmDown;
			return false;
		} else {
			if (this.pos == BlockPos.ZERO && this.mob instanceof Human investigator) {
				this.pos = investigator.investigateSound();
			}
			if (this.pos == BlockPos.ZERO) return false;
			return this.mob.blockPosition().distSqr(this.pos) < 1000D;
		}
	}

	/**
	 * Returns whether an in-progress EntityAIBase should continue executing
	 */
	public boolean canContinueToUse() {
		if (this.mob.isSleeping() || this.mob.getTarget() != null) return false;
		return !this.hasInvestigated || this.searchTicksRemaining > 0;
	}

	/**
	 * Execute a one shot task or start executing a continuous task
	 */
	public void start() {
		if (this.mob instanceof Human investigator) {
			this.pos = investigator.investigateSound();
			if (this.mob.level().getBlockState(pos).isAir()) {
				this.pos = pos.below();
			}
		}
		this.hasInvestigated = false;
		this.searchTicksRemaining = reducedTickDelay(60);
	}

	/**
	 * Reset the task's internal state. Called when this task is interrupted by another one
	 */
	public void stop() {
		this.pos = BlockPos.ZERO;
		if (this.mob instanceof Human investigator) {
			investigator.setInvestigateSound(BlockPos.ZERO);;
		}
		this.mob.getNavigation().stop();
		if (this.hasInvestigated) {
			this.calmDown = reducedTickDelay(100);
		}
		this.isRunning = false;
	}

	/**
	 * Keep ticking a continuous task that has already been started
	 */
	public void tick() {
		if (this.mob.blockPosition().distSqr(this.pos) < 5D) {
			this.mob.getNavigation().stop();
			this.hasInvestigated = true;
			if (--this.searchTicksRemaining <= 0 && this.mob instanceof Human investigator) {
				investigator.setInvestigateSound(BlockPos.ZERO);
			}
		} else {
			if (this.mob.tickCount % 10 == 0 || this.mob.getNavigation().isDone()) {
				this.mob.getNavigation().moveTo(this.pos.getX(), this.pos.getY(), this.pos.getZ(), this.speedModifier);
			}
		}
	}

	/**
	 * @see #isRunning
	 */
	public boolean isRunning() {
		return this.isRunning;
	}
}
