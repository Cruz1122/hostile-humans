package com.craftix.hostile_humans.entity.loadout;

import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import com.craftix.hostile_humans.entity.spawner.SpawnContext;
import com.craftix.hostile_humans.progression.WorldGearProgressionSnapshot;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;

/** Builds a self-contained operator showcase for every tier and progression profile. */
public final class HumanLoadoutShowcase {
    public static final String SHOWCASE_TAG = "hh_loadout_showcase";
    private static final String LABEL_TAG = "hh_loadout_showcase_label";
    private static final int NPCS_PER_CUBICLE = 5;
    private static final int CUBICLE_WIDTH = 11;
    private static final int CUBICLE_DEPTH = 11;
    private static final int SECTION_WIDTH = 35;
    private static final int SECTION_DEPTH = 25;
    private static final int SECTION_SPAN_X = 43;
    private static final int SECTION_SPAN_Z = 35;
    private static final long DEMONSTRATION_AGE_TICKS = 5_184_000L;
    private static final Map<ServerLevel, BlockPos> LAST_ORIGINS = new IdentityHashMap<>();

    private static final List<Profile> PROFILES = List.of(
            new Profile("IRON_ONLY", "IRON ONLY - iron unlocked", SpawnContext.OVERWORLD_SURFACE,
                    new WorldGearProgressionSnapshot(true, false, false, false, false),
                    Blocks.IRON_BLOCK.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState(),
                    Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState()),
            new Profile("NETHER", "NETHER - all materials unlocked, End not visited", SpawnContext.NETHER_WILDS,
                    new WorldGearProgressionSnapshot(true, true, true, true, false),
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(),
                    Blocks.RED_STAINED_GLASS.defaultBlockState()),
            new Profile("END", "END - End visited", SpawnContext.END_CITY,
                    new WorldGearProgressionSnapshot(true, true, true, true, true),
                    Blocks.END_STONE_BRICKS.defaultBlockState(), Blocks.PURPUR_BLOCK.defaultBlockState(),
                    Blocks.MAGENTA_STAINED_GLASS.defaultBlockState()),
            new Profile("POST_END_OVERWORLD", "OVERWORLD POST-END - End visited", SpawnContext.OVERWORLD_SURFACE,
                    new WorldGearProgressionSnapshot(true, true, true, true, true),
                    Blocks.QUARTZ_BLOCK.defaultBlockState(), Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                    Blocks.CYAN_STAINED_GLASS.defaultBlockState())
    );

    private HumanLoadoutShowcase() {
    }

    public static int build(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition()).offset(3, 0, 3);
        clearPreviousShowcase(level, origin);
        buildPlatform(level, origin);

        int spawned = 0;
        for (int profileIndex = 0; profileIndex < PROFILES.size(); profileIndex++) {
            Profile profile = PROFILES.get(profileIndex);
            BlockPos sectionOrigin = origin.offset((profileIndex % 2) * SECTION_SPAN_X, 0,
                    (profileIndex / 2) * SECTION_SPAN_Z);
            buildSection(level, sectionOrigin, profile);
            for (int tierIndex = 0; tierIndex < CombatSkillTier.values().length; tierIndex++) {
                CombatSkillTier tier = CombatSkillTier.values()[tierIndex];
                BlockPos cubicleOrigin = sectionOrigin.offset((tierIndex % 3) * 12,
                        0, (tierIndex / 3) * 14);
                addLabel(level, cubicleOrigin.offset(5, 6, -1), tier + " - 5 samples");
                for (int sample = 0; sample < NPCS_PER_CUBICLE; sample++) {
                    Human human = spawnSample(level, cubicleOrigin, profile, tier, sample);
                    if (human != null) spawned++;
                }
            }
            addLabel(level, sectionOrigin.offset(SECTION_WIDTH / 2, 7, -2), profile.title());
        }

