package net.fabricmc.example.listener;

import net.fabricmc.example.service.MobitoneServiceImpl;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

public class MobTargetListener {
    public static void register() {
        // Registering the event listener for when a mob targets an entity
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            MobitoneServiceImpl.removeOutdatedMobitones();
            for (Entity entity : world.iterateEntities()) {
                if (entity instanceof MobEntity) {
                    MobEntity mob = (MobEntity) entity;
                    if (mob.getTarget() instanceof PlayerEntity) {
                        PlayerEntity targetPlayer = (PlayerEntity) mob.getTarget();
                        if (isSpecificMob(mob)) {
                            onMobTargetPlayer(mob, targetPlayer);
                        }
                    }
                }
            }
        });
    }

    public static boolean isSpecificMob(MobEntity mob) {
        return mob instanceof CreeperEntity ||
                mob instanceof RaiderEntity ||
                mob instanceof SkeletonEntity ||
                mob instanceof WitchEntity ||
                mob instanceof ZombieEntity;
    }

    public static void onMobTargetPlayer(MobEntity mob, PlayerEntity targetPlayer) {
        // Implement your custom functionality here
        // For example, print a message to the console
        MobitoneServiceImpl.addMobitone(mob);
        //System.out.println(mob.getName().asString() + " is targeting " + targetPlayer.getName().asString());

        // Add your desired functionality here
    }
}