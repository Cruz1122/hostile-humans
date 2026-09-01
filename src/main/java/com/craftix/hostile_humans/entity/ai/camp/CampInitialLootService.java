package com.craftix.hostile_humans.entity.ai.camp;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.progression.WorldGearProgressionSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** Moderate debug/foundation supplies, bounded by the existing world progression. */
public final class CampInitialLootService {
    private CampInitialLootService() {}

    public static void populate(ServerLevel level, Camp camp) {
        if (!Config.campInitialLoot.get() || camp.storagePositions().isEmpty()
                || !(level.getBlockEntity(camp.storagePositions().get(0)) instanceof ChestBlockEntity chest)) return;
        int slot = 0;
        chest.setItem(slot++, new ItemStack(Items.COOKED_BEEF, 6));
        chest.setItem(slot++, new ItemStack(Items.ARROW, 8));
        chest.setItem(slot++, new ItemStack(Items.COAL, 4));
        chest.setItem(slot++, new ItemStack(Items.COBBLESTONE, 8));
        WorldGearProgressionSavedData progression = WorldGearProgressionSavedData.get(level);
        if (progression.isIronUnlocked()) chest.setItem(slot++, new ItemStack(Items.IRON_INGOT, 2));
        if (progression.isGoldUnlocked()) chest.setItem(slot++, new ItemStack(Items.GOLD_INGOT, 1));
        if (progression.isDiamondUnlocked() && level.random.nextFloat() < .02F) chest.setItem(slot++, new ItemStack(Items.DIAMOND, 1));
        chest.setChanged();
    }
}
