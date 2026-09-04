package com.craftix.hostile_humans.entity.ai.camp;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.ai.action.WorldActionSupport;
import com.craftix.hostile_humans.persona.PersonaFaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

/** Creates the deliberately small physical camp layout used by debug and AI entry points. */
public final class CampService {
    private CampService() {}

    public static Camp createDebugCamp(ServerLevel level, BlockPos center, PersonaFaction faction) {
        if (!level.getWorldBorder().isWithinBounds(center) || !loaded(level, center)) return null;
        BlockPos campfire = center;
        BlockPos chest = center.east();
        BlockPos table = center.west();
        BlockPos furnace = center.south();
        level.setBlock(campfire, Blocks.CAMPFIRE.defaultBlockState(), 3);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(table, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        level.setBlock(furnace, Blocks.FURNACE.defaultBlockState(), 3);
        Camp camp = new Camp(UUID.randomUUID(), level.dimension(), center, faction,
                List.of(chest), table, furnace, campfire);
        CampSavedData.get(level).add(camp);
        CampInitialLootService.populate(level, camp);
        return camp;
    }

    /** Registers an established camp whose stations were generated as part of a settlement. */
    public static Camp createGeneratedCamp(ServerLevel level, BlockPos center, PersonaFaction faction,
                                           List<BlockPos> storage, BlockPos table, BlockPos furnace) {
        if (!Config.enableCamps.get() || faction == null || center == null || storage.isEmpty()
                || table == null || furnace == null) return null;
        CampSavedData data = CampSavedData.get(level);
        if (data.count(level.dimension()) >= Config.maxCampsPerDimension.get()
                || data.hasNearby(level.dimension(), center, Config.minCampSpacing.get())) return null;
        Camp camp = new Camp(UUID.randomUUID(), level.dimension(), center, faction, storage, table, furnace, center);
        return data.add(camp) ? camp : null;
    }

    /** Attempts a cheap, resource-backed foundation for a loaded squad member. */
    public static Camp tryFound(Human founder) {
        if (!(founder.level() instanceof ServerLevel level) || !Config.enableCamps.get()
                || !founder.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)
                || founder.getSquadId() == null || founder.getCampId() != null || founder.getData() == null) return null;
        CampSavedData data = CampSavedData.get(level);
        BlockPos center = founder.blockPosition().east(2);
        if (data.count(level.dimension()) >= Config.maxCampsPerDimension.get()
                || data.hasNearby(level.dimension(), center, Config.minCampSpacing.get())
                || !canFound(level, center)) return null;
        // A foundation consumes real items rather than generating a free camp.
        if (!hasItem(founder, net.minecraft.world.item.Items.CHEST)
                || !hasItem(founder, net.minecraft.world.item.Items.CRAFTING_TABLE)
                || !hasItem(founder, net.minecraft.world.item.Items.FURNACE)
                || !hasItem(founder, net.minecraft.world.item.Items.CAMPFIRE)) return null;
        BlockPos chest = center.east(), table = center.west(), furnace = center.south();
        PersonaFaction faction = founder.getPersonaDefinition().map(def -> def.faction()).orElse(null);
        if (faction == null) return null;
        List<BlockPos> positions = List.of(chest, table, furnace, center);
        List<net.minecraft.world.item.Item> items = List.of(net.minecraft.world.item.Items.CHEST,
                net.minecraft.world.item.Items.CRAFTING_TABLE, net.minecraft.world.item.Items.FURNACE,
                net.minecraft.world.item.Items.CAMPFIRE);
        List<net.minecraft.world.level.block.state.BlockState> originalBlocks = positions.stream()
                .map(level::getBlockState).toList();
        List<net.minecraft.world.item.ItemStack> originalInventory = new ArrayList<>();
        for (int slot = 0; slot < founder.getData().getInventoryItemsSize(); slot++) originalInventory.add(founder.getData().getInventoryItem(slot).copy());
        for (int index = 0; index < positions.size(); index++) {
            if (!placeFromInventory(founder, positions.get(index), items.get(index))) {
                for (int restore = 0; restore < positions.size(); restore++) level.setBlock(positions.get(restore), originalBlocks.get(restore), 3);
                for (int slot = 0; slot < originalInventory.size(); slot++) founder.getData().setInventoryItem(slot, originalInventory.get(slot));
                return null;
            }
        }
        Camp camp = new Camp(UUID.randomUUID(), level.dimension(), center, faction, List.of(chest), table, furnace, center);
        if (!data.add(camp)) return null;
        founder.setCampId(camp.id());
        return camp;
    }

    private static boolean canFound(ServerLevel level, BlockPos center) {
        for (BlockPos pos : List.of(center, center.east(), center.west(), center.south())) {
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).canBeReplaced()) return false;
        }
        return true;
    }

    private static boolean loaded(ServerLevel level, BlockPos center) {
        return List.of(center, center.east(), center.west(), center.south()).stream().allMatch(level::hasChunkAt);
    }

    private static boolean hasItem(Human human, net.minecraft.world.item.Item item) {
        return human.getData().getInventoryItems().stream().anyMatch(stack -> stack.is(item));
    }

    private static boolean placeFromInventory(Human human, BlockPos pos, net.minecraft.world.item.Item item) {
        for (int slot = 0; slot < human.getData().getInventoryItemsSize(); slot++) {
            var stack = human.getData().getInventoryItem(slot);
            if (!stack.is(item)) continue;
            if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)) return false;
            if (!WorldActionSupport.place(human, pos, stack, blockItem.getBlock().defaultBlockState(), net.minecraft.core.Direction.UP)) return false;
            human.getData().setInventoryItem(slot, stack.copy());
            return true;
        }
        return false;
    }
}
