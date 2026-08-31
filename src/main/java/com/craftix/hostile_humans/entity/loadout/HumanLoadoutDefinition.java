package com.craftix.hostile_humans.entity.loadout;

import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.List;

/** Fully rolled, validated equipment plan. It has no entity or world side effects. */
public final class HumanLoadoutDefinition {
    private final HumanLoadoutGenerator.Quality quality;
    private final EnumMap<EquipmentSlot, ItemStack> equipment;
    private final List<ItemStack> inventory;

    public HumanLoadoutDefinition(HumanLoadoutGenerator.Quality quality,
                                  EnumMap<EquipmentSlot, ItemStack> equipment,
                                  List<ItemStack> inventory) {
        this.quality = quality;
        this.equipment = new EnumMap<>(EquipmentSlot.class);
        equipment.forEach((slot, stack) -> this.equipment.put(slot, stack.copy()));
        this.inventory = inventory.stream().map(ItemStack::copy).toList();
    }

    public HumanLoadoutGenerator.Quality quality() { return quality; }
    public ItemStack equipment(EquipmentSlot slot) { return equipment.getOrDefault(slot, ItemStack.EMPTY); }
    public List<ItemStack> inventory() { return inventory; }

    public int armorPieces() {
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!equipment(slot).isEmpty()) count++;
        }
        return count;
    }

    public boolean hasBow() { return equipment(EquipmentSlot.MAINHAND).is(net.minecraft.world.item.Items.BOW); }
    public boolean hasAmmo() {
        return inventory.stream().anyMatch(stack -> stack.getItem() instanceof net.minecraft.world.item.ArrowItem && stack.getCount() > 0);
    }

    /** Applies only after generation/validation; all writes use the Human data bridge. */
    public void applyTo(Human human) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            human.setItemSlot(slot, equipment(slot).copy());
        }
        if (human.getData() == null) return;
        int slot = 0;
        for (ItemStack stack : inventory) {
            if (slot >= human.getData().getInventoryItemsSize()) break;
            human.getData().setInventoryItem(slot++, stack.copy());
        }
        human.markEquipmentDirty();
        human.queueUsefulInventoryEquipment();
        human.queueEquipmentReevaluation();
    }
}
