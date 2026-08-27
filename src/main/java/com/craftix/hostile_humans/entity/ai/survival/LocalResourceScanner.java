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
        int radius = Config.resourceScanRadius.get();
        BlockPos origin = human.blockPosition();
        EnumMap<SquadNeed, BlockPos> nearest = new EnumMap<>(SquadNeed.class);
        EnumMap<SquadNeed, Double> distances = new EnumMap<>(SquadNeed.class);
        for (BlockPos mutable : BlockPos.betweenClosed(origin.offset(-radius, -Math.min(6, radius), -radius),
                origin.offset(radius, Math.min(6, radius), radius))) {
            if (!human.level().hasChunkAt(mutable)) continue;
            BlockState state = human.level().getBlockState(mutable);
            SquadNeed matchingNeed = null;
            for (SquadNeed need : needs) {
                if (matches(state, need)) {
                    matchingNeed = need;
                    break;
                }
            }
            if (matchingNeed == null || ignored.test(mutable) || !exposed(human, mutable)
                    || SurvivalClaimManager.resourceClaimedByOther(human, mutable)
                    || !toolCanHarvest(human, state)) continue;
            double distance = mutable.distSqr(origin);
            if (distance < distances.getOrDefault(matchingNeed, Double.MAX_VALUE)) {
                nearest.put(matchingNeed, mutable.immutable());
                distances.put(matchingNeed, distance);
            }
        }
        for (SquadNeed need : needs) {
            BlockPos pos = nearest.get(need);
            if (pos == null) continue;
            if (!validatePath) return Optional.of(new ResourceTarget(need, pos));
            Optional<BlockPos> interaction = interactionPosition(human, pos);
            if (interaction.isEmpty()) continue;
            if (withinGatherRange(human, pos) || reachable(human, interaction.get())) {
                return Optional.of(new ResourceTarget(need, pos));
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
        return human.distanceToSqr(resource.getX() + 0.5D, resource.getY() + 0.5D, resource.getZ() + 0.5D) <= 9.0D;
    }

    private static boolean reachable(Human human, BlockPos interaction) {
        var path = human.getNavigation().createPath(interaction, 0);
        return path != null && path.canReach();
    }

    public static Optional<BlockPos> interactionPosition(Human human, BlockPos resource) {
        return java.util.Arrays.stream(Direction.values())
                .filter(direction -> direction.getAxis().isHorizontal())
                .map(resource::relative)
                .filter(human.level()::hasChunkAt)
                .filter(pos -> human.level().getBlockState(pos).getCollisionShape(human.level(), pos).isEmpty())
                .filter(pos -> human.level().getBlockState(pos.above()).getCollisionShape(human.level(), pos.above()).isEmpty())
                .filter(pos -> human.level().getBlockState(pos.below()).isSolidRender(human.level(), pos.below()))
                .findFirst();
    }

    public static boolean matches(BlockState state, SquadNeed need) {
        return switch (need) {
            case WOOD -> state.is(BlockTags.LOGS);
            case APPLES -> state.getBlock() instanceof LeavesBlock;
            case STONE -> state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.DEEPSLATE);
            case FUEL -> state.is(BlockTags.COAL_ORES);
            case IRON -> state.is(BlockTags.IRON_ORES);
            case GOLD -> state.is(BlockTags.GOLD_ORES);
            case DIAMOND -> state.is(BlockTags.DIAMOND_ORES);
            case FLINT -> state.is(Blocks.GRAVEL);
            default -> false;
        };
    }

    private static boolean toolCanHarvest(Human human, BlockState state) {
        return MiningToolSelector.select(human, state).isPresent();
    }
}
