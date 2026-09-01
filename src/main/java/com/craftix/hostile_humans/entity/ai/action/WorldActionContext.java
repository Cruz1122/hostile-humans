package com.craftix.hostile_humans.entity.ai.action;

import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

public record WorldActionContext(Human human, @Nullable BlockPos objective) {
    public WorldActionContext(Human human) {
        this(human, null);
    }
}
