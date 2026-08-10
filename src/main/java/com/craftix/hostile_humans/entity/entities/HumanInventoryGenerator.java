package com.craftix.hostile_humans.entity.entities;

import com.craftix.hostile_humans.HostileHumans;
import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.compat.TravelersBackpack;
import com.craftix.hostile_humans.entity.loadout.HumanLoadoutManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import static com.craftix.hostile_humans.HumanUtil.isRangedWeapon;

public class HumanInventoryGenerator {

    public static void generateInventory(Human human, boolean forceRanged) {
        if (human.getData() == null) {
            HostileHumans.LOGGER.warn("Missing data during inventory generation {}", human);
            human.discard();
            return;
        }

        HumanLoadoutManager.HumanLoadout loadout = HumanLoadoutManager.get(human.getTier());
        if (loadout == null) {
            applyFallbackInventory(human, forceRanged);
            return;
        }

        RandomSource random = human.getRandom();
        HumanLoadoutManager.ItemPool mainhandPool = forceRanged && !loadout.rangedMainhand.isEmpty()
                ? loadout.rangedMainhand
                : loadout.mainhand;

        ItemStack mainhand = createStack(mainhandPool.roll(random), human, loadout.rules.damagePercentMin, loadout.rules.damagePercentMax);
        if (mainhand.isEmpty()) {
            applyFallbackInventory(human, forceRanged);
            return;
        }
        human.setItemSlot(EquipmentSlot.MAINHAND, mainhand);

        if (random.nextFloat() < loadout.offhand.chance) {
            ItemStack offhand = createStack(loadout.offhand.roll(random), human, loadout.rules.damagePercentMin, loadout.rules.damagePercentMax);
            if (!offhand.isEmpty()) {
                human.setItemSlot(EquipmentSlot.OFFHAND, offhand);
            }
        }

        int backupSlot = 0;
        boolean mainhandRanged = HumanUtil.isRangedWeapon(human.getItemBySlot(EquipmentSlot.MAINHAND));
        HumanLoadoutManager.ItemPool alternatePool = mainhandRanged
                ? loadout.inventory : loadout.rangedMainhand;
        backupSlot = storeBackupWeapon(human, alternatePool, loadout, random, backupSlot);
        storeBackupWeapon(human, loadout.inventory, loadout, random, backupSlot);

        HumanLoadoutManager.ArmorSetEntry armorSet = loadout.armorSets.roll(random);
        if (armorSet != null) {
            equipArmorSet(human, armorSet, loadout.rules.damagePercentMin, loadout.rules.damagePercentMax);
        }

        human.applySpawnedWeaponEnchantments(random, loadout.rules.enchantChance);
        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            if (equipmentSlot.getType() == EquipmentSlot.Type.ARMOR) {
                human.applySpawnedArmorEnchantments(random, loadout.rules.enchantChance, equipmentSlot);
            }
        }

        if (random.nextFloat() < loadout.bonusMainhand.chance) {
            ItemStack bonusMainhand = createStack(loadout.bonusMainhand.roll(random), human, loadout.rules.damagePercentMin, loadout.rules.damagePercentMax);
            if (!bonusMainhand.isEmpty()) {
                human.setItemSlot(EquipmentSlot.MAINHAND, bonusMainhand);
            }
        }

        if (ModList.get().isLoaded("travelersbackpack") && ModList.get().isLoaded("curios")) {
            TravelersBackpack.apply(human);
        }
    }

    private static void equipArmorSet(Human human, HumanLoadoutManager.ArmorSetEntry armorSet, float damagePercentMin, float damagePercentMax) {
        equipArmorSlot(human, EquipmentSlot.HEAD, armorSet.head, damagePercentMin, damagePercentMax);
        equipArmorSlot(human, EquipmentSlot.CHEST, armorSet.chest, damagePercentMin, damagePercentMax);
        equipArmorSlot(human, EquipmentSlot.LEGS, armorSet.legs, damagePercentMin, damagePercentMax);
        equipArmorSlot(human, EquipmentSlot.FEET, armorSet.feet, damagePercentMin, damagePercentMax);
    }

    private static void equipArmorSlot(Human human, EquipmentSlot slot, net.minecraft.resources.ResourceLocation itemId, float damagePercentMin, float damagePercentMax) {
        if (itemId == null) {
            return;
        }

        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item != null && item != Items.AIR) {
            human.setItemSlot(slot, damage(human, item.getDefaultInstance(), damagePercentMin, damagePercentMax));
        }
    }

    private static ItemStack createStack(HumanLoadoutManager.ItemEntry entry, Human human, float damagePercentMin, float damagePercentMax) {
        if (entry == null) {
            return ItemStack.EMPTY;
        }

        Item item = ForgeRegistries.ITEMS.getValue(entry.itemId);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }

        return damage(human, item.getDefaultInstance(), damagePercentMin, damagePercentMax);
    }

    private static int storeBackupWeapon(Human human, HumanLoadoutManager.ItemPool pool,
                                         HumanLoadoutManager.HumanLoadout loadout,
                                         RandomSource random, int slot) {
        if (pool == null || pool.isEmpty() || slot >= human.getData().getInventoryItemsSize()) return slot;
        ItemStack backup = createStack(pool.roll(random), human,
                loadout.rules.damagePercentMin, loadout.rules.damagePercentMax);
        if (backup.isEmpty()) return slot;
        backup.enchant(Enchantments.VANISHING_CURSE, 1);
        human.getData().setInventoryItem(slot, backup);
        return slot + 1;
    }

    private static void applyFallbackInventory(Human human, boolean forceRanged) {
        HostileHumans.LOGGER.warn("Missing human loadout for tier {}, using fallback defaults", human.getTier());
        ItemStack fallbackWeapon = forceRanged
                ? Items.BOW.getDefaultInstance()
                : (human.getTier() == HumanTier.LEVEL2 ? Items.DIAMOND_SWORD.getDefaultInstance() : Items.IRON_SWORD.getDefaultInstance());
        human.setItemSlot(EquipmentSlot.MAINHAND, damage(human, fallbackWeapon, 0.0F, 0.85F));
        if (!isRangedWeapon(fallbackWeapon)) {
            human.setItemSlot(EquipmentSlot.OFFHAND, damage(human, Items.SHIELD.getDefaultInstance(), 0.0F, 0.85F));
        }
    }

    public static ItemStack damage(Human human, ItemStack inStack, float damagePercentMin, float damagePercentMax) {
        if (!inStack.isDamageableItem()) {
            return inStack;
        }

        float min = Math.max(0.0F, damagePercentMin);
        float max = Math.max(min, damagePercentMax);
        int maxDamage = Math.max(inStack.getMaxDamage() - 1, 1);
        float percent = min + human.getRandom().nextFloat() * (max - min);
        int damage = (int) (maxDamage * percent);
        inStack.setDamageValue(Math.min(damage, maxDamage));
        return inStack;
    }
}
