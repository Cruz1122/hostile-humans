package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.entity.ai.squad.SquadManager;
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

import java.util.ArrayList;
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
        UUID squad = source.getSquadId() == null ? source.getUUID() : source.getSquadId();
        Key key = new Key(source.level().dimension(), squad);
        long now = source.level().getGameTime();
        Cached cached = CACHE.get(key);
        if (cached != null && now < cached.expiresAt) return cached.needs;
        List<Human> members = new ArrayList<>();
        members.add(source);
        members.addAll(SquadManager.nearbyMembers(source));
        SquadNeeds needs = calculate(members);
        CACHE.put(key, new Cached(needs, now + Config.needsEvaluationIntervalTicks.get()));
        return needs;
    }

    public static void invalidate(Human source) {
        UUID squad = source.getSquadId() == null ? source.getUUID() : source.getSquadId();
        CACHE.remove(new Key(source.level().dimension(), squad));
    }

    public static SquadNeeds calculate(List<Human> members) {
        EnumMap<SquadNeed, Integer> deficits = new EnumMap<>(SquadNeed.class);
        int food = sum(members, stack -> stack.getFoodProperties(null) != null && !isRawFood(stack) ? stack.getCount() : 0);
        int rawFood = sum(members, stack -> isRawFood(stack) ? stack.getCount() : 0);
        put(deficits, SquadNeed.FOOD, members.size() * FOOD_PER_MEMBER - food - Math.min(rawFood, members.size() * FOOD_PER_MEMBER));

        int wood = sum(members, stack -> stack.is(ItemTags.LOGS) ? stack.getCount() * 4
                : stack.is(ItemTags.PLANKS) || stack.is(Items.STICK) ? stack.getCount() : 0);
        boolean missingBasicTool = members.stream().anyMatch(member -> !hasTool(member, PickaxeItem.class) || !hasTool(member, AxeItem.class));
        put(deficits, SquadNeed.WOOD, Math.max(missingBasicTool ? 4 : 0, members.size() * WOOD_UNITS_PER_MEMBER - wood));

        int rawOre = sum(members, stack -> stack.is(Items.RAW_IRON) || stack.is(Items.RAW_GOLD) ? stack.getCount() : 0);
        int fuel = sum(members, stack -> stack.is(Items.COAL) || stack.is(Items.CHARCOAL) ? stack.getCount() : 0);
        if (rawFood + rawOre > 0) put(deficits, SquadNeed.FUEL, Math.max(0, 2 - fuel));

        int missingStoneTools = 0;
        for (Human member : members) {
            if (!hasStoneTool(member, PickaxeItem.class)) missingStoneTools++;
            if (!hasStoneTool(member, AxeItem.class)) missingStoneTools++;
        }
        if (missingStoneTools > 0) put(deficits, SquadNeed.STONE,
                Math.max(0, missingStoneTools * STONE_PER_BASIC_TOOL
                        - count(members, Items.COBBLESTONE) - count(members, Items.COBBLED_DEEPSLATE)));

        int ironGearMissing = 0;
        int diamondGearMissing = 0;
        for (Human member : members) {
            if (!hasTool(member, PickaxeItem.class) || !hasTool(member, SwordItem.class) || !hasTool(member, AxeItem.class)) ironGearMissing += 3;
            for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
                    net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                    net.minecraft.world.entity.EquipmentSlot.FEET}) {
                ItemStack armor = member.getItemBySlot(slot);
                if (!(armor.getItem() instanceof ArmorItem)) ironGearMissing += 4;
                if (!(armor.getItem() instanceof ArmorItem armorItem)
                        || armorItem.getMaterial() != net.minecraft.world.item.ArmorMaterials.DIAMOND
                        && armorItem.getMaterial() != net.minecraft.world.item.ArmorMaterials.NETHERITE) diamondGearMissing++;
            }
        }
        int iron = count(members, Items.IRON_INGOT) + count(members, Items.RAW_IRON);
        put(deficits, SquadNeed.IRON, Math.max(0, ironGearMissing - iron));

        boolean canMineDiamond = members.stream().anyMatch(member -> SurvivalInventory.contains(member,
                stack -> stack.is(Items.IRON_PICKAXE) || stack.is(Items.DIAMOND_PICKAXE) || stack.is(Items.NETHERITE_PICKAXE)));
        if (canMineDiamond) put(deficits, SquadNeed.DIAMOND, Math.max(0, Math.min(3, diamondGearMissing) - count(members, Items.DIAMOND)));

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
