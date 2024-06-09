package net.fabricmc.example.mixin;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.fabricmc.example.mobai.BreakPlaceAndChaseGoal;
import net.fabricmc.example.mobai.CustomTargetGoal;
import net.fabricmc.example.service.MobitoneServiceImpl;
import net.fabricmc.example.util.MinecraftServerUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitchEntity.class)
public class WitchEntityMixin extends PathAwareEntity {
    protected WitchEntityMixin(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addCustomGoals(CallbackInfo info) {
        if (BloodmoonHandler.INSTANCE.isBloodmoonActive() && random.nextFloat() < 0.9) {
            this.discard();
        }
        //GoalBlock goal = new GoalBlock(0, 60, 200);
        //BaritoneAPI.getProvider().createBaritone(MinecraftServerUtil.getMinecraftServer(),  this);
        if (!BloodmoonHandler.INSTANCE.isBloodmoonActive()) {
            MobitoneServiceImpl.addMobitone(this);
            MobitoneServiceImpl.fillInQueue();
        }
        this.goalSelector.add(1, new BreakPlaceAndChaseGoal(this ));
        this.goalSelector.add(6, new CustomTargetGoal(this));
        // BaritoneAPI.getProvider().getBaritoneForEntity(this).getCustomGoalProcess().setGoalAndPath(goal);
        //System.out.println("Baritone goal successfully added to WitchEntity");
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void checkWitchState(CallbackInfo info) {
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

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void onWitchDespawn(CallbackInfo info) {
        if (!this.isAlive()) {
            /*IBaritone goalBaritone = BaritoneAPI.getProvider().getBaritoneForEntity(this);
            if (goalBaritone != null) {
                // Clean up Baritone instance for this entity
                BaritoneAPI.getProvider().destroyBaritone(goalBaritone);
                // Debug log to verify cleanup
                //System.out.println("Baritone instance successfully removed for WitchEntity on despawn");
            }*/
            MobitoneServiceImpl.removeMobitone(this);
        }
    }
}
