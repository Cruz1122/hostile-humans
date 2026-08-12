package com.craftix.hostile_humans.entity.ai.action;

import com.craftix.hostile_humans.HostileHumans;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class TacticalTags {
    public static final TagKey<Item> PILLAR_BLOCKS = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            new ResourceLocation(HostileHumans.MOD_ID, "pillar_blocks"));
    public static final TagKey<Item> BRIDGE_BLOCKS = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            new ResourceLocation(HostileHumans.MOD_ID, "bridge_blocks"));
    public static final TagKey<Item> FORBIDDEN_CONSTRUCTION_BLOCKS = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            new ResourceLocation(HostileHumans.MOD_ID, "forbidden_construction_blocks"));
    public static final TagKey<Block> NAVIGATION_BREAKABLE = BlockTags.create(new ResourceLocation(HostileHumans.MOD_ID, "navigation_breakable"));
    public static final TagKey<Block> NEVER_BREAK = BlockTags.create(new ResourceLocation(HostileHumans.MOD_ID, "never_break"));

    private TacticalTags() {}
}
