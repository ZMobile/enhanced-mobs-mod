package net.fabricmc.zmobile;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.zmobile.bloodmoon.config.BloodmoonConfig;
import net.fabricmc.zmobile.bloodmoon.lib.Reference;
import net.fabricmc.zmobile.bloodmoon.proxy.ClientProxy;
import net.fabricmc.zmobile.bloodmoon.proxy.CommonProxy;
import net.fabricmc.zmobile.bloodmoon.server.CommandBloodmoon;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExampleMod implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);
	public static CommonProxy proxy;
	public static BloodmoonConfig config;

	@Override
	public void onInitialize() {
		// Initialize Bloodmoon instance and proxy
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			proxy = new ClientProxy();
			((ClientProxy) proxy).onInitializeClient();
		} else {
			proxy = new CommonProxy();
		}

		// Initialize proxy
		proxy.preInit();
		proxy.init();
		proxy.postInit();

		// Register Bloodmoon command
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			CommandBloodmoon.register(dispatcher);
		});

		// Server starting event
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			LOGGER.info("Server is starting");
		});

		LOGGER.info("ExampleMod has been initialized");
	}
}