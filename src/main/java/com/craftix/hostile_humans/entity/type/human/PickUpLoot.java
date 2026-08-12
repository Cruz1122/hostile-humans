package com.craftix.hostile_humans.entity.type.human;

import com.craftix.hostile_humans.entity.HumanAbility;
import com.craftix.hostile_humans.entity.HumanEntity;
import com.craftix.hostile_humans.entity.data.HumanData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import java.util.List;

public class PickUpLoot extends HumanAbility {

    private static final short TICK_RATE = 1;

    public PickUpLoot(HumanEntity humanEntity, Level level) {
        super(humanEntity, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide && ticker++ >= TICK_RATE) {
            List<ItemEntity> itemEntities = this.level.getEntities(EntityType.ITEM,
                    // Match vanilla Mob pickup reach instead of searching a
                    // large cube around the block position.
                    humanEntity.getBoundingBox().inflate(1.0D, 0.5D, 1.0D),
                    entity -> true);
            if (!itemEntities.isEmpty()) {
                HumanData humanMobData = humanEntity.getData();
                if (humanMobData != null) {

                    for (ItemEntity itemEntity : itemEntities) {
                        if (itemEntity.isRemoved() || itemEntity.hasPickUpDelay()) continue;
                        if (itemEntity.isAlive() && humanEntity.isAlive() && !humanEntity.isDeadOrDying()
                                && humanMobData.storeInventoryItem(itemEntity.getItem())) {
                            ItemStack itemstack = itemEntity.getItem();
                            if (itemstack.isEmpty()) {
                                itemEntity.discard();
                            } else {
                                itemEntity.setItem(itemstack);
                            }
                            if (humanEntity instanceof com.craftix.hostile_humans.entity.entities.Human human) {
                                human.markEquipmentDirty();
                                human.reevaluateEquipment();
                            }
                        }
                    }
                }
            }
            ticker = 0;
        }
    }
}
