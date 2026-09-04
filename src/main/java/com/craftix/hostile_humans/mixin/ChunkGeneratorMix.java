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
import static com.craftix.hostile_humans.entity.ai.settlement.GeneratedSettlementManager.SETTLEMENT_KEY;
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

            if (!isLegal(x, z, chunkAccess, randomState, chunkGenerator) || !isLegal(x - 16, z, chunkAccess, randomState, chunkGenerator) || !isLegal(x + 16, z, chunkAccess, randomState, chunkGenerator) || !isLegal(x, z - 16, chunkAccess, randomState, chunkGenerator) || !isLegal(x, z + 16, chunkAccess, randomState, chunkGenerator)) {
                cir.setReturnValue(false);
                return;
            }

            if (isStructureDisabled(key.get().location().getPath())) {
                cir.setReturnValue(false);
                return;
            }
            if (SETTLEMENT_KEY.equals(key.get().location()) && !Config.enableGeneratedSettlements.get()) {
                cir.setReturnValue(false);
            }
        }
    }

    @Unique
    private boolean isLegal(int x, int z, ChunkAccess chunkAccess, RandomState randomState, ChunkGenerator chunkGenerator) {
        int k = chunkGenerator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, chunkAccess, randomState);

        return k <= 78 && k >= 55;
    }
}
