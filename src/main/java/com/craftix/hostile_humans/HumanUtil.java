package com.craftix.hostile_humans;

import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.equipment.MeleeWeaponSelector;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Function;

public class HumanUtil {
    public static ItemStack[] EDIBLE_ITEMS = new ItemStack[]{Items.BREAD.getDefaultInstance(), Items.COOKED_PORKCHOP.getDefaultInstance(), Items.COOKED_COD.getDefaultInstance(), Items.COOKED_SALMON.getDefaultInstance(), Items.COOKED_BEEF.getDefaultInstance(), Items.COOKED_CHICKEN.getDefaultInstance(), Items.COOKED_RABBIT.getDefaultInstance(), Items.COOKED_MUTTON.getDefaultInstance()};
    public static ItemStack[] EDIBLE_ITEMS_2 = new ItemStack[]{Items.BREAD.getDefaultInstance(), Items.COOKED_PORKCHOP.getDefaultInstance(), Items.COOKED_COD.getDefaultInstance(), Items.COOKED_SALMON.getDefaultInstance(), Items.COOKED_BEEF.getDefaultInstance(), Items.COOKED_CHICKEN.getDefaultInstance(), Items.COOKED_RABBIT.getDefaultInstance(), Items.COOKED_MUTTON.getDefaultInstance()};
    public static String[] greetings = {
            "Huh? INTRUDER!",
            "Who are you? GET OUT OF HERE.",
            "You will die intruder!",
            "You stepped into the wrong place.",
            "Leave this area!",
            "Leave this place!",
            "I would give up.",
            "You made a mistake coming here.",
            "Run or die!",
            "What the hell, who are you?",
            "Give up and I'll let you run.",
            "You in the wrong place mate.",
            "You think you can just barge into this place and live?",
            "Your life has come to a end stranger.",
            "Why are you here?",
            "Run or i'll slit your throat.",
            "GET OUT OF HERE!",
            "Your in the wrong place and I needed to relieve some stress, i'm going to cut you up.",
            "I have pent up anger and you're trespassing, staand stillll!",
            "LEAVE",
            "You think your slick?",
            "HEY YOU, STOP!",
            "Whoever you are, LEAVE OR DIE.",
            "I don't know who you are but you just made a mistake.",
            "Please leave this area or else.",
            "I'm going to enjoy slicing you up.",
            "I don't know who you are but I'm going to cut you up AND FEED YOUR PARTS TO WOLVES!",
            "Wrong place mate, run now while you can.",
            "Why are you doing this?",
            "You just made the biggest mistake of your life mate.",
            "I hope your ready to lose your life stranger.",
            "Thou Shan't pass, heh.",
            "How did- ? Your not welcomed here.",
            "Go back to where you came from.",
            "You're a fool to be here. You won't survive.",
            "You don't belong here.",
            "There's no escape for you.",
            "I'm only warning you once, get out of here.",
            "You're playing with fire, intruder. You'll get burned.",
            "You're in over your head, you don't stand a chance against me!",
            "You're wasting your time here.",
            "You're a brave one, I'll give you that. But bravery won't save you.",
            "Your in for a world of pain mate. I'll make you suffer!",
            "You're out of my league, you can't handle me!",
            "Mate if your asking for trouble, trouble is what you'll get.",
            "You shouldn't be here.",
            "I'll squash you like a bug, DIE!",
            "I'll make you beg for mercy.",
            "There's nothing but death for you here.",
            "Please I don't want to fight, run away.",
            "I hope you're ready.",
            "Please just run.",
            "GET OUT OF HERE."
    };

    public static boolean isStructureDisabled(String value) {
        return Arrays.asList(Config.disabledStructures.get().replace(" ", "").split(",")).contains(value);
    }

    public static boolean isRangedWeapon(ItemStack value) {
        return !value.isEmpty() && (value.getItem() instanceof CrossbowItem || value.getItem() instanceof BowItem);
    }

    public static boolean isMeleeWeapon(ItemStack value) {
        return MeleeWeaponSelector.isMeleeCandidate(value);
    }

    public static boolean isTrident(ItemStack value) {
        return !value.isEmpty() && (value.getItem() instanceof TridentItem);
    }

    public static boolean isShield(ItemStack value) {
        return !value.isEmpty() && (value.getItem() instanceof ShieldItem);
    }