        BlockPos end = origin.offset(SECTION_SPAN_X + SECTION_WIDTH, 0, SECTION_SPAN_Z + SECTION_DEPTH);
        int totalSpawned = spawned;
        source.sendSuccess(() -> Component.literal("Built loadout showcase: " + totalSpawned
                + " NPCs in 4 sections, 20 cubicles, 5 samples per tier."), true);
        source.sendSuccess(() -> Component.literal("Profiles: IRON_ONLY, NETHER, END, POST_END_OVERWORLD."
                + " Progression bypass is synthetic and does not change the world save."), false);
        source.sendSuccess(() -> Component.literal("Inspect one NPC with: /hostilehumans inspect @e[tag="
                + SHOWCASE_TAG + ",limit=1,sort=nearest]"), false);
        source.sendSuccess(() -> Component.literal("Showcase area: " + origin.toShortString() + " to "
                + end.toShortString()), false);
        return 1;
    }

    private static void clearPreviousShowcase(ServerLevel level, BlockPos origin) {
        BlockPos previousOrigin = LAST_ORIGINS.put(level, origin);
        if (previousOrigin != null) clearShowcaseBlocks(level, previousOrigin);
        AABB area = new AABB(origin).inflate(100.0D, 20.0D, 100.0D);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, area,
                candidate -> candidate.getTags().contains(SHOWCASE_TAG)
                        || candidate.getTags().contains(LABEL_TAG))) {
            entity.discard();
        }
    }

    private static void clearShowcaseBlocks(ServerLevel level, BlockPos origin) {
        int width = SECTION_SPAN_X + SECTION_WIDTH;
        int depth = SECTION_SPAN_Z + SECTION_DEPTH;
        for (int x = 0; x <= width; x++) {
            for (int y = 0; y <= 7; y++) {
                for (int z = 0; z <= depth; z++) {
                    level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void buildPlatform(ServerLevel level, BlockPos origin) {
        int width = SECTION_SPAN_X + SECTION_WIDTH;
        int depth = SECTION_SPAN_Z + SECTION_DEPTH;
        for (int x = 0; x <= width; x++) {
            for (int z = 0; z <= depth; z++) {
                level.setBlock(origin.offset(x, 0, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            }
        }
    }

    private static void buildSection(ServerLevel level, BlockPos origin, Profile profile) {
        int[] cubicleX = {0, 12, 24, 0, 12};
        int[] cubicleZ = {0, 0, 0, 14, 14};
        for (int i = 0; i < cubicleX.length; i++) {
            buildCubicle(level, origin.offset(cubicleX[i], 0, cubicleZ[i]), profile);
        }
        for (int x = 0; x < SECTION_WIDTH; x++) {
            level.setBlock(origin.offset(x, 1, SECTION_DEPTH), profile.wall(), 3);
        }
    }

    private static void buildCubicle(ServerLevel level, BlockPos origin, Profile profile) {
        for (int x = 0; x < CUBICLE_WIDTH; x++) {
            for (int z = 0; z < CUBICLE_DEPTH; z++) {
                level.setBlock(origin.offset(x, 0, z), profile.floor(), 3);
                level.setBlock(origin.offset(x, 6, z), profile.glass(), 3);
            }
        }
        for (int y = 1; y < 6; y++) {
            for (int x = 0; x < CUBICLE_WIDTH; x++) {
                for (int z = 0; z < CUBICLE_DEPTH; z++) {
                    boolean boundary = x == 0 || x == CUBICLE_WIDTH - 1 || z == 0 || z == CUBICLE_DEPTH - 1;
                    if (!boundary) continue;
                    BlockState state = z == 0 && x > 0 && x < CUBICLE_WIDTH - 1
                            ? profile.glass() : profile.wall();
                    level.setBlock(origin.offset(x, y, z), state, 3);
                }
            }
        }
    }

    private static Human spawnSample(ServerLevel level, BlockPos cubicleOrigin, Profile profile,
                                     CombatSkillTier tier, int sample) {
        Human human = ModEntityType.ROAMER.get().create(level);
        if (human == null) return null;
        human.moveTo(cubicleOrigin.getX() + 2.0D + sample, cubicleOrigin.getY() + 1.0D,
                cubicleOrigin.getZ() + 5.0D, 0.0F, 0.0F);
        human.addTag(SHOWCASE_TAG);
        human.addTag("hh_showcase_" + profile.id().toLowerCase(java.util.Locale.ROOT));
        human.addTag("hh_showcase_" + tier.name().toLowerCase(java.util.Locale.ROOT));
        human.setCustomName(Component.literal(profile.id() + " / " + tier + " / sample " + (sample + 1)));
        human.setCustomNameVisible(true);
        human.setNoAi(true);
        human.setInvulnerable(true);
        human.setSpawnContext(profile.context());
        human.setCombatSkillTierOverride(tier);
        human.initializeProceduralLoadoutForDebug(profile.progression(), DEMONSTRATION_AGE_TICKS);
        if (!level.addFreshEntity(human)) {
            human.discard();
            return null;
        }
        // Registration synchronizes HumanData after the initial procedural
        // roll. Repair any empty armor slots once that handoff is complete.
        HumanLoadoutGenerator.ensureFullArmorForDebug(level, human, profile.progression(), DEMONSTRATION_AGE_TICKS);
        return human;
    }

    private static void addLabel(ServerLevel level, BlockPos position, String text) {
        ArmorStand label = EntityType.ARMOR_STAND.create(level);
        if (label == null) return;
        label.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, 0.0F, 0.0F);
        label.addTag(LABEL_TAG);
        label.setInvisible(true);
        label.setNoGravity(true);
        label.setCustomName(Component.literal(text));
        label.setCustomNameVisible(true);
        level.addFreshEntity(label);
    }

    private record Profile(String id, String title, SpawnContext context,
                           WorldGearProgressionSnapshot progression, BlockState floor,
                           BlockState wall, BlockState glass) {
    }
}
