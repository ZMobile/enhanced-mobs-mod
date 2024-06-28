package net.fabricmc.example.enforcement;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ClientModPacket implements CustomPayload {
    public static final Identifier ID = new Identifier("mobitone", "mobitone_mod_packet");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ID, ClientModPacket::handle);
    }

    public static void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        server.execute(() -> {
            // Server received the packet, player has the mod
            System.out.println("Set has mod to true");
            ((ModPlayerData) player).setHasMod(true);
        });
    }

    @Override
    public void write(PacketByteBuf buf) {

    }

    @Override
    public Identifier id() {
        return ID;
    }
}