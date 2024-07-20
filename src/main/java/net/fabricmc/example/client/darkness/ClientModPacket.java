package net.fabricmc.example.client.darkness;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

public class ClientModPacket implements CustomPayload {
    /*public static final Identifier ID = Identifier.of("mobitone", "mobitone_mod_packet");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(new Id<>(ID), (payload, context) -> {
            MinecraftServer minecraftServer = context.server();
            PlayerEntity player = context.player();
            handle(minecraftServer, player);
        });
    }

    public static void handle(MinecraftServer minecraftServer, PlayerEntity player) {
        minecraftServer.execute(() -> {
            // Server received the packet, player has the mod
            System.out.println("Set has mod to true");
            ((ModPlayerData) player).setHasMod(true);
        });
    }*/

    @Override
    public Id<? extends CustomPayload> getId() {
        return null;//new Id<>(ID);
    }
}