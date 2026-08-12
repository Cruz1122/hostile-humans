package com.craftix.hostile_humans.entity.ai.action;

import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.Comparator;
import java.util.Optional;

public final class WorldActionSupport {
    private WorldActionSupport() {}

    public static boolean permitted(Human human) {
        return !human.level().isClientSide
                && ForgeEventFactory.getMobGriefingEvent(human.level(), human);
    }

    public static Optional<ItemStack> constructionStack(Human human, boolean bridge) {
        if (human.getData() == null) return Optional.empty();
        return java.util.stream.IntStream.range(0, human.getData().getInventoryItemsSize())
                .mapToObj(slot -> human.getData().getInventoryItem(slot))
                .filter(stack -> validConstructionStack(stack, bridge))
                .sorted(Comparator.comparingInt(ItemStack::getCount).reversed()
                        .thenComparing(stack -> stack.getItem().toString()))
                .findFirst();
    }

    public static boolean validConstructionStack(ItemStack stack, boolean bridge) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) return false;
        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();
        return stack.is(bridge ? TacticalTags.BRIDGE_BLOCKS : TacticalTags.PILLAR_BLOCKS)
                && !stack.is(TacticalTags.FORBIDDEN_CONSTRUCTION_BLOCKS)
                && !(block instanceof FallingBlock)
                && !state.hasBlockEntity()
                && !state.liquid()
                && state.isSolid();
    }

    public static boolean place(Human human, BlockPos pos, ItemStack stack, BlockState state, Direction face) {
        Level level = human.level();
        if (!permitted(human) || !level.hasChunkAt(pos) || !level.getBlockState(pos).canBeReplaced()
                || level.getBlockEntity(pos) != null || !state.canSurvive(level, pos)
                || !level.isUnobstructed(state, pos, CollisionContext.of(human))) return false;
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos, Block.UPDATE_ALL);
        if (ForgeEventFactory.onBlockPlace(human, snapshot, face) || !level.setBlock(pos, state, Block.UPDATE_ALL)) return false;
        stack.shrink(1);
        level.gameEvent(net.minecraft.world.level.gameevent.GameEvent.BLOCK_PLACE, pos,
                net.minecraft.world.level.gameevent.GameEvent.Context.of(human, state));
        level.playSound(null, pos, state.getSoundType(level, pos, human).getPlaceSound(),
                net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 0.9F);
        return true;
    }

    public static Optional<ItemStack> findInventoryStack(Human human, java.util.function.Predicate<ItemStack> predicate) {
        if (human.getData() == null) return Optional.empty();
        return java.util.stream.IntStream.range(0, human.getData().getInventoryItemsSize())
                .mapToObj(slot -> human.getData().getInventoryItem(slot))
                .filter(predicate)
                .findFirst();
    }

    public static boolean critical(Human human) {
        return human.isFleeing || human.healingAfterFleeTicks > 0 || human.isUsingItem()
                || human.isSleepingOrLyingDown() || human.isInLava() || human.isFallFlying()
                || human.isPassenger() || human.isSwimming();
    }
}
