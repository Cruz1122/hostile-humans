package com.craftix.hostile_humans.entity.ai.combat;

import com.craftix.hostile_humans.entity.entities.HumanTier;

/** Competitive-style skill tier. T1 is strongest; T5 is weakest. */
public enum CombatSkillTier {
    T1(0, 2, 12, 24, 3, 14, 0.40D, 0.50D),
    T2(1, 3, 14, 26, 4, 14, 0.45D, 0.30D),
    T3(2, 5, 16, 28, 5, 16, 0.50D, 0.12D),
    T4(4, 7, 18, 30, 7, 16, 0.55D, 0.04D),
    T5(6, 10, 20, 34, 9, 18, 0.60D, 0.0D);

    private final int reactionMin;
    private final int reactionMax;
    private final int minimumBlockTicks;
    private final int maxBlockTicks;
    private final int reblockDelay;
    private final int comboEscapeTicks;
    private final double attackCooldownMultiplier;
    private final double criticalChance;

    CombatSkillTier(int reactionMin, int reactionMax, int minimumBlockTicks, int maxBlockTicks,
                    int reblockDelay, int comboEscapeTicks, double attackCooldownMultiplier,
                    double criticalChance) {
        this.reactionMin = reactionMin;
        this.reactionMax = reactionMax;
        this.minimumBlockTicks = minimumBlockTicks;
        this.maxBlockTicks = maxBlockTicks;
        this.reblockDelay = reblockDelay;
        this.comboEscapeTicks = comboEscapeTicks;
        this.attackCooldownMultiplier = attackCooldownMultiplier;
        this.criticalChance = criticalChance;
    }

    public int reactionTicks(int stableRoll) {
        return reactionMin + Math.floorMod(stableRoll, reactionMax - reactionMin + 1);
    }

    public int minimumBlockTicks() { return minimumBlockTicks; }
    public int maxBlockTicks() { return maxBlockTicks; }
    public int reblockDelay() { return reblockDelay; }
    public int comboEscapeTicks() { return comboEscapeTicks; }
    public int attackCooldownMin(int configuredMin) {
        return Math.max(2, (int) Math.floor(configuredMin * attackCooldownMultiplier));
    }
    public int attackCooldownMax(int configuredMax, int cooldownMin) {
        return Math.max(cooldownMin,
                (int) Math.ceil(configuredMax * attackCooldownMultiplier));
    }
    public double criticalChance() { return criticalChance; }

    public static CombatSkillTier forHuman(HumanTier tier, int stableRoll) {
        int base = switch (tier) {
            case LEVEL2 -> 1;
            case ROAMER -> 2;
            default -> 3;
        };
        int variation = Math.floorMod(stableRoll, 3) - 1;
        int index = Math.max(0, Math.min(values().length - 1, base + variation));
        return values()[index];
    }
}
