package net.fabricmc.example.bloodmoon.network;


import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.example.bloodmoon.network.handler.HandleBloodmoonStatus;
import net.fabricmc.example.bloodmoon.network.messages.MessageBloodmoonStatus;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public class PacketHandler {
	public static final Identifier BLOODMOON_STATUS_PACKET_ID = new Identifier("modid", "bloodmoon_status");

	public static void init() {
		// Register the server-side packet handler
		ServerPlayNetworking.registerGlobalReceiver(BLOODMOON_STATUS_PACKET_ID, (server, player, handler, buf, responseSender) -> {
			MessageBloodmoonStatus message = new MessageBloodmoonStatus(buf);
			server.execute(() -> HandleBloodmoonStatus.handle(message, player));
		});
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		// Register the client-side packet handler
		ClientPlayNetworking.registerGlobalReceiver(BLOODMOON_STATUS_PACKET_ID, (client, handler, buf, responseSender) -> {
			MessageBloodmoonStatus message = new MessageBloodmoonStatus(buf);
			client.execute(() -> HandleBloodmoonStatus.handle(message, client.player));
		});
	}

	public static void sendToAll(ServerWorld world, MessageBloodmoonStatus message) {
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		message.toBytes(buf);
		world.getPlayers().forEach(player -> ServerPlayNetworking.send(player, BLOODMOON_STATUS_PACKET_ID, buf));
	}

	public static void sendTo(ServerPlayerEntity player, MessageBloodmoonStatus message) {
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		message.toBytes(buf);
		ServerPlayNetworking.send(player, BLOODMOON_STATUS_PACKET_ID, buf);
	}
}