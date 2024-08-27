package net.fabricmc.example.mixin;

import net.fabricmc.example.config.ConfigManager;
import net.fabricmc.example.mobai.BreakPlaceAndChaseGoal;
import net.fabricmc.example.mobai.tracker.BreakPlaceAndChaseGoalTracker;
import net.fabricmc.example.mobai.CustomTargetGoal;
import net.fabricmc.example.mobai.tracker.MobPathTracker;
import net.fabricmc.example.service.MobitoneServiceImpl;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombieEntity.class)
public abstract class ZombieEntityMixin extends PathAwareEntity {

    protected ZombieEntityMixin(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addCustomGoals(CallbackInfo info) {
        //GoalBlock goal = new GoalBlock(0, 60, 200);
        //BaritoneAPI.getProvider().createBaritone(MinecraftServerUtil.getMinecraftServer(),  this);v
        //if (!BloodmoonHandler.INSTANCE.isBloodmoonActive()) {
        if (ConfigManager.getConfig().isZombiesBreakAndPlaceBlocks()) {
            if (!ConfigManager.getConfig().isOptimizedMobitone()) {
                MobitoneServiceImpl.addMobitone(this);
                MobitoneServiceImpl.fillInQueue();
            }
            //}
            BreakPlaceAndChaseGoal goal = new BreakPlaceAndChaseGoal(this);
            this.goalSelector.add(1, goal);
            BreakPlaceAndChaseGoalTracker.addGoal(this.getId(), goal);
        }
        this.goalSelector.add(6, new CustomTargetGoal(this));
        // BaritoneAPI.getProvider().getBaritoneForEntity(this).getCustomGoalProcess().setGoalAndPath(goal);

    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void checkZombieState(CallbackInfo info) {
        /*ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            Vec3d playerPosition = MinecraftClient.getInstance().player.getPos();
            GoalBlock goal = new GoalBlock((int) playerPosition.x, (int) playerPosition.y, (int) playerPosition.z);
            IBaritone goalBaritone = BaritoneAPI.getProvider().getBaritoneForEntity(this);
            if (goalBaritone != null) {
                goalBaritone.getCustomGoalProcess().setGoalAndPath(goal);
            }
        }*/
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onZombieDespawn(CallbackInfo info) {
        if (!this.isAlive()) {
            /*IBaritone goalBaritone = BaritoneAPI.getProvider().getBaritoneForEntity(this);
            if (goalBaritone != null) {
                // Clean up Baritone instance for this entity
                BaritoneAPI.getProvider().destroyBaritone(goalBaritone);
                // Debug log to verify cleanup
                //System.out.println("Baritone instance successfully removed for ZombieEntity on despawn");
            }*/
            MobitoneServiceImpl.removeMobitone(this);
            BreakPlaceAndChaseGoalTracker.removeGoal(this.getId());
            MobPathTracker.removePath(this.getUuidAsString());
        }
    }
}
