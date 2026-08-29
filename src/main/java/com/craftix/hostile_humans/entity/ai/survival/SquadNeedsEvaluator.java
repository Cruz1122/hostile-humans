package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SquadNeedsEvaluator {
    private static final int FOOD_PER_MEMBER = 8;
    private static final int ARROWS_PER_BOW = 24;
    private static final int WOOD_UNITS_PER_MEMBER = 12;
    private static final int STONE_PER_BASIC_TOOL = 3;
    private static final Map<Key, Cached> CACHE = new HashMap<>();

    private SquadNeedsEvaluator() {}

    public static SquadNeeds evaluate(Human source) {
        Key key = new Key(source.level().dimension(), source.getUUID());
        long now = source.level().getGameTime();
        Cached cached = CACHE.get(key);
        if (cached != null && now < cached.expiresAt) return cached.needs;
        // Progression is owned by each human. Squad sharing remains an optimization,
        // but another member's equipment must never satisfy this human's gate.
        SquadNeeds needs = calculate(List.of(source));
        CACHE.put(key, new Cached(needs, now + Config.needsEvaluationIntervalTicks.get()));
        return needs;
    }

    public static void invalidate(Human source) {
        CACHE.remove(new Key(source.level().dimension(), source.getUUID()));
    }

    public static SquadNeeds calculate(List<Human> members) {
        EnumMap<SquadNeed, Integer> deficits = new EnumMap<>(SquadNeed.class);
        int food = sum(members, stack -> stack.getFoodProperties(null) != null && !isRawFood(stack) ? stack.getCount() : 0);
        int rawFood = sum(members, stack -> isRawFood(stack) ? stack.getCount() : 0);
        put(deficits, SquadNeed.FOOD, members.size() * FOOD_PER_MEMBER - food - Math.min(rawFood, members.size() * FOOD_PER_MEMBER));

        int wood = sum(members, stack -> stack.is(ItemTags.LOGS) ? stack.getCount() * 4
                : stack.is(ItemTags.PLANKS) || stack.is(Items.STICK) ? stack.getCount() : 0);
        // Wood is a bootstrap dependency for the first pickaxe only. Once a
        // pickaxe exists, stone becomes the next actionable mining target.
        boolean missingBasicTool = members.stream().anyMatch(member -> !hasTool(member, PickaxeItem.class));
        put(deficits, SquadNeed.WOOD, Math.max(missingBasicTool ? 4 : 0, members.size() * WOOD_UNITS_PER_MEMBER - wood));

        int rawOre = sum(members, stack -> stack.is(Items.RAW_IRON) || stack.is(Items.RAW_GOLD) ? stack.getCount() : 0);
        int fuel = sum(members, stack -> stack.is(Items.COAL) || stack.is(Items.CHARCOAL) ? stack.getCount() : 0);

        int missingStoneTools = 0;
        for (Human member : members) {
            if (!hasStoneTool(member, PickaxeItem.class)) missingStoneTools++;
            if (!hasStoneTool(member, AxeItem.class)) missingStoneTools++;
            if (!hasStoneTool(member, SwordItem.class)) missingStoneTools++;
        }
        if (missingStoneTools > 0) put(deficits, SquadNeed.STONE,
                Math.max(0, missingStoneTools * STONE_PER_BASIC_TOOL
                        - count(members, Items.COBBLESTONE) - count(members, Items.COBBLED_DEEPSLATE)));

        int ironGearMissing = 0;
        int diamondGearMissing = 0;
        for (Human member : members) {
            if (!hasIronTool(member, PickaxeItem.class)) ironGearMissing += 3;
            if (!hasShield(member)) ironGearMissing += 1;
            for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
                    net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                    net.minecraft.world.entity.EquipmentSlot.FEET}) {
                ItemStack armor = member.getItemBySlot(slot);
                if (!(armor.getItem() instanceof ArmorItem armorItem)
                        || armorItem.getMaterial() != net.minecraft.world.item.ArmorMaterials.DIAMOND
                        && armorItem.getMaterial() != net.minecraft.world.item.ArmorMaterials.NETHERITE) {
                    diamondGearMissing += switch (slot.getName()) {
                        case "head" -> 5;
                        case "chest" -> 8;
                        case "legs" -> 7;
                        default -> 4;
                    };
                }
            }
        }
        int iron = count(members, Items.IRON_INGOT) + count(members, Items.RAW_IRON);
        put(deficits, SquadNeed.IRON, Math.max(0, ironGearMissing - iron));
        // Gather a small fuel reserve before mining iron as well as after raw
        // materials exist. Otherwise a stone-equipped human can walk past coal,
        // mine iron, and only then discover that the furnace cannot run.
        boolean stonePickReady = members.stream().anyMatch(member -> hasStoneTool(member, PickaxeItem.class));
        if (rawFood + rawOre > 0 || stonePickReady && ironGearMissing > iron) {
            put(deficits, SquadNeed.FUEL, Math.max(0, 2 - fuel));
        }

        boolean canMineDiamond = members.stream().anyMatch(member -> hasIronTool(member, PickaxeItem.class)
                && hasShield(member));
        if (canMineDiamond) {
            int diamondToolsMissing = 0;
            for (Human member : members) {
                if (!hasDiamondTool(member, PickaxeItem.class)) diamondToolsMissing += 3;
                if (!hasDiamondTool(member, AxeItem.class)) diamondToolsMissing += 3;
                if (!hasDiamondTool(member, SwordItem.class)) diamondToolsMissing += 3;
            }
            put(deficits, SquadNeed.DIAMOND, Math.max(0, diamondToolsMissing + diamondGearMissing - count(members, Items.DIAMOND)));
        }

        int apples = count(members, Items.APPLE);
        int gapples = count(members, Items.GOLDEN_APPLE);
        if (gapples < members.size()) {
            put(deficits, SquadNeed.APPLES, members.size() - gapples - apples);
            put(deficits, SquadNeed.GOLD, Math.max(0, members.size() - gapples - count(members, Items.GOLD_INGOT) / 8));
        }

        int missingBows = (int) members.stream().filter(member -> !SurvivalInventory.contains(member, HumanUtil::isRangedWeapon)).count();
        put(deficits, SquadNeed.STRING, Math.max(0, missingBows * 3 - count(members, Items.STRING)));
        int bowUsers = (int) members.stream().filter(member -> SurvivalInventory.contains(member, stack -> stack.getItem() instanceof BowItem)).count();
        int arrows = count(members, Items.ARROW);
        int arrowDeficit = bowUsers * ARROWS_PER_BOW - arrows;
        if (arrowDeficit > 0) {
            put(deficits, SquadNeed.FEATHERS, Math.max(0, (arrowDeficit + 3) / 4 - count(members, Items.FEATHER)));
            put(deficits, SquadNeed.FLINT, Math.max(0, (arrowDeficit + 3) / 4 - count(members, Items.FLINT)));
        }
        return new SquadNeeds(deficits);
    }

    private static boolean hasTool(Human member, Class<?> type) {
        return SurvivalInventory.contains(member, stack -> type.isInstance(stack.getItem()));
    }

    private static boolean hasStoneTool(Human member, Class<?> type) {
        return SurvivalInventory.contains(member, stack -> type.isInstance(stack.getItem())
                && stack.getItem() instanceof net.minecraft.world.item.TieredItem tiered
                && tiered.getTier().getLevel() >= 1);
    }

    private static boolean hasIronTool(Human member, Class<?> type) {
        return SurvivalInventory.contains(member, stack -> type.isInstance(stack.getItem())
                && stack.getItem() instanceof net.minecraft.world.item.TieredItem tiered
                && tiered.getTier().getLevel() >= 2);
    }

    private static boolean hasDiamondTool(Human member, Class<?> type) {
        return SurvivalInventory.contains(member, stack -> type.isInstance(stack.getItem())
                && stack.getItem() instanceof net.minecraft.world.item.TieredItem tiered
                && tiered.getTier().getLevel() >= 3);
    }

    private static boolean hasShield(Human member) {
        return SurvivalInventory.contains(member, stack -> stack.is(Items.SHIELD));
    }

    private static boolean isRawFood(ItemStack stack) {
        return stack.is(Items.BEEF) || stack.is(Items.PORKCHOP) || stack.is(Items.CHICKEN)
                || stack.is(Items.MUTTON) || stack.is(Items.RABBIT) || stack.is(Items.COD) || stack.is(Items.SALMON);
    }

    private static int count(List<Human> members, net.minecraft.world.level.ItemLike item) {
        return members.stream().mapToInt(member -> SurvivalInventory.count(member, item)).sum();
    }

    private static int sum(List<Human> members, java.util.function.ToIntFunction<ItemStack> value) {
        int total = 0;
        for (Human member : members) {
            if (member.getData() == null) continue;
            for (ItemStack stack : member.getData().getInventoryItems()) total += value.applyAsInt(stack);
        }
        return total;
    }

    private static void put(EnumMap<SquadNeed, Integer> deficits, SquadNeed need, int amount) {
        if (amount > 0) deficits.put(need, amount);
    }

    private record Key(ResourceKey<Level> dimension, UUID squad) {}
    private record Cached(SquadNeeds needs, long expiresAt) {}
}
