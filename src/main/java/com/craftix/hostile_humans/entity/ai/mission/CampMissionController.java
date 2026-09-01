package com.craftix.hostile_humans.entity.ai.mission;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.ai.camp.Camp;
import com.craftix.hostile_humans.entity.ai.camp.CampSavedData;
import com.craftix.hostile_humans.entity.ai.camp.CampService;
import com.craftix.hostile_humans.entity.ai.camp.CampStorageService;
import com.craftix.hostile_humans.entity.ai.squad.SquadManager;
import com.craftix.hostile_humans.entity.ai.survival.SquadNeed;
import com.craftix.hostile_humans.entity.ai.survival.SquadNeeds;
import com.craftix.hostile_humans.entity.ai.survival.SquadNeedsEvaluator;
import com.craftix.hostile_humans.entity.ai.survival.SurvivalInventory;
import com.craftix.hostile_humans.entity.entities.Human;
import com.craftix.hostile_humans.entity.type.human.HumanLootPolicy;
import com.craftix.hostile_humans.persona.PersonaFaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Small server-side coordinator. It delegates gathering, combat and path recovery to existing systems. */
public final class CampMissionController {
    private static final int DECISION_INTERVAL = 100;
    private static final int ARRIVAL_RADIUS_SQR = 100;
    private static final int DISCOVERY_RADIUS = 48;

    private CampMissionController() {}

    public static void tick(Human human) {
        if (human.level().isClientSide || !(human.level() instanceof ServerLevel level) || human.tickCount % 5 != 0) return;
        if (!Config.enableCamps.get()) return;
        if (human.getSquadId() == null) return;
        List<Human> members = members(human);
        if (members.isEmpty()) return;
        discoverEnemyCamp(human, level);
        Camp camp = human.getCampId() == null ? null : CampSavedData.get(level).get(human.getCampId());
        if (camp == null && shouldFound(human, members)) {
            Camp created = CampService.tryFound(human);
            if (created != null) for (Human member : members) member.setCampId(created.id());
            camp = created;
        }
        if (camp == null) return;

        SquadMissionState state = SquadMissionSavedData.get(level).state(human.getSquadId());
        if (state.type() == MissionType.RETURN_TO_CAMP) {
            tickReturn(human, camp, state, members);
            return;
        }
        if (state.type() == MissionType.EXPEDITION) {
            tickExpedition(human, camp, state, members);
            return;
        }
        if (state.type() == MissionType.RAID) {
            tickRaid(human, camp, state, members);
            return;
        }
        if (human.getTarget() != null || human.isFleeing || human.healingAfterFleeTicks > 0) return;
        CampStorageService.withdrawNeeded(human, camp,
                stack -> human.isFood(stack), Math.max(0, 8 - SurvivalInventory.count(human, stack -> human.isFood(stack))));
        maybeStartRaid(human, camp, state, members, level);
        if (state.type() == MissionType.NONE) maybeStartExpedition(human, camp, members, level);
    }

    /** Lets ordinary survival progression run only after an expedition reaches its area. */
    public static boolean allowsSurvivalProgression(Human human) {
        if (!(human.level() instanceof ServerLevel level) || human.getSquadId() == null) return true;
        SquadMissionState state = SquadMissionSavedData.get(level).state(human.getSquadId());
        if (state.type() == MissionType.NONE) return true;
        if (state.type() != MissionType.EXPEDITION || state.targetPos() == null) return false;
        return human.distanceToSqr(state.targetPos().getX() + .5, state.targetPos().getY(), state.targetPos().getZ() + .5) <= ARRIVAL_RADIUS_SQR;
    }

    public static BlockPos worldActionObjective(Human human) {
        if (!(human.level() instanceof ServerLevel level) || human.getSquadId() == null) return null;
        SquadMissionState state = SquadMissionSavedData.get(level).state(human.getSquadId());
        return state.type() == MissionType.RAID ? state.targetPos() : null;
    }

    public static boolean forceExpedition(Human human) {
        if (!(human.level() instanceof ServerLevel level) || !Config.enableExpeditions.get()
                || human.getCampId() == null || human.getSquadId() == null) return false;
        Camp camp = CampSavedData.get(level).get(human.getCampId());
        if (camp == null) return false;
        SquadNeeds needs = SquadNeedsEvaluator.calculate(members(human));
        SquadNeed need = needs.highestPriority().orElse(SquadNeed.FOOD);
        return startExpedition(human, camp, members(human), need, level);
    }

