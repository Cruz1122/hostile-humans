package com.craftix.hostile_humans.entity.type.human;

import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.entity.ai.action.TacticalTags;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.equipment.MeleeWeaponSelector;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Items;

/** The small, shared policy for items a human can actually use. */
public final class HumanLootPolicy {
    private HumanLootPolicy() {}

    public static boolean isUseful(Human human, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (MeleeWeaponSelector.isMeleeCandidate(stack)
                || HumanUtil.isRangedWeapon(stack)
                || HumanUtil.isTrident(stack)
                || HumanUtil.isShield(stack)) return true;
        if (human.isFood(stack) || stack.is(Items.COBWEB) || stack.getItem() instanceof ArrowItem) return true;
        if (stack.getItem() instanceof ProjectileWeaponItem || stack.getItem() instanceof TieredItem) return true;
        if (stack.getItem() instanceof BlockItem) {
            return stack.is(TacticalTags.PILLAR_BLOCKS) || stack.is(TacticalTags.BRIDGE_BLOCKS);
        }
        return false;
    }
}
