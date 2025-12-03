package net.fabricmc.example.bloodmoon.network.handler;


import net.fabricmc.example.bloodmoon.network.messages.MessageBloodmoonStatus;
import net.minecraft.server.network.ServerPlayerEntity;

public class HandleBloodmoonStatus {
	public static void handle(MessageBloodmoonStatus message, ServerPlayerEntity player) {
		boolean isBloodmoon = message.bloodmoonActive;
		// Perform server-side logic with isBloodmoon
	}

	// Client-side handler is in the client source set
}