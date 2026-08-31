package com.craftix.hostile_humans;

import com.craftix.hostile_humans.entity.data.HumanData;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;
import com.craftix.hostile_humans.entity.loadout.HumanLoadoutShowcase;
import com.craftix.hostile_humans.entity.spawner.SpawnContext;
import com.craftix.hostile_humans.entity.spawner.SpawnContextClassifier;
import com.craftix.hostile_humans.progression.WorldGearProgressionSavedData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
import java.util.Locale;
import java.util.Optional;
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
                        .executes(context -> HumanLoadoutShowcase.build(context.getSource()))));
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
}
