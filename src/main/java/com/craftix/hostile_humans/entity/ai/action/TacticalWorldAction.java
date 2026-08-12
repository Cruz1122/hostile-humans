package com.craftix.hostile_humans.entity.ai.action;

public interface TacticalWorldAction {
    boolean canStart(WorldActionContext context);

    WorldActionResult tick(WorldActionContext context);

    void stop(WorldActionContext context);

    WorldActionType type();

    default int placedBlockCount() { return 0; }

    default int brokenBlockCount() { return 0; }
}
