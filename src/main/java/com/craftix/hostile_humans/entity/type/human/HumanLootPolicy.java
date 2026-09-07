package com.craftix.hostile_humans.entity.type.human;

import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.entity.ai.action.TacticalTags;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.equipment.MeleeWeaponSelector;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;

/** The small, shared policy for items a human can actually use. */
public final class HumanLootPolicy {
    private HumanLootPolicy() {}

    public static boolean isUseful(Human human, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (MeleeWeaponSelector.isMeleeCandidate(stack)
                || HumanUtil.isRangedWeapon(stack)
                || HumanUtil.isTrident(stack)
                || HumanUtil.isShield(stack)) return true;
        if (human.isFood(stack) || isRawAnimalFood(stack)
                || stack.is(Items.COBWEB) || stack.getItem() instanceof ArrowItem) return true;
        if (stack.getItem() instanceof PotionItem) return true;
        EquipmentSlot equipmentSlot = LivingEntity.getEquipmentSlotForItem(stack);
        if (equipmentSlot.getType() == EquipmentSlot.Type.ARMOR
                || stack.is(Items.TOTEM_OF_UNDYING)) return true;
        if (stack.getItem() instanceof ProjectileWeaponItem || stack.getItem() instanceof TieredItem) return true;
        if (stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS) || stack.is(Items.STICK)
                || stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || stack.is(Items.RAW_IRON)
                || stack.is(Items.RAW_GOLD) || stack.is(Items.IRON_INGOT) || stack.is(Items.GOLD_INGOT)
                || stack.is(Items.DIAMOND) || stack.is(Items.APPLE) || stack.is(Items.COBBLESTONE)
                || stack.is(Items.COBBLED_DEEPSLATE)) return true;
        if (stack.getItem() instanceof BlockItem) {
            return stack.is(TacticalTags.PILLAR_BLOCKS) || stack.is(TacticalTags.BRIDGE_BLOCKS);
        }
        return false;
    }

    private static boolean isRawAnimalFood(ItemStack stack) {
        return stack.is(Items.BEEF) || stack.is(Items.PORKCHOP) || stack.is(Items.CHICKEN)
                || stack.is(Items.MUTTON) || stack.is(Items.RABBIT) || stack.is(Items.COD)
                || stack.is(Items.SALMON);
    }
}
