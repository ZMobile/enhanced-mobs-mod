package net.fabricmc.example.bloodmoon.proxy;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.example.bloodmoon.client.ClientBloodmoonHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ClientProxy extends CommonProxy implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Register the ClientBloodmoonHandler for client tick events
		ClientTickEvents.END_CLIENT_TICK.register(ClientBloodmoonHandler.INSTANCE::onClientTick);

		// Initialize client-side path rendering
		new net.fabricmc.example.client.ClientPathManager().onInitializeClient();
		new net.fabricmc.example.client.ClientPlacingBlockHighlighter().onInitializeClient();
		new net.fabricmc.example.client.ClientTargetBlockHighlighter().onInitializeClient();

		// Additional client-side initialization logic, if needed
		/*ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			// Any other client-specific initialization can go here
		});*/
	}

	@Override
	public boolean isBloodmoon() {
		return ClientBloodmoonHandler.INSTANCE.isBloodmoonActive();
	}
}