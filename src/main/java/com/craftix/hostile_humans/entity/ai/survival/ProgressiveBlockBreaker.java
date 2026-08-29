package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.entity.ai.action.MiningSpeedCalculator;
import com.craftix.hostile_humans.entity.ai.action.MiningToolSelector;
import com.craftix.hostile_humans.entity.ai.action.WorldActionResult;
import com.craftix.hostile_humans.entity.ai.action.WorldActionSupport;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Shared progressive break executor for navigation and needs-driven gathering. */
public final class ProgressiveBlockBreaker {
    private final Human human;
    private final BlockPos pos;
    private final BlockState expected;
    private final boolean requiresIdle;
    private final boolean requireCorrectTool;
    private float progress;
    private int crackStage = -1;
    private int animationTicks;

    public ProgressiveBlockBreaker(Human human, BlockPos pos) {
        this(human, pos, true, true);
    }

    public ProgressiveBlockBreaker(Human human, BlockPos pos, boolean requiresIdle) {
        this(human, pos, requiresIdle, true);
    }

    public ProgressiveBlockBreaker(Human human, BlockPos pos, boolean requiresIdle, boolean requireCorrectTool) {
        this.human = human;
        this.pos = pos.immutable();
        this.expected = human.level().getBlockState(pos);
        this.requiresIdle = requiresIdle;
        this.requireCorrectTool = requireCorrectTool;
    }

    public WorldActionResult tick() {
        if (!WorldActionSupport.permitted(human) || requiresIdle && human.getTarget() != null
                || human.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 9.0D
                || !human.level().getBlockState(pos).equals(expected)) return abort();
        var selected = MiningToolSelector.select(human, expected);
        if (selected.isEmpty() || !MiningToolSelector.equip(human, selected.get())) return abort();
        ItemStack tool = human.getMainHandItem();
        boolean correct = tool.isCorrectToolForDrops(expected);
        if (requireCorrectTool && expected.requiresCorrectToolForDrops() && !correct) return abort();
        boolean obtainsDrops = correct || !expected.requiresCorrectToolForDrops();
        // Re-trigger at roughly a player's visible mining cadence. LivingEntity
        // still owns the swing animation and rejects impossible per-tick resets.
        if (animationTicks++ % 4 == 0) human.swing(InteractionHand.MAIN_HAND);
        progress += MiningSpeedCalculator.progressPerTick(human, expected, pos, tool, obtainsDrops);
        int nextStage = Math.min(9, (int) (progress * 10.0F));
        if (nextStage != crackStage) {
            human.level().destroyBlockProgress(human.getId(), pos, nextStage);
            crackStage = nextStage;
        }
        if (progress < 1.0F) return WorldActionResult.RUNNING;
        human.level().destroyBlockProgress(human.getId(), pos, -1);
        BlockEntity blockEntity = human.level().getBlockEntity(pos);
        ItemStack lootTool = tool.copy();
        human.suppressSurvivalSounds(2);
        tool.getItem().mineBlock(tool, human.level(), expected, pos, human);
        if (!human.level().destroyBlock(pos, false, human, Block.UPDATE_LIMIT)) return abort();
        if (human.level() instanceof ServerLevel serverLevel && obtainsDrops) {
            // Keep vanilla-calculated loot, but eject it beside the miner with
            // no pickup delay. Spawning at the old block cell made both exposed
            // and hidden drops vulnerable to failed paths and action switching.
            List<ItemStack> drops = Block.getDrops(expected, serverLevel, pos, blockEntity, human, lootTool);
            for (ItemStack drop : drops) {
                ItemEntity item = new ItemEntity(serverLevel, human.getX(), human.getY() + 0.2D, human.getZ(), drop);
                item.setPickUpDelay(0);
                serverLevel.addFreshEntity(item);
            }
            expected.spawnAfterBreak(serverLevel, pos, lootTool, true);
        }
        return WorldActionResult.SUCCESS;
    }

    public WorldActionResult abort() {
        human.level().destroyBlockProgress(human.getId(), pos, -1);
        return WorldActionResult.ABORTED;
    }
}
