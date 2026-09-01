package com.craftix.hostile_humans.entity.ai.camp;

import com.craftix.hostile_humans.persona.PersonaFaction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/** Immutable definition of a small physical camp. Inventories remain in blocks. */
public record Camp(UUID id, ResourceKey<Level> dimension, BlockPos center, PersonaFaction faction,
                   List<BlockPos> storagePositions, BlockPos craftingTablePos,
                   BlockPos furnacePos, BlockPos campfirePos) {
    public Camp {
        center = center.immutable();
        storagePositions = storagePositions.stream().map(BlockPos::immutable).toList();
        craftingTablePos = craftingTablePos == null ? null : craftingTablePos.immutable();
        furnacePos = furnacePos == null ? null : furnacePos.immutable();
        campfirePos = campfirePos == null ? null : campfirePos.immutable();
        storagePositions = List.copyOf(storagePositions);
    }
}
