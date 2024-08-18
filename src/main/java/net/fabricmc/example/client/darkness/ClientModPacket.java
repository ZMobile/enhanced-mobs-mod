package net.fabricmc.example.client.darkness;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ClientModPacket {
    public static final Identifier ID = new Identifier("mobitone", "mobitone_mod_packet");

    public static void register() {
        // Registering the packet receiver using the new API format
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, responseSender) -> handle(server, player, handler, buf));
    }

    private static void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf) {
        server.execute(() -> {
            // Handle the packet on the server thread
            System.out.println("Set has mod to true");
            ((ModPlayerData) player).setHasMod(true);
        });
    }
}