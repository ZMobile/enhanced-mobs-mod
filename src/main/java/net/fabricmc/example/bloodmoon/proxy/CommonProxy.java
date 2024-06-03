package net.fabricmc.example.bloodmoon.proxy;

import net.fabricmc.example.bloodmoon.handler.BloodmoonEventHandler;
import net.fabricmc.example.bloodmoon.network.PacketHandler;
import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;

public class CommonProxy {
	public void preInit() {
		BloodmoonEventHandler handler = new BloodmoonEventHandler();
		handler.registerEvents();

		PacketHandler.init();
	}

	public void init() {
		// Initialization code here if needed
	}

	public void postInit() {
		// Post-initialization code here if needed
	}

	public boolean isBloodmoon() {
		if (BloodmoonHandler.INSTANCE == null) {
			return false;
		} else {
			return BloodmoonHandler.INSTANCE.isBloodmoonActive();
		}
	}
}