package com.craftix.hostile_humans.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Beardifier.class)
public abstract class BeardifierMix {

    @Inject(method = "forStructuresInChunk", at = @At("HEAD"), cancellable = true)
    private static void injected(StructureManager structureManager, ChunkPos chunkPos, CallbackInfoReturnable<Beardifier> cir) {
        int minBlockX = chunkPos.getMinBlockX();
        int minBlockZ = chunkPos.getMinBlockZ();
        ObjectList<Beardifier.Rigid> rigids = new ObjectArrayList<>(10);
        ObjectList<JigsawJunction> junctions = new ObjectArrayList<>(32);

        for (StructureStart structureStart : structureManager.startsForStructure(chunkPos, structure -> structure.terrainAdaptation() != TerrainAdjustment.NONE)) {
            TerrainAdjustment terrainAdjustment = structureStart.getStructure().terrainAdaptation();
            ObjectList<Beardifier.Rigid> structureRigids = new ObjectArrayList<>();
            ObjectList<JigsawJunction> structureJunctions = new ObjectArrayList<>();
            for (StructurePiece structurePiece : structureStart.getPieces()) {
                if (!structurePiece.isCloseToChunk(chunkPos, 12)) {
                    continue;
                }

                if (structurePiece instanceof PoolElementStructurePiece poolElementStructurePiece) {
                    if (poolElementStructurePiece.getElement().getProjection() == StructureTemplatePool.Projection.RIGID) {
                        structureRigids.add(new Beardifier.Rigid(poolElementStructurePiece.getBoundingBox(), terrainAdjustment, poolElementStructurePiece.getGroundLevelDelta()));
                    }

                    for (JigsawJunction jigsawJunction : poolElementStructurePiece.getJunctions()) {
                        int sourceX = jigsawJunction.getSourceX();
                        int sourceZ = jigsawJunction.getSourceZ();
                        if (sourceX > minBlockX - 12 && sourceZ > minBlockZ - 12 && sourceX < minBlockX + 15 + 12 && sourceZ < minBlockZ + 15 + 12) {
                            structureJunctions.add(jigsawJunction);
                        }
                    }
                } else {
                    structureRigids.add(new Beardifier.Rigid(structurePiece.getBoundingBox(), terrainAdjustment, 0));
                }
            }

            rigids.addAll(structureRigids);
            junctions.addAll(structureJunctions);
        }

        cir.setReturnValue(new Beardifier(rigids.iterator(), junctions.iterator()));
    }
}