    public static boolean forceRaid(Human human, UUID targetCampId) {
        if (!(human.level() instanceof ServerLevel level) || !Config.enableRaids.get()
                || human.getCampId() == null || human.getSquadId() == null) return false;
        Camp home = CampSavedData.get(level).get(human.getCampId());
        Camp target = CampSavedData.get(level).get(targetCampId);
        if (home == null || target == null || !home.dimension().equals(target.dimension()) || home.faction() == target.faction()) return false;
        if (home.center().distSqr(target.center()) > (double) Config.maxRaidDistance.get() * Config.maxRaidDistance.get()) return false;
        SquadMissionSavedData missions = SquadMissionSavedData.get(level);
        return missions.startIfNone(human.getSquadId(), MissionType.RAID, home.id(), null, target.center(), target.id(),
                level.getGameTime(), level.getGameTime() + Config.expeditionMaxDurationTicks.get(), members(human).size());
    }

    private static void maybeStartExpedition(Human human, Camp camp, List<Human> members, ServerLevel level) {
        if (!Config.enableExpeditions.get()) return;
        SquadMissionState state = SquadMissionSavedData.get(level).state(human.getSquadId());
        long now = level.getGameTime();
        if (now < state.expeditionCooldownUntil() || !leader(human, members) || !grouped(human, members)) return;
        SquadNeeds needs = SquadNeedsEvaluator.calculate(members);
        SquadNeed need = needs.highestPriority().orElse(null);
        if (need == null) return;
        startExpedition(human, camp, members, need, level);
    }

    private static boolean startExpedition(Human human, Camp camp, List<Human> members, SquadNeed need, ServerLevel level) {
        long now = level.getGameTime();
        BlockPos target = camp.center().relative(direction(human.getSquadId()), Math.min(64, Config.expeditionMaxDistance.get() - 8));
        if (!level.getWorldBorder().isWithinBounds(target) || camp.center().distSqr(target) > (double) Config.expeditionMaxDistance.get() * Config.expeditionMaxDistance.get()) return false;
        return SquadMissionSavedData.get(level).startIfNone(human.getSquadId(), MissionType.EXPEDITION, camp.id(), need, target, null,
                now, now + Config.expeditionMaxDurationTicks.get(), members.size());
    }

    private static void tickExpedition(Human human, Camp camp, SquadMissionState state, List<Human> members) {
        if (human.getTarget() != null || human.isFleeing || human.healingAfterFleeTicks > 0) return;
        if (state.targetPos() != null && human.distanceToSqr(state.targetPos().getX() + .5, state.targetPos().getY(), state.targetPos().getZ() + .5) > ARRIVAL_RADIUS_SQR) {
            human.getNavigation().moveTo(state.targetPos().getX() + .5, state.targetPos().getY(), state.targetPos().getZ() + .5, 1.0D);
            return;
        }
        SquadNeeds needs = SquadNeedsEvaluator.calculate(members);
        long now = human.level().getGameTime();
        if (state.primaryNeed() == null || !needs.needs(state.primaryNeed()) || now >= state.deadlineTick()
                || human.distanceToSqr(camp.center().getX() + .5, camp.center().getY(), camp.center().getZ() + .5)
                > (double) Config.expeditionMaxDistance.get() * Config.expeditionMaxDistance.get()
                || unhealthy(members, state.startingMembers())) {
            SquadMissionSavedData.get((ServerLevel) human.level()).returnToCamp(human.getSquadId());
        }
    }

    private static void maybeStartRaid(Human human, Camp home, SquadMissionState state, List<Human> members, ServerLevel level) {
        if (!Config.enableRaids.get() || !leader(human, members) || !grouped(human, members)) return;
        long now = level.getGameTime();
        if (now < state.raidCooldownUntil() || now < state.failedRaidCooldownUntil() || state.knownEnemyCampId() == null
                || now > state.knownEnemyCampUntil() || human.getRandom().nextDouble() > 0.01D) return;
        Camp target = CampSavedData.get(level).get(state.knownEnemyCampId());
        if (target == null || target.faction() == home.faction() || !target.dimension().equals(home.dimension())
                || home.center().distSqr(target.center()) > (double) Config.maxRaidDistance.get() * Config.maxRaidDistance.get()
                || unhealthy(members, members.size())) return;
        SquadMissionSavedData.get(level).startIfNone(human.getSquadId(), MissionType.RAID, home.id(), null,
                target.center(), target.id(), now, now + Config.expeditionMaxDurationTicks.get(), members.size());
    }

