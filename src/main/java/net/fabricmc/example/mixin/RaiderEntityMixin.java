package net.fabricmc.example.mixin;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import net.fabricmc.example.mobai.BreakBlockAndChaseGoal;
import net.fabricmc.example.mobai.CustomTargetGoal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RaiderEntity.class)
public abstract class RaiderEntityMixin extends PathAwareEntity {

    protected RaiderEntityMixin(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addCustomGoals(CallbackInfo info) {
        //GoalBlock goal = new GoalBlock(0, 60, 200);
        BaritoneAPI.getProvider().createBaritone(MinecraftClient.getInstance(),  this);
        this.goalSelector.add(6, new BreakBlockAndChaseGoal(this ));
        this.goalSelector.add(6, new CustomTargetGoal(this));
        // BaritoneAPI.getProvider().getBaritoneForEntity(this).getCustomGoalProcess().setGoalAndPath(goal);
        //System.out.println("Baritone goal successfully added to IllagerEntity");
    }

    @Inject(method = "onDeath", at = @At("TAIL"))
    private void onIllagerDespawn(CallbackInfo info) {
        if (!this.isAlive()) {
            IBaritone goalBaritone = BaritoneAPI.getProvider().getBaritoneForEntity(this);
            if (goalBaritone != null) {
                // Clean up Baritone instance for this entity
                BaritoneAPI.getProvider().destroyBaritone(goalBaritone);
                // Debug log to verify cleanup
                //System.out.println("Baritone instance successfully removed for IllagerEntity on despawn");
            }
        }
    }
}
