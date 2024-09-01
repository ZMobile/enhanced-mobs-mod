package net.fabricmc.example.client;

import net.fabricmc.example.client.payload.BaritoneCustomPayload;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

import static net.fabricmc.example.client.ClientPathManager.MOD_PACKET_ID;

public class CustomPayloadRegistry {
    private static final Map<Identifier, CustomPayload.Type<?, ?>> TYPES = new HashMap<>();

    public static <B extends PacketByteBuf, T extends CustomPayload> void register(CustomPayload.Type<B, T> type) {
        TYPES.put(type.id().id(), type);
    }

    public static CustomPayload.Type<?, ?> get(Identifier id) {
        return TYPES.get(id);
    }

    public static void initialize() {
        // Register your custom payload types here
        CustomPayload.Type<PacketByteBuf, BaritoneCustomPayload> modPacketType = new CustomPayload.Type<>(
                new CustomPayload.Id<>(MOD_PACKET_ID), ModPacketCodec.CODEC
        );
        register(modPacketType);
    }
}
