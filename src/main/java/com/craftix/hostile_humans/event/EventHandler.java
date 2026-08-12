package com.craftix.hostile_humans.event;

import com.craftix.hostile_humans.compat.CollectiveVillagerNames;
import com.craftix.hostile_humans.compat.FarmersDelight;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.equipment.MeleeWeaponSelector;
//import com.natamus.villagernames_common_forge.util.Names;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

import static com.craftix.hostile_humans.entity.entities.HumanInventoryGenerator.generateInventory;

import com.craftix.hostile_humans.HostileHumans;

public class EventHandler {

    static boolean addedFarmerItems;
    // random number is used for when the gear generates during the development
    String randomTag = "gave_gear" + (int) (Math.random() * 10000);
    
    @SubscribeEvent
    public void explode(ExplosionEvent.Detonate event) {
        if (!event.getLevel().isClientSide() && event.getExplosion().getExploder() != null) {
            for (Human human : event.getLevel().getEntitiesOfClass(Human.class, event.getExplosion().getExploder().getBoundingBox().inflate(16.0))) {
                human.setInvestigateSound(BlockPos.containing(event.getExplosion().getPosition()));
            }
        }
    }
    
    @SubscribeEvent
    public void damage(LivingDamageEvent event) {
        if (!event.getEntity().level().isClientSide) {
            for (Human human : event.getEntity().level().getEntitiesOfClass(Human.class, event.getEntity().getBoundingBox().inflate(16.0))) {
                human.setInvestigateSound(event.getEntity().blockPosition());
            }
        }
    }

    @SubscribeEvent
    public void serverStart(ServerStartedEvent event) {
    	HostileHumans.patreonNames.forEach(name -> {
            CollectiveVillagerNames.addCustomName(name);
    	});
    	
        if (!addedFarmerItems && ModList.get().isLoaded("farmersdelight")) {
            FarmersDelight.addFoodItems();
            addedFarmerItems = true;
        }
        validateEquipmentTags();
    }

    private void validateEquipmentTags() {
        validateTag(MeleeWeaponSelector.PRIMARY_MELEE_WEAPONS, "primary melee weapons");
        validateTag(MeleeWeaponSelector.FALLBACK_MELEE_TOOLS, "fallback melee tools");
        validateTag(MeleeWeaponSelector.NEVER_USE_AS_MELEE_WEAPON, "never-use melee weapons");
    }

    private void validateTag(net.minecraft.tags.TagKey<Item> tag, String name) {
        int size = BuiltInRegistries.ITEM.getTag(tag).map(values -> values.size()).orElse(0);
        if (size == 0) {
            HostileHumans.LOGGER.warn("Critical equipment tag {} is empty: {}", name, tag.location());
        } else {
            HostileHumans.LOGGER.debug("Loaded {} equipment candidates from {}", size, tag.location());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlace(BlockEvent.EntityPlaceEvent event) {
        var placer = event.getEntity();
        if (placer != null) {
            var humans = placer.level().getEntities(placer, placer.getBoundingBox().inflate(10), entity -> entity instanceof Human otherHuman);
            for (Entity otherHuman : humans) {
                ((Human) otherHuman).isAlert = true;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(LivingDamageEvent event) {
        var damaged = event.getEntity();
        if (damaged instanceof Player) {
            var humans = damaged.level().getEntities(damaged, damaged.getBoundingBox().inflate(10), entity -> entity instanceof Human otherHuman);
            for (Entity otherHuman : humans) {
                ((Human) otherHuman).isAlert = true;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void joinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity.hasCustomName()) {
            String tag = entity.getCustomName().getString();
            if (tag.contains("give_random_gear") && !entity.getTags().contains(randomTag)) {

                if (entity instanceof Human human) {
                    human.setHomePos(human.blockPosition());
                    for (EquipmentSlot equipmentslot : EquipmentSlot.values()) {
                        human.setItemSlot(equipmentslot, ItemStack.EMPTY);
                    }

                    generateInventory(human, tag.contains("ranged"));
                    entity.setCustomName(null);
                }
                if (entity instanceof ArmorStand stand && ((ArmorStand) entity).getRandom().nextFloat() < 0.2f) {
                    int[] armorstandArmor = new int[]{0, 1, 2, 0, 1, 2, 3, 3, 3};
                    int staticPick = armorstandArmor[stand.getRandom().nextInt(armorstandArmor.length)];
                    for (EquipmentSlot equipmentslot : EquipmentSlot.values()) {
                        Item item = Mob.getEquipmentForSlot(equipmentslot, staticPick);

                        if (item != null) {
                            ItemStack is = item.getDefaultInstance();
                            stand.setItemSlot(equipmentslot, is);
                        }
                    }
                    entity.setCustomName(null);
                }

                entity.addTag(randomTag);
            }
        }
        if (entity instanceof Human human) {
            if (human.getPersonaId().isEmpty() && ModList.get().isLoaded("villagernames")) {
                CollectiveVillagerNames.nameEntity(human);
            }
        }
    }
}
