package net.fabricmc.example.client;

import net.fabricmc.example.client.payload.BaritoneCustomPayload;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public class ModPacketCodec {
    public static final PacketCodec<PacketByteBuf, BaritoneCustomPayload> CODEC = CustomPayload.codecOf(
            (BaritoneCustomPayload baritoneCustomPayload, PacketByteBuf buf) -> buf.writeString(baritoneCustomPayload.getJson()), // Encoder
            BaritoneCustomPayload::new         // Decoder
    );
}

