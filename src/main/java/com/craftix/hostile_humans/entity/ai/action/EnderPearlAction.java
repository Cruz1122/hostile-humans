package com.craftix.hostile_humans.entity.ai.action;

import com.craftix.hostile_humans.Config;
import com.craftix.hostile_humans.entity.entities.Human;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Server-side ender-pearl throws with explicit target and destination validation. */
public final class EnderPearlAction {
    private EnderPearlAction() {}

    public static boolean tryThrowOffensively(Human human) {
        LivingEntity target = human.getTarget();
        if (target == null || !target.isAlive() || !human.hasLineOfSight(target)
                || human.distanceTo(target) < Config.enderPearlOffensiveDistance.get()
                || human.distanceTo(target) > Config.enderPearlMaximumDistance.get()) return false;
        return throwAt(human, target.position().add(0.0D, target.getBbHeight() * 0.35D, 0.0D));
    }

    public static boolean tryThrowDefensively(Human human) {
        LivingEntity threat = human.isFleeing ? human.toAvoid : human.getTarget();
        if (threat == null || !threat.isAlive() || human.distanceTo(threat) > 16.0D) return false;
        Vec3 away = human.position().subtract(threat.position());
        if (away.horizontalDistanceSqr() < 0.01D) return false;
        away = new Vec3(away.x, 0.0D, away.z).normalize();
        for (double distance : new double[]{8.0D, 6.0D, 10.0D}) {
            Vec3 destination = human.position().add(away.scale(distance));
            if (isSafeDestination(human, destination) && throwAt(human, destination)) return true;
        }
        return false;
    }

    private static boolean throwAt(Human human, Vec3 destination) {
        if (human.level().isClientSide || !Config.enableEnderPearlTactics.get()
                || human.isUsingItem() || human.enderPearlCooldown > 0) return false;
        ItemStack pearl = UtilityItemSupport.find(human, Items.ENDER_PEARL);
        if (pearl.isEmpty()) return false;

        ThrownEnderpearl projectile = new ThrownEnderpearl(human.level(), human);
        projectile.setPos(human.getX(), human.getEyeY() - 0.1D, human.getZ());
        double dx = destination.x - projectile.getX();
        double dy = destination.y - projectile.getY();
        double dz = destination.z - projectile.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        projectile.shoot(dx, dy + horizontal * 0.2D, dz, 1.5F, 1.0F);
        if (!human.level().addFreshEntity(projectile)) return false;
        if (!UtilityItemSupport.consumeOne(human, pearl)) {
            projectile.discard();
            return false;
        }
        human.enderPearlCooldown = Config.enderPearlCooldownTicks.get();
        human.playSound(net.minecraft.sounds.SoundEvents.ENDER_PEARL_THROW, 0.5F,
                0.4F / (human.getRandom().nextFloat() * 0.4F + 0.8F));
        return true;
    }

    private static boolean isSafeDestination(Human human, Vec3 destination) {
        BlockPos feet = BlockPos.containing(destination);
        BlockPos head = feet.above();
        BlockPos floor = feet.below();
        if (!human.level().hasChunkAt(feet) || !human.level().hasChunkAt(head)
                || !human.level().getFluidState(feet).isEmpty()
                || !human.level().getFluidState(head).isEmpty()
                || human.level().getBlockState(feet).getCollisionShape(human.level(), feet).isEmpty() == false
                || !human.level().getBlockState(head).getCollisionShape(human.level(), head).isEmpty()
                || human.level().getBlockState(floor).getCollisionShape(human.level(), floor).isEmpty()) return false;
        Vec3 delta = destination.subtract(human.position());
        return human.level().noCollision(human, human.getBoundingBox().move(delta));
    }
}
