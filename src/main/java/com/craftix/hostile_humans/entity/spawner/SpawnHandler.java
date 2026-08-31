package com.craftix.hostile_humans.entity.spawner;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.HumanEntity;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import com.craftix.hostile_humans.entity.entities.SpawnerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import net.minecraft.util.RandomSource;

import static net.minecraft.world.entity.Mob.checkMobSpawnRules;

@Mod.EventBusSubscriber()
public class SpawnHandler {

    protected SpawnHandler() {
    }

    @SubscribeEvent
    public static void onWorldLoad(ServerAboutToStartEvent event) {

    }

    public static void registerSpawnPlacements(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            SpawnPlacements.register(ModEntityType.ROAMER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SpawnHandler::checkHumanSpawnRules);
            SpawnPlacements.register(ModEntityType.SPAWNER_ENTITY.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SpawnHandler::checkSpawnerEntityRules);
        });
    }

    public static boolean checkSpawnerEntityRules(EntityType<SpawnerEntity> p_33018_, ServerLevelAccessor p_33019_, MobSpawnType p_33020_, BlockPos p_33021_, RandomSource random) {
        int spawnRoll = Math.max(1, Config.battleEventNaturalSpawnRoll.get());
        if (random.nextInt(spawnRoll) != 0) return false;
        return isBrightEnoughToSpawn(p_33019_, p_33021_, random) && checkMobSpawnRules(p_33018_, p_33019_, p_33020_, p_33021_, random);
    }

    public static boolean checkHumanSpawnRules(EntityType<? extends Human> p_33018_, ServerLevelAccessor p_33019_, MobSpawnType p_33020_, BlockPos p_33021_, RandomSource random) {
        return checkMobSpawnRules(p_33018_, p_33019_, p_33020_, p_33021_, random);
    }

    public static boolean isBrightEnoughToSpawn(ServerLevelAccessor p_33009_, BlockPos p_33010_, RandomSource p_33011_) {
        return p_33009_.getBrightness(LightLayer.SKY, p_33010_) > 10;
    }
}
