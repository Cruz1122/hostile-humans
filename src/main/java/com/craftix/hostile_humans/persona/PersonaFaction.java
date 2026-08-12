package com.craftix.hostile_humans.persona;

public enum PersonaFaction {
    HISPANIC_CREATORS,
    INTERNATIONAL_CREATORS,
    MINECRAFT_LEGENDS;

    public boolean isAlliedWith(PersonaFaction other) {
        return this == other;
    }
}
