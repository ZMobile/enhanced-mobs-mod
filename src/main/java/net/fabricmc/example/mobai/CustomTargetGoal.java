package net.fabricmc.example.mobai;

import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.fabricmc.example.service.CustomVisibilityCheckService;
import net.fabricmc.example.service.CustomVisibilityCheckServiceImpl;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.EnumSet;

public class CustomTargetGoal extends Goal {
    private final MobEntity mob;
    private PlayerEntity targetPlayer;
    private int sightCounter;
    private static final int SIGHT_DURATION = 40; // 2 seconds (40 ticks)
    private static final int MAX_UNOBSTRUCTED_DISTANCE = 200;
    private static final int MAX_GLASS_OBSTRUCTED_DISTANCE = 50;
    boolean playerTargeted = false;

    public CustomTargetGoal(MobEntity mob) {
        this.mob = mob;
        this.setControls(EnumSet.of(Control.TARGET));
    }

    @Override
    public boolean canStart() {
        targetPlayer = mob.getWorld().getClosestPlayer(mob, MAX_UNOBSTRUCTED_DISTANCE);
        if (BloodmoonHandler.INSTANCE.isBloodmoonActive()) {
            return true;
        }
        if (mob.getTarget() == null && targetPlayer != null &&
                CustomVisibilityCheckServiceImpl.canSee(mob, targetPlayer) && !CustomVisibilityCheckServiceImpl.isInCreativeMode(targetPlayer) && CustomVisibilityCheckServiceImpl.isFacingTarget(mob, targetPlayer)) {
            {
                return true;
            }
        }

        targetPlayer = mob.getWorld().getClosestPlayer(mob, MAX_GLASS_OBSTRUCTED_DISTANCE);
        return mob.getTarget() == null && targetPlayer != null && (
                CustomVisibilityCheckServiceImpl.canSeeThroughGlassWithException(mob, targetPlayer) && !CustomVisibilityCheckServiceImpl.isInCreativeMode(targetPlayer) && CustomVisibilityCheckServiceImpl.isFacingTarget(mob, targetPlayer));
    }

    @Override
    public void start() {
        sightCounter = 0;
    }

    @Override
    public void stop() {
        targetPlayer = null;
        sightCounter = 0;
    }

    @Override
    public boolean shouldContinue() {
        if (targetPlayer == null) {
            return false;
        }

        if (mob.getTarget() == null && CustomVisibilityCheckServiceImpl.canSee(mob, targetPlayer) && !CustomVisibilityCheckServiceImpl.isInCreativeMode(targetPlayer) && CustomVisibilityCheckServiceImpl.isFacingTarget(mob, targetPlayer)) {
            return true;
        }

        return mob.getTarget() == null && CustomVisibilityCheckServiceImpl.canSeeThroughGlassWithException(mob, targetPlayer) && !CustomVisibilityCheckServiceImpl.isInCreativeMode(targetPlayer) && CustomVisibilityCheckServiceImpl.isFacingTarget(mob, targetPlayer);
    }

    @Override
    public void tick() {
        if (targetPlayer != null) {
            if (BloodmoonHandler.INSTANCE.isBloodmoonActive()) {
                mob.setTarget(targetPlayer);
                stop();
            }
            if (
                    mob.getTarget() == null && CustomVisibilityCheckServiceImpl.canSeeThroughGlassWithException(mob, targetPlayer) && !CustomVisibilityCheckServiceImpl.isInCreativeMode(targetPlayer) && CustomVisibilityCheckServiceImpl.isFacingTarget(mob, targetPlayer)) {
                sightCounter++;
                lookAtPlayer();
                if (sightCounter >= SIGHT_DURATION) {
                    mob.setTarget(targetPlayer);
                    stop();
                }
            } else {
                sightCounter = 0;
            }
        }
    }

    private void lookAtPlayer() {
        double dx = targetPlayer.getX() - mob.getX();
        double dy = targetPlayer.getEyeY() - mob.getEyeY();
        double dz = targetPlayer.getZ() - mob.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        mob.setYaw((float)(Math.atan2(dz, dx) * (180 / Math.PI)) - 90);
        mob.setPitch((float)(-Math.atan2(dy, distance) * (180 / Math.PI)));
        mob.getLookControl().lookAt(targetPlayer, 30.0F, 30.0F);
    }
}
