package net.fabricmc.example.service;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import net.fabricmc.example.client.path.PathUpdateListener;
import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.fabricmc.example.model.MobitoneProvision;
import net.fabricmc.example.util.MinecraftServerUtil;
import net.minecraft.entity.LivingEntity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class MobitoneServiceImpl implements MobitoneService {
    private static int maxProvisionDeadlineSeconds = 120;
    private static int maxMobitoneProvisions = 25;
    private static List<MobitoneProvision> mobitoneProvisions = new ArrayList<>();
    private static List<LivingEntity> queue = new ArrayList<>();

    public MobitoneServiceImpl() {

    }

    public static void addMobitone(LivingEntity livingEntity) {
        MobitoneProvision existingMobitoneProvision = mobitoneProvisions.stream()
                .filter(mobitoneProvisionQuery -> mobitoneProvisionQuery.getLivingEntity().equals(livingEntity))
                .findFirst().orElse(null);
        if (existingMobitoneProvision != null) {
            existingMobitoneProvision.updateProvisionTime();
            return;
        }
        //if (mobitoneProvisions.size() < maxMobitoneProvisions || !BloodmoonHandler.INSTANCE.isBloodmoonActive()) {
            mobitoneProvisions.add(new MobitoneProvision(livingEntity));
            BaritoneAPI.getProvider().createBaritone(MinecraftServerUtil.getMinecraftServer(), livingEntity);

            IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForEntity(livingEntity);
            if (baritone != null && baritone.getPathingBehavior() != null) {
                PathUpdateListener pathUpdateListener = new PathUpdateListener(livingEntity.getId(), baritone.getPathingBehavior());
                baritone.getGameEventHandler().registerEventListener(pathUpdateListener);
            }
            System.out.println("Baritone instance successfully added for " + livingEntity.getName().getString());
        /*} else {
            if (!queue.contains(livingEntity)) {
                queue.add(livingEntity);
            }
        }*/
        //removeOutdatedMobitones();
        //fillInQueue();
    }

    public static void removeMobitone(LivingEntity livingEntity) {
        IBaritone goalBaritone = BaritoneAPI.getProvider().getBaritoneForEntity(livingEntity);
        if (goalBaritone != null) {
            // Clean up Baritone instance for this entity
            BaritoneAPI.getProvider().destroyBaritone(goalBaritone);
            System.out.println("Baritone instance successfully removed from " + livingEntity.getName().getString());
            // Debug log to verify cleanup
            //System.out.println("Baritone instance successfully removed for ZombieEntity on despawn");
        }
        if (queue.contains(livingEntity)) {
            queue.remove(livingEntity);
        } else {
            MobitoneProvision existingMobitoneProvision = mobitoneProvisions.stream()
                    .filter(mobitoneProvisionQuery -> mobitoneProvisionQuery.getLivingEntity().equals(livingEntity))
                    .findFirst().orElse(null);
            if (existingMobitoneProvision != null) {
                mobitoneProvisions.remove(existingMobitoneProvision);
            }
        }
        //removeOutdatedMobitones();
        fillInQueue();
    }

    public static void removeOutdatedMobitones() {
        if (!BloodmoonHandler.INSTANCE.isBloodmoonActive()) {
            return;
        }
        mobitoneProvisions.stream()
                .filter(mobitoneProvisionQuery -> {
                    // Get current time in UTC
                    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

                    // Calculate the duration between provisionTime and now
                    Duration duration = Duration.between(mobitoneProvisionQuery.getProvisionTime(), now);
                    return duration.getSeconds() > maxProvisionDeadlineSeconds;
                })
                .findFirst().ifPresent(mobitoneProvision -> {
                    IBaritone goalBaritone = BaritoneAPI.getProvider().getBaritoneForEntity(mobitoneProvision.getLivingEntity());
                    if (goalBaritone != null) {
                        // Clean up Baritone instance for this entity
                        BaritoneAPI.getProvider().destroyBaritone(goalBaritone);
                        // Debug log to verify cleanup
                        //System.out.println("Baritone instance successfully removed for ZombieEntity on despawn");
                    }
                    mobitoneProvisions.remove(mobitoneProvision);
                });
        //fillInQueue();
    }

    public static void fillInQueue() {
        /*while ((mobitoneProvisions.size() < maxMobitoneProvisions || !BloodmoonHandler.INSTANCE.isBloodmoonActive()) && !queue.isEmpty()) {
            LivingEntity entityToAdd = queue.stream()
                    .filter(livingEntity -> mobitoneProvisions.stream()
                            .noneMatch(mobitoneProvisionQuery -> mobitoneProvisionQuery.getLivingEntity().equals(livingEntity)))
                    .findFirst().orElse(null);

            if (entityToAdd != null) {
                mobitoneProvisions.add(new MobitoneProvision(entityToAdd));
                BaritoneAPI.getProvider().createBaritone(MinecraftServerUtil.getMinecraftServer(), entityToAdd);
                queue.remove(entityToAdd);
            } else {
                break;
            }
        }*/
    }

    public static void updateMobitoneProvision(LivingEntity livingEntity) {
        mobitoneProvisions.stream() // Find the MobitoneProvision with the matching entity
                .filter(mobitoneProvisionQuery -> mobitoneProvisionQuery.getLivingEntity().equals(livingEntity))
                .findFirst().ifPresent(MobitoneProvision::updateProvisionTime);
    }
}
