package com.craftix.hostile_humans.entity.ai.camp;

import com.craftix.hostile_humans.entity.ai.survival.SurvivalInventory;
import com.craftix.hostile_humans.entity.ai.survival.SurvivalClaimManager;
import com.craftix.hostile_humans.entity.data.HumanData;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.type.human.HumanLootPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.function.Predicate;

/** Conservative transfers between real Human inventories and real camp chests. */
public final class CampStorageService {
    private CampStorageService() {}

    public static boolean depositExcess(Human human, Camp camp) {
        BlockPos storagePos = firstStorage(human, camp);
        if (!SurvivalClaimManager.claimStation(human, storagePos)
                || !(human.level().getBlockEntity(storagePos) instanceof ChestBlockEntity chest)
                || human.getData() == null) return false;
        boolean changed = false;
        HumanData data = human.getData();
        for (int slot = 0; slot < data.getInventoryItemsSize(); slot++) {
            ItemStack source = data.getInventoryItem(slot);
            if (source.isEmpty() || !isExcess(human, source)) continue;
            ItemStack moving = source.copy();
            int moved = insert(chest, moving);
            if (moved > 0) {
                source.shrink(moved);
                data.setInventoryItem(slot, source.copy());
                chest.setChanged();
                changed = true;
            }
        }
        return changed;
    }

    public static boolean withdrawNeeded(Human human, Camp camp, Predicate<ItemStack> wanted, int maximum) {
        BlockPos storagePos = firstStorage(human, camp);
        if (!SurvivalClaimManager.claimStation(human, storagePos)
                || !(human.level().getBlockEntity(storagePos) instanceof ChestBlockEntity chest)
                || human.getData() == null) return false;
        int remaining = Math.max(0, maximum);
        boolean changed = false;
        for (int slot = 0; slot < chest.getContainerSize() && remaining > 0; slot++) {
            ItemStack source = chest.getItem(slot);
            if (source.isEmpty() || !wanted.test(source)) continue;
            int amount = Math.min(remaining, source.getCount());
            ItemStack moving = source.copyWithCount(amount);
            int moved = SurvivalInventory.insert(human, moving);
            if (moved > 0) {
                source.shrink(moved);
                chest.setItem(slot, source);
                remaining -= moved;
                changed = true;
            }
        }
        if (changed) chest.setChanged();
        return changed;
    }

    public static boolean hasUsefulItem(Camp camp, Human human) {
        if (!(human.level().getBlockEntity(firstStorage(human, camp)) instanceof ChestBlockEntity chest)) return false;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (HumanLootPolicy.isUseful(human, chest.getItem(i))) return true;
        }
        return false;
    }

    public static BlockPos firstStorage(Human human, Camp camp) {
        for (BlockPos pos : camp.storagePositions()) if (human.level().hasChunkAt(pos)) return pos;
        return camp.storagePositions().isEmpty() ? camp.center() : camp.storagePositions().get(0);
    }

    private static int insert(ChestBlockEntity chest, ItemStack offered) {
        int before = offered.getCount();
        for (int slot = 0; slot < chest.getContainerSize() && !offered.isEmpty(); slot++) {
            ItemStack existing = chest.getItem(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, offered)) {
                int moved = Math.min(offered.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(Math.max(0, moved));
                offered.shrink(Math.max(0, moved));
            }
        }
        for (int slot = 0; slot < chest.getContainerSize() && !offered.isEmpty(); slot++) {
            if (chest.getItem(slot).isEmpty()) {
                int moved = Math.min(offered.getCount(), offered.getMaxStackSize());
                chest.setItem(slot, offered.copyWithCount(moved));
                offered.shrink(moved);
            }
        }
        return before - offered.getCount();
    }

    private static boolean isExcess(Human human, ItemStack stack) {
        if (!HumanLootPolicy.isUseful(human, stack)) return false;
        if (stack.is(Items.WATER_BUCKET) || stack.is(Items.TOTEM_OF_UNDYING)
                || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return false;
        if (stack.getItem() instanceof ArrowItem) return SurvivalInventory.count(human, s -> s.getItem() instanceof ArrowItem) > 16;
        if (human.isFood(stack)) return SurvivalInventory.count(human, s -> human.isFood(s)) > 8;
        if (stack.getItem() instanceof TieredItem || stack.getItem() instanceof ArmorItem) return duplicateGear(human, stack);
        return true;
    }

    private static boolean duplicateGear(Human human, ItemStack candidate) {
        if (candidate.getItem() instanceof ArmorItem armor) {
            EquipmentSlot slot = armor.getEquipmentSlot();
            return !human.getItemBySlot(slot).isEmpty();
        }
        if (!(candidate.getItem() instanceof TieredItem)) return false;
        return human.getMainHandItem().getItem() instanceof TieredItem
                && human.getMainHandItem().getItem().getClass() == candidate.getItem().getClass();
    }
}
