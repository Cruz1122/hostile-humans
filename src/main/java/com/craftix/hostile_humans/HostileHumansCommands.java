package com.craftix.hostile_humans;

import com.craftix.hostile_humans.entity.data.HumanData;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;
import com.craftix.hostile_humans.entity.loadout.HumanLoadoutAudit;
import com.craftix.hostile_humans.entity.loadout.HumanLoadoutShowcase;
import com.craftix.hostile_humans.entity.spawner.SpawnContext;
import com.craftix.hostile_humans.entity.spawner.SpawnContextClassifier;
import com.craftix.hostile_humans.entity.ai.camp.Camp;
import com.craftix.hostile_humans.entity.ai.camp.CampSavedData;
import com.craftix.hostile_humans.entity.ai.mission.CampMissionController;
import com.craftix.hostile_humans.entity.ai.camp.CampService;
import com.craftix.hostile_humans.entity.ai.settlement.GeneratedSettlementManager;
import com.craftix.hostile_humans.persona.PersonaFaction;
import com.craftix.hostile_humans.progression.WorldGearProgressionSavedData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.nio.file.Path;
import java.util.stream.Collectors;

/** Operator-only commands used to inspect the server-side Human state. */
public final class HostileHumansCommands {
    private static final int DEFAULT_RADIUS = 32;
    private static final int MAX_RADIUS = 128;

