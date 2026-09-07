package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.equipment.MeleeWeaponSelector;
import com.craftix.hostile_humans.entity.type.human.HumanLootPolicy;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Single authoritative path for transferring a world item into survival storage. */
public final class LootCollector {
    private LootCollector() {}

    public static int collect(Human human, ItemEntity entity) {
        if (entity == null || !entity.isAlive() || entity.hasPickUpDelay()) return 0;
        ItemStack stack = entity.getItem();
        if (stack.isEmpty() || !HumanLootPolicy.isUseful(human, stack)) return 0;
        boolean swapMainhand = !SurvivalInventory.canStore(human, stack)
                && !human.getMainHandItem().isEmpty()
                && MeleeWeaponSelector.isBetterMelee(stack, human.getMainHandItem(),
                human.getTarget() == null ? net.minecraft.world.entity.MobType.UNDEFINED : human.getTarget().getMobType());
        if (!SurvivalInventory.canStore(human, stack) && !swapMainhand) return 0;

        ItemStack previousMainhand = swapMainhand ? human.getMainHandItem().copy() : ItemStack.EMPTY;
        if (swapMainhand) human.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        int inserted = SurvivalInventory.insert(human, stack);
        if (inserted <= 0 && !swapMainhand
                && !human.getMainHandItem().isEmpty()
                && MeleeWeaponSelector.isBetterMelee(stack, human.getMainHandItem(),
                human.getTarget() == null ? net.minecraft.world.entity.MobType.UNDEFINED : human.getTarget().getMobType())) {
            swapMainhand = true;
            previousMainhand = human.getMainHandItem().copy();
            human.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            inserted = SurvivalInventory.insert(human, stack);
        }
        if (inserted <= 0) {
            if (swapMainhand) human.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, previousMainhand);
            return 0;
        }
        if (swapMainhand && !previousMainhand.isEmpty()) human.spawnAtLocation(previousMainhand);
        if (stack.isEmpty()) entity.discard();
        else entity.setItem(stack);
        human.markEquipmentDirty();
        human.queueUsefulInventoryEquipment();
        human.queueEquipmentReevaluation();
        SquadNeedsEvaluator.invalidate(human);
        LocalResourceScanner.invalidate(human);
        return inserted;
    }

    /** Includes the combat-only hand replacement path used by ItemLootGoal. */
    public static boolean canCollect(Human human, ItemStack stack) {
        return HumanLootPolicy.isUseful(human, stack)
                && (SurvivalInventory.canStore(human, stack)
                || !human.getMainHandItem().isEmpty()
                && MeleeWeaponSelector.isBetterMelee(stack, human.getMainHandItem(),
                human.getTarget() == null ? net.minecraft.world.entity.MobType.UNDEFINED : human.getTarget().getMobType()));
    }
}
