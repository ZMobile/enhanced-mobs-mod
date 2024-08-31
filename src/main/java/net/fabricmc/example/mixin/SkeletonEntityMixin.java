package net.fabricmc.example.mixin;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.fabricmc.example.config.ConfigManager;
import net.fabricmc.example.mobai.BreakPlaceAndChaseGoal;
import net.fabricmc.example.mobai.CustomTargetGoal;
import net.fabricmc.example.service.MobitoneServiceImpl;
import net.fabricmc.example.util.MinecraftServerUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSkeletonEntity.class)
public abstract class SkeletonEntityMixin extends PathAwareEntity {

    protected SkeletonEntityMixin(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addCustomGoals(CallbackInfo info) {
        //BaritoneAPI.getProvider().createBaritone(MinecraftServerUtil.getMinecraftServer(), (SkeletonEntity) (Object) this);
        //if (!BloodmoonHandler.INSTANCE.isBloodmoonActive()) {
        if (ConfigManager.getConfig().isSkeletonsBreakBlocks()) {
            if (!ConfigManager.getConfig().isOptimizedMobitone()) {
                MobitoneServiceImpl.addMobitone(this);
                MobitoneServiceImpl.fillInQueue();
            }
            //}
            this.goalSelector.add(1, new BreakPlaceAndChaseGoal(this));
        }
        this.goalSelector.add(6, new CustomTargetGoal(this));
        //System.out.println("Baritone goal successfully added to SkeletonEntity");
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void checkSkeletonState(CallbackInfo info) {
        // Add any custom behavior for the tick method
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void onSkeletonDespawn(CallbackInfo info) {
        if (!this.isAlive()) {
            /*IBaritone goalBaritone = BaritoneAPI.getProvider().getBaritoneForEntity(this);
            if (goalBaritone != null) {
                BaritoneAPI.getProvider().destroyBaritone(goalBaritone);
                //System.out.println("Baritone instance successfully removed for SkeletonEntity on despawn");
            }*/
            MobitoneServiceImpl.removeMobitone(this);
        }
    }
}