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

import java.util.EnumMap;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Bounded loaded-block scan. Candidates must expose a face and have a normal path to an adjacent cell. */
public final class LocalResourceScanner {
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
        if (!SurvivalQueryBudget.tryResourceScan(human)) return Optional.empty();
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
        int radius = Config.resourceScanRadius.get();
        BlockPos origin = human.blockPosition();
        EnumMap<SquadNeed, List<BlockPos>> candidates = new EnumMap<>(SquadNeed.class);
        for (BlockPos mutable : BlockPos.betweenClosed(origin.offset(-radius, -Math.min(6, radius), -radius),
                origin.offset(radius, Math.min(6, radius), radius))) {
            if (!human.level().hasChunkAt(mutable)) continue;
            BlockState state = human.level().getBlockState(mutable);
            SquadNeed matchingNeed = null;
            for (SquadNeed need : prioritizedNeeds) {
                if (matches(state, need)) {
                    matchingNeed = need;
                    break;
                }
            }
            boolean hiddenOre = matchingNeed != null && isOreNeed(matchingNeed) && !exposed(human, mutable);
            if (matchingNeed == null || ignored.test(mutable)
                    || (!exposed(human, mutable) && (!hiddenOre || !Config.allowHiddenOreMining.get()))
                    || SurvivalClaimManager.resourceClaimedByOther(human, mutable)
                    || !toolCanHarvest(human, state)) continue;
            candidates.computeIfAbsent(matchingNeed, ignoredNeed -> new java.util.ArrayList<>())
                    .add(mutable.immutable());
        }
        for (SquadNeed need : prioritizedNeeds) {
            List<BlockPos> options = candidates.get(need);
            if (options == null) continue;
            options.sort(Comparator.comparingDouble(pos -> pos.distSqr(origin)));
            // Keep a small fallback set. The nearest block is not necessarily
            // the reachable one (a common cause of apparently idle humans).
            for (BlockPos pos : options.subList(0, Math.min(4, options.size()))) {
                if (!validatePath) return Optional.of(new ResourceTarget(need, pos));
                Optional<BlockPos> interaction = interactionPosition(human, pos);
                // A nearby block can be mined without a walkable cell at the
                // block's own height. This matters for vertical trees and ledges.
                if (withinGatherRange(human, pos)
                        && (Config.allowHiddenOreMining.get() && isOreNeed(need) || exposed(human, pos))
                        || interaction.isPresent() && reachable(human, interaction.get())) {
                    return Optional.of(new ResourceTarget(need, pos));
                }
            }
        }
        return Optional.empty();
    }

    public record ResourceTarget(SquadNeed need, BlockPos pos) {}

    public static boolean exposed(Human human, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = pos.relative(direction);
            if (!human.level().hasChunkAt(adjacent)) continue;
            BlockState state = human.level().getBlockState(adjacent);
            if (state.isAir() || !state.isCollisionShapeFullBlock(human.level(), adjacent)) return true;
        }
        return false;
    }

    public static boolean withinGatherRange(Human human, BlockPos resource) {
        return ProgressiveBlockBreaker.withinReach(human, resource);
    }

    private static boolean reachable(Human human, BlockPos interaction) {
        if (!SurvivalQueryBudget.tryPath(human)) return false;
        var path = human.getNavigation().createPath(interaction, 0);
        return path != null && path.canReach();
    }

    public static Optional<BlockPos> interactionPosition(Human human, BlockPos resource) {
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
                .map(BlockPos::immutable)
                // The mining cell must be selected by its distance to the
                // resource first. Choosing only by distance to the human can
                // leave the human two blocks away from a same-level block,
                // even when an adjacent cell is available.
                .min(Comparator
                        .comparingDouble((BlockPos pos) -> resource.distToCenterSqr(
                                pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D))
                        .thenComparingDouble(pos -> pos.distSqr(origin)));
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
