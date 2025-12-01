package net.fabricmc.example.bloodmoon.limiter;

import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import java.util.ArrayList;
import java.util.List;

public class BloodmoonMobLimiter {
    private static final int CHUNK_RADIUS = 6;
    private static final int BLOCK_RADIUS = CHUNK_RADIUS * 16;
    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter % 20 != 0) return; // Every second

            for (ServerWorld world : server.getWorlds()) {
                if (BloodmoonHandler.INSTANCE == null || !BloodmoonHandler.INSTANCE.isBloodmoonActive()) continue;

                // Create a copy of entities to avoid concurrent modification
                List<Entity> entities = new ArrayList<>();
                for (Entity entity : world.iterateEntities()) {
                    entities.add(entity);
                }

                for (Entity mob : entities) {
                    if (!(mob instanceof MobEntity mobEntity) || !(mob instanceof Monster)) continue;
                    if (mobEntity.isPersistent()) {
                        continue; // Skip persistent mobs
                    }
                    boolean tooFar = true;
                    Vec3d mobPos = mob.getPos();

                    for (ServerPlayerEntity player : world.getPlayers()) {
                        if (player.squaredDistanceTo(mobPos) <= BLOCK_RADIUS * BLOCK_RADIUS) {
                            tooFar = false;
                            break;
                        }
                    }

                    if (tooFar) {
                        mob.discard(); // Despawn mob
                    }
                }
            }
        });
    }
}
