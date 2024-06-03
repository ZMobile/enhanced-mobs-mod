package net.fabricmc.zmobile.bloodmoon.network.handler;


import net.fabricmc.zmobile.bloodmoon.client.ClientBloodmoonHandler;
import net.fabricmc.zmobile.bloodmoon.network.messages.MessageBloodmoonStatus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public class HandleBloodmoonStatus {
	public static void handle(MessageBloodmoonStatus message, ServerPlayerEntity player) {
		boolean isBloodmoon = message.bloodmoonActive;
		// Perform server-side logic with isBloodmoon
	}

	public static void handle(MessageBloodmoonStatus message, ClientPlayerEntity player) {
		boolean isBloodmoon = message.bloodmoonActive;
		// Perform client-side logic with isBloodmoon
		MinecraftClient.getInstance().execute(() -> {
			ClientBloodmoonHandler.INSTANCE.setBloodmoon(isBloodmoon);
		});
	}
}