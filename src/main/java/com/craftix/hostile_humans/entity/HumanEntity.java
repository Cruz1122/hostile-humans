package com.craftix.hostile_humans.entity;

import com.craftix.hostile_humans.HumanUtil;
import com.craftix.hostile_humans.entity.ai.control.HumanEntityWalkControl;
import com.craftix.hostile_humans.entity.data.HumanData;
import com.craftix.hostile_humans.entity.data.HumanServerData;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.ai.survival.HumanExperienceCollector;
import com.craftix.hostile_humans.entity.loadout.HumanDeathRewardCalculator;
import com.craftix.hostile_humans.entity.type.human.PickUpLoot;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.List;

import javax.annotation.Nullable;

@EventBusSubscriber
public class HumanEntity extends HumanMobEntityData {

    public static final MobCategory CATEGORY = MobCategory.CREATURE;
    protected PickUpLoot pick;

    public HumanEntity(EntityType<? extends HumanEntity> entityType, Level level) {
        super(entityType, level);

        this.setSyncReference(this);
        navigation.setCanFloat(true);
        setDataSyncNeeded();
        this.moveControl = new HumanEntityWalkControl(this);

        this.setAggressionLevel(AggressionMode.AGGRESSIVE_MONSTER);
        this.pick = new PickUpLoot(this, level);
    }

    public Item getTameItem() {
        return null;
    }

    public Ingredient getFoodItems() {
    	List<ItemStack> all = Lists.newArrayList(HumanUtil.EDIBLE_ITEMS);
    	all.addAll(Lists.newArrayList(HumanUtil.EDIBLE_ITEMS_2));
    	all.addAll(Lists.newArrayList(Human.EXTRA_EDIBLE_ITEMS));
        return Ingredient.of(all.stream());
    }

    protected void pet() {
        if (this.getHealth() < this.getMaxHealth()) {
            this.setHealth((float) (this.getHealth() + 0.1));
        }
    }

    public void follow() {

        this.setOrderedToSit(false);
        this.navigation.recomputePath();
    }

    public boolean eat(ItemStack itemStack, Player player) {
        if (!canEat(itemStack)) {
            return false;
        }
        this.gameEvent(GameEvent.ENTITY_INTERACT, player);

        Item item = itemStack.getItem();
        this.heal(item.getFoodProperties() != null ? item.getFoodProperties().getNutrition() : 0.5F);
        if (item.getFoodProperties() != null) {
            for (Pair<MobEffectInstance, Float> pair : item.getFoodProperties().getEffects()) {
                if (!this.level().isClientSide && pair.getFirst() != null && this.getRandom().nextFloat() < pair.getSecond()) {
                    this.addEffect(new MobEffectInstance(pair.getFirst()));
                }
            }
        }
        if (player != null && !player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }
        this.gameEvent(GameEvent.EAT);
        return true;
    }

    public boolean canEat() {
        return this.getHealth() < this.getMaxHealth() && ((Human)this).needsFood();
    }

    public boolean canEat(ItemStack itemStack) {
        return this.isFood(itemStack) && this.getHealth() < this.getMaxHealth() && ((Human)this).needsFood();
    }

    protected void sit() {
        this.setOrderedToSit(true);
        this.navigation.stop();
        entityData.set(DATA_SIT_POS, this.blockPosition());
        super.setTarget(null);
    }

    @Override
    public void tick() {
        super.tick();
        pick.tick();
        if (this instanceof Human human) HumanExperienceCollector.tick(human);
    }

    public void handleCommand(HumanCommand command) {
        switch (command) {
            case SIT -> sit();
            case FOLLOW -> follow();
            case SIT_FOLLOW_TOGGLE -> {
                if (isOrderedToSit()) {

                    follow();
                } else {
                    sit();
                }
            }
            case AGGRESSION_LEVEL_TOGGLE -> this.toggleAggressionLevel();
            case PET -> pet();
        }
    }

    public void finalizeSpawn() {
        if (!this.hasCustomName()) {
//            this.setCustomName(this.getCustomHumanMobNameComponent());
        }

        if (!this.level().isClientSide && this.getData() == null) {
            HumanServerData serverData = HumanServerData.get();
            if (serverData != null) {
                serverData.registerHumanMob(this);
            }
        }

        registerData();
    }

    public void sendOwnerMessage(Component component) {
        LivingEntity owner = this.getOwner();
        if (component != null && owner instanceof Player player) {
            player.displayClientMessage(component, false);
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    public float getSoundVolume() {
        return 1.0F;
    }

    @Override
    public void setTarget(@Nullable LivingEntity livingEntity) {
        if (this.getTarget() == livingEntity) {
            return;
        }

        if (livingEntity == null || !livingEntity.isAlive()) {
            super.setTarget(null);
            setDataSyncNeeded();
            return;
        }

        super.setTarget(livingEntity);
        setDataSyncNeeded();
    }

    @Override
    public void tame(Player player) {
        super.tame(player);

        if (player instanceof ServerPlayer) {
            this.registerData();
        }
    }

    @Override
    public HumanEntity getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return null;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;

        // ItemStack itemStack = player.getItemInHand(player.getUsedItemHand());

        // if (this.level.isClientSide) {
        //     return InteractionResult.PASS;
        // }

        // if (this.canEat(itemStack) && this.eat(itemStack, player)) {
        //     return InteractionResult.sidedSuccess(this.level.isClientSide);
        // }

        // return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        if (this.getFoodItems() != null) {
            return this.getFoodItems().test(itemStack);
        }

        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        setDataSyncNeeded();
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficulty, MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData, @Nullable CompoundTag compoundTag) {
        spawnGroupData = super.finalizeSpawn(serverLevelAccessor, difficulty, mobSpawnType, spawnGroupData, compoundTag);

        finalizeSpawn();

        return spawnGroupData;
    }

    @Override
    protected void dropEquipment() {
        if (this.level().isClientSide) return;

        if (this instanceof Human human) human.cacheDeathExperienceReward();
        HumanData data = HumanServerData.get().getHumanMob(getUUID());
        if (data != null) {
            NonNullList<ItemStack> inventory = data.getInventoryItems();
            for (int index = 0; index < inventory.size(); index++) {
                ItemStack stack = inventory.get(index);
                data.setInventoryItem(index, ItemStack.EMPTY);
                dropOwnedStack(stack);
            }
        }

        // Armor and hands are live stores. HumanData.armorItems/handItems are
        // snapshots and must never be traversed as additional possessions.
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = getItemBySlot(slot);
            setItemSlot(slot, ItemStack.EMPTY);
            dropOwnedStack(stack);
        }
    }

    private void dropOwnedStack(ItemStack stack) {
        if (stack.isEmpty() || EnchantmentHelper.hasVanishingCurse(stack)) return;
        spawnAtLocation(stack.copy());
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);

        clearFire();
        dropLeash(true, true);
        removeAllEffects();
    }

    @Override
    public void setOrderedToSit(boolean sit) {
        if (this.isOrderedToSit() != sit) {
            super.setOrderedToSit(sit);
            setDataSyncNeeded();
        }
    }
}
