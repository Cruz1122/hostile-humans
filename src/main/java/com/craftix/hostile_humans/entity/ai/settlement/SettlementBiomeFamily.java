package com.craftix.hostile_humans.entity.ai.settlement;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/** Visual/material family used by the settlement worldgen pools. */
public enum SettlementBiomeFamily {
    PLAINS("plains"),
    TAIGA("taiga"),
    DESERT("desert"),
    SAVANNA("savanna"),
    SNOW("snow");

    private final String id;

    SettlementBiomeFamily(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public ResourceLocation templateId(SettlementSize size) {
        return ResourceLocation.fromNamespaceAndPath("hostile_humans", "settlement_" + id + "_" + size.id());
    }

    public static SettlementBiomeFamily byId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        for (SettlementBiomeFamily family : values()) if (family.id.equals(normalized)) return family;
        return PLAINS;
    }
}
