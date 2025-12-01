package net.fabricmc.example.bloodmoon.handler;

import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class BloodmoonEventHandler {

	public void registerEvents() {
		// Only register LOAD event here - tick and join events are registered in BloodmoonHandler.initialize()
		// to prevent duplicate handler registrations that cause performance issues and freezes
		ServerWorldEvents.LOAD.register(this::loadWorld);
	}

	public void loadWorld(MinecraftServer server, ServerWorld world) {
		BloodmoonHandler.initialize(world);
		if (/*!world.isClient && */world.getRegistryKey() == World.OVERWORLD) {
			BloodmoonHandler.INSTANCE = world.getPersistentStateManager().getOrCreate(
					BloodmoonHandler.BLOODMOON_HANDLER_TYPE
			);

			if (BloodmoonHandler.INSTANCE == null) {
				BloodmoonHandler.INSTANCE = new BloodmoonHandler();
				BloodmoonHandler.INSTANCE.markDirty();
			}

			BloodmoonHandler.INSTANCE.updateClients();
		}
	}

}