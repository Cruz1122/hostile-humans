package com.craftix.hostile_humans.progression;

/** Immutable progression view consumed by natural loadout rolls. */
public record WorldGearProgressionSnapshot(boolean ironUnlocked, boolean goldUnlocked,
                                           boolean diamondUnlocked, boolean netheriteUnlocked,
                                           boolean endVisited) {
    public static WorldGearProgressionSnapshot from(WorldGearProgressionSavedData data) {
        return new WorldGearProgressionSnapshot(data.isIronUnlocked(), data.isGoldUnlocked(),
                data.isDiamondUnlocked(), data.isNetheriteUnlocked(), data.hasVisitedEnd());
    }
}
