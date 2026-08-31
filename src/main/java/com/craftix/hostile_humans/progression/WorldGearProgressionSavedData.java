package com.craftix.hostile_humans.progression;

import com.craftix.hostile_humans.HostileHumans;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent, irreversible gear milestones shared by every dimension. */
public final class WorldGearProgressionSavedData extends SavedData {
    private static final String FILE_ID = HostileHumans.MOD_ID + "_world_gear_progression";
    private static final String IRON = "IronUnlocked";
    private static final String GOLD = "GoldUnlocked";
    private static final String DIAMOND = "DiamondUnlocked";
    private static final String NETHERITE = "NetheriteUnlocked";

    private boolean ironUnlocked;
    private boolean goldUnlocked;
    private boolean diamondUnlocked;
    private boolean netheriteUnlocked;

    public static WorldGearProgressionSavedData get(ServerLevel level) {
        MinecraftServer server = level.getServer();
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                WorldGearProgressionSavedData::load,
                WorldGearProgressionSavedData::new,
                FILE_ID);
    }

    public boolean isIronUnlocked() {
        return ironUnlocked;
    }

    public boolean isGoldUnlocked() {
        return goldUnlocked;
    }

    public boolean isDiamondUnlocked() {
        return diamondUnlocked;
    }

    public boolean isNetheriteUnlocked() {
        return netheriteUnlocked;
    }

    public void unlockIron() {
        if (!ironUnlocked) {
            ironUnlocked = true;
            setDirty();
        }
    }

    public void unlockGold() {
        if (!goldUnlocked) {
            goldUnlocked = true;
            setDirty();
        }
    }

    public void unlockDiamond() {
        if (!diamondUnlocked) {
            diamondUnlocked = true;
            setDirty();
        }
    }

    public void unlockNetherite() {
        if (!netheriteUnlocked) {
            netheriteUnlocked = true;
            setDirty();
        }
    }

    public void unlockOverworldGear() {
        unlockIron();
        unlockGold();
        unlockDiamond();
    }

    public static void unlockForDimension(WorldGearProgressionSavedData data,
                                           net.minecraft.resources.ResourceKey<Level> dimension) {
        if (dimension == Level.NETHER || dimension == Level.END) data.unlockOverworldGear();
    }

    public static WorldGearProgressionSavedData load(CompoundTag root) {
        WorldGearProgressionSavedData data = new WorldGearProgressionSavedData();
        data.ironUnlocked = root.getBoolean(IRON);
        data.goldUnlocked = root.getBoolean(GOLD);
        data.diamondUnlocked = root.getBoolean(DIAMOND);
        data.netheriteUnlocked = root.getBoolean(NETHERITE);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        root.putBoolean(IRON, ironUnlocked);
        root.putBoolean(GOLD, goldUnlocked);
        root.putBoolean(DIAMOND, diamondUnlocked);
        root.putBoolean(NETHERITE, netheriteUnlocked);
        return root;
    }
}
