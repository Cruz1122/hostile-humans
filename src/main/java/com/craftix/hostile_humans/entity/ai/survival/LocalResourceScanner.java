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

import java.util.Comparator;
import java.util.Optional;

/** Bounded loaded-block scan. Candidates must expose a face and have a normal path to an adjacent cell. */
public final class LocalResourceScanner {
    private LocalResourceScanner() {}

    public static Optional<BlockPos> find(Human human, SquadNeed need) {
        int radius = Config.resourceScanRadius.get();
        BlockPos origin = human.blockPosition();
        return BlockPos.betweenClosedStream(origin.offset(-radius, -Math.min(6, radius), -radius),
                        origin.offset(radius, Math.min(6, radius), radius))
                .filter(human.level()::hasChunkAt)
                .filter(pos -> matches(human.level().getBlockState(pos), need))
                .filter(pos -> exposed(human, pos))
                .filter(pos -> !SurvivalClaimManager.resourceClaimedByOther(human, pos))
                .filter(pos -> toolCanHarvest(human, human.level().getBlockState(pos)))
                .filter(pos -> interactionPosition(human, pos).isPresent())
                .map(BlockPos::immutable)
                .min(Comparator.comparingDouble(pos -> pos.distSqr(origin)));
    }

    public static boolean exposed(Human human, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = pos.relative(direction);
            if (!human.level().hasChunkAt(adjacent)) continue;
            BlockState state = human.level().getBlockState(adjacent);
            if (state.isAir() || !state.isCollisionShapeFullBlock(human.level(), adjacent)) return true;
        }
        return false;
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
