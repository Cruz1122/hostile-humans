package com.craftix.hostile_humans.entity.entities;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.compat.DungeonMobs;
import com.craftix.hostile_humans.persona.PersonaFaction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpawnerEntity extends Mob {

    protected SpawnerEntity(EntityType<? extends Mob> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }
    
    public enum SpawnType {
    	HumanVsHuman, HumanVsPillager, Random, Disabled
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            spawn((ServerLevel) level());
            discard();
        }
    }

    private boolean spawn(ServerLevel level) {
        BlockPos blockpos = this.blockPosition();
        if (this.hasEnoughSpace(level, blockpos) && Config.eventType.get() != SpawnType.Disabled) {
            if (level.getBiome(blockpos).is(Biomes.THE_VOID)) {
                return false;
            }

            boolean currentTeam = false;
            boolean isPillagers = Config.eventType.get() == SpawnType.HumanVsPillager 
            		|| (random.nextFloat() < 0.6f && Config.eventType.get() != SpawnType.HumanVsHuman);

            int totalAmount = random.nextInt(3, 18);
            boolean bannerLeft = false;
            boolean bannerRight = false;

            var spawnedEntities = new ArrayList<LivingEntity>();
            for (int j = 0; j < totalAmount; j++) {
                var pos = this.findSpawnPositionNear(level, blockpos, 4);
                if (pos != null) {
                    if (isPillagers && currentTeam) {
                        if (random.nextBoolean())
                            spawnedEntities.add((LivingEntity) getRandomPillager().spawn(level, (CompoundTag) null, null, pos, MobSpawnType.EVENT, false, false));
                        spawnedEntities.add((LivingEntity) getRandomPillager().spawn(level, (CompoundTag) null, null, pos, MobSpawnType.EVENT, false, false));
                    } else {
                        var newHuman = (random.nextFloat() < 0.05 ? ModEntityType.HUMAN2.get() : ModEntityType.HUMAN1.get()).spawn(level, (CompoundTag) null, null, pos, MobSpawnType.EVENT, false, false);
                        spawnedEntities.add(newHuman);

                        if (newHuman != null) {
                            if (totalAmount > 5) {
                                if (currentTeam && !bannerLeft) {
                                    bannerLeft = true;
                                    newHuman.setBanner(HumanUtil.createSwordBanner());
                                }
                                if (!currentTeam && !bannerRight) {
                                    bannerRight = true;
                                    newHuman.setBanner(HumanUtil.createSwordBanner());
                                }
                            }
                            newHuman.team = currentTeam ? "team1" : "team2";
                        }
                    }
                    currentTeam = !currentTeam;
                }
            }

            for (var entity : spawnedEntities) {
                entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * (entity instanceof Human ? 15 : 10), 255, false, false, false));
            }
            assignSquads(spawnedEntities);

            return true;
        }

        return false;
    }

    private void assignSquads(List<LivingEntity> spawnedEntities) {
        Map<PersonaFaction, List<Human>> byFaction = new HashMap<>();
        for (LivingEntity entity : spawnedEntities) {
            if (entity instanceof Human human) {
                human.getPersonaDefinition().ifPresent(definition ->
                        byFaction.computeIfAbsent(definition.faction(), ignored -> new ArrayList<>()).add(human));
            }
        }
        for (List<Human> factionHumans : byFaction.values()) {
            int index = 0;
            while (factionHumans.size() - index >= 2) {
                int remaining = factionHumans.size() - index;
                int groupSize = Math.min(5, remaining == 6 ? 3 : remaining);
                if (remaining - groupSize == 1) groupSize--;
                UUID squadId = UUID.randomUUID();
                for (int member = 0; member < groupSize; member++) {
                    factionHumans.get(index++).setSquadId(squadId);
                }
            }
        }
    }

    EntityType getRandomPillager() {
        if (random.nextFloat() < 0.05f) return EntityType.RAVAGER;
        if (random.nextFloat() < 0.05f && ModList.get().isLoaded("dungeon_mobs")) return DungeonMobs.getRedstoneGolem();

        EntityType[] list = {EntityType.PILLAGER, EntityType.PILLAGER, EntityType.EVOKER, EntityType.PILLAGER, EntityType.ILLUSIONER, EntityType.VINDICATOR, EntityType.VINDICATOR, EntityType.VINDICATOR};
        return list[(int) (list.length * random.nextFloat())];
    }

    @Nullable
    private BlockPos findSpawnPositionNear(LevelReader levelReader, BlockPos blockPos, int radius) {
        BlockPos outPos = null;

        for (int i = 0; i < 10; ++i) {
            int j = blockPos.getX() + this.random.nextInt(radius * 2) - radius;
            int k = blockPos.getZ() + this.random.nextInt(radius * 2) - radius;
            int l = levelReader.getHeight(Heightmap.Types.WORLD_SURFACE, j, k);
            BlockPos blockpos1 = new BlockPos(j, l, k);
            if (NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, levelReader, blockpos1, EntityType.WANDERING_TRADER)) {
                outPos = blockpos1;
                break;
            }
        }

        return outPos;
    }

    private boolean hasEnoughSpace(BlockGetter p_35926_, BlockPos p_35927_) {
        for (BlockPos blockpos : BlockPos.betweenClosed(p_35927_, p_35927_.offset(1, 2, 1))) {
            if (!p_35926_.getBlockState(blockpos).getCollisionShape(p_35926_, blockpos).isEmpty()) {
                return false;
            }
        }

        return true;
    }
}
