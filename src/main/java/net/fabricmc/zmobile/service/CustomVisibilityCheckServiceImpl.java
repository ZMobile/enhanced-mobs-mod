package net.fabricmc.zmobile.service;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class CustomVisibilityCheckServiceImpl implements CustomVisibilityCheckService {
    public static boolean canSee(MobEntity mob, Entity target) {
        World world = mob.getWorld();
        Vec3d mobPos = new Vec3d(mob.getX(), mob.getEyeY(), mob.getZ());
        Vec3d targetPos = new Vec3d(target.getX(), target.getEyeY(), target.getZ());
        RaycastContext context = new RaycastContext(mobPos, targetPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mob);

        return world.raycast(context).getType() == HitResult.Type.MISS;
    }

    public static boolean canSeeThroughGlass(MobEntity mob, Entity target) {
        World world = mob.getWorld();
        Vec3d mobPos = new Vec3d(mob.getX(), mob.getEyeY(), mob.getZ());
        Vec3d targetPos = new Vec3d(target.getX(), target.getEyeY(), target.getZ());
        RaycastContext context = new RaycastContext(mobPos, targetPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mob);
        return world.raycast(context).getType() == HitResult.Type.MISS;
    }

    public static boolean isGlassBlock(World world, BlockPos pos) {
        return world.getBlockState(pos).isOf(Blocks.GLASS);
    }

    public static boolean canSeeThroughGlassWithException(MobEntity mob, Entity target) {
        Vec3d mobPos = new Vec3d(mob.getX(), mob.getEyeY(), mob.getZ());
        Vec3d targetPos = new Vec3d(target.getX(), target.getEyeY(), target.getZ());
        double dx = targetPos.x - mobPos.x;
        double dy = targetPos.y - mobPos.y;
        double dz = targetPos.z - mobPos.z;
        int steps = (int) Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) * 100;

        for (int i = 0; i <= steps; i++) {
            double factor = i / (double) steps;
            BlockPos pos = new BlockPos(
                    (int) (mobPos.x + dx * factor),
                    (int) (mobPos.y + dy * factor),
                    (int) (mobPos.z + dz * factor)
            );
            if (!mob.getWorld().isAir(pos) && !isGlassBlock(mob.getWorld(), pos)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isInCreativeMode(Entity entity) {
        return entity instanceof PlayerEntity && ((PlayerEntity) entity).isCreative();
    }

    public static boolean isFacingTarget(MobEntity mob, Entity target) {
        Vec3d mobPos = new Vec3d(mob.getX(), mob.getEyeY(), mob.getZ());
        Vec3d targetPos = new Vec3d(target.getX(), target.getEyeY(), target.getZ());
        double dx = targetPos.x - mobPos.x;
        double dz = targetPos.z - mobPos.z;
        double angleToTarget = MathHelper.atan2(dz, dx) * (180 / Math.PI) - 90;
        double angleDifference = MathHelper.wrapDegrees(mob.getYaw() - angleToTarget);
        return Math.abs(angleDifference) < 45; // Adjust the threshold as needed
    }
}