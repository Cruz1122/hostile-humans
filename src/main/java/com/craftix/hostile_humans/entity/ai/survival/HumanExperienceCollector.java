package com.craftix.hostile_humans.entity.ai.survival;

import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/** Pulls nearby experience orbs to a Human and converts them to persistent XP. */
public final class HumanExperienceCollector {
    private static final double SEARCH_RADIUS = 8.0D;
    private static final double PICKUP_RADIUS_SQUARED = 1.25D * 1.25D;

    private HumanExperienceCollector() {
    }

    public static int tick(Human human) {
        if (human.level().isClientSide || !human.isAlive()) return 0;

        AABB searchArea = human.getBoundingBox().inflate(SEARCH_RADIUS);
        return human.level().getEntitiesOfClass(ExperienceOrb.class, searchArea,
                        orb -> orb.isAlive() && orb.getValue() > 0)
                .stream()
                .sorted(Comparator.comparingDouble(human::distanceToSqr))
                .mapToInt(orb -> collectOrAttract(human, orb))
                .sum();
    }

    private static int collectOrAttract(Human human, ExperienceOrb orb) {
        double distanceSqr = human.distanceToSqr(orb);
        if (distanceSqr <= PICKUP_RADIUS_SQUARED) {
            int value = orb.getValue();
            human.giveExperiencePoints(value);
            orb.discard();
            return value;
        }

        Vec3 towardHuman = human.position().subtract(orb.position());
        if (towardHuman.lengthSqr() > 0.0D) {
            orb.setDeltaMovement(orb.getDeltaMovement().scale(0.8D)
                    .add(towardHuman.normalize().scale(0.08D)));
        }
        return 0;
    }
}
