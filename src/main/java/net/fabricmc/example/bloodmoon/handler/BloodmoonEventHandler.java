package net.fabricmc.example.bloodmoon.handler;

import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BloodmoonEventHandler {
	private static final Logger LOGGER = LogManager.getLogger("bloodmoon");

	public void registerEvents() {
		// Only register LOAD event here - tick and join events are registered in BloodmoonHandler.initialize()
		// to prevent duplicate handler registrations that cause performance issues and freezes
		ServerWorldEvents.LOAD.register(this::loadWorld);
	}

	public void loadWorld(MinecraftServer server, ServerWorld world) {
		LOGGER.info("BloodmoonEventHandler.loadWorld() START for dimension: {}", world.getRegistryKey().getValue());
		BloodmoonHandler.initialize(world);
		if (/*!world.isClient && */world.getRegistryKey() == World.OVERWORLD) {
			LOGGER.info("BloodmoonEventHandler: Getting persistent state for Overworld");
			BloodmoonHandler.INSTANCE = world.getPersistentStateManager().getOrCreate(
					BloodmoonHandler.BLOODMOON_HANDLER_TYPE
			);

			if (BloodmoonHandler.INSTANCE == null) {
				LOGGER.info("BloodmoonEventHandler: Creating new BloodmoonHandler instance");
				BloodmoonHandler.INSTANCE = new BloodmoonHandler();
				BloodmoonHandler.INSTANCE.markDirty();
			}

			LOGGER.info("BloodmoonEventHandler: Calling updateClients()");
			BloodmoonHandler.INSTANCE.updateClients();
			LOGGER.info("BloodmoonEventHandler: updateClients() completed");
		}
		LOGGER.info("BloodmoonEventHandler.loadWorld() END for dimension: {}", world.getRegistryKey().getValue());
	}

}