package com.craftix.hostile_humans;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.craftix.hostile_humans.client.keymapping.ModKeyMapping;
import com.craftix.hostile_humans.client.renderer.ClientRenderer;
import com.craftix.hostile_humans.entity.loadout.HumanLoadoutManager;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import com.craftix.hostile_humans.entity.spawner.NaturalHumanSpawner;
import com.craftix.hostile_humans.entity.spawner.SpawnHandler;
import com.craftix.hostile_humans.event.EventHandler;
import com.craftix.hostile_humans.item.ModItems;
import com.craftix.hostile_humans.network.NetworkHandler;
import com.craftix.hostile_humans.sounds.ModSoundEvents;
import com.mojang.logging.LogUtils;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(HostileHumans.MOD_ID)
public class HostileHumans {

    public static final String MOD_ID = "hostile_humans";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HostileHumans() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        final IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC, "hostile_humans.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC, "hostile_humans-server.toml");

        modEventBus.addListener(NetworkHandler::registerNetworkHandler);
        modEventBus.addListener(HostileHumans::buildCreativeTabContents);

        ModEntityType.ENTITIES.register(modEventBus);

        ModItems.ITEMS.register(modEventBus);

        ModSoundEvents.SOUNDS.register(modEventBus);

        modEventBus.addListener(SpawnHandler::registerSpawnPlacements);

        forgeEventBus.addListener(ServerSetup::handleServerStartingEvent);
        forgeEventBus.addListener(HumanLoadoutManager::addReloadListener);
        forgeEventBus.addListener(HostileHumansCommands::register);

        MinecraftForge.EVENT_BUS.register(new EventHandler());
        NaturalHumanSpawner.register(MinecraftForge.EVENT_BUS);
        
        populatePatreonNames(new File(FMLPaths.GAMEDIR.get().toFile(), "hhpatreonnamescache.txt"), true);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modEventBus.addListener(ClientRenderer::registerEntityLayerDefinitions);
            modEventBus.addListener(ClientRenderer::registerEntityRenderers);
            modEventBus.addListener(ClientSetup::new);
            modEventBus.addListener(ModKeyMapping::registerKeyMapping);
        });
    }

    private static void buildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.HUMAN1_SPAWN_EGG);
            event.accept(ModItems.HUMAN2_SPAWN_EGG);
            event.accept(ModItems.ROAMER_SPAWN_EGG);
        }
    }
	
    public static List<String> patreonNames = new ArrayList<>();
	public static void cacheSupporters(URL url, File cache) throws IOException {
		BufferedReader store = new BufferedReader(new InputStreamReader(url.openStream()));
		if (!cache.exists()) cache.createNewFile();
		try(PrintWriter printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(cache), StandardCharsets.UTF_8))){
			store.lines().forEach((line) -> printwriter.println(line));
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		store.close();
	}

	public static void populatePatreonNames(File cache, boolean createCache) {
		BufferedReader read = null;
		try {
			URL url = new URI("https://raw.githubusercontent.com/L0WSP0T/Patreon-Names/refs/heads/main/names.txt").toURL();
			System.out.println("Caching patreon names");
			URLConnection connection = url.openConnection();
			connection.setConnectTimeout(10000);
			connection.connect();
			read = new BufferedReader(new InputStreamReader(url.openStream()));
			if (createCache) cacheSupporters(url, cache);
			
		} catch (IOException e) {
			if (cache.exists()) {
				FileInputStream stream;
				try {
					stream = new FileInputStream(cache);
					InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
					read = new BufferedReader(reader);
				} catch (FileNotFoundException e1) {
				}
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		if (read != null) {
			read.lines().forEach(patreonNames::add);
			try {
				read.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
		}
	}
}
