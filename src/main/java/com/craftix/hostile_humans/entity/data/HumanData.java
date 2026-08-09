package com.craftix.hostile_humans.entity.data;

import com.craftix.hostile_humans.entity.AggressionMode;
import com.craftix.hostile_humans.entity.HumanEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.UUID;

public class HumanData {

    public static final String UUID_TAG = "UUID";
    private static final String ENTITY_AGGRESSION_LEVEL = "EntityAggressionLevel";
    private static final String ENTITY_DATA_TAG = "EntityData";
    private static final String ENTITY_ID_TAG = "EntityId";
    private static final String ENTITY_SITTING_TAG = "EntitySitting";
    private static final String ENTITY_TYPE_TAG = "EntityType";
    private static final String LEVEL_TAG = "Level";
    private static final String NAME_TAG = "Name";
    private static final String OWNER_NAME_TAG = "OwnerName";
    private static final String OWNER_TAG = "Owner";
    private static final String POSITION_TAG = "Position";
    private AggressionMode entityAggressionLevel = AggressionMode.PASSIVE;
    private BlockPos blockPos;
    private ClientLevel clientLevel;
    private CompoundTag entityData;
    private EntityType<?> entityType;
    private NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);
    private NonNullList<ItemStack> handItems = NonNullList.withSize(2, ItemStack.EMPTY);
    private NonNullList<ItemStack> inventoryItems = NonNullList.withSize(30, ItemStack.EMPTY);
    private HumanEntity humanMobEntity;

    private ResourceKey<Level> level;
    private ServerLevel serverLevel;
    private String levelName = "";
    private String name = "";
    private String ownerName = "";
    private UUID humanMobUUID = null;
    private UUID ownerUUID = null;

    private boolean entitySitting = false;
    private boolean hasOwner = false;

    private int entityId;

    public HumanData(HumanEntity humanMob) {
        load(humanMob);
    }

    public HumanData(CompoundTag compoundTag) {
        load(compoundTag);
    }

    public boolean hasOwner() {
        return this.ownerUUID != null;
    }

    public UUID getUUID() {
        return this.humanMobUUID;
    }

    public String getName() {
        return this.name;
    }

    public UUID getOwnerUUID() {
        if (this.ownerUUID == null) {
            return null;
        }
        return this.ownerUUID;
    }

    public HumanEntity getHHFollowerEntity() {
        if (this.humanMobEntity == null) {
            Entity entity = null;
            if (this.serverLevel != null && this.humanMobUUID != null) {
                entity = this.serverLevel.getEntity(this.humanMobUUID);
            } else if (this.clientLevel != null && this.entityId > 0) {
                entity = this.clientLevel.getEntity(this.entityId);
            }
            if (entity instanceof HumanEntity humanEntity) {
                this.humanMobEntity = humanEntity;
            }
        }
        return this.humanMobEntity;
    }

    public NonNullList<ItemStack> getArmorItems() {
        return this.armorItems;
    }

    public void setArmorItems(NonNullList<ItemStack> armor) {
        this.armorItems = armor;
        this.setDirty();
    }

    public void setArmorItem(int index, ItemStack itemStack) {
        this.armorItems.set(index, itemStack);
        this.setDirty();
    }

    public NonNullList<ItemStack> getHandItems() {
        return this.handItems;
    }

    public void setHandItems(NonNullList<ItemStack> hand) {
        this.handItems = hand;
        this.setDirty();
    }

    public void setHandItem(int index, ItemStack itemStack) {
        this.handItems.set(index, itemStack);
        this.setDirty();
    }

    public NonNullList<ItemStack> getInventoryItems() {
        return this.inventoryItems;
    }

    public void setInventoryItem(int index, ItemStack itemStack) {
        this.inventoryItems.set(index, itemStack);
        this.setDirty();
    }

    @Nonnull
    public ItemStack getInventoryItem(int index) {
        return this.inventoryItems.get(index);
    }

    public int getInventoryItemsSize() {
        return this.inventoryItems.size();
    }

    public boolean storeInventoryItem(ItemStack itemStack) {
        boolean stored = false;
        int firstPickupSlot = getInventoryItemsSize() - 10;
        for (int index = firstPickupSlot; index < getInventoryItemsSize() && !itemStack.isEmpty(); index++) {
            ItemStack existingItems = getInventoryItem(index);
            if (!existingItems.isEmpty() && ItemStack.isSameItemSameTags(existingItems, itemStack)) {
                int room = Math.min(existingItems.getMaxStackSize(), itemStack.getMaxStackSize()) - existingItems.getCount();
                if (room > 0) {
                    int moved = Math.min(room, itemStack.getCount());
                    existingItems.grow(moved);
                    itemStack.shrink(moved);
                    stored = true;
                }
            }
        }
        for (int index = firstPickupSlot; index < getInventoryItemsSize() && !itemStack.isEmpty(); index++) {
            if (getInventoryItem(index).isEmpty()) {
                int moved = Math.min(itemStack.getCount(), itemStack.getMaxStackSize());
                ItemStack inserted = itemStack.copyWithCount(moved);
                setInventoryItem(index, inserted);
                itemStack.shrink(moved);
                stored = true;
            }
        }
        return stored;
    }

    public void load(HumanEntity humanMob) {
        this.humanMobEntity = humanMob;
        this.humanMobUUID = humanMob.getUUID();
        this.name = humanMob.getCustomHumanMobName();

        this.hasOwner = humanMob.hasOwner();
        if (this.hasOwner) {
            this.ownerUUID = humanMob.getOwnerUUID();
            if (humanMob.getOwner() != null) {
                this.ownerName = humanMob.getOwner().getName().getString();
            }
        }
        this.blockPos = humanMob.blockPosition();
        this.level = humanMob.level().dimension();
        this.levelName = this.level.registry() + "/" + this.level.location();
        this.entityId = humanMob.getId();
        this.entityAggressionLevel = humanMob.getAggressionLevel();

        this.entityType = humanMob.getType();

        this.entitySitting = humanMob.isOrderedToSit();

        this.entityData = humanMob.serializeNBT();

        Level humanMobLevel = humanMob.level();

        if (humanMobLevel.isClientSide) {
            this.clientLevel = (ClientLevel) humanMob.level();
        } else {
            this.serverLevel = (ServerLevel) humanMob.level();
        }

        setArmorItems((NonNullList<ItemStack>) humanMob.getArmorSlots());
        setHandItems((NonNullList<ItemStack>) humanMob.getHandSlots());
    }

    public void load(CompoundTag compoundTag) {
        this.humanMobUUID = compoundTag.getUUID(UUID_TAG);
        this.name = compoundTag.getString(NAME_TAG);

        this.hasOwner = compoundTag.hasUUID(OWNER_TAG);
        if (this.hasOwner) {
            this.ownerUUID = compoundTag.getUUID(OWNER_TAG);
            if (compoundTag.contains(OWNER_NAME_TAG)) {
                this.ownerName = compoundTag.getString(OWNER_NAME_TAG);
            }
        }

        this.blockPos = NbtUtils.readBlockPos(compoundTag.getCompound(POSITION_TAG));
        if (compoundTag.contains(LEVEL_TAG)) {
            this.levelName = compoundTag.getString(LEVEL_TAG);
            if (this.levelName.contains("/")) {
                String[] levelNameParts = this.levelName.split("/");
                ResourceLocation registryName = new ResourceLocation(levelNameParts[0]);
                ResourceLocation locationName = new ResourceLocation(levelNameParts[1]);
                this.level = ResourceKey.create(ResourceKey.createRegistryKey(registryName), locationName);
            }
        }
        if (compoundTag.contains(ENTITY_ID_TAG)) {
            this.entityId = compoundTag.getInt(ENTITY_ID_TAG);
        }
        if (compoundTag.contains(ENTITY_TYPE_TAG)) {
            this.entityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(compoundTag.getString(ENTITY_TYPE_TAG)));
        }
        this.entityData = compoundTag.getCompound(ENTITY_DATA_TAG);

        this.entitySitting = compoundTag.getBoolean(ENTITY_SITTING_TAG);

        HumanHelper.loadArmorItems(compoundTag, this.armorItems);

        HumanHelper.loadHandItems(compoundTag, this.handItems);

        HumanHelper.loadInventoryItems(compoundTag, this.inventoryItems);

        if (compoundTag.contains(ENTITY_AGGRESSION_LEVEL)) {
            this.entityAggressionLevel =
                    AggressionMode.get(compoundTag.getString(ENTITY_AGGRESSION_LEVEL));
        }
    }

    public CompoundTag save(CompoundTag compoundTag) {
        return save(compoundTag, true);
    }

    public CompoundTag saveMetaData(CompoundTag compoundTag) {
        return save(compoundTag, false);
    }

    public CompoundTag save(CompoundTag compoundTag, boolean includeData) {
        compoundTag.putUUID(UUID_TAG, this.humanMobUUID);
        compoundTag.putString(NAME_TAG, this.name);

        compoundTag.put(POSITION_TAG, NbtUtils.writeBlockPos(this.blockPos));
        if (!this.levelName.isEmpty()) {
            compoundTag.putString(LEVEL_TAG, this.levelName);
        }
        compoundTag.putInt(ENTITY_ID_TAG, this.entityId);
        ResourceLocation entityTypeId = ForgeRegistries.ENTITY_TYPES.getKey(this.entityType);
        if (entityTypeId != null) {
            compoundTag.putString(ENTITY_TYPE_TAG, entityTypeId.toString());
        }

        HumanEntity humanEntity = this.getHHFollowerEntity();

        if (humanEntity != null && humanEntity.isAlive()) {
            this.entityData = humanEntity.serializeNBT();
        }

        if (includeData && this.entityData != null) {
            compoundTag.put(ENTITY_DATA_TAG, this.entityData);
        }

        if (humanEntity != null && humanEntity.isAlive()) {

            compoundTag.putBoolean(ENTITY_SITTING_TAG, humanEntity.isOrderedToSit());

            compoundTag.putString(ENTITY_AGGRESSION_LEVEL,
                    humanEntity.getAggressionLevel().name());

            if (humanEntity.getOwner() != null) {
                compoundTag.putUUID(OWNER_TAG, humanEntity.getOwner().getUUID());
                compoundTag.putString(OWNER_NAME_TAG,
                        humanEntity.getOwner().getName().getString());
            }

            setArmorItems((NonNullList<ItemStack>) humanEntity.getArmorSlots());

            setHandItems((NonNullList<ItemStack>) humanEntity.getHandSlots());
        } else {

            compoundTag.putBoolean(ENTITY_SITTING_TAG, this.entitySitting);

            compoundTag.putString(ENTITY_AGGRESSION_LEVEL, this.entityAggressionLevel.name());

            if (this.ownerUUID != null) {
                compoundTag.putUUID(OWNER_TAG, this.ownerUUID);
                compoundTag.putString(OWNER_NAME_TAG, this.ownerName);
            }
        }

        HumanHelper.saveArmorItems(compoundTag, this.armorItems);

        HumanHelper.saveHandItems(compoundTag, this.handItems);

        HumanHelper.saveInventoryItems(compoundTag, this.inventoryItems);

        return compoundTag;
    }

    private void setDirty() {
        HumanServerData serverData = HumanServerData.get();
        if (serverData != null) {
            serverData.setDirty();
        }
    }

    public boolean is(HumanData humanMobEntity) {
        return this.humanMobUUID == humanMobEntity.humanMobUUID;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (!(object instanceof HumanData)) {
            return false;
        }

        HumanData humanMobEntity = (HumanData) object;
        return humanMobEntity.getUUID().equals(this.humanMobUUID);
    }

    @Override
    public int hashCode() {
        return this.humanMobUUID.hashCode();
    }
}
