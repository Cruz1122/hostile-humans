package com.craftix.hostile_humans.mixin;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.craftix.hostile_humans.HumanUtil.isStructureDisabled;
import static com.craftix.hostile_humans.entity.ai.settlement.GeneratedSettlementManager.isSettlementKey;
import com.craftix.hostile_humans.Config;

@Mixin(value = ChunkGenerator.class)
public abstract class ChunkGeneratorMix {

    @Inject(method = "tryGenerateStructure", at = @At("HEAD"), cancellable = true)
    private void injected(StructureSet.StructureSelectionEntry structureSelectionEntry, StructureManager structureManager, RegistryAccess registryAccess, RandomState randomState, StructureTemplateManager structureTemplateManager, long seed, ChunkAccess chunkAccess, ChunkPos chunkPos, SectionPos sectionPos, CallbackInfoReturnable<Boolean> cir) {
        var key = structureSelectionEntry.structure().unwrapKey();

        if (key.isPresent() && key.get().location().toString().contains("hostile_humans")) {
            int x = chunkPos.getMiddleBlockX();
            int z = chunkPos.getMiddleBlockZ();
            ChunkGenerator chunkGenerator = (ChunkGenerator) (Object) this;

            boolean settlement = isSettlementKey(key.get().location());
            if ((!settlement && (!isLegal(x, z, chunkAccess, randomState, chunkGenerator)
                    || !isLegal(x - 16, z, chunkAccess, randomState, chunkGenerator)
                    || !isLegal(x + 16, z, chunkAccess, randomState, chunkGenerator)
                    || !isLegal(x, z - 16, chunkAccess, randomState, chunkGenerator)
                    || !isLegal(x, z + 16, chunkAccess, randomState, chunkGenerator)))
                    || (settlement && !isSettlementTerrainLegal(x, z, chunkAccess, randomState, chunkGenerator))) {
                cir.setReturnValue(false);
                return;
            }

            if (isStructureDisabled(key.get().location().getPath())) {
                cir.setReturnValue(false);
                return;
            }
            if (settlement && !Config.enableGeneratedSettlements.get()) {
                cir.setReturnValue(false);
            }
        }
    }

    @Unique
    private boolean isLegal(int x, int z, ChunkAccess chunkAccess, RandomState randomState, ChunkGenerator chunkGenerator) {
        int k = chunkGenerator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, chunkAccess, randomState);

        return k <= 78 && k >= 55;
    }

    @Unique
    private boolean isSettlementTerrainLegal(int x, int z, ChunkAccess chunkAccess, RandomState randomState,
                                             ChunkGenerator chunkGenerator) {
        // ChunkGenerator.tryGenerateStructure is called before neighboring
        // chunks are available. Sample the complete 16x16 candidate chunk
        // rather than wrapping ChunkAccess coordinates into other chunks.
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int height = surfaceHeight(x - 8 + localX, z - 8 + localZ, chunkAccess, randomState, chunkGenerator);
                if (height < 55 || height > 100) return false;
                minimum = Math.min(minimum, height);
                maximum = Math.max(maximum, height);
            }
        }
        // The base settlement is rigid. It must sit on a genuinely flat
        // platform; slopes are handled only by independent modules later.
        return maximum - minimum <= 3;
    }

    @Unique
    private int surfaceHeight(int x, int z, ChunkAccess chunkAccess, RandomState randomState,
                              ChunkGenerator chunkGenerator) {
        return chunkGenerator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, chunkAccess, randomState);
    }
}
