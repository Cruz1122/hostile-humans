package com.craftix.hostile_humans.client.renderer;

import com.craftix.hostile_humans.HostileHumans;
import com.craftix.hostile_humans.persona.PersonaDefinition;
import com.craftix.hostile_humans.persona.SkinStrategy;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public final class PersonaSkinCache {
    private static final Map<String, ResourceLocation> RESOLVED = new ConcurrentHashMap<>();
    private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();

    private PersonaSkinCache() {
    }

    public static ResourceLocation getOrRequest(PersonaDefinition persona, ResourceLocation fallback) {
        ResourceLocation resolved = RESOLVED.get(persona.id());
        if (resolved != null) return resolved;

        if (persona.skinStrategy() == SkinStrategy.BUNDLED) {
            // Bundled persona assets are intentionally absent until licensed textures are supplied.
            return fallback;
        }
        if (StringUtil.isNullOrEmpty(persona.minecraftUsername())) return fallback;
        if (REQUESTED.add(persona.id())) requestProfileSkin(persona);
        return fallback;
    }

    private static void requestProfileSkin(PersonaDefinition persona) {
        UUID uuid = parseUuid(persona.uuid(), persona.minecraftUsername());
        GameProfile profile = new GameProfile(uuid, persona.minecraftUsername());
        Minecraft.getInstance().getSkinManager().registerSkins(profile, (type, location, texture) -> {
            if (type == MinecraftProfileTexture.Type.SKIN) RESOLVED.put(persona.id(), location);
        }, false);
    }

    private static UUID parseUuid(String value, String username) {
        if (!StringUtil.isNullOrEmpty(value)) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException error) {
                HostileHumans.LOGGER.warn("Invalid UUID for persona skin {}: {}", username, value);
            }
        }
        return null;
    }
}
