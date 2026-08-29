package com.craftix.hostile_humans.entity.type.human;

import com.craftix.hostile_humans.entity.HumanAbility;
import com.craftix.hostile_humans.entity.HumanEntity;
import com.craftix.hostile_humans.entity.ai.survival.LootCollector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
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
                    humanEntity.getBoundingBox().inflate(1.0D, 0.5D, 1.0D),
                    entity -> entity.distanceToSqr(humanEntity) <= 1.01D);
            if (!itemEntities.isEmpty()) {
                for (ItemEntity itemEntity : itemEntities) {
                    if (itemEntity.isRemoved() || itemEntity.hasPickUpDelay()) continue;
                    if (humanEntity.isAlive() && !humanEntity.isDeadOrDying()
                            && humanEntity instanceof com.craftix.hostile_humans.entity.entities.Human human) {
                        LootCollector.collect(human, itemEntity);
                    }
                }
            }
            ticker = 0;
        }
    }
}
