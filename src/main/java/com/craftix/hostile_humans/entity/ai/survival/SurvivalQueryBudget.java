package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/** Per-dimension circuit breaker for expensive survival queries. */
public final class SurvivalQueryBudget {
    private static final Map<ResourceKey<Level>, Window> WINDOWS = new HashMap<>();

    private SurvivalQueryBudget() {}

    public static boolean tryPath(Human human) {
        // Directly driven GameTests intentionally use no-AI entities and are
        // exercising one query, not the live server scheduler. Do not let
        // unrelated tests in the same game tick consume their budget.
        if (human.isNoAi()) return true;
        Window window = window(human);
        if (window.paths >= Config.survivalPathBudgetPerTick.get()) return false;
        window.paths++;
        return true;
    }

    public static boolean tryResourceScan(Human human) {
        if (human.isNoAi()) return true;
        Window window = window(human);
        if (window.scans >= Config.survivalScanBudgetPerTick.get()) return false;
        window.scans++;
        return true;
    }

    private static Window window(Human human) {
        ResourceKey<Level> dimension = human.level().dimension();
        long now = human.level().getGameTime();
        WINDOWS.entrySet().removeIf(entry -> entry.getValue().tick + 200 < now);
        Window window = WINDOWS.get(dimension);
        if (window == null || window.tick != now) {
            window = new Window(now);
            WINDOWS.put(dimension, window);
        }
        return window;
    }

    private static final class Window {
        private final long tick;
        private int paths;
        private int scans;

        private Window(long tick) {
            this.tick = tick;
        }
    }
}
