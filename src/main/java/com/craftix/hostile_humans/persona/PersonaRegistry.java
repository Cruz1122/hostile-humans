package com.craftix.hostile_humans.persona;

import com.craftix.hostile_humans.HostileHumans;
import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PersonaRegistry {
    private static final String RESOURCE_PATH = "/data/hostile_humans/personas/personas.json";
    private static final Gson GSON = new Gson();
    private static final PersonaRegistry INSTANCE = load();

    private final Map<String, PersonaDefinition> byId;
    private final Map<CombatSkillTier, List<PersonaDefinition>> byTier;
    private final Map<PersonaFaction, List<PersonaDefinition>> byFaction;
    private final EnumSet<CombatSkillTier> exhaustedTierWarnings = EnumSet.noneOf(CombatSkillTier.class);

    private PersonaRegistry(List<PersonaDefinition> definitions) {
        Map<String, PersonaDefinition> idIndex = new LinkedHashMap<>();
        Map<CombatSkillTier, List<PersonaDefinition>> tierIndex = new EnumMap<>(CombatSkillTier.class);
        Map<PersonaFaction, List<PersonaDefinition>> factionIndex = new EnumMap<>(PersonaFaction.class);
        for (CombatSkillTier tier : CombatSkillTier.values()) tierIndex.put(tier, new ArrayList<>());
        for (PersonaFaction faction : PersonaFaction.values()) factionIndex.put(faction, new ArrayList<>());

        for (PersonaDefinition definition : definitions) {
            if (idIndex.putIfAbsent(definition.id(), definition) != null) {
                throw new IllegalStateException("Duplicate persona ID: " + definition.id());
            }
            tierIndex.get(definition.combatSkillTier()).add(definition);
            factionIndex.get(definition.faction()).add(definition);
        }

        this.byId = Collections.unmodifiableMap(idIndex);
        tierIndex.replaceAll((tier, values) -> List.copyOf(values));
        factionIndex.replaceAll((faction, values) -> List.copyOf(values));
        this.byTier = Collections.unmodifiableMap(tierIndex);
        this.byFaction = Collections.unmodifiableMap(factionIndex);
    }

    public static PersonaRegistry get() {
        return INSTANCE;
    }

    public Optional<PersonaDefinition> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public List<PersonaDefinition> forTier(CombatSkillTier tier) {
        return byTier.get(tier);
    }

    public List<PersonaDefinition> forFaction(PersonaFaction faction) {
        return byFaction.get(faction);
    }

    public int size() {
        return byId.size();
    }

    public void warnPoolExhausted(CombatSkillTier tier) {
        if (exhaustedTierWarnings.add(tier)) {
            HostileHumans.LOGGER.warn("Persona pool {} is exhausted; new humans will use generic identity", tier);
        }
    }

    private static PersonaRegistry load() {
        try (InputStream stream = PersonaRegistry.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) throw new IllegalStateException("Missing persona resource " + RESOURCE_PATH);
            JsonArray root = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonArray.class);
            List<PersonaDefinition> definitions = new ArrayList<>(root.size());
            for (JsonElement element : root) definitions.add(parse(element.getAsJsonObject()));
            PersonaRegistry registry = new PersonaRegistry(definitions);
            HostileHumans.LOGGER.info("Loaded {} personas: {} Hispanic, {} International, {} Legends",
                    registry.size(),
                    registry.forFaction(PersonaFaction.HISPANIC_CREATORS).size(),
                    registry.forFaction(PersonaFaction.INTERNATIONAL_CREATORS).size(),
                    registry.forFaction(PersonaFaction.MINECRAFT_LEGENDS).size());
            return registry;
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Unable to load persona registry", error);
        }
    }

    private static PersonaDefinition parse(JsonObject object) {
        int tier = GsonHelper.getAsInt(object, "tier");
        if (tier < 1 || tier > 5) throw new IllegalArgumentException("Persona tier must be between 1 and 5");
        return new PersonaDefinition(
                requiredString(object, "id"),
                requiredString(object, "displayName"),
                GsonHelper.getAsString(object, "minecraftUsername", ""),
                GsonHelper.getAsString(object, "minecraftUsernameStatus", ""),
                GsonHelper.getAsString(object, "uuid", ""),
                GsonHelper.getAsString(object, "skinUrl", ""),
                SkinStrategy.fromSerializedName(requiredString(object, "skinStrategy")),
                PersonaFaction.valueOf(requiredString(object, "faction")),
                tier,
                GsonHelper.getAsBoolean(object, "uniqueWhileAlive"),
                GsonHelper.getAsString(object, "legendKind", "")
        );
    }

    private static String requiredString(JsonObject object, String key) {
        String value = GsonHelper.getAsString(object, key).trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Persona " + key + " must not be empty");
        return value;
    }
}
