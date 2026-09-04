package com.craftix.hostile_humans.item;

import com.craftix.hostile_humans.entity.ai.combat.CombatSkillTier;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.entities.ModEntityType;
import com.craftix.hostile_humans.entity.loadout.HumanLoadoutGenerator;
import com.craftix.hostile_humans.entity.loadout.LoadoutRollContext;
import com.craftix.hostile_humans.persona.PersonaDefinition;
import com.craftix.hostile_humans.persona.PersonaRegistry;
import com.craftix.hostile_humans.persona.PersonaFaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeSpawnEggItem;

import java.util.List;

/** Spawn egg whose tier, faction and loadout profile are selected by its registry entry. */
public final class ConfiguredHumanSpawnEggItem extends ForgeSpawnEggItem {
    private final HumanSpawnEggSpec spec;

    public ConfiguredHumanSpawnEggItem(HumanSpawnEggSpec spec, int primaryColor, int secondaryColor, Item.Properties properties) {
        super(ModEntityType.ROAMER, primaryColor, secondaryColor, properties);
        this.spec = spec;
    }

    public HumanSpawnEggSpec spec() {
        return spec;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return spec.enchanted();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.FAIL;

        BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
        if (!serverLevel.getBlockState(spawnPos).isAir()) return InteractionResult.FAIL;
        Player player = context.getPlayer();
        Human human = ModEntityType.ROAMER.get().create(serverLevel);
        if (human == null || !serverLevel.noCollision(human, new AABB(spawnPos))) return InteractionResult.FAIL;

        human.moveTo(spawnPos, 0.0F, 0.0F);
        if (spec.tier() != null) human.setCombatSkillTierOverride(spec.tier());
        human.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), MobSpawnType.SPAWN_EGG, null, null);
        ItemStack egg = context.getItemInHand();
        assignPersona(human, serverLevel, egg.getItem().hashCode());
        HumanLoadoutGenerator.generateAndApply(serverLevel, human, spec.enchanted());
        serverLevel.addFreshEntity(human);
        if (player == null || !player.getAbilities().instabuild) egg.shrink(1);
        return InteractionResult.CONSUME;
    }

    private void assignPersona(Human human, ServerLevel level, int salt) {
        PersonaRegistry registry = PersonaRegistry.get();
        List<PersonaDefinition> candidates;
        if (spec.faction() != null) {
            candidates = registry.forFaction(spec.faction());
        } else {
            candidates = registry.forTier(spec.tier());
        }
        if (candidates.isEmpty()) return;
        RandomSource random = human.getRandom();
        int start = Math.floorMod(random.nextInt() ^ salt, candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            if (human.setPersonaId(candidates.get((start + i) % candidates.size()).id())) return;
        }
    }
}
