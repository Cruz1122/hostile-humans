package com.craftix.hostile_humans.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Event-first progression detector with an infrequent inventory safety net. */
@Mod.EventBusSubscriber
public final class WorldGearProgressionTracker {
    private static final int SAFETY_SCAN_INTERVAL = 200;
    private static int safetyScanTicks;

    private WorldGearProgressionTracker() {
    }

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) observe(player, event.getItem().getItem());
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) observe(player, event.getCrafting());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) observePlayer(player);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WorldGearProgressionSavedData data = WorldGearProgressionSavedData.get(player.serverLevel());
            WorldGearProgressionSavedData.unlockForDimension(data, event.getTo());
            if (event.getTo() == Level.END) data.markEndVisited();
            observePlayer(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++safetyScanTicks < SAFETY_SCAN_INTERVAL) return;
        safetyScanTicks = 0;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!(player instanceof FakePlayer)) observePlayer(player);
        }
    }

    public static void observePlayer(ServerPlayer player) {
        if (player instanceof FakePlayer) return;
        WorldGearProgressionSavedData data = WorldGearProgressionSavedData.get(player.serverLevel());
        WorldGearProgressionSavedData.unlockForDimension(data, player.level().dimension());
        if (player.level().dimension() == Level.END) data.markEndVisited();
        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventory.items) observe(data, stack);
        for (ItemStack stack : inventory.armor) observe(data, stack);
        for (ItemStack stack : inventory.offhand) observe(data, stack);
    }

    public static void observe(ServerPlayer player, ItemStack stack) {
        if (player instanceof FakePlayer) return;
        observe(WorldGearProgressionSavedData.get(player.serverLevel()), stack);
    }

    public static void observe(WorldGearProgressionSavedData data, ItemStack stack) {
        Material material = materialOf(stack);
        if (material == null) return;
        switch (material) {
            case IRON -> data.unlockIron();
            case GOLD -> data.unlockGold();
            case DIAMOND -> data.unlockDiamond();
            case NETHERITE -> data.unlockNetherite();
        }
    }

    public static Material materialOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof TieredItem tiered
                && (stack.getItem() instanceof SwordItem || stack.getItem() instanceof DiggerItem)) {
            return materialOf(tiered.getTier());
        }
        if (stack.getItem() instanceof ArmorItem armor) {
            if (armor.getMaterial() == ArmorMaterials.IRON) return Material.IRON;
            if (armor.getMaterial() == ArmorMaterials.GOLD) return Material.GOLD;
            if (armor.getMaterial() == ArmorMaterials.DIAMOND) return Material.DIAMOND;
            if (armor.getMaterial() == ArmorMaterials.NETHERITE) return Material.NETHERITE;
        }
        return null;
    }

    private static Material materialOf(Tier tier) {
        if (tier == Tiers.IRON) return Material.IRON;
        if (tier == Tiers.GOLD) return Material.GOLD;
        if (tier == Tiers.DIAMOND) return Material.DIAMOND;
        if (tier == Tiers.NETHERITE) return Material.NETHERITE;
        return null;
    }

    public enum Material {
        IRON, GOLD, DIAMOND, NETHERITE
    }
}
