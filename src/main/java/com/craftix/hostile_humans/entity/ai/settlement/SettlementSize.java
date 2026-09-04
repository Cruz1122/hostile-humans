package com.craftix.hostile_humans.entity.ai.settlement;

import net.minecraft.core.BlockPos;

/** Size contract shared by worldgen, validation and population. */
public enum SettlementSize {
    SMALL("small", 2, 3, 24, 50),
    MEDIUM("medium", 4, 5, 40, 35),
    LARGE("large", 6, 8, 64, 15);

    private final String id;
    private final int minimumPopulation;
    private final int maximumPopulation;
    private final int maximumFootprint;
    private final int selectionWeight;

    SettlementSize(String id, int minimumPopulation, int maximumPopulation, int maximumFootprint, int selectionWeight) {
        this.id = id;
        this.minimumPopulation = minimumPopulation;
        this.maximumPopulation = maximumPopulation;
        this.maximumFootprint = maximumFootprint;
        this.selectionWeight = selectionWeight;
    }

    public String id() { return id; }
    public int minimumPopulation() { return minimumPopulation; }
    public int maximumPopulation() { return maximumPopulation; }
    public int maximumFootprint() { return maximumFootprint; }
    public int selectionWeight() { return selectionWeight; }

    public static SettlementSize fromBounds(BlockPos min, BlockPos max) {
        int footprint = Math.max(max.getX() - min.getX() + 1, max.getZ() - min.getZ() + 1);
        if (footprint <= SMALL.maximumFootprint) return SMALL;
        if (footprint <= MEDIUM.maximumFootprint) return MEDIUM;
        return LARGE;
    }
}
