package com.craftix.hostile_humans.entity.spawner;

import com.craftix.hostile_humans.persona.PersonaFaction;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.UUID;

/** Small server-side handoff object between encounter selection and initialization. */
public record HumanSpawnRequest(BlockPos position, SpawnContext context,
                                PersonaFaction faction, @Nullable UUID squadId) {
}
