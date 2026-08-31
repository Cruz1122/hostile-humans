package com.craftix.hostile_humans.entity.ai.survival;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class SquadNeeds {
    private static final SquadNeed[] PRIORITY = {
            SquadNeed.WOOD,
            SquadNeed.FUEL,
            SquadNeed.STONE,
            SquadNeed.IRON,
            SquadNeed.GOLD,
            SquadNeed.DIAMOND,
            SquadNeed.APPLES,
            SquadNeed.FOOD
    };
    private final EnumMap<SquadNeed, Integer> deficits;

    public SquadNeeds(Map<SquadNeed, Integer> deficits) {
        this.deficits = new EnumMap<>(SquadNeed.class);
        deficits.forEach((need, deficit) -> {
            if (deficit > 0) this.deficits.put(need, deficit);
        });
    }

    public boolean needs(SquadNeed need) {
        return deficits.getOrDefault(need, 0) > 0;
    }

    public int deficit(SquadNeed need) {
        return deficits.getOrDefault(need, 0);
    }

    public Map<SquadNeed, Integer> deficits() {
        return Collections.unmodifiableMap(deficits);
    }

    public Optional<SquadNeed> highestPriority() {
        for (SquadNeed need : PRIORITY) if (needs(need)) return Optional.of(need);
        return Optional.empty();
    }
}
