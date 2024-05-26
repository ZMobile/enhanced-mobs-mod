package net.fabricmc.example.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombieAttackGoal.class)
public class ZombieAttackGoalMixin {


    /*@Mixin(ZombieAttackGoal.class)
    public abstract class ZombieAttackGoalMixin {

        @Shadow
        protected PathAwareEntity mob;

        @Shadow
        private boolean pauseWhenMobIdle;

        @Inject(method = "shouldContinue", at = @At("HEAD"), cancellable = true)
        private void onShouldContinue(CallbackInfoReturnable<Boolean> cir) {
            LivingEntity target = mob.getTarget();

            if (target == null) {
                System.out.println("Target is null");
                cir.setReturnValue(false);
            } else if (!target.isAlive()) {
                System.out.println("Target is not alive");
                cir.setReturnValue(false);
            } else if (!pauseWhenMobIdle) {
                boolean notIdle = !mob.getNavigation().isIdle();
                System.out.println("Navigation is " + (notIdle ? "not idle" : "idle"));
                if (!notIdle) {
                    cir.setReturnValue(false);
                }
            } else if (!mob.isInWalkTargetRange(target.getBlockPos())) {
                System.out.println("Target is out of walk target range");
                cir.setReturnValue(false);
            } else {
                boolean validPlayer = !(target instanceof PlayerEntity) || (!target.isSpectator() && !((PlayerEntity) target).isCreative());
                System.out.println("Target is " + (validPlayer ? "a valid player" : "not a valid player"));
                if (!validPlayer) {
                    cir.setReturnValue(false);
                }
            }
        }
    }*/
}