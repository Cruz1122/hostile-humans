package com.craftix.hostile_humans.entity.ai.survival;

/** Maps resource needs to the objective represented by a survival intent. */
public final class SurvivalPlanner {
    private SurvivalPlanner() {}

    public static SurvivalObjective objectiveFor(SquadNeed need) {
        return switch (need) {
            case FOOD, APPLES -> SurvivalObjective.FOOD;
            case WOOD -> SurvivalObjective.WOOD_BOOTSTRAP;
            case STONE -> SurvivalObjective.STONE_TOOLS;
            case FUEL -> SurvivalObjective.FUEL_RESERVE;
            case IRON -> SurvivalObjective.IRON_GEAR;
            case GOLD, DIAMOND -> SurvivalObjective.DIAMOND_GEAR;
        };
    }

}
