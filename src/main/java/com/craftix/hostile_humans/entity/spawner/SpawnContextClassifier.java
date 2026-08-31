package com.craftix.hostile_humans.entity.spawner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.core.Registry;

/** Classifies only structures already present in loaded chunks. */
public final class SpawnContextClassifier {
    public static final TagKey<Structure> OVERWORLD_STRUCTURES = tag("overworld_structures");
    public static final TagKey<Structure> NETHER_FORTRESSES = tag("nether_fortresses");
    public static final TagKey<Structure> BASTIONS = tag("bastions");
    public static final TagKey<Structure> END_CITIES = tag("end_cities");

    private SpawnContextClassifier() {
    }

    public static SpawnContext classify(ServerLevel level, BlockPos position) {
        StructureFlags structures = findStructures(level, position);
        boolean cave = isCave(level, position);
        return classify(level.dimension(), structures, cave);
    }

    public static SpawnContext classify(ResourceKey<Level> dimension, StructureFlags structures, boolean cave) {
        if (dimension == Level.NETHER) {
            if (structures.bastion()) return SpawnContext.BASTION;
            if (structures.fortress()) return SpawnContext.NETHER_FORTRESS;
            return SpawnContext.NETHER_WILDS;
        }
        if (dimension == Level.END) return structures.endCity() ? SpawnContext.END_CITY : SpawnContext.END_WILDS;
        if (structures.village()) return SpawnContext.VILLAGE;
        if (structures.overworldStructure()) return SpawnContext.OVERWORLD_STRUCTURE;
        return cave ? SpawnContext.OVERWORLD_CAVE : SpawnContext.OVERWORLD_SURFACE;
    }

    public static boolean isCave(ServerLevel level, BlockPos position) {
        if (level.dimension() != Level.OVERWORLD) return false;
        if (level.canSeeSky(position)) return false;
        int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                position.getX(), position.getZ());
        return position.getY() + 1 < surfaceY;
    }

    public static StructureFlags findStructures(ServerLevel level, BlockPos position) {
        boolean village = false;
        boolean overworld = false;
        boolean fortress = false;
        boolean bastion = false;
        boolean endCity = false;
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);

        int chunkX = position.getX() >> 4;
        int chunkZ = position.getZ() >> 4;
        for (int x = chunkX - 1; x <= chunkX + 1; x++) {
            for (int z = chunkZ - 1; z <= chunkZ + 1; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;
                for (StructureStart start : chunk.getAllStarts().values()) {
                    if (start == null || !start.isValid()) continue;
                    boolean contains = start.getBoundingBox().isInside(position);
                    if (!contains) {
                        contains = start.getPieces().stream()
                                .anyMatch(piece -> piece.getBoundingBox().isInside(position));
                    }
                    if (!contains) continue;
                    Structure structure = start.getStructure();
                    if (registry.wrapAsHolder(structure).is(StructureTags.VILLAGE)) village = true;
                    if (registry.wrapAsHolder(structure).is(OVERWORLD_STRUCTURES)) overworld = true;
                    if (registry.wrapAsHolder(structure).is(NETHER_FORTRESSES)) fortress = true;
                    if (registry.wrapAsHolder(structure).is(BASTIONS)) bastion = true;
                    if (registry.wrapAsHolder(structure).is(END_CITIES)) endCity = true;
                }
            }
        }
        return new StructureFlags(village, overworld, fortress, bastion, endCity);
    }

    public static boolean isLoaded(ServerLevel level, BlockPos position) {
        return level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4) != null;
    }

    private static TagKey<Structure> tag(String path) {
        return TagKey.create(Registries.STRUCTURE,
                ResourceLocation.fromNamespaceAndPath("hostile_humans", "human_spawn/" + path));
    }

    public record StructureFlags(boolean village, boolean overworldStructure, boolean fortress,
                                 boolean bastion, boolean endCity) {
    }
}
