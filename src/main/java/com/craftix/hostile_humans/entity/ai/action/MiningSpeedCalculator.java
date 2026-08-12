package com.craftix.hostile_humans.entity.ai.action;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

public final class MiningSpeedCalculator {
    private MiningSpeedCalculator() {}

    public static float progressPerTick(Human human, BlockState state, BlockPos pos, ItemStack tool, boolean correctTool) {
        float speed = tool.getDestroySpeed(state);
        int efficiency = EnchantmentHelper.getBlockEfficiency(human);
        if (speed > 1.0F && efficiency > 0 && !tool.isEmpty()) speed += efficiency * efficiency + 1;
        if (MobEffectUtil.hasDigSpeed(human)) speed *= 1.0F + 0.2F * (MobEffectUtil.getDigSpeedAmplification(human) + 1);
        MobEffectInstance fatigue = human.getEffect(MobEffects.DIG_SLOWDOWN);
        if (fatigue != null) speed *= switch (Math.min(fatigue.getAmplifier(), 3)) {
            case 0 -> 0.3F; case 1 -> 0.09F; case 2 -> 0.0027F; default -> 0.00081F;
        };
        if (human.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) speed /= 5.0F;
        if (!human.onGround()) speed /= 5.0F;
        float hardness = state.getDestroySpeed(human.level(), pos);
        if (hardness < 0.0F) return 0.0F;
        return Math.max(0.0F, speed / hardness / (correctTool ? 30.0F : 100.0F)
                * Config.miningSpeedMultiplier.get().floatValue());
    }
}