    private HostileHumansCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("hostilehumans")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("inventory")
                        .executes(context -> inspectInventory(context, DEFAULT_RADIUS))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_RADIUS))
                                 .executes(context -> inspectInventory(context,
                                         IntegerArgumentType.getInteger(context, "radius")))))
                .then(Commands.literal("inspect")
                        .then(Commands.argument("npc", EntityArgument.entity())
                                .executes(HostileHumansCommands::inspectNpc)))
                .then(Commands.literal("progression")
                        .executes(HostileHumansCommands::inspectProgression))
                .then(Commands.literal("context")
                        .executes(HostileHumansCommands::inspectContext))
                .then(Commands.literal("loadout")
                        .then(Commands.literal("overworld").executes(c -> spawnDebugLoadout(c.getSource(), SpawnContext.OVERWORLD_SURFACE, null)))
                        .then(Commands.literal("nether").executes(c -> spawnDebugLoadout(c.getSource(), SpawnContext.NETHER_WILDS, null)))
                        .then(Commands.literal("bastion").executes(c -> spawnDebugLoadout(c.getSource(), SpawnContext.BASTION, null)))
                        .then(Commands.literal("end").executes(c -> spawnDebugLoadout(c.getSource(), SpawnContext.END_WILDS, null)))
                         .then(Commands.literal("tiers").executes(HostileHumansCommands::spawnDebugTiers)))
                .then(Commands.literal("showcase")
                        .executes(context -> HumanLoadoutShowcase.build(context.getSource())))
                         .then(Commands.literal("audit")
                        .executes(context -> auditLoadouts(context, 10_000))
                        .then(Commands.argument("samples", IntegerArgumentType.integer(100, 100_000))
                                 .executes(context -> auditLoadouts(context,
                                         IntegerArgumentType.getInteger(context, "samples")))))
                 .then(Commands.literal("camp").executes(HostileHumansCommands::createDebugCamp))
                 .then(Commands.literal("settlement")
                         .executes(HostileHumansCommands::initializeDebugSettlement)
                         .then(Commands.argument("variant", StringArgumentType.word())
                                 .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                         List.of("plains", "taiga", "desert", "savanna", "snow"), builder))
                                 .executes(HostileHumansCommands::initializeDebugSettlementVariant)))
                .then(Commands.literal("expedition").executes(HostileHumansCommands::startDebugExpedition))
                .then(Commands.literal("raid").executes(HostileHumansCommands::startDebugRaid)));
    }

    public static Optional<Human> findNearest(ServerLevel level, Vec3 origin, double radius) {
        AABB area = new AABB(origin.x - radius, origin.y - radius, origin.z - radius,
                origin.x + radius, origin.y + radius, origin.z + radius);
        double maxDistanceSqr = radius * radius;
        return level.getEntitiesOfClass(Human.class, area)
                .stream()
                .filter(human -> human.distanceToSqr(origin.x, origin.y, origin.z) <= maxDistanceSqr)
                .min(Comparator.comparingDouble((Human human) -> human.distanceToSqr(origin.x, origin.y, origin.z))
                        .thenComparing(Human::getUUID));
    }

    private static int inspectInventory(CommandContext<CommandSourceStack> context, int radius) {
        CommandSourceStack source = context.getSource();
        Human human = findNearest(source.getLevel(), source.getPosition(), radius).orElse(null);
        if (human == null) {
            source.sendFailure(Component.literal("No Hostile Humans NPC found within " + radius + " blocks."));
            return 0;
        }

        Vec3 origin = source.getPosition();
        double distance = Math.sqrt(human.distanceToSqr(origin.x, origin.y, origin.z));
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Nearest Human: %s (%s), UUID=%s, position=(%d, %d, %d), distance=%.1f",
                human.getName().getString(), BuiltInRegistries.ENTITY_TYPE.getKey(human.getType()), human.getUUID(),
                human.getBlockX(), human.getBlockY(), human.getBlockZ(), distance)), false);
        sendExperience(source, human);
        sendEquipment(source, human);

        sendInventory(source, human);
        return 1;
    }

    private static int inspectNpc(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Entity entity = EntityArgument.getEntity(context, "npc");
        if (!(entity instanceof Human human)) {
            source.sendFailure(Component.literal("The selected entity is not a Hostile Humans NPC."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Selected Human: " + human.getName().getString()
                + ", UUID=" + human.getUUID() + ", position=(" + human.getBlockX() + ", "
                + human.getBlockY() + ", " + human.getBlockZ() + ")"), false);
        sendExperience(source, human);
        sendEquipment(source, human);
        sendInventory(source, human);
        return 1;
    }

    private static void sendInventory(CommandSourceStack source, Human human) {
        HumanData data = human.getData();
        boolean hasInventory = false;
        if (data != null) {
            for (int slot = 0; slot < data.getInventoryItemsSize(); slot++) {
                ItemStack stack = data.getInventoryItem(slot);
                if (stack.isEmpty()) continue;
                hasInventory = true;
                int slotNumber = slot;
                source.sendSuccess(() -> Component.literal("  inventory[" + slotNumber + "]: " + describe(stack)), false);
            }
        }
        if (!hasInventory) source.sendSuccess(() -> Component.literal("  inventory: empty"), false);
    }

    private static void sendExperience(CommandSourceStack source, Human human) {
        source.sendSuccess(() -> Component.literal("  experience: " + human.getExperiencePoints()
                + " points, level " + human.getExperienceLevel()), false);
    }

    private static void sendEquipment(CommandSourceStack source, Human human) {
        source.sendSuccess(() -> Component.literal("  main hand: " + describe(human.getMainHandItem())), false);
        source.sendSuccess(() -> Component.literal("  off hand: " + describe(human.getOffhandItem())), false);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = human.getItemBySlot(slot);
            source.sendSuccess(() -> Component.literal("  " + slot.getName() + ": " + describe(stack)), false);
        }
    }

    private static String describe(ItemStack stack) {
        if (stack.isEmpty()) return "empty";
        String enchantments = EnchantmentHelper.getEnchantments(stack).entrySet().stream()
                .map(entry -> entry.getKey().getFullname(entry.getValue()).getString())
                .collect(Collectors.joining(", "));
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount()
                + (enchantments.isEmpty() ? "" : " [enchants: " + enchantments + "]");
    }

    private static int inspectProgression(CommandContext<CommandSourceStack> context) {
        WorldGearProgressionSavedData data = WorldGearProgressionSavedData.get(context.getSource().getLevel());
        context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "World gear progression: IRON=%s GOLD=%s DIAMOND=%s NETHERITE=%s",
                data.isIronUnlocked(), data.isGoldUnlocked(), data.isDiamondUnlocked(),
                data.isNetheriteUnlocked())), false);
        return 1;
    }

    private static int createDebugCamp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Human human = findNearest(level, source.getPosition(), 32).orElse(null);
        PersonaFaction faction = human == null ? PersonaFaction.HISPANIC_CREATORS
                : human.getPersonaDefinition().map(definition -> definition.faction()).orElse(PersonaFaction.HISPANIC_CREATORS);
        Camp camp = CampService.createDebugCamp(level, BlockPos.containing(source.getPosition()), faction);
        if (camp == null) {
            source.sendFailure(Component.literal("Could not create a camp at the current position."));
            return 0;
        }
        if (human != null) {
            human.setCampId(camp.id());
            if (human.getSquadId() != null) {
                for (Human member : level.getEntitiesOfClass(Human.class, human.getBoundingBox().inflate(28))) {
                    if (human.getSquadId().equals(member.getSquadId()) && human.getPersonaDefinition().map(a ->
                            member.getPersonaDefinition().map(b -> a.faction() == b.faction()).orElse(false)).orElse(false)) member.setCampId(camp.id());
                }
            }
        }
        source.sendSuccess(() -> Component.literal("Created camp " + camp.id() + " for " + faction), false);
        return 1;
    }

    private static int startDebugExpedition(CommandContext<CommandSourceStack> context) {
        Human human = findNearest(context.getSource().getLevel(), context.getSource().getPosition(), 64).orElse(null);
        if (human == null || !CampMissionController.forceExpedition(human)) {
            context.getSource().sendFailure(Component.literal("No camp Human could start an expedition."));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Expedition started for squad " + human.getSquadId()), false);
        return 1;
    }

    private static int initializeDebugSettlement(CommandContext<CommandSourceStack> context) {
        return initializeDebugSettlement(context, "plains");
    }

    private static int initializeDebugSettlementVariant(CommandContext<CommandSourceStack> context) {
        return initializeDebugSettlement(context, StringArgumentType.getString(context, "variant"));
    }

    private static int initializeDebugSettlement(CommandContext<CommandSourceStack> context, String variant) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());
        if (!GeneratedSettlementManager.placeDebugSettlement(level, origin, variant, "command")) {
            source.sendFailure(Component.literal("Could not place a settlement here. Use a clear area and try again."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Placed and initialized a generated " + variant
                + " settlement at " + origin + "."), false);
        return 1;
    }

    private static int startDebugRaid(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        Human human = findNearest(level, context.getSource().getPosition(), 128).orElse(null);
        if (human == null || human.getCampId() == null) {
            context.getSource().sendFailure(Component.literal("No camp Human found."));
            return 0;
        }
        Camp home = CampSavedData.get(level).get(human.getCampId());
        Camp target = CampSavedData.get(level).nearby(level.dimension(), human.blockPosition(), 256).stream()
                .filter(candidate -> !candidate.id().equals(home.id()) && candidate.faction() != home.faction())
                .min(Comparator.comparingDouble(candidate -> candidate.center().distSqr(human.blockPosition()))).orElse(null);
        if (target == null || !CampMissionController.forceRaid(human, target.id())) {
            context.getSource().sendFailure(Component.literal("No nearby enemy camp could be raided."));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Raid started against camp " + target.id()), false);
        return 1;
    }

    private static int inspectContext(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("Spawn context: "
                + SpawnContextClassifier.classify(source.getLevel(), BlockPos.containing(source.getPosition()))), false);
        return 1;
    }

    private static int spawnDebugLoadout(CommandSourceStack source, SpawnContext spawnContext,
                                         CombatSkillTier tier) {
        ServerLevel level = source.getLevel();
        Human human = ModEntityType.ROAMER.get().create(level);
        if (human == null) return 0;
        human.moveTo(source.getPosition());
        human.addTag("debug_loadout");
        human.setSpawnContext(spawnContext);
        if (tier != null) human.setCombatSkillTierOverride(tier);
        human.initializeProceduralLoadoutForDebug();
        level.addFreshEntity(human);
        source.sendSuccess(() -> Component.literal("Spawned procedural loadout: " + spawnContext
                + (tier == null ? "" : " " + tier)), false);
        return 1;
    }

    private static int spawnDebugTiers(CommandContext<CommandSourceStack> command) {
        CommandSourceStack source = command.getSource();
        for (CombatSkillTier tier : CombatSkillTier.values()) spawnDebugLoadout(source, SpawnContext.OVERWORLD_SURFACE, tier);
        return 1;
    }

    private static int auditLoadouts(CommandContext<CommandSourceStack> context, int samples) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        var progression = com.craftix.hostile_humans.progression.WorldGearProgressionSnapshot.from(
                WorldGearProgressionSavedData.get(level));
        long age = level.getServer().overworld().getGameTime();
        List<HumanLoadoutAudit.Report> reports = new ArrayList<>();
        source.sendSuccess(() -> Component.literal("=== Human Spawn Loadout Audit ==="), false);
        for (SpawnContext spawnContext : SpawnContext.values()) {
            if (spawnContext == SpawnContext.UNKNOWN) continue;
            HumanLoadoutAudit.Report report = HumanLoadoutAudit.sample(spawnContext, progression,
                    CombatSkillTier.T3, age, 0x504832L, samples);
            reports.add(report);
            source.sendSuccess(() -> Component.literal(report.summary()), false);
        }
        try {
            HumanLoadoutAudit.writeCsv(Path.of("build", "reports", "hostile-humans", "loadout-audit.csv"), reports);
            source.sendSuccess(() -> Component.literal("CSV written to build/reports/hostile-humans/loadout-audit.csv"), false);
        } catch (java.io.IOException exception) {
            source.sendFailure(Component.literal("Could not write loadout audit CSV: " + exception.getMessage()));
            return 0;
        }
        return reports.size();
    }
}
