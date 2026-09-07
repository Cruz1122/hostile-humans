package com.craftix.hostile_humans.entity.ai.squad;

import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.persona.PersonaFaction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/** Local, event-driven squad cooperation. It never owns entities or loads chunks. */
public final class SquadManager {
    public static final double COOPERATION_RADIUS = 28.0D;
    public static final int MEMORY_TICKS = 20 * 8;

    private SquadManager() {
    }

    public static List<Human> nearbyMembers(Human source) {
        if (source.getSquadId() == null) return List.of();
        Optional<PersonaFaction> faction = faction(source);
        if (faction.isEmpty()) return List.of();
        AABB area = source.getBoundingBox().inflate(COOPERATION_RADIUS, COOPERATION_RADIUS / 2.0D,
                COOPERATION_RADIUS);
        return source.level().getEntitiesOfClass(Human.class, area, candidate -> candidate != source
                && candidate.isAlive()
                && source.getSquadId().equals(candidate.getSquadId())
                && faction(candidate).map(faction.get()::equals).orElse(false));
    }

    public static boolean canShareSquad(Human first, Human second) {
        if (first.getSquadId() == null || !first.getSquadId().equals(second.getSquadId())) return false;
        Optional<PersonaFaction> firstFaction = faction(first);
        Optional<PersonaFaction> secondFaction = faction(second);
        return firstFaction.isPresent() && firstFaction.equals(secondFaction);
    }

    public static void shareTarget(Human source, LivingEntity target, SquadAlertReason reason) {
        if (!validThreat(source, target)) return;
        BlockPos seenPos = target.blockPosition();
        long gameTime = source.level().getGameTime();
        source.rememberSquadThreat(target.getUUID(), seenPos, gameTime);
        for (Human member : nearbyMembers(source)) {
            if (!validThreat(member, target)) continue;
            member.receiveSquadAlert(target, seenPos, gameTime, reason);
        }
    }

    public static void refreshVisibleTarget(Human source, LivingEntity target) {
        if (!source.hasLineOfSight(target) || !validThreat(source, target)) return;
        shareTarget(source, target, SquadAlertReason.SHARED_AGGRO);
    }

    public static void alertRetreatingAlly(Human retreating, @Nullable LivingEntity attacker) {
        if (attacker == null || !retreating.isFleeing || !validThreat(retreating, attacker)) return;
        shareTarget(retreating, attacker, SquadAlertReason.PROTECT_RETREATING_ALLY);
    }

    /** A squad may use a beneficial splash potion once its active combat group is together. */
    public static boolean readyForCombatSplash(Human source) {
        LivingEntity target = source.getTarget();
        if (target == null || source.getSquadId() == null) return false;
        if (combatSplashHolder(source) != source) return false;
        List<Human> members = nearbyMembers(source);
        if (members.isEmpty()) return false;
        return members.stream().allMatch(member -> member.getTarget() == target
                && member.distanceToSqr(source) <= 16.0D);
    }

    @Nullable
    public static Human combatSplashHolder(Human source) {
        if (source.hasCombatSplashAvailable()) return source;
        return nearbyMembers(source).stream()
                .filter(Human::hasCombatSplashAvailable)
                .min(java.util.Comparator.comparingDouble(source::distanceToSqr))
                .orElse(null);
    }

    /** Returns the local rally point used as the impact point for squad buffs. */
    public static Vec3 combatSplashPoint(Human source) {
        Human holder = combatSplashHolder(source);
        return holder == null ? source.position() : holder.position();
    }

    private static boolean validThreat(Human human, LivingEntity target) {
        return target != human && target.isAlive() && human.canAttack(target)
                && (!(target instanceof Human other) || Human.areEnemies(human, other));
    }

    private static Optional<PersonaFaction> faction(Human human) {
        return human.getPersonaDefinition().map(definition -> definition.faction());
    }
}
