package net.fabricmc.example.persistent;

import com.mojang.authlib.GameProfile;
import net.fabricmc.example.persistent.FakePlayerEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class FakePlayerManager {
    // Map to track fake players by their real player UUID
    private static final Map<UUID, FakePlayerEntity> fakePlayers = new HashMap<>();

    public static void registerEvents() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            ServerWorld world = (ServerWorld) player.getWorld();
            UUID playerUUID = player.getUuid();

            // Check if a fake player already exists for this player
            if (fakePlayers.containsKey(playerUUID)) {
                System.out.println("Fake player already exists for " + player.getName().getString());
                return;
            }

            // Create and spawn the fake player
            BlockPos pos = player.getBlockPos();
            GameProfile gameProfile = new GameProfile(UUID.randomUUID(), "FakePlayerName");
            /*FakePlayerEntity fakePlayer = new FakePlayerEntity(world, pos, gameProfile);
            fakePlayer.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            fakePlayer.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);

            // Spawn the fake player in the world
            if (world.spawnEntity(fakePlayer)) {
                fakePlayers.put(playerUUID, fakePlayer);
                addFakePlayer(fakePlayer);
                System.out.println("Fake player spawned for " + player.getName().getString());
            } else {
                System.err.println("Failed to spawn fake player for " + player.getName().getString());
            }*/
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            UUID playerUUID = player.getUuid();

            // Remove the corresponding fake player, if it exists
            FakePlayerEntity fakePlayer = fakePlayers.remove(playerUUID);
            if (fakePlayer != null) {
                fakePlayer.discard();
                System.out.println("Removed fake player for " + player.getName().getString());
            }
        });
    }

    public static void addFakePlayer(FakePlayerEntity fakePlayer) {
        // Add the fake player to the tab list
        PlayerListS2CPacket addToTabListPacket = new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, fakePlayer);

        // Create spawn and metadata packets
        EntitySpawnS2CPacket spawnPacket = new EntitySpawnS2CPacket(
                fakePlayer.getId(),                           // Entity ID
                fakePlayer.getUuid(),                         // Entity UUID
                fakePlayer.getX(),                            // X coordinate
                fakePlayer.getY(),                            // Y coordinate
                fakePlayer.getZ(),                            // Z coordinate
                fakePlayer.getPitch(),                        // Pitch (rotation)
                fakePlayer.getYaw(),                          // Yaw (rotation)
                fakePlayer.getType(),
                0, // Entity type (player)
                Vec3d.ZERO   ,                                            // Head yaw (for look direction)
                0                                  // Motion (initial velocity)
        );
        List<DataTracker.SerializedEntry<?>> trackedValues = fakePlayer.getDataTracker().getChangedEntries();
        EntityTrackerUpdateS2CPacket metadataPacket = new EntityTrackerUpdateS2CPacket(fakePlayer.getId(), trackedValues);

        // Send packets to all connected players
        for (ServerPlayerEntity player : fakePlayer.getServer().getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(addToTabListPacket);
            player.networkHandler.sendPacket(spawnPacket);
            player.networkHandler.sendPacket(metadataPacket);
        }
    }
}