    private static void tickRaid(Human human, Camp home, SquadMissionState state, List<Human> members) {
        ServerLevel level = (ServerLevel) human.level();
        Camp target = state.targetCampId() == null ? null : CampSavedData.get(level).get(state.targetCampId());
        long now = level.getGameTime();
        if (target == null || !target.dimension().equals(home.dimension()) || now >= state.deadlineTick() || unhealthy(members, state.startingMembers())) {
            state.setFailedRaidCooldown(now + Config.failedRaidCooldownTicks.get());
            SquadMissionSavedData.get(level).returnToCamp(human.getSquadId());
            return;
        }
        if (human.getTarget() != null) return;
        if (human.distanceToSqr(target.center().getX() + .5, target.center().getY(), target.center().getZ() + .5) > ARRIVAL_RADIUS_SQR) {
            human.getNavigation().moveTo(target.center().getX() + .5, target.center().getY(), target.center().getZ() + .5, 1.05D);
            return;
        }
        boolean looted = CampStorageService.withdrawNeeded(human, target, stack -> HumanLootPolicy.isUseful(human, stack), 64);
        if (looted || !CampStorageService.hasUsefulItem(target, human)) {
            state.setRaidCooldown(now + Config.raidCooldownTicks.get());
            SquadMissionSavedData.get(level).returnToCamp(human.getSquadId());
        }
    }

    private static void tickReturn(Human human, Camp camp, SquadMissionState state, List<Human> members) {
        if (human.distanceToSqr(camp.center().getX() + .5, camp.center().getY(), camp.center().getZ() + .5) > ARRIVAL_RADIUS_SQR) {
            human.getNavigation().moveTo(camp.center().getX() + .5, camp.center().getY(), camp.center().getZ() + .5, 1.0D);
            return;
        }
        CampStorageService.depositExcess(human, camp);
        long arrived = members.stream().filter(member -> member.distanceToSqr(camp.center().getX() + .5,
                camp.center().getY(), camp.center().getZ() + .5) <= ARRIVAL_RADIUS_SQR).count();
        if (arrived * 2 < Math.max(1, state.startingMembers())) return;
        SquadMissionSavedData missions = SquadMissionSavedData.get((ServerLevel) human.level());
        missions.complete(human.getSquadId(), human.level().getGameTime());
        missions.state(human.getSquadId()).setExpeditionCooldown(human.level().getGameTime() + Config.expeditionCooldownTicks.get());
        missions.markDirty();
    }

    private static void discoverEnemyCamp(Human human, ServerLevel level) {
        if (human.getCampId() == null || human.getSquadId() == null || human.tickCount % (DECISION_INTERVAL * 2) != 0) return;
        Camp home = CampSavedData.get(level).get(human.getCampId());
        if (home == null) return;
        PersonaFaction faction = home.faction();
        Camp enemy = CampSavedData.get(level).nearby(level.dimension(), human.blockPosition(), DISCOVERY_RADIUS).stream()
                .filter(candidate -> !candidate.id().equals(home.id()) && candidate.faction() != faction)
                .min(Comparator.comparingDouble(candidate -> candidate.center().distSqr(human.blockPosition()))).orElse(null);
        SquadMissionState state = SquadMissionSavedData.get(level).state(human.getSquadId());
        if (enemy == null) { if (state.knownEnemyCampUntil() < level.getGameTime()) state.clearKnownEnemyCamp(); }
        else { state.rememberEnemyCamp(enemy.id(), level.getGameTime() + 12000); SquadMissionSavedData.get(level).markDirty(); }
    }

    private static boolean shouldFound(Human human, List<Human> members) {
        return leader(human, members) && grouped(human, members) && human.getTarget() == null
                && human.tickCount % 600 == 0 && human.getRandom().nextDouble() < Config.campCreationChance.get();
    }

    private static boolean leader(Human human, List<Human> members) { return members.stream().map(Human::getUUID).min(UUID::compareTo).map(human.getUUID()::equals).orElse(false); }
    private static boolean grouped(Human human, List<Human> members) { return members.stream().allMatch(member -> member.distanceToSqr(human) <= 28 * 28); }
    private static boolean unhealthy(List<Human> members, int starting) { return members.stream().filter(member -> member.getHealth() < member.getMaxHealth() * .35F || member.isFleeing).count() * 2 >= Math.max(2, starting); }
    private static List<Human> members(Human source) { List<Human> result = new ArrayList<>(SquadManager.nearbyMembers(source)); result.add(source); return result; }
    private static net.minecraft.core.Direction direction(UUID squad) { return net.minecraft.core.Direction.from2DDataValue(Math.floorMod(squad.hashCode(), 4)); }
}