    public static boolean canStartEating(Human human) {
        if (!human.isAlive()) return false;
        if (human.isUsingItem()) return false;
        if (human.eatingColldown != 0) return false;
        if (human.isSleepingOrLyingDown()) return false;
        if (human.isEyeInFluid(FluidTags.WATER)) {
            return human.shouldDrinkWaterBreathingPotion();
        }
        if (human.toAvoid != null || human.isFleeing) return false;
        if (human.getTarget() != null) {
            if (isLowHp(human)) {
                human.shouldStartFleeingThisCombat();
            }
            return false;
        }
        if (!isLowHp(human)) return false;
        if (human.isUsingItem()) return false;
        if (human.healingAfterFleeTicks <= 0 && human.tickCount < 20 * 6 + human.lastCombatTime) return false;

        return true;
    }

    public static boolean isLookingAtTarget(LivingEntity mob, Entity target) {
        Vec3 vec3 = mob.getViewVector(1.0F).normalize();
        Vec3 vec31 = new Vec3(target.getX() - mob.getX(), target.getEyeY() - mob.getEyeY(), target.getZ() - mob.getZ());
        double d0 = vec31.length();
        vec31 = vec31.normalize();
        double d1 = vec3.dot(vec31);
        return d1 > 1.0D - /*0.025D*/ 0.4 / d0 && mob.hasLineOfSight(target);
    }

    @NotNull
    public static ItemStack createSwordBanner() {
        var banner = Items.WHITE_BANNER.getDefaultInstance();
        // minecraft:white_banner{BlockEntityTag: {Patterns: [ {Pattern: "flo", Color:4}, {Pattern: "hh", Color:7}, {Pattern: "cs", Color:0}, {Pattern: "br", Color:7}, {Pattern: "bl", Color:7}, {Pattern: "cbo", Color:7} ]}}

        CompoundTag blockData = banner.getOrCreateTagElement("BlockEntityTag");
        ListTag patterns = new ListTag();

        CompoundTag c1;
        c1 = new CompoundTag();
        c1.putInt("Color", 4);
        c1.putString("Pattern", "flo");
        patterns.add(c1);

        c1 = new CompoundTag();
        c1.putInt("Color", 7);
        c1.putString("Pattern", "hh");
        patterns.add(c1);

        c1 = new CompoundTag();
        c1.putInt("Color", 0);
        c1.putString("Pattern", "cs");
        patterns.add(c1);

        c1 = new CompoundTag();
        c1.putInt("Color", 7);
        c1.putString("Pattern", "br");
        patterns.add(c1);

        c1 = new CompoundTag();
        c1.putInt("Color", 7);
        c1.putString("Pattern", "bl");
        patterns.add(c1);

        c1 = new CompoundTag();
        c1.putInt("Color", 7);
        c1.putString("Pattern", "cbo");
        patterns.add(c1);

        blockData.put("Patterns", patterns);
        return banner;
    }

    public static boolean isLadder(BlockState state, LivingEntity entity, BlockPos pos) {
        return state.isLadder(entity.level(), pos, entity);
    }

    public static int createLadderNodeFor(int nodeID, Node[] nodes, Node origin, Function<BlockPos, Node> nodeGetter, BlockGetter getter, Mob mob) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(origin.x, origin.y + 1, origin.z);
        if (isLadder(getter.getBlockState(pos), mob, pos)) {
            Node node = nodeGetter.apply(pos);
            if (node != null && !node.closed) {
                node.costMalus = 0;
                node.type = BlockPathTypes.WALKABLE;
                if (nodeID + 1 < nodes.length)
                    nodes[nodeID++] = node;
            }
        }
        pos.set(pos.getX(), pos.getY() - 2, pos.getZ());
        if (isLadder(getter.getBlockState(pos), mob, pos)) {
            Node node = nodeGetter.apply(pos);
            if (node != null && !node.closed) {
                node.costMalus = 0;
                node.type = BlockPathTypes.WALKABLE;
                if (nodeID + 1 < nodes.length)
                    nodes[nodeID++] = node;
            }
        }
        return nodeID;
    }

    public static boolean isLowHpInCombat(LivingEntity human) {
        if (human.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING)
            return false;
        
        if (human instanceof Human huma && !huma.needsFood() && huma.getTarget() == null)
        	return false;

        return human.getHealth() < human.getMaxHealth() * (Config.healCombatPercent.get());
    }

    public static boolean isLowHp(LivingEntity human) {
        if (human.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING)
            return false;

        return human.getHealth() < human.getMaxHealth() * Config.fleeHpPercent.get();
    }

    public static boolean shouldFightCreeper(LivingEntity human) {

        return String.valueOf(human.getId()).hashCode() % 100 < 20;
    }
}
