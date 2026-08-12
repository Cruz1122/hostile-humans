package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.item.Items;

public final class ProgressionCraftingPolicy {
    private ProgressionCraftingPolicy() {}

    public static boolean shouldCraftGoldenApple(Human human, SquadNeeds needs) {
        return SurvivalInventory.count(human, Items.GOLDEN_APPLE) < 1
                && !needs.needs(SquadNeed.FOOD) && !needs.needs(SquadNeed.IRON);
    }
}
