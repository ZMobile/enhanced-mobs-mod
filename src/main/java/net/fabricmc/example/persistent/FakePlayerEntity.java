package net.fabricmc.example.persistent;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.impl.event.interaction.FakePlayerNetworkHandler;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Arm;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

import java.util.UUID;

public class FakePlayerEntity extends ServerPlayerEntity {
    public FakePlayerEntity(ServerWorld world, BlockPos pos, GameProfile gameProfile) {
        // Provide a default instance of SyncedClientOptions
        super(
                world.getServer(),
                world,
                gameProfile,
                new SyncedClientOptions(
                        "en_us", // Language
                        8, // View distance
                        ChatVisibility.FULL, // Chat visibility
                        true, // Chat colors enabled
                        0x7F, // All player model parts enabled
                        Arm.RIGHT, // Main arm
                        false, // Text filtering disabled
                        false, // Server listing allowed
                        ParticlesMode.ALL // All particles enabled
                )
        );

        //this.setUuid(UUID.randomUUID());

        // Set the position of the FakePlayerEntity
        this.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        this.networkHandler = new FakePlayerNetworkHandler(this);
        // Set the default game mode to survival or desired mode
        this.changeGameMode(GameMode.SURVIVAL);

        // Optional: Mark the fake player as invulnerable if needed
        this.setInvulnerable(false);
    }

    @Override
    public void tick() {
        super.tick();
        //System.out.println("FakePlayerEntity ticking at: " + this.getEntityPos());
    }

    @Override
    public boolean isSpectator() {
        return false; // Ensure mobs can target the fake player
    }

    @Override
    public boolean isCreative() {
        return false; // Prevent creative mode behavior
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
        return super.createSpawnPacket(entityTrackerEntry);
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        System.out.println("Fake player took damage from: " + source.getName());
        return super.damage(world, source, amount);
    }
}
