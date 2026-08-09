package com.craftix.hostile_humans.entity.type.human;

import com.craftix.hostile_humans.entity.HumanAbility;
import com.craftix.hostile_humans.entity.HumanEntity;
import com.craftix.hostile_humans.entity.data.HumanData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.List;

@EventBusSubscriber
public class PickUpLoot extends HumanAbility {

    private static final short TICK_RATE = 20 * 3;
    private static int radius = 2;

    public PickUpLoot(HumanEntity humanEntity, Level level) {
        super(humanEntity, level);
    }

    @SubscribeEvent
    public static void handleServerAboutToStartEvent(ServerAboutToStartEvent event) {
        radius = 2;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide && radius > 0 && ticker++ >= TICK_RATE) {
            List<ItemEntity> itemEntities = this.level.getEntities(EntityType.ITEM,
                    new AABB(humanEntity.blockPosition()).inflate(radius),
                    entity -> true);
            if (!itemEntities.isEmpty()) {
                HumanData humanMobData = humanEntity.getData();
                if (humanMobData != null) {

                    for (ItemEntity itemEntity : itemEntities) {
                        if (itemEntity.isRemoved()) continue;
                        if (itemEntity.isAlive() && humanEntity.isAlive() && !humanEntity.isDeadOrDying() && humanMobData.storeInventoryItem(itemEntity.getItem())) {
                            ItemStack itemstack = itemEntity.getItem();
                            humanEntity.take(itemEntity, itemstack.getCount());
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
