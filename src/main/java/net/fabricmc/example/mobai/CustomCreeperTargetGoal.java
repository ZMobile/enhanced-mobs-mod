package net.fabricmc.example.mobai;

import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.fabricmc.example.service.CustomVisibilityCheckServiceImpl;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.EnumSet;

public class CustomCreeperTargetGoal extends Goal {
    private final MobEntity mob;
    private PlayerEntity targetPlayer;
    private int sightCounter;
    private static final int SIGHT_DURATION = 10; // 2 seconds (40 ticks)
    private static final int MAX_HEARING_DISTANCE = 20;
    private static final int MAX_UNOBSTRUCTED_DISTANCE = 200;
    private static final int MAX_GLASS_OBSTRUCTED_DISTANCE = 50;
    boolean playerTargeted = false;

    public CustomCreeperTargetGoal(MobEntity mob) {
        this.mob = mob;
        this.setControls(EnumSet.of(Control.TARGET));
    }

    @Override
    public boolean canStart() {
        targetPlayer = mob.getWorld().getClosestPlayer(mob, MAX_HEARING_DISTANCE);
        if (targetPlayer != null && !CustomVisibilityCheckServiceImpl.isInCreativeMode(targetPlayer) && !targetPlayer.isSneaking()) {
            return true;
        }
        targetPlayer = mob.getWorld().getClosestPlayer(mob, MAX_UNOBSTRUCTED_DISTANCE);
        if (BloodmoonHandler.INSTANCE.isBloodmoonActive() && within40Y(mob, targetPlayer)) {
            return true;
        }
        if (mob.getTarget() == null && targetPlayer != null  && within40Y(mob, targetPlayer) &&
                CustomVisibilityCheckServiceImpl.canSee(mob, targetPlayer) && !CustomVisibilityCheckServiceImpl.isInCreativeMode(targetPlayer) && CustomVisibilityCheckServiceImpl.isFacingTarget(mob, targetPlayer)) {
            {
                return true;
            }
        }

        targetPlayer = mob.getWorld().getClosestPlayer(mob, MAX_GLASS_OBSTRUCTED_DISTANCE);
        return mob.getTarget() == null && targetPlayer != null && within40Y(mob, targetPlayer) && (
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

        if (mob.getTarget() == null && within40Y(mob, targetPlayer) && CustomVisibilityCheckServiceImpl.canSee(mob, targetPlayer) && !CustomVisibilityCheckServiceImpl.isInCreativeMode(targetPlayer) && CustomVisibilityCheckServiceImpl.isFacingTarget(mob, targetPlayer)) {
            return true;
        }

        return mob.getTarget() == null && within40Y(mob, targetPlayer) && CustomVisibilityCheckServiceImpl.canSeeThroughGlassWithException(mob, targetPlayer) && !CustomVisibilityCheckServiceImpl.isInCreativeMode(targetPlayer) && CustomVisibilityCheckServiceImpl.isFacingTarget(mob, targetPlayer);
    }

    @Override
    public void tick() {
        PlayerEntity hearingTargetPlayer = mob.getWorld().getClosestPlayer(mob, MAX_HEARING_DISTANCE);
        if (hearingTargetPlayer != null) {
            mob.setTarget(hearingTargetPlayer);
            stop();
        }
        if (targetPlayer != null) {
            if (BloodmoonHandler.INSTANCE.isBloodmoonActive() && within40Y(mob, targetPlayer)) {
                mob.setTarget(targetPlayer);
                stop();
            }
            if (
                    mob.getTarget() == null && within40Y(mob, targetPlayer) && CustomVisibilityCheckServiceImpl.canSeeThroughGlassWithException(mob, targetPlayer) && !CustomVisibilityCheckServiceImpl.isInCreativeMode(targetPlayer) && CustomVisibilityCheckServiceImpl.isFacingTarget(mob, targetPlayer)) {
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

    private boolean within40Y(Entity entity1, Entity entity2) {
        if (entity1 == null || entity2 == null) return false;
        return Math.abs(entity1.getY() - entity2.getY()) <= 40;
    }

}
