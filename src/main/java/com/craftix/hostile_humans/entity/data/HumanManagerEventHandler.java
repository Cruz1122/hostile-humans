package com.craftix.hostile_humans.entity.data;

import com.craftix.hostile_humans.entity.HumanEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber
public class HumanManagerEventHandler {

    private static final Set<Entity> entitySet = ConcurrentHashMap.newKeySet();
    private static final short SYNC_TICK = 25;
    private static short ticks = 0;

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void handleEntityJoinWorldEvent(EntityJoinLevelEvent event) {
        updateOrRegisterHumanMob(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void handleEntityTeleportEvent(EntityTeleportEvent event) {
        updateOrRegisterHumanMob(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void handleEntityTravelToDimensionEvent(EntityTravelToDimensionEvent event) {
        updateOrRegisterHumanMob(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void handleEntityLeaveWorldEvent(EntityLeaveLevelEvent event) {
        updateHumanMobData(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void handleLivingDamageEvent(LivingDamageEvent event) {
        scheduleHumanMobDataUpdate(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void handleLivingHurtEvent(LivingHurtEvent event) {
        scheduleHumanMobDataUpdate(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void handleLivingHealEvent(LivingHealEvent event) {
        scheduleHumanMobDataUpdate(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void handleLivingDeathEvent(LivingDeathEvent event) {
        updateHumanMobData(event.getEntity());
    }

    @SubscribeEvent
    public static void handleClientServerTickEvent(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ticks++ >= SYNC_TICK) {
            syncHumanMobData();
            ticks = 0;
        }
    }

    @SubscribeEvent
    public static void handlePlayerChangedDimensionEvent(PlayerChangedDimensionEvent event) {
        verifyHHFollowerForPlayer(event.getEntity());
        syncHHFollowersDataToPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void handlePlayerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event) {
        syncHHFollowersDataToPlayer(event.getEntity());
    }

    private static void scheduleHumanMobDataUpdate(Entity entity) {
        if (entity instanceof HumanEntity humanEntity && humanEntity.hasOwner()) {
            entitySet.add(entity);
        }
    }

    private static void syncHumanMobData() {
        if (entitySet.isEmpty()) {
            return;
        }
        Iterator<Entity> entityIterator = entitySet.iterator();
        while (entityIterator.hasNext()) {
            Entity entity = entityIterator.next();
            if (entity != null) {
                updateOrRegisterHumanMob(entity);
            }
            entityIterator.remove();
        }
    }

    private static void syncHHFollowersDataToPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            HumanServerData.get().syncHumanData(serverPlayer.getUUID());
        }
    }

    private static void updateOrRegisterHumanMob(Entity entity) {
        if (entity instanceof HumanEntity humanEntity && !humanEntity.level().isClientSide) {

            HumanServerData.get().updateOrRegisterHumanMob(humanEntity);
        }
    }

    private static void updateHumanMobData(Entity entity) {
        if (entity instanceof HumanEntity humanEntity && !humanEntity.level().isClientSide && humanEntity.hasOwner()) {
            HumanServerData.get().updateHumanData(humanEntity);
        }
    }

    private static void verifyHHFollowerForPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            HumanServerData data = HumanServerData.get();
            if (data == null) {
                return;
            }
            MinecraftServer server = serverPlayer.getServer();
            Iterator<ServerLevel> serverLevels = server.getAllLevels().iterator();

            Set<Entity> humanMobEntitiesEntityInOwnersDimension = data.getHumanMobsEntity(player.getUUID(), serverPlayer.serverLevel());

            while (serverLevels.hasNext()) {
                ServerLevel serverLevel = serverLevels.next();
                if (serverPlayer.serverLevel() != serverLevel) {
                    for (Entity humanEntity : humanMobEntitiesEntityInOwnersDimension) {
                        Entity entity = serverLevel.getEntity(humanEntity.getUUID());
                        if (entity != null) {
                            entity.remove(RemovalReason.CHANGED_DIMENSION);
                        }
                    }
                }
            }
        }
    }
}
