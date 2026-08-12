package com.craftix.hostile_humans.client.renderer;

import com.craftix.hostile_humans.HostileHumans;
import com.craftix.hostile_humans.persona.PersonaDefinition;
import com.craftix.hostile_humans.persona.SkinStrategy;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
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
    private static volatile GameProfileRepository profileRepository;

    private PersonaSkinCache() {
    }

    public static ResourceLocation getOrRequest(PersonaDefinition persona, ResourceLocation fallback) {
        ResourceLocation resolved = RESOLVED.get(persona.id());
        if (resolved != null) return resolved;

        if (persona.skinStrategy() == SkinStrategy.BUNDLED) {
            return ResourceLocation.fromNamespaceAndPath(HostileHumans.MOD_ID,
                    "textures/entity/personas/" + persona.id() + ".png");
        }
        if (StringUtil.isNullOrEmpty(persona.minecraftUsername())) return fallback;
        if (REQUESTED.add(persona.id())) requestProfileSkin(persona);
        return fallback;
    }

    private static void requestProfileSkin(PersonaDefinition persona) {
        UUID uuid = parseUuid(persona.uuid(), persona.minecraftUsername());
        if (uuid != null) {
            registerProfileSkin(persona, new GameProfile(uuid, persona.minecraftUsername()));
            return;
        }

        Util.backgroundExecutor().execute(() -> getProfileRepository().findProfilesByNames(
                new String[]{persona.minecraftUsername()}, Agent.MINECRAFT, new ProfileLookupCallback() {
                    @Override
                    public void onProfileLookupSucceeded(GameProfile profile) {
                        registerProfileSkin(persona, profile);
                    }

                    @Override
                    public void onProfileLookupFailed(GameProfile profile, Exception error) {
                        HostileHumans.LOGGER.debug("Could not resolve Minecraft profile skin for {}",
                                persona.minecraftUsername(), error);
                    }
                }));
    }

    private static void registerProfileSkin(PersonaDefinition persona, GameProfile profile) {
        Minecraft.getInstance().getSkinManager().registerSkins(profile, (type, location, texture) -> {
            if (type == MinecraftProfileTexture.Type.SKIN) RESOLVED.put(persona.id(), location);
        }, false);
    }

    private static GameProfileRepository getProfileRepository() {
        GameProfileRepository repository = profileRepository;
        if (repository == null) {
            synchronized (PersonaSkinCache.class) {
                repository = profileRepository;
                if (repository == null) {
                    repository = new YggdrasilAuthenticationService(Minecraft.getInstance().getProxy())
                            .createProfileRepository();
                    profileRepository = repository;
                }
            }
        }
        return repository;
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
