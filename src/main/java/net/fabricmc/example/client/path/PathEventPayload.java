package net.fabricmc.example.client.path;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class PathEventPayload {
    public static final Identifier ID = new Identifier("modid", "path_event");
    private final String json;

    public PathEventPayload(String json) {
        this.json = json;
    }

    public static void registerReceiver() {
        // Registering the S2C receiver on the client
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, handler, buf, responseSender) -> {
            String receivedJson = buf.readString();
            client.execute(() -> {
                // Handle the received JSON data on the client side
                System.out.println("Received Path Event JSON: " + receivedJson);
                // You can now process the received data
            });
        });
    }

    public static void send(ServerPlayerEntity player, String json) {
        // Sending the packet from the server to the client
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString(json);
        ServerPlayNetworking.send(player, ID, buf);
    }

    public String getJson() {
        return json;
    }

    public void write(PacketByteBuf buffer) {
        buffer.writeString(json);
    }
}