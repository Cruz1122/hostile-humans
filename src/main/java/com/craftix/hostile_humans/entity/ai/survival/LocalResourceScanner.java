package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.ai.action.MiningToolSelector;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.EnumMap;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/** Bounded loaded-block scan. Candidates must expose a face and have a normal path to an adjacent cell. */
public final class LocalResourceScanner {
    private static final int CANDIDATE_CACHE_TTL_TICKS = 30;
    private static final int MAX_CACHED_ENTRIES = 512;
    private static final int MAX_CACHED_CANDIDATES_PER_NEED = 64;
    private static final Map<CacheKey, CandidateCache> CANDIDATE_CACHE = new HashMap<>();

    private LocalResourceScanner() {}

    public static Optional<BlockPos> find(Human human, SquadNeed need) {
        return findFirst(human, List.of(need)).map(ResourceTarget::pos);
    }

    /** Scans the bounded volume once and returns the nearest resource for the first satisfiable need. */
    public static Optional<ResourceTarget> findFirst(Human human, List<SquadNeed> needs) {
        return findFirst(human, needs, pos -> false, false);
    }

    public static Optional<ResourceTarget> findFirst(Human human, List<SquadNeed> needs, Predicate<BlockPos> ignored) {
        return findFirst(human, needs, ignored, true);
    }

    public static Optional<ResourceTarget> findReachableFirst(Human human, List<SquadNeed> needs, Predicate<BlockPos> ignored) {
        return findFirst(human, needs, ignored, true);
    }

    private static Optional<ResourceTarget> findFirst(Human human, List<SquadNeed> needs,
                                                       Predicate<BlockPos> ignored, boolean validatePath) {
        if (needs.isEmpty()) return Optional.empty();
        pruneExpired(human.level().getGameTime());
        // Wood/stone must be completed before chasing ore. Keep the
        // vanilla priority WOOD -> STONE -> FUEL -> IRON for early game.
        List<SquadNeed> prioritizedNeeds = needs.stream()
                .sorted(Comparator.comparingInt(need -> switch (need) {
                    case WOOD -> 0;
                    case STONE -> 1;
                    case FUEL -> 2;
                    case IRON -> 3;
                    case GOLD -> 4;
                    case DIAMOND -> 5;
                    default -> 6;
                }))
                .toList();
        BlockPos origin = human.blockPosition();
        CacheKey cacheKey = new CacheKey(human.level().dimension(), origin.getX() >> 4, origin.getY(),
                origin.getZ() >> 4, prioritizedNeeds, Config.resourceScanRadius.get(),
                Config.allowHiddenOreMining.get());
        CandidateCache cache = CANDIDATE_CACHE.get(cacheKey);
        boolean cacheUsable = cache != null && cache.expiresAt > human.level().getGameTime();
        EnumMap<SquadNeed, List<BlockPos>> candidates;
        if (cacheUsable) {
            candidates = cache.candidates;
        } else {
            if (!SurvivalQueryBudget.tryResourceScan(human)) return Optional.empty();
            candidates = scanCandidates(human, prioritizedNeeds, origin);
            trimCandidates(candidates, new BlockPos((origin.getX() >> 4) * 16 + 8, origin.getY(),
                    (origin.getZ() >> 4) * 16 + 8));
            CANDIDATE_CACHE.put(cacheKey, new CandidateCache(
                    human.level().getGameTime() + CANDIDATE_CACHE_TTL_TICKS, candidates));
            pruneExpired(human.level().getGameTime());
        }
        return selectCandidate(human, prioritizedNeeds, ignored, validatePath, candidates, origin);
    }

    private static EnumMap<SquadNeed, List<BlockPos>> scanCandidates(Human human, List<SquadNeed> prioritizedNeeds,
                                                                       BlockPos origin) {
        int radius = Config.resourceScanRadius.get() + 8;
        BlockPos scanOrigin = new BlockPos((origin.getX() >> 4) * 16 + 8, origin.getY(),
                (origin.getZ() >> 4) * 16 + 8);
        EnumMap<SquadNeed, List<BlockPos>> candidates = new EnumMap<>(SquadNeed.class);
        for (BlockPos mutable : BlockPos.betweenClosed(scanOrigin.offset(-radius, -Math.min(6, radius), -radius),
                scanOrigin.offset(radius, Math.min(6, radius), radius))) {
            // Never turn a survival scan into synchronous chunk generation.
            // GoalSelector runs on the Server thread; getBlockState on an
            // unloaded chunk can join chunk generation and freeze every NPC.
            if (human.level().getChunkSource().getChunkNow(mutable.getX() >> 4, mutable.getZ() >> 4) == null) {
                continue;
            }
            BlockState state = human.level().getBlockState(mutable);
            SquadNeed matchingNeed = null;
            for (SquadNeed need : prioritizedNeeds) {
                if (matches(state, need)) {
                    matchingNeed = need;
                    break;
                }
            }
            boolean hiddenOre = matchingNeed != null && isOreNeed(matchingNeed) && !exposed(human, mutable);
            if (matchingNeed == null
                    || (matchingNeed != SquadNeed.WOOD
                    && !exposed(human, mutable) && (!hiddenOre || !Config.allowHiddenOreMining.get()))
                    ) continue;
            candidates.computeIfAbsent(matchingNeed, ignoredNeed -> new java.util.ArrayList<>())
                    .add(mutable.immutable());
        }
        return candidates;
    }

