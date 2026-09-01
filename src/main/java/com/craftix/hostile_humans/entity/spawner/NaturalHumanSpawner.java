package com.craftix.hostile_humans.entity.spawner;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.HostileHumans;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import com.craftix.hostile_humans.persona.PersonaDefinition;
import com.craftix.hostile_humans.persona.PersonaFaction;
import com.craftix.hostile_humans.persona.PersonaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** One bounded, server-only source of natural Human encounters. */
public final class NaturalHumanSpawner {
    private static final int MAX_CANDIDATES_PER_ATTEMPT = 3;
    private static final int GROUP_POSITION_ATTEMPTS = 12;
    private static final int PERSONA_SELECTION_ATTEMPTS = 16;
    private static final int INITIAL_END_ISLAND_RADIUS = 256;
    private static final int INITIAL_END_SQUAD_SIZE = 2;
    private static final int INITIAL_END_MAX_SQUADS = 3;
    private static final int INITIAL_END_POSITION_ATTEMPTS = 48;
    private static final Map<ServerLevel, Set<UUID>> LOADED_HUMANS = new IdentityHashMap<>();

    private NaturalHumanSpawner() {
    }

    public static void register(IEventBus eventBus) {
        eventBus.register(NaturalHumanSpawner.class);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) tick(event.getServer());
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Human human
                && event.getLevel() instanceof ServerLevel level) remember(level, human);
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Human human && event.getLevel() instanceof ServerLevel level) {
            forget(level, human);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) LOADED_HUMANS.remove(level);
    }

    public static void tick(MinecraftServer server) {
        if (!Config.enableNaturalHumanSpawning.get()) return;
        int interval = Math.max(1, Config.spawnAttemptIntervalTicks.get());
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getGameTime() % interval != 0L) continue;
            if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) continue;
            if (tryPopulateInitialEndIsland(level)) continue;
            attempt(level);
        }
    }

    private static boolean tryPopulateInitialEndIsland(ServerLevel level) {
        if (level.dimension() != Level.END) return false;
        NaturalSpawnSavedData data = NaturalSpawnSavedData.get(level);
        if (data.isInitialEndIslandPopulated()) return false;
        List<ServerPlayer> players = level.players().stream()
                .filter(player -> !player.isSpectator() && isWithinInitialEndIsland(player.blockPosition()))
                .toList();
        if (players.isEmpty()) return false;
        populateInitialEndIsland(level, players.get(level.random.nextInt(players.size())), data, level.random);
        // While the guaranteed event is pending, do not let an ordinary End
        // encounter consume the capacity needed to complete it.
        return true;
    }

    /**
     * Populates the central End island atomically with one to three two-member squads.
     * Empty results are retryable and do not mark the one-time event complete.
     */
    public static List<List<Human>> populateInitialEndIsland(ServerLevel level, ServerPlayer player,
                                                              NaturalSpawnSavedData data, RandomSource random) {
        if (level.dimension() != Level.END || data.isInitialEndIslandPopulated()
                || player.isSpectator() || !isWithinInitialEndIsland(player.blockPosition())) return List.of();

        int nearby = countNearbyHumans(level, player.position(), INITIAL_END_ISLAND_RADIUS);
        int capacity = Math.min(Config.dimensionHumanCap.get() - loadedHumanCount(level),
                Config.nearbyHumanCapPerPlayer.get() - nearby);
        int maxSquads = Math.min(INITIAL_END_MAX_SQUADS, capacity / INITIAL_END_SQUAD_SIZE);
        if (maxSquads < 1) return List.of();

        int squadCount = 1 + random.nextInt(maxSquads);
        List<List<Human>> squads = new ArrayList<>();
        List<BlockPos> anchors = new ArrayList<>();
        for (int index = 0; index < squadCount; index++) {
            BlockPos anchor = findInitialEndIslandAnchor(level, player, random, anchors);
            if (anchor == null) {
                discardSquads(squads);
                return List.of();
            }
            List<Human> squad = spawnGroup(level, anchor, SpawnContext.END_WILDS,
                    INITIAL_END_SQUAD_SIZE, random);
            if (squad.size() != INITIAL_END_SQUAD_SIZE || squad.get(0).getSquadId() == null) {
                squad.forEach(Human::discard);
                discardSquads(squads);
                return List.of();
            }
            anchors.add(anchor);
            squads.add(List.copyOf(squad));
        }

        data.markInitialEndIslandPopulated();
        HostileHumans.LOGGER.info("Populated the initial End island with {} Human squads ({} Humans)",
                squads.size(), squads.stream().mapToInt(List::size).sum());
        return List.copyOf(squads);
    }

    public static boolean isWithinInitialEndIsland(BlockPos position) {
        return (long) position.getX() * position.getX() + (long) position.getZ() * position.getZ()
                <= (long) INITIAL_END_ISLAND_RADIUS * INITIAL_END_ISLAND_RADIUS;
    }

    private static void attempt(ServerLevel level) {
        if (loadedHumanCount(level) >= Config.dimensionHumanCap.get()) return;
        List<ServerPlayer> players = level.players().stream()
                .filter(player -> !player.isSpectator())
                .toList();
        if (players.isEmpty()) return;

        RandomSource random = level.random;
        ServerPlayer player = players.get(random.nextInt(players.size()));
        for (int attempt = 0; attempt < MAX_CANDIDATES_PER_ATTEMPT; attempt++) {
            BlockPos candidate = findCandidate(level, player, random, attempt == 1);
            if (candidate == null || !isWithinSpawnDistance(player.position(), candidate)) continue;
            SpawnContext context = SpawnContextClassifier.classify(level, candidate);
            if (random.nextDouble() > contextChance(context)) continue;
            int nearby = countNearbyHumans(level, player.position(), Config.maxSpawnDistance.get());
            int remaining = Math.min(Config.dimensionHumanCap.get() - loadedHumanCount(level),
                    Config.nearbyHumanCapPerPlayer.get() - nearby);
            if (remaining <= 0) return;
            int requested = chooseSquadSize(context, random);
            int groupSize = Math.min(requested, remaining);
            List<Human> spawned = spawnGroup(level, candidate, context, groupSize, random);
            if (!spawned.isEmpty()) {
                HostileHumans.LOGGER.debug("Natural Human spawn: context={}, size={}, faction={}, player={}",
                        context, spawned.size(), spawned.get(0).getPersonaDefinition()
                                .map(PersonaDefinition::faction).orElse(null), player.getGameProfile().getName());
                return;
            }
        }
    }

    @Nullable
    private static BlockPos findCandidate(ServerLevel level, ServerPlayer player, RandomSource random,
                                          boolean preferCave) {
        int minimum = Math.max(1, Config.minSpawnDistance.get());
        int maximum = Math.max(minimum, Config.maxSpawnDistance.get());
        double distance = minimum + random.nextDouble() * (maximum - minimum);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
        LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) return null;

        int localX = x & 15;
        int localZ = z & 15;
        if (localX == 0 || localX == 15 || localZ == 0 || localZ == 15) return null;
        if (level.dimension() == Level.OVERWORLD && preferCave) {
            int playerY = player.getBlockY();
            for (int offset = 0; offset < 24; offset++) {
                int y = playerY - 16 + random.nextInt(25);
                BlockPos cave = new BlockPos(x, y, z);
                if (isValidPosition(level, cave) && SpawnContextClassifier.isCave(level, cave)) return cave;
            }
        }
        if (level.dimension() == Level.NETHER) {
            int playerY = player.getBlockY();
            for (int offset = 0; offset <= 24; offset++) {
                int delta = offset == 0 ? 0 : (offset + 1) / 2 * (offset % 2 == 0 ? -1 : 1);
                BlockPos interior = new BlockPos(x, playerY + delta, z);
                if (isValidPosition(level, interior)) return interior;
            }
            return null;
        }
        int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, localX, localZ) + 1;
        BlockPos surface = new BlockPos(x, y, z);
        return isValidPosition(level, surface) ? surface : null;
    }

    public static List<Human> spawnGroup(ServerLevel level, BlockPos anchor, SpawnContext context,
                                          int requestedSize, RandomSource random) {
        if (requestedSize < 1 || !isLoaded(level, anchor) || !isValidPosition(level, anchor)) return List.of();
        int size = Math.min(5, requestedSize);
        while (size > 0) {
            List<BlockPos> positions = findGroupPositions(level, anchor, context, size, random);
            if (positions.size() < size) {
                size--;
                continue;
            }
            if (loadedHumanCount(level) + size > Config.dimensionHumanCap.get()) return List.of();
            PersonaFaction faction = chooseFaction(random);
            UUID squadId = size > 1 ? UUID.randomUUID() : null;
            List<Human> humans = new ArrayList<>();
            boolean complete = true;
            for (BlockPos position : positions) {
                Human human = ModEntityType.ROAMER.get().create(level);
                if (human == null) {
                    complete = false;
                    break;
                }
                if (!assignPersona(human, faction, random)) {
                    PersonaFaction fallback = fallbackFaction(faction);
                    if (fallback == null || !assignPersona(human, fallback, random)) {
                        discardSpawnCandidate(human);
                        complete = false;
                        break;
                    }
                    faction = fallback;
                }
                HumanSpawnRequest request = new HumanSpawnRequest(position, context, faction, squadId);
                human.moveTo(request.position(), random.nextFloat() * 360.0F, 0.0F);
                human.setSpawnContext(request.context());
                if (request.squadId() != null && !human.setSquadId(request.squadId())) {
                    discardSpawnCandidate(human);
                    complete = false;
                    break;
                }
                humans.add(human);
            }
            if (!complete) {
                cleanup(humans);
                size--;
                continue;
            }

            DifficultyInstance difficulty = level.getCurrentDifficultyAt(anchor);
            for (Human human : humans) {
                human.finalizeSpawn(level, difficulty, MobSpawnType.NATURAL, null, null);
                if (!level.addFreshEntity(human)) {
                    complete = false;
                    break;
                }
                remember(level, human);
            }
            if (complete) return humans;
            cleanup(humans);
            size--;
        }
        return List.of();
    }

    private static List<BlockPos> findGroupPositions(ServerLevel level, BlockPos anchor, SpawnContext context,
                                                       int size, RandomSource random) {
        List<BlockPos> positions = new ArrayList<>();
        positions.add(anchor);
        for (int attempt = 0; positions.size() < size && attempt < GROUP_POSITION_ATTEMPTS; attempt++) {
            BlockPos candidate = anchor.offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4);
            if (isLoaded(level, candidate) && isValidPosition(level, candidate)
                    && positions.stream().noneMatch(existing -> existing.equals(candidate))) positions.add(candidate);
        }
        return positions;
    }

    private static boolean assignPersona(Human human, PersonaFaction faction, RandomSource random) {
        List<PersonaDefinition> candidates = PersonaRegistry.get().forFaction(faction);
        if (candidates.isEmpty()) return false;
        int attempts = Math.min(PERSONA_SELECTION_ATTEMPTS, candidates.size());
        int start = random.nextInt(candidates.size());
        for (int offset = 0; offset < attempts; offset++) {
            if (human.setPersonaId(candidates.get((start + offset) % candidates.size()).id())) return true;
        }
        return false;
    }

    public static PersonaFaction chooseFaction(RandomSource random) {
        double legends = Math.max(0.0D, Math.min(1.0D, Config.legendSpawnWeight.get()));
        double roll = random.nextDouble();
        if (roll < legends) return PersonaFaction.MINECRAFT_LEGENDS;
        return roll < legends + (1.0D - legends) / 2.0D
                ? PersonaFaction.HISPANIC_CREATORS : PersonaFaction.INTERNATIONAL_CREATORS;
    }

    @Nullable
    private static PersonaFaction fallbackFaction(PersonaFaction faction) {
        return switch (faction) {
            case MINECRAFT_LEGENDS -> PersonaFaction.HISPANIC_CREATORS;
            case HISPANIC_CREATORS -> PersonaFaction.INTERNATIONAL_CREATORS;
            case INTERNATIONAL_CREATORS -> PersonaFaction.HISPANIC_CREATORS;
        };
    }

    public static int chooseSquadSize(SpawnContext context, RandomSource random) {
        double squad = clamp(Config.squadChance.get());
        if (random.nextDouble() >= squad) return 1;
        return random.nextDouble() < clamp(Config.largeSquadChance.get())
                ? 4 + random.nextInt(2) : 2 + random.nextInt(2);
    }

    public static double contextSpawnChance(SpawnContext context) {
        return switch (context) {
            case OVERWORLD_SURFACE -> clamp(Config.spawnSurfaceChance.get());
            case OVERWORLD_CAVE -> clamp(Config.spawnCaveChance.get());
            case VILLAGE, OVERWORLD_STRUCTURE -> clamp(Config.spawnStructureChance.get());
            case NETHER_WILDS -> clamp(Config.spawnNetherChance.get());
            case NETHER_FORTRESS, BASTION -> clamp(Config.spawnFortressChance.get());
            case END_WILDS -> clamp(Config.spawnEndChance.get());
            case END_CITY -> clamp(Config.spawnEndCityChance.get());
            default -> 0.0D;
        };
    }

    private static double contextChance(SpawnContext context) {
        return contextSpawnChance(context);
    }

    public static boolean isWithinSpawnDistance(net.minecraft.world.phys.Vec3 playerPosition, BlockPos position) {
        double deltaX = playerPosition.x - (position.getX() + 0.5D);
        double deltaZ = playerPosition.z - (position.getZ() + 0.5D);
        double distanceSqr = deltaX * deltaX + deltaZ * deltaZ;
        double minimum = Math.max(0, Config.minSpawnDistance.get());
        double maximum = Math.max(minimum, Config.maxSpawnDistance.get());
        return distanceSqr >= minimum * minimum && distanceSqr <= maximum * maximum;
    }

    public static int countNearbyHumans(ServerLevel level, net.minecraft.world.phys.Vec3 origin, double radius) {
        AABB area = new AABB(origin.x - radius, origin.y - radius, origin.z - radius,
                origin.x + radius, origin.y + radius, origin.z + radius);
        double radiusSqr = radius * radius;
        return level.getEntitiesOfClass(Human.class, area,
                human -> human.distanceToSqr(origin.x, origin.y, origin.z) <= radiusSqr).size();
    }

    public static int loadedHumanCount(ServerLevel level) {
        Set<UUID> humans = LOADED_HUMANS.get(level);
        if (humans == null) return 0;
        humans.removeIf(uuid -> !(level.getEntity(uuid) instanceof Human human) || !human.isAlive());
        return humans.size();
    }

    public static boolean isLoaded(ServerLevel level, BlockPos position) {
        return level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4) != null;
    }

    public static boolean isValidNaturalPosition(ServerLevel level, BlockPos position) {
        return isValidPosition(level, position);
    }

    private static boolean isValidPosition(ServerLevel level, BlockPos position) {
        if (!isLoaded(level, position) || !level.isInWorldBounds(position)
                || !level.getWorldBorder().isWithinBounds(position)
                || !level.isNaturalSpawningAllowed(position)) return false;
        BlockState feet = level.getBlockState(position);
        BlockState head = level.getBlockState(position.above());
        BlockState floor = level.getBlockState(position.below());
        if (!feet.getCollisionShape(level, position).isEmpty()
                || !head.getCollisionShape(level, position.above()).isEmpty()
                || floor.getCollisionShape(level, position.below()).isEmpty()) return false;
        if (!level.getFluidState(position).isEmpty() || !level.getFluidState(position.above()).isEmpty()
                || !level.getFluidState(position.below()).isEmpty()) return false;
        return true;
    }

    private static void remember(ServerLevel level, Human human) {
        LOADED_HUMANS.computeIfAbsent(level, ignored -> Collections.newSetFromMap(new HashMap<>()))
                .add(human.getUUID());
    }

    private static void forget(ServerLevel level, Human human) {
        Set<UUID> humans = LOADED_HUMANS.get(level);
        if (humans != null) humans.remove(human.getUUID());
    }

    private static void cleanup(List<Human> humans) {
        for (Human human : humans) {
            discardSpawnCandidate(human);
        }
    }

    private static void discardSquads(List<List<Human>> squads) {
        squads.stream().flatMap(List::stream).forEach(Human::discard);
    }

    @Nullable
    private static BlockPos findInitialEndIslandAnchor(ServerLevel level, ServerPlayer player,
                                                        RandomSource random, List<BlockPos> existingAnchors) {
        for (int attempt = 0; attempt < INITIAL_END_POSITION_ATTEMPTS; attempt++) {
            double distance = 8.0D + random.nextDouble() * 40.0D;
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            if (chunk == null) continue;
            int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1;
            BlockPos candidate = new BlockPos(x, y, z);
            if (isWithinInitialEndIsland(candidate) && isValidPosition(level, candidate)
                    && existingAnchors.stream().allMatch(anchor -> anchor.distSqr(candidate) >= 64.0D)) {
                return candidate;
            }
        }
        return null;
    }

    private static void discardSpawnCandidate(Human human) {
        if (!human.isRemoved()) human.kill();
    }

    private static double clamp(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
