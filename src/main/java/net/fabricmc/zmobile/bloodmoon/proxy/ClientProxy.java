package net.fabricmc.zmobile.bloodmoon.proxy;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.zmobile.bloodmoon.client.ClientBloodmoonHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ClientProxy extends CommonProxy implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Register the ClientBloodmoonHandler for client tick events
		ClientTickEvents.END_CLIENT_TICK.register(ClientBloodmoonHandler.INSTANCE::onClientTick);

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