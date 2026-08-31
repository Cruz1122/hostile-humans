package com.craftix.hostile_humans.entity.loadout;

import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.spawner.SpawnContext;
import com.craftix.hostile_humans.progression.WorldGearProgressionSavedData;
import com.craftix.hostile_humans.progression.WorldGearProgressionSnapshot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;

/** Immutable inputs to the single natural Human loadout generator. */
public record LoadoutRollContext(SpawnContext spawnContext, ResourceKey<Level> dimension,
                                 WorldGearProgressionSnapshot progression,
                                 CombatSkillTier tier, long serverAgeTicks, RandomSource random) {
    public static LoadoutRollContext forHuman(ServerLevel level, Human human, RandomSource random) {
        WorldGearProgressionSavedData data = WorldGearProgressionSavedData.get(level);
        return new LoadoutRollContext(human.getSpawnContext(), level.dimension(), WorldGearProgressionSnapshot.from(data),
                human.getCombatTacticsController().skillTier(), level.getServer().overworld().getGameTime(), random);
    }

    public double ageFactor() {
        return HumanLoadoutGenerator.ageFactor(serverAgeTicks);
    }
}
