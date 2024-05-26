package net.fabricmc.example.mixin;

import net.fabricmc.example.mobai.BreakBlockAndChaseGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Set;

@Mixin(ZombieEntity.class)
public abstract class ZombieEntityMixin extends PathAwareEntity {

    protected ZombieEntityMixin(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addCustomGoals(CallbackInfo info) {
        // Debug log to indicate injection point
        // Ensure we don't block the default behavior
        // Add the custom goal with a lower priority to avoid blocking essential default goals
        //this.goalSelector.add(6, new BreakBlockAndChaseGoal(this, this.goalSelector));
        // Debug log to verify goal addition
        System.out.println("BreakBlockAndChaseGoal successfully added to ZombieEntity");
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void checkZombieState(CallbackInfo info) {
        if (this.goalSelector.getRunningGoals().toArray().length > 0) {
            this.goalSelector.getRunningGoals().forEach(prioritizedGoal -> {
                if (prioritizedGoal.getGoal() instanceof ZombieAttackGoal) {
                    System.out.println("Running goal: " + prioritizedGoal.getGoal().getClass().getName() + ", Priority: " + prioritizedGoal.getPriority());
                }
            });
        }
        if (this.getTarget() != null) {
            System.out.println("Mob target: " + this.getTarget());

            System.out.println("Mob is navigating: " + this.isNavigating());
            System.out.println("Mob is attacking: " + this.isAttacking());
        }
    }
    /*public boolean isChasingPlayer() {
        try {
            Field runningGoalsField = GoalSelector.class.getDeclaredField("runningGoals");
            runningGoalsField.setAccessible(true);
            Set<?> runningGoals = (Set<?>) runningGoalsField.get(this.goalSelector);

            for (Object item : runningGoals) {
                Field goalField = item.getClass().getDeclaredField("goal");
                goalField.setAccessible(true);
                Goal goal = (Goal) goalField.get(item);

                if (goal instanceof ZombieAttackGoal) {
                    return true;
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void checkZombieState(CallbackInfo info) {
        if (isChasingPlayer()) {
            System.out.println("Zombie is chasing a player.");
        } else {
            System.out.println("Zombie is idle or performing another action.");
        }
    }*/
}