    private static void trimCandidates(EnumMap<SquadNeed, List<BlockPos>> candidates, BlockPos origin) {
        for (Map.Entry<SquadNeed, List<BlockPos>> entry : candidates.entrySet()) {
            List<BlockPos> options = entry.getValue();
            options.sort(Comparator.comparingDouble(pos -> pos.distSqr(origin)));
            if (options.size() > MAX_CACHED_CANDIDATES_PER_NEED) {
                entry.setValue(List.copyOf(options.subList(0, MAX_CACHED_CANDIDATES_PER_NEED)));
            }
        }
    }

    private static Optional<ResourceTarget> selectCandidate(Human human, List<SquadNeed> prioritizedNeeds,
                                                              Predicate<BlockPos> ignored, boolean validatePath,
                                                              EnumMap<SquadNeed, List<BlockPos>> candidates,
                                                              BlockPos origin) {
        for (SquadNeed need : prioritizedNeeds) {
            List<BlockPos> options = candidates.get(need);
            if (options == null) continue;
            List<BlockPos> validOptions = options.stream()
                    .filter(pos -> isCurrentCandidate(human, need, pos, ignored))
                    .sorted(Comparator.comparingDouble(pos -> pos.distSqr(origin)))
                    .toList();
            // Keep a small fallback set. The nearest block is not necessarily
            // the reachable one (a common cause of apparently idle humans).
            int fallbackLimit = need == SquadNeed.WOOD ? validOptions.size() : Math.min(4, validOptions.size());
            for (BlockPos pos : validOptions.subList(0, fallbackLimit)) {
                // Candidate lists are intentionally cached, but their world
                // state, claim and tool validity are never trusted across
                // ticks. This keeps the cache an acceleration only, not a
                // source of stale mining decisions.
                if (!validatePath) return Optional.of(new ResourceTarget(need, pos));
                Optional<BlockPos> interaction = interactionPosition(human, pos, need == SquadNeed.WOOD);
                // A nearby block can be mined without a walkable cell at the
                // block's own height. This matters for vertical trees and ledges.
                if (withinGatherRange(human, pos)
                        && (Config.allowHiddenOreMining.get() && isOreNeed(need) || exposed(human, pos))
                        || interaction.isPresent() && reachable(human, pos, need == SquadNeed.WOOD)) {
                    return Optional.of(new ResourceTarget(need, pos));
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isCurrentCandidate(Human human, SquadNeed need, BlockPos pos,
                                               Predicate<BlockPos> ignored) {
        if (ignored.test(pos)
                || human.level().getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) {
            return false;
        }
        BlockState state = human.level().getBlockState(pos);
        boolean hiddenOre = isOreNeed(need) && !exposed(human, pos);
        return matches(state, need)
                && (need == SquadNeed.WOOD || exposed(human, pos)
                || hiddenOre && Config.allowHiddenOreMining.get())
                && !SurvivalClaimManager.resourceClaimedByOther(human, pos)
                && toolCanHarvest(human, state);
    }

    public static void invalidate(Human human) {
        CANDIDATE_CACHE.entrySet().removeIf(entry -> entry.getKey().dimension.equals(human.level().dimension()));
    }

    private static void pruneExpired(long now) {
        CANDIDATE_CACHE.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        while (CANDIDATE_CACHE.size() > MAX_CACHED_ENTRIES) {
            CacheKey oldestKey = null;
            long oldestExpiry = Long.MAX_VALUE;
            for (Map.Entry<CacheKey, CandidateCache> entry : CANDIDATE_CACHE.entrySet()) {
                if (entry.getValue().expiresAt < oldestExpiry) {
                    oldestExpiry = entry.getValue().expiresAt;
                    oldestKey = entry.getKey();
                }
            }
            if (oldestKey == null) break;
            CANDIDATE_CACHE.remove(oldestKey);
        }
    }

    private record CacheKey(ResourceKey<Level> dimension, int regionX, int sectionY, int regionZ,
                            List<SquadNeed> needs, int radius, boolean allowHiddenOreMining) {}

    private static final class CandidateCache {
        private final long expiresAt;
        private final EnumMap<SquadNeed, List<BlockPos>> candidates;

        private CandidateCache(long expiresAt, EnumMap<SquadNeed, List<BlockPos>> candidates) {
            this.expiresAt = expiresAt;
            this.candidates = candidates;
        }
    }

    public record ResourceTarget(SquadNeed need, BlockPos pos) {}

    public static boolean exposed(Human human, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = pos.relative(direction);
            if (human.level().getChunkSource().getChunkNow(adjacent.getX() >> 4, adjacent.getZ() >> 4) == null) {
                continue;
            }
            BlockState state = human.level().getBlockState(adjacent);
            if (state.isAir() || !state.isCollisionShapeFullBlock(human.level(), adjacent)) return true;
        }
        return false;
    }

    public static boolean withinGatherRange(Human human, BlockPos resource) {
        return ProgressiveBlockBreaker.withinReach(human, resource);
    }

    private static boolean reachable(Human human, BlockPos resource, boolean allowOccluded) {
        return SurvivalPathing.createPath(human, interactionPositions(human, resource, allowOccluded), 0).isPresent();
    }

    public static Optional<BlockPos> interactionPosition(Human human, BlockPos resource) {
        return interactionPosition(human, resource, false);
    }

    public static Optional<BlockPos> interactionPosition(Human human, BlockPos resource, boolean allowOccluded) {
        return interactionPositions(human, resource, allowOccluded).stream().findFirst();
    }

    /** Returns supported, empty cells from which the resource can be reached. */
    public static List<BlockPos> interactionPositions(Human human, BlockPos resource) {
        return interactionPositions(human, resource, false);
    }

    public static List<BlockPos> interactionPositions(Human human, BlockPos resource, boolean allowOccluded) {
        BlockPos origin = human.blockPosition();
        return BlockPos.betweenClosedStream(resource.offset(-2, -2, -2), resource.offset(2, 3, 2))
                .filter(human.level()::hasChunkAt)
                .filter(pos -> human.level().getBlockState(pos).getCollisionShape(human.level(), pos).isEmpty())
                .filter(pos -> human.level().getBlockState(pos.above()).getCollisionShape(human.level(), pos.above()).isEmpty())
                // A mining target can be elevated, but the human still needs
                // a real supporting block below its feet. Do not select an
                // empty cell above the resource or a floating air cell just
                // because it is close to the current position.
                .filter(pos -> pos.getY() <= resource.getY())
                .filter(pos -> !human.level().getBlockState(pos.below())
                        .getCollisionShape(human.level(), pos.below()).isEmpty())
                // Elevated resources can still be approached from a lower
                // supported cell; pathfinding validates the complete route.
                .filter(pos -> ProgressiveBlockBreaker.withinReachFrom(human, pos, resource))
                .filter(pos -> allowOccluded || ProgressiveBlockBreaker.hasLineOfSightFrom(human, pos, resource))
                .map(BlockPos::immutable)
                // The mining cell must be selected by its distance to the
                // resource first. Choosing only by distance to the human can
                // leave the human two blocks away from a same-level block,
                // even when an adjacent cell is available.
                .sorted(Comparator
                        .comparingDouble((BlockPos pos) -> resource.distToCenterSqr(
                        pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D))
                        .thenComparingDouble(pos -> pos.distSqr(origin)))
                .toList();
    }

    public static boolean matches(BlockState state, SquadNeed need) {
        return switch (need) {
            case WOOD -> state.is(BlockTags.LOGS);
            case APPLES -> state.getBlock() instanceof LeavesBlock;
            case STONE -> state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE)
                    || state.is(Blocks.DEEPSLATE) || state.is(Blocks.COBBLED_DEEPSLATE);
            case FUEL -> state.is(BlockTags.COAL_ORES);
            case IRON -> state.is(BlockTags.IRON_ORES);
            case GOLD -> state.is(BlockTags.GOLD_ORES);
            case DIAMOND -> state.is(BlockTags.DIAMOND_ORES);
            default -> false;
        };
    }

    private static boolean toolCanHarvest(Human human, BlockState state) {
        return MiningToolSelector.select(human, state).isPresent();
    }

    private static boolean isOreNeed(SquadNeed need) {
        return need == SquadNeed.FUEL || need == SquadNeed.IRON || need == SquadNeed.GOLD || need == SquadNeed.DIAMOND;
    }
}
