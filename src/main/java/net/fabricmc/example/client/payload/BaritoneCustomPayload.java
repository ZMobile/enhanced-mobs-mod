package net.fabricmc.example.client.payload;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;


public class BaritoneCustomPayload {
    public static final Identifier ID = new Identifier("modid", "path_update");
    private final String json;

    public BaritoneCustomPayload(String json) {
        this.json = json;
    }

    public static void registerReceiver() {
        // Registering the client-side receiver for the S2C packet
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, handler, buf, responseSender) -> {
            String receivedJson = buf.readString();
            client.execute(() -> {
                // Handle the received data on the client thread
                System.out.println("Received JSON: " + receivedJson);
                // You can now process the received JSON data
            });
        });
    }

    public static void send(ServerPlayerEntity player, String json) {
        // Sending the S2C packet from the server
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