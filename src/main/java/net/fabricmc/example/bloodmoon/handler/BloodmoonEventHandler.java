package net.fabricmc.example.bloodmoon.handler;

import net.fabricmc.example.EnhancedMobsMod;
import net.fabricmc.example.bloodmoon.config.BloodmoonConfig;
import net.fabricmc.example.bloodmoon.reference.Reference;
import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BloodmoonEventHandler {

	public void registerEvents() {
		ServerWorldEvents.LOAD.register(this::loadWorld);
		ServerTickEvents.END_WORLD_TICK.register(this::endWorldTick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> playerJoinedWorld(handler.getPlayer()));
	}

	public void loadWorld(MinecraftServer server, ServerWorld world) {
		BloodmoonHandler.initialize(world);

		if (!world.isClient && world.getRegistryKey() == World.OVERWORLD) {
			BloodmoonHandler.INSTANCE = world.getPersistentStateManager().getOrCreate(
                    BloodmoonHandler::readNbt, // This is the read function that reads from NBT and returns a BloodmoonHandler
                    BloodmoonHandler::new, // This is the supplier that provides a new instance if one doesn't already exist
					"bloodmoon" // This is the id under which the state is stored
			);


			// This null check is no longer necessary, since `getOrCreate` ensures a valid instance is returned.
			BloodmoonHandler.INSTANCE.updateClients();
		}
	}

	public ActionResult sleepInBed(ServerPlayerEntity player, BlockPos pos) {
		if (BloodmoonHandler.INSTANCE != null && BloodmoonConfig.GENERAL.NO_SLEEP) {
			if (EnhancedMobsMod.proxy.isBloodmoon()) {
				player.sendMessage(Text.translatable("text.bloodmoon.nosleep").formatted(Formatting.RED), true);
				return ActionResult.FAIL;
			}
		}
		return ActionResult.PASS;
	}

	public void playerJoinedWorld(ServerPlayerEntity player) {
		if (BloodmoonHandler.INSTANCE != null) {
			BloodmoonHandler.INSTANCE.playerJoinedWorld(player);
		}
	}

	public void endWorldTick(ServerWorld world) {
		if (BloodmoonHandler.INSTANCE != null) {
			BloodmoonHandler.endWorldTick(world);
		}
	}
}