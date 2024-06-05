package net.fabricmc.example;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.example.bloodmoon.config.BloodmoonConfig;
import net.fabricmc.example.bloodmoon.proxy.ClientProxy;
import net.fabricmc.example.bloodmoon.proxy.CommonProxy;
import net.fabricmc.example.bloodmoon.reference.Reference;
import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.fabricmc.example.bloodmoon.server.BloodmoonSpawner;
import net.fabricmc.example.bloodmoon.server.CommandBloodmoon;
import net.fabricmc.example.listener.MobTargetListener;
import net.fabricmc.example.util.MinecraftServerUtil;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents.START_SLEEPING;

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
			MinecraftServerUtil.setMinecraftServer(server);
			LOGGER.info("Server is starting");
		});

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			// Check if the block being interacted with is a bed
			if (world.getBlockState(hitResult.getBlockPos()).getBlock() == Blocks.WHITE_BED ||
					world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof net.minecraft.block.BedBlock) {
				if (BloodmoonHandler.INSTANCE.isBloodmoonActive()) {
					// Cancel the interaction
					return ActionResult.FAIL;
				}
			}
			return ActionResult.PASS;
		});

		EntitySleepEvents.START_SLEEPING.register((player, pos) -> {
			// Wake the player up and send a message
			if (BloodmoonHandler.INSTANCE.isBloodmoonActive()) {
				player.wakeUp();
				player.sendMessage(Text.literal("You cannot sleep right now! The bloodmoon is active"));
			}
		});
		MobTargetListener.register();

		LOGGER.info("ExampleMod has been initialized");
	}
}