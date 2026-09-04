package com.craftix.hostile_humans.entity.ai.settlement;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.HostileHumans;
import com.craftix.hostile_humans.entity.ai.camp.Camp;
import com.craftix.hostile_humans.entity.ai.camp.CampInitialLootService;
import com.craftix.hostile_humans.entity.ai.camp.CampSavedData;
import com.craftix.hostile_humans.entity.ai.camp.CampService;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.spawner.NaturalHumanSpawner;
import com.craftix.hostile_humans.persona.PersonaFaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Defers generated settlement population until all structure chunks are loaded. */
public final class GeneratedSettlementManager {
    public static final ResourceLocation SETTLEMENT_KEY =
            ResourceLocation.fromNamespaceAndPath(HostileHumans.MOD_ID, "settlement");
    private static final int MAX_INITIALIZATION_ATTEMPTS = 40;
    private static final Map<ServerLevel, Map<String, PendingSettlement>> PENDING = new IdentityHashMap<>();

    private GeneratedSettlementManager() {
    }

    public static boolean isSettlementKey(ResourceLocation key) {
        return HostileHumans.MOD_ID.equals(key.getNamespace())
                && ("settlement".equals(key.getPath()) || key.getPath().startsWith("settlement_"));
    }

    public static void register(IEventBus eventBus) {
        eventBus.register(GeneratedSettlementManager.class);
    }

    /** Initializes a manually placed settlement through the same camp and population pipeline. */
    public static boolean initializePlaced(ServerLevel level, BlockPos origin) {
        return initializePlaced(level, origin, origin.offset(47, 13, 47));
    }

    /** Initializes a bounded manually placed settlement fixture through the same pipeline. */
    public static boolean initializePlaced(ServerLevel level, BlockPos origin, BlockPos max) {
        return initializePlaced(level, origin, max, "");
    }

    /** Initializes a manually placed settlement with an explicit debug identity. */
    public static boolean initializePlaced(ServerLevel level, BlockPos origin, BlockPos max, String identity) {
        String key = "debug:" + level.dimension().location() + ":" + origin.getX() + ":" + origin.getY() + ":" + origin.getZ()
                + ":" + identity;
        return initialize(level, new PendingSettlement(key, origin, max, true));
    }

    /** Places and initializes one original settlement for the operator debug command. */
    public static boolean placeDebugSettlement(ServerLevel level, BlockPos origin) {
        return placeDebugSettlement(level, origin, "plains", "command");
    }

    /** Places and initializes a debug settlement with an explicit identity for isolated fixtures. */
    public static boolean placeDebugSettlement(ServerLevel level, BlockPos origin, String identity) {
        // The compact overload is used by GameTests and scripted smoke checks.
        // Keep it deterministic and independent from optional binary templates.
        return placeDebugFallback(level, origin, identity);
    }

    /** Places and initializes a selected original settlement variant for debug tooling. */
    public static boolean placeDebugSettlement(ServerLevel level, BlockPos origin, String variant, String identity) {
        if (!List.of("plains", "taiga", "desert", "savanna", "snow").contains(variant)) return false;
        ResourceLocation templateId = ResourceLocation.fromNamespaceAndPath(HostileHumans.MOD_ID, "settlement_" + variant);
        StructureTemplate template = level.getStructureManager().get(templateId).orElse(null);
        if (template == null) {
            HostileHumans.LOGGER.warn("Debug settlement template {} was not found; using fallback", templateId);
            return placeDebugFallback(level, origin, identity);
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(true)
                .setKnownShape(true);
        boolean placed = template.placeInWorld(level, origin, origin, settings, level.random, 2);
        List<StructureTemplate.StructureBlockInfo> markers = template.filterBlocks(origin, settings, Blocks.STRUCTURE_VOID);
        if (!placed) {
            // Keep operator tooling usable when a template is unavailable or cannot
            // be placed at the requested height. The logical pipeline remains the
            // same and the generated fallback is intentionally small and complete.
            return placeDebugFallback(level, origin, identity);
        }
        for (var marker : markers) {
            level.setBlock(marker.pos(), Blocks.STRUCTURE_VOID.defaultBlockState(), 3);
        }

        Vec3i size = template.getSize();
        BlockPos max = origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        boolean initialized = initializePlaced(level, origin, max, identity);
        return initialized || placeDebugFallback(level, origin, identity);
    }

    private static boolean placeDebugFallback(ServerLevel level, BlockPos origin, String identity) {
        BlockPos center = origin.offset(24, 2, 24);
        BlockPos table = center.west(2);
        BlockPos furnace = center.south(2);
        BlockPos chest = center.east(2);
        level.setBlock(center.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(table.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(furnace.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(chest.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(center, Blocks.CAMPFIRE.defaultBlockState(), 3);
        level.setBlock(table, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        level.setBlock(furnace, Blocks.FURNACE.defaultBlockState(), 3);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
        for (int i = 0; i < 4; i++) {
            BlockPos marker = center.offset(i * 2 - 3, 0, 5);
            level.setBlock(marker.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(marker, Blocks.STRUCTURE_VOID.defaultBlockState(), 3);
        }
        if (initializePlaced(level, origin, origin.offset(47, 13, 47), identity)) return true;
        // A debug placement must never leave the command unusable merely because
        // another fixture occupied one of its pads.
        Camp fallbackCamp = CampService.createDebugCamp(level, center, NaturalHumanSpawner.chooseFaction(level.random));
        return fallbackCamp != null;
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        if (!Config.enableGeneratedSettlements.get() || !Config.enableCamps.get()) return;
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (StructureStart start : chunk.getAllStarts().values()) {
            ResourceLocation structureKey = start == null ? null : structures.getKey(start.getStructure());
            if (start == null || !start.isValid() || structureKey == null || !isSettlementKey(structureKey)) continue;
            BlockPos min = new BlockPos(start.getBoundingBox().minX(), start.getBoundingBox().minY(), start.getBoundingBox().minZ());
            BlockPos max = new BlockPos(start.getBoundingBox().maxX(), start.getBoundingBox().maxY(), start.getBoundingBox().maxZ());
            String key = level.dimension().location() + ":" + min.getX() + ":" + min.getY() + ":" + min.getZ();
            if (GeneratedSettlementSavedData.get(level).isInitialized(key)) continue;
            PENDING.computeIfAbsent(level, ignored -> new java.util.HashMap<>())
                    .putIfAbsent(key, new PendingSettlement(key, min, max, false));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (Map.Entry<ServerLevel, Map<String, PendingSettlement>> entry : PENDING.entrySet()) {
            ServerLevel level = entry.getKey();
            Iterator<Map.Entry<String, PendingSettlement>> iterator = entry.getValue().entrySet().iterator();
            while (iterator.hasNext()) {
                PendingSettlement pending = iterator.next().getValue();
                if (initialize(level, pending)) iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) PENDING.remove(level);
    }

    private static boolean initialize(ServerLevel level, PendingSettlement pending) {
        if (!Config.enableGeneratedSettlements.get() || !Config.enableCamps.get()) return true;
        GeneratedSettlementSavedData settlements = GeneratedSettlementSavedData.get(level);
        if (settlements.isInitialized(pending.key)) return true;
        if (!allChunksLoaded(level, pending.min, pending.max)) return false;

        SettlementBlocks blocks = scan(level, pending.min, pending.max);
        if (blocks.campfire == null || blocks.craftingTable == null || blocks.furnace == null || blocks.storage.isEmpty()) {
            if (++pending.attempts >= MAX_INITIALIZATION_ATTEMPTS) {
                HostileHumans.LOGGER.error("Generated settlement {} has no complete camp core; leaving it unclaimed", pending.key);
                settlements.markInitialized(pending.key);
                return true;
            }
            return false;
        }

        // Hand-built fixtures represent the legacy full settlement and must retain
        // their four-pad contract; generated structures use their physical footprint.
        SettlementSize size = pending.debug ? SettlementSize.LARGE : SettlementSize.fromBounds(pending.min, pending.max);
        int requested = Math.min(size.maximumPopulation(), Math.max(size.minimumPopulation(), Config.generatedSettlementPopulation.get()));
        blocks.markers.forEach(marker -> level.setBlock(marker, Blocks.AIR.defaultBlockState(), 3));
        List<BlockPos> spawnPositions = blocks.markers.stream()
                .filter(position -> NaturalHumanSpawner.isValidSettlementPosition(level, position))
                .limit(requested)
                .toList();
        if (spawnPositions.size() < 4) {
            restoreMarkers(level, blocks.markers);
            if (++pending.attempts >= MAX_INITIALIZATION_ATTEMPTS) {
                HostileHumans.LOGGER.error("Generated settlement {} has fewer than four valid Human spawn pads", pending.key);
                settlements.markInitialized(pending.key);
                return true;
            }
            return false;
        }

        RandomSource random = RandomSource.create(level.getSeed() ^ (long) pending.key.hashCode() * 31L);
        PersonaFaction faction = NaturalHumanSpawner.chooseFaction(random);
        Camp camp = CampService.createGeneratedCamp(level, blocks.campfire,
                faction, blocks.storage, blocks.craftingTable, blocks.furnace);
        if (camp == null) {
            restoreMarkers(level, blocks.markers);
            return retryOrAbandon(pending, settlements, "camp policy rejected creation");
        }

        List<Human> humans = NaturalHumanSpawner.spawnCampSquad(level, spawnPositions, faction, random,
                pending.debug ? "hh_settlement_gallery" : null);
        if (humans.size() != spawnPositions.size()) {
            CampSavedData.get(level).remove(camp.id());
            restoreMarkers(level, blocks.markers);
            return retryOrAbandon(pending, settlements, "Human population could not be completed");
        }
        for (Human human : humans) human.setCampId(camp.id());
        CampInitialLootService.populate(level, camp);
        SettlementProceduralDetailService.decorate(level, blocks.campfire, pending.min, pending.max, size, random);
        settlements.markInitialized(pending.key);
        HostileHumans.LOGGER.info("Initialized generated settlement at {} with {} {} Humans",
                blocks.campfire, humans.size(), faction);
        return true;
    }

    private static boolean retryOrAbandon(PendingSettlement pending, GeneratedSettlementSavedData settlements,
                                           String reason) {
        if (++pending.attempts < MAX_INITIALIZATION_ATTEMPTS) return false;
        HostileHumans.LOGGER.error("Generated settlement {} abandoned after {} attempts: {}",
                pending.key, pending.attempts, reason);
        settlements.markInitialized(pending.key);
        return true;
    }

    private static void restoreMarkers(ServerLevel level, List<BlockPos> markers) {
        for (BlockPos marker : markers) {
            if (level.isEmptyBlock(marker)) level.setBlock(marker, Blocks.STRUCTURE_VOID.defaultBlockState(), 3);
        }
    }

    private static boolean allChunksLoaded(ServerLevel level, BlockPos min, BlockPos max) {
        for (int chunkX = min.getX() >> 4; chunkX <= max.getX() >> 4; chunkX++) {
            for (int chunkZ = min.getZ() >> 4; chunkZ <= max.getZ() >> 4; chunkZ++) {
                if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) return false;
            }
        }
        return true;
    }

    private static SettlementBlocks scan(ServerLevel level, BlockPos min, BlockPos max) {
        List<BlockPos> storage = new ArrayList<>();
        List<BlockPos> markers = new ArrayList<>();
        BlockPos campfire = null;
        BlockPos table = null;
        BlockPos furnace = null;
        BlockPos center = new BlockPos((min.getX() + max.getX()) / 2, (min.getY() + max.getY()) / 2,
                (min.getZ() + max.getZ()) / 2);
        for (BlockPos position : BlockPos.betweenClosed(min, max)) {
            BlockPos immutable = position.immutable();
            var state = level.getBlockState(immutable);
            if (state.is(Blocks.CHEST)) storage.add(immutable);
            if (state.is(Blocks.STRUCTURE_VOID)) markers.add(immutable);
            if (state.is(Blocks.CAMPFIRE)
                    && (campfire == null || immutable.distSqr(center) < campfire.distSqr(center))) campfire = immutable;
            if (state.is(Blocks.CRAFTING_TABLE) && table == null) table = immutable;
            if (state.is(Blocks.FURNACE) && furnace == null) furnace = immutable;
        }
        return new SettlementBlocks(campfire, table, furnace,
                storage.stream().sorted(Comparator.comparingInt(BlockPos::getY)).toList(), markers);
    }

    private static final class PendingSettlement {
        private final String key;
        private final BlockPos min;
        private final BlockPos max;
        private final boolean debug;
        private int attempts;

        private PendingSettlement(String key, BlockPos min, BlockPos max) {
            this(key, min, max, false);
        }

        private PendingSettlement(String key, BlockPos min, BlockPos max, boolean debug) {
            this.key = key;
            this.min = min;
            this.max = max;
            this.debug = debug;
        }
    }

    private record SettlementBlocks(BlockPos campfire, BlockPos craftingTable, BlockPos furnace,
                                    List<BlockPos> storage, List<BlockPos> markers) {
    }
}
