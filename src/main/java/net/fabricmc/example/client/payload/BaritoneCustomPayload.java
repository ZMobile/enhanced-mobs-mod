package net.fabricmc.example.client.payload;

import net.fabricmc.example.client.ClientPathManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;


public class BaritoneCustomPayload implements CustomPayload {
    public static final Id<BaritoneCustomPayload> ID = new Id<>(ClientPathManager.MOD_PACKET_ID);
    public static final PacketCodec<PacketByteBuf, BaritoneCustomPayload> CODEC = CustomPayload.codecOf(
            (payload, buf) -> buf.writeString(payload.json),  // Encoder
            buf -> new BaritoneCustomPayload(buf.readString()) // Decoder
    );

    private final String json;

    public BaritoneCustomPayload(String json) {
        this.json = json;
    }

    public BaritoneCustomPayload(PacketByteBuf buffer) {
        this.json = buffer.readString();
    }

    public void write(PacketByteBuf buffer) {
        buffer.writeString(json);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public String getJson() {
        return json;
    }
}