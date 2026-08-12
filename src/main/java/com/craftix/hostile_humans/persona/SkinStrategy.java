package com.craftix.hostile_humans.persona;

public enum SkinStrategy {
    MINECRAFT_PROFILE_LOOKUP("minecraft_profile_lookup"),
    BUNDLED("bundled");

    private final String serializedName;

    SkinStrategy(String serializedName) {
        this.serializedName = serializedName;
    }

    public static SkinStrategy fromSerializedName(String value) {
        for (SkinStrategy strategy : values()) {
            if (strategy.serializedName.equals(value)) return strategy;
        }
        throw new IllegalArgumentException("Unknown skin strategy: " + value);
    }
}
