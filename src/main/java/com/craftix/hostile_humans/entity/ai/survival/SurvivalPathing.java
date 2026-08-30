package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Path;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

/** Shared vanilla pathfinder adapter for survival destinations. */
public final class SurvivalPathing {
    private SurvivalPathing() {}

    /**
     * Builds one path to any valid destination cell instead of asking the
     * navigation object to infer a route to a solid block or stale coordinate.
     */
    public static Optional<Path> createPath(Human human, Collection<BlockPos> destinations, int reachRange) {
        if (destinations.isEmpty() || !SurvivalQueryBudget.tryPath(human)) return Optional.empty();
        Path path = human.getNavigation().createPath(new HashSet<>(destinations), reachRange);
        return path != null && path.canReach() ? Optional.of(path) : Optional.empty();
    }

    public static boolean moveTo(Human human, Collection<BlockPos> destinations, int reachRange, double speed) {
        return createPath(human, destinations, reachRange)
                .map(path -> human.getNavigation().moveTo(path, speed))
                .orElse(false);
    }
}
