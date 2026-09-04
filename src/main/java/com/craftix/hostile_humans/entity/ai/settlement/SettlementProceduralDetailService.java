package com.craftix.hostile_humans.entity.ai.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BiomeTags;

/** Adds small, seed-driven modules after the main template has been placed. */
public final class SettlementProceduralDetailService {
    private SettlementProceduralDetailService() {
    }

    public static void decorate(ServerLevel level, BlockPos center, BlockPos min, BlockPos max,
                                SettlementSize size, RandomSource random) {
        int radius = Math.max(4, Math.min(size.maximumFootprint() / 2, 10));
        int details = switch (size) {
            case SMALL -> 2;
            case MEDIUM -> 4;
            case LARGE -> 7;
        };
        for (int i = 0; i < details; i++) {
            int x = center.getX() + random.nextInt(radius * 2 + 1) - radius;
            int z = center.getZ() + random.nextInt(radius * 2 + 1) - radius;
            BlockPos ground = findGround(level, new BlockPos(x, center.getY() + 8, z), min.getY() - 4, max.getY() + 8);
            if (ground == null || !inside(ground, min, max)) continue;
            placeDetail(level, ground, random.nextInt(4));
        }

        // Architectural modules are deliberately independent from the starter
        // template. Each module gets its own ground probe and can be omitted
        // without invalidating the camp core.
        int modules = switch (size) {
            case SMALL -> 2;
            case MEDIUM -> 4;
            case LARGE -> 7;
        };
        for (int i = 0; i < modules; i++) {
            int x = center.getX() + random.nextInt(radius * 2 + 1) - radius;
            int z = center.getZ() + random.nextInt(radius * 2 + 1) - radius;
            BlockPos anchor = findGround(level, new BlockPos(x, center.getY() + 8, z), min.getY() - 4, max.getY() + 8);
            if (anchor == null || !inside(anchor, min, max)) continue;
            switch (random.nextInt(4)) {
                case 0 -> {
                    // A shelter is a 3x3 architectural module. It is only valid
                    // when every support block is on the same solid surface.
                    if (fitsFootprint(level, anchor, 3, 3, 0, min, max)) {
                        placeShelter(level, anchor, min, max, wood(level, center), roof(level, center));
                    }
                }
                case 1 -> {
                    if (fitsFootprint(level, anchor, 3, 3, 1, min, max)) {
                        placeWatchtower(level, anchor, min, max, wood(level, center));
                    }
                }
                case 2 -> placePerimeter(level, anchor, min, max);
                default -> placePath(level, center, anchor, min, max);
            }
        }
    }

    private static BlockPos findGround(ServerLevel level, BlockPos start, int minimumY, int maximumY) {
        BlockPos cursor = start;
        for (int y = Math.min(start.getY(), maximumY); y >= minimumY; y--) {
            cursor = new BlockPos(start.getX(), y, start.getZ());
            if (level.getBlockState(cursor).isAir() && !level.getBlockState(cursor.below()).isAir()) return cursor;
        }
        return null;
    }

    private static boolean inside(BlockPos position, BlockPos min, BlockPos max) {
        return position.getX() >= min.getX() && position.getX() <= max.getX()
                && position.getZ() >= min.getZ() && position.getZ() <= max.getZ();
    }

    private static void placeDetail(ServerLevel level, BlockPos position, int variant) {
        if (!level.isEmptyBlock(position)) return;
        switch (variant) {
            case 0 -> level.setBlock(position, Blocks.COBBLESTONE_WALL.defaultBlockState(), 3);
            case 1 -> level.setBlock(position, Blocks.LANTERN.defaultBlockState(), 3);
            case 2 -> {
                level.setBlock(position, Blocks.OAK_FENCE.defaultBlockState(), 3);
                if (level.isEmptyBlock(position.above())) level.setBlock(position.above(), Blocks.OAK_FENCE.defaultBlockState(), 3);
            }
            default -> level.setBlock(position, Blocks.CAMPFIRE.defaultBlockState(), 3);
        }
    }

    private static Block wood(ServerLevel level, BlockPos center) {
        if (isDesert(level, center)) return Blocks.CUT_SANDSTONE;
        if (level.getBiome(center).is(BiomeTags.IS_TAIGA)) return Blocks.SPRUCE_LOG;
        if (level.getBiome(center).is(BiomeTags.IS_SAVANNA)) return Blocks.ACACIA_LOG;
        return Blocks.OAK_LOG;
    }

    private static Block roof(ServerLevel level, BlockPos center) {
        if (isDesert(level, center)) return Blocks.CUT_SANDSTONE_SLAB;
        if (level.getBiome(center).is(BiomeTags.IS_SAVANNA)) return Blocks.ACACIA_SLAB;
        return Blocks.SPRUCE_SLAB;
    }

