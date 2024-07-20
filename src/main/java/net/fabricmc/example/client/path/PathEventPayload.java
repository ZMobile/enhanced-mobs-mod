package net.fabricmc.example.client.path;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.util.Identifier;

public class PathEventPayload implements CustomPayload {

    public static final Identifier ID = Identifier.of("modid", "path_event");

    public PathEventPayload(PacketByteBuf buf) {
        //super(ID, buf);
    }

    public PathEventPayload(String json) {
        //super(ID, new PacketByteBuf(Unpooled.buffer()).writeString(json));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return new Id<>(ID);
    }
}
