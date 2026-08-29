package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Server-thread-only, non-persistent claims for resources and shared furnaces. */
public final class SurvivalClaimManager {
    private static final int RESOURCE_TTL = 200;
    private static final int STATION_TTL = 240;
    private static final int CLEANUP_INTERVAL_TICKS = 20;
    private static final Map<Key, Claim> RESOURCE_CLAIMS = new HashMap<>();
    private static final Map<Key, Claim> STATION_CLAIMS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Long> RESOURCE_CLEANUP = new HashMap<>();
    private static final Map<ResourceKey<Level>, Long> STATION_CLEANUP = new HashMap<>();

    private SurvivalClaimManager() {}

    public static boolean claimResource(Human human, BlockPos pos) {
        return claim(RESOURCE_CLAIMS, RESOURCE_CLEANUP, human, pos, RESOURCE_TTL);
    }

    public static boolean claimStation(Human human, BlockPos pos) {
        return claim(STATION_CLAIMS, STATION_CLEANUP, human, pos, STATION_TTL);
    }

    public static boolean resourceClaimedByOther(Human human, BlockPos pos) {
        return claimedByOther(RESOURCE_CLAIMS, RESOURCE_CLEANUP, human, pos);
    }

    public static boolean stationClaimedByOther(Human human, BlockPos pos) {
        return claimedByOther(STATION_CLAIMS, STATION_CLEANUP, human, pos);
    }

    public static void releaseResource(Human human, BlockPos pos) {
        release(RESOURCE_CLAIMS, human, pos);
    }

    public static void releaseStation(Human human, BlockPos pos) {
        release(STATION_CLAIMS, human, pos);
    }

    public static void releaseAll(Human human) {
        RESOURCE_CLAIMS.entrySet().removeIf(entry -> entry.getValue().owner.equals(human.getUUID()));
        STATION_CLAIMS.entrySet().removeIf(entry -> entry.getValue().owner.equals(human.getUUID()));
    }

    private static boolean claim(Map<Key, Claim> claims, Map<ResourceKey<Level>, Long> cleanupTicks,
                                 Human human, BlockPos pos, int ttl) {
        cleanupIfDue(claims, cleanupTicks, human.level().dimension(), human.level().getGameTime());
        Key key = new Key(human.level().dimension(), pos.immutable());
        Claim existing = claims.get(key);
        if (existing != null && !existing.owner.equals(human.getUUID())) return false;
        claims.put(key, new Claim(human.getUUID(), human.level().getGameTime() + ttl));
        return true;
    }

    private static boolean claimedByOther(Map<Key, Claim> claims, Map<ResourceKey<Level>, Long> cleanupTicks,
                                          Human human, BlockPos pos) {
        cleanupIfDue(claims, cleanupTicks, human.level().dimension(), human.level().getGameTime());
        Claim claim = claims.get(new Key(human.level().dimension(), pos));
        return claim != null && !claim.owner.equals(human.getUUID());
    }

    private static void release(Map<Key, Claim> claims, Human human, BlockPos pos) {
        Key key = new Key(human.level().dimension(), pos);
        Claim claim = claims.get(key);
        if (claim != null && claim.owner.equals(human.getUUID())) claims.remove(key);
    }

    private static void cleanupIfDue(Map<Key, Claim> claims, Map<ResourceKey<Level>, Long> cleanupTicks,
                                     ResourceKey<Level> dimension, long now) {
        long next = cleanupTicks.getOrDefault(dimension, Long.MIN_VALUE);
        if (now < next) return;
        cleanupTicks.put(dimension, now + CLEANUP_INTERVAL_TICKS);
        Iterator<Claim> iterator = claims.values().iterator();
        while (iterator.hasNext()) if (iterator.next().expiresAt <= now) iterator.remove();
    }

    private record Key(ResourceKey<Level> dimension, BlockPos pos) {}
    private record Claim(UUID owner, long expiresAt) {}
}