    private static boolean isDesert(ServerLevel level, BlockPos center) {
        return level.getBiome(center).unwrapKey()
                .map(key -> key.location().getPath().contains("desert"))
                .orElse(false);
    }

    private static boolean canPlace(ServerLevel level, BlockPos position) {
        return level.getBlockState(position).isAir() || level.getBlockState(position).canBeReplaced();
    }

    /**
     * Validates an individual module footprint before placing any of its blocks.
     * The tolerance is measured in surface blocks, not world Y distance guessed
     * from the anchor, so slopes cannot leave part of a module unsupported.
     */
    private static boolean fitsFootprint(ServerLevel level, BlockPos anchor, int width, int depth,
                                         int heightTolerance, BlockPos min, BlockPos max) {
        int halfWidth = width / 2;
        int halfDepth = depth / 2;
        int minimumY = Integer.MAX_VALUE;
        int maximumY = Integer.MIN_VALUE;
        for (int x = -halfWidth; x <= halfWidth; x++) {
            for (int z = -halfDepth; z <= halfDepth; z++) {
                BlockPos probe = anchor.offset(x, 0, z);
                if (!inside(probe, min, max)) return false;
                BlockPos surface = findGround(level, probe.above(8), min.getY() - 4, max.getY() + 8);
                if (surface == null || !level.getFluidState(surface.below()).isEmpty()
                        || !level.getBlockState(surface.below()).isFaceSturdy(level, surface.below(), net.minecraft.core.Direction.UP)) {
                    return false;
                }
                minimumY = Math.min(minimumY, surface.getY());
                maximumY = Math.max(maximumY, surface.getY());
            }
        }
        return maximumY - minimumY <= heightTolerance;
    }

    private static void setIfEmpty(ServerLevel level, BlockPos position, BlockState state) {
        if (canPlace(level, position)) level.setBlock(position, state, 3);
    }

    private static void placeShelter(ServerLevel level, BlockPos anchor, BlockPos min, BlockPos max,
                                     Block frame, Block roof) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos pos = anchor.offset(x, 0, z);
            if (inside(pos, min, max)) setIfEmpty(level, pos, Blocks.OAK_PLANKS.defaultBlockState());
        }
        for (int x : new int[]{-1, 1}) for (int z : new int[]{-1, 1}) {
            for (int y = 1; y <= 3; y++) {
                BlockPos pos = anchor.offset(x, y, z);
                if (inside(pos, min, max)) setIfEmpty(level, pos, frame.defaultBlockState());
            }
        }
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos pos = anchor.offset(x, 4, z);
            if (inside(pos, min, max)) setIfEmpty(level, pos, roof.defaultBlockState());
        }
    }

    private static void placeWatchtower(ServerLevel level, BlockPos anchor, BlockPos min, BlockPos max, Block frame) {
        for (int x : new int[]{-1, 1}) for (int z : new int[]{-1, 1}) {
            for (int y = 0; y <= 4; y++) {
                BlockPos pos = anchor.offset(x, y, z);
                if (inside(pos, min, max)) setIfEmpty(level, pos, frame.defaultBlockState());
            }
        }
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos pos = anchor.offset(x, 5, z);
            if (inside(pos, min, max)) setIfEmpty(level, pos, Blocks.SPRUCE_SLAB.defaultBlockState());
        }
    }

    private static void placePerimeter(ServerLevel level, BlockPos anchor, BlockPos min, BlockPos max) {
        for (int i = -4; i <= 4; i += 2) {
            for (BlockPos pos : new BlockPos[]{anchor.offset(i, 0, -4), anchor.offset(i, 0, 4),
                    anchor.offset(-4, 0, i), anchor.offset(4, 0, i)}) {
                if (inside(pos, min, max)) setIfEmpty(level, pos, Blocks.OAK_FENCE.defaultBlockState());
            }
        }
    }

    private static void placePath(ServerLevel level, BlockPos from, BlockPos to, BlockPos min, BlockPos max) {
        int steps = Math.max(Math.abs(to.getX() - from.getX()), Math.abs(to.getZ() - from.getZ()));
        if (steps == 0) return;
        for (int i = 1; i < steps; i++) {
            double ratio = (double) i / steps;
            int x = (int) Math.round(from.getX() + (to.getX() - from.getX()) * ratio);
            int z = (int) Math.round(from.getZ() + (to.getZ() - from.getZ()) * ratio);
            BlockPos ground = findGround(level, new BlockPos(x, Math.max(from.getY(), to.getY()) + 8, z), min.getY() - 4, max.getY() + 8);
            if (ground != null && inside(ground, min, max)) setIfEmpty(level, ground, Blocks.DIRT_PATH.defaultBlockState());
        }
    }
}
