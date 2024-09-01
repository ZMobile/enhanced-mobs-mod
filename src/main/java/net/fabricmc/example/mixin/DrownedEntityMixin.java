package net.fabricmc.example.mixin;

import baritone.api.utils.BetterBlockPos;
import net.fabricmc.example.mobai.tracker.MobPathTracker;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DrownedEntity.class)
public abstract class DrownedEntityMixin extends PathAwareEntity {

    protected DrownedEntityMixin(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "updateSwimming", at = @At("HEAD"))
    private void travel(CallbackInfo info) {
        if (this.getTarget() != null && this.isTouchingWater()) {
            List<BetterBlockPos> path = MobPathTracker.getPath(this.getUuidAsString());
            if (path != null) {
                int currentNodeIndex = 0;
                for (int i = 0; i < path.size(); i++) {
                    if (path.get(i).equals(new BetterBlockPos(this.getBlockPos()))) {
                        currentNodeIndex = i;
                        break;
                    }
                }
                if (currentNodeIndex < path.size() - 1) {
                    try {
                        BlockPos nextPos = path.get(currentNodeIndex + 1);

                        // Consolidate velocity application
                        if (this.getTarget().getY() > this.getY()) {
                            this.setVelocity(this.getVelocity().x, 0.1, this.getVelocity().z);
                        } else {
                            applyVelocityTowardsTarget(this, nextPos);
                        }

                        for (int i = currentNodeIndex; i < path.size(); i++) {
                            BlockPos pathNodePos = path.get(i);
                            BlockPos blockBelow = pathNodePos.down();

                            if (getWorld(this).getBlockState(blockBelow).isSolidBlock(getWorld(), blockBelow)
                                    && getWorld().getBlockState(pathNodePos).isOf(Blocks.AIR)) {
                                if (this.getBlockPos().isWithinDistance(new Vec3d(pathNodePos.getX(), pathNodePos.getY(), pathNodePos.getZ()), 2)) {
                                    boostUpOutOfWater(this, pathNodePos);
                                    break;
                                }
                            }
                        }
                    } catch (IndexOutOfBoundsException e) {
                        System.out.println("Index out of bounds exception");
                    }
                }
            }
        }
    }

    private void boostUpOutOfWater(MobEntity mob, BlockPos targetPos) {
        Vec3d mobPos = mob.getPos();
        Vec3d targetVec = new Vec3d(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        Vec3d directionToTarget = targetVec.subtract(mobPos).normalize();

        // Get the current velocity direction
        Vec3d currentVelocity = mob.getVelocity().normalize();

        // Check if the Drowned is not moving in the opposite direction of the boost
        if (currentVelocity.dotProduct(directionToTarget) > 0) {
            // Define the vertical boost and horizontal speed
            double verticalBoost = 0.7; // Adjust the vertical boost as needed
            double horizontalSpeed = 0.1; // Adjust the horizontal speed as needed

            // Apply the velocity
            mob.setVelocity(directionToTarget.x * horizontalSpeed, verticalBoost, directionToTarget.z * horizontalSpeed);
            System.out.println("Boosting up out of water towards: " + targetPos);
        } else {
            System.out.println("Boost cancelled due to opposite direction movement");
        }
    }

    private void applyVelocityTowardsTarget(MobEntity mob, BlockPos targetPos) {
        Vec3d mobPos = mob.getPos();
        Vec3d targetVec = new Vec3d(targetPos.getX() + 0.1, targetPos.getY(), targetPos.getZ() + 0.1);
        Vec3d direction = targetVec.subtract(mobPos).normalize();

        // Define a maximum speed to prevent zombies from zooming
        double maxSpeed = 0.1; // Adjust this value as needed

        // Scale the direction vector by the maximum speed
        direction = direction.multiply(maxSpeed);

        // Apply the clamped velocity
        mob.setVelocity(direction);
    }

    public World getWorld(LivingEntity mob) {
        return mob.getWorld();
    }
}