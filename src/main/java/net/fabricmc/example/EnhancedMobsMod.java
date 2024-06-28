package net.fabricmc.example;

import baritone.api.BaritoneAPI;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.example.bloodmoon.config.BloodmoonConfig;
import net.fabricmc.example.bloodmoon.proxy.ClientProxy;
import net.fabricmc.example.bloodmoon.proxy.CommonProxy;
import net.fabricmc.example.bloodmoon.reference.Reference;
import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.fabricmc.example.bloodmoon.server.CommandBloodmoon;
import net.fabricmc.example.command.RenderMobPathingCommand;
import net.fabricmc.example.command.mob.*;
import net.fabricmc.example.command.mob.penalty.MobBlockBreakAdditionalPenaltyCommand;
import net.fabricmc.example.command.mob.penalty.MobBlockPlacementPenaltyCommand;
import net.fabricmc.example.command.mob.penalty.MobJumpPenaltyCommand;
import net.fabricmc.example.config.ConfigManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EnhancedMobsMod implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);
	public static CommonProxy proxy;
	public static BloodmoonConfig config;

	@Override
	public void onInitialize() {
		ConfigManager.loadConfig();
		BaritoneAPI.getSettings().blockPlacementPenalty.value = ConfigManager.getConfig().getMobBlockPlacementPenalty();
		BaritoneAPI.getSettings().blockBreakAdditionalPenalty.value = ConfigManager.getConfig().getMobBlockBreakAdditionalPenalty();
		BaritoneAPI.getSettings().jumpPenalty.value = ConfigManager.getConfig().getMobJumpPenalty();
		BaritoneAPI.getSettings().allowPlace.value = ConfigManager.getConfig().isAllowPlace();
		BaritoneAPI.getSettings().allowBreak.value = ConfigManager.getConfig().isAllowBreak();
		if (ConfigManager.getConfig().isRenderMobPathing()) {
			BaritoneAPI.getSettings().renderPath.value = true;
			BaritoneAPI.getSettings().renderSelectionBoxes.value = true;
			BaritoneAPI.getSettings().renderGoal.value = true;
			BaritoneAPI.getSettings().renderCachedChunks.value = true;
			BaritoneAPI.getSettings().renderSelectionCorners.value = false;
			BaritoneAPI.getSettings().renderGoalAnimated.value = false;
			BaritoneAPI.getSettings().renderPathAsLine.value = false;
			BaritoneAPI.getSettings().renderGoalXZBeacon.value = false;
		} else {
			BaritoneAPI.getSettings().renderPath.value = false;
			BaritoneAPI.getSettings().renderSelectionBoxes.value = false;
			BaritoneAPI.getSettings().renderGoal.value = false;
			BaritoneAPI.getSettings().renderCachedChunks.value = false;
			BaritoneAPI.getSettings().renderSelectionCorners.value = false;
			BaritoneAPI.getSettings().renderGoalAnimated.value = false;
			BaritoneAPI.getSettings().renderPathAsLine.value = false;
			BaritoneAPI.getSettings().renderGoalXZBeacon.value = false;
		}
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, registryAccess) -> {
			AllowPlaceCommand.register(dispatcher);
			AllowBreakCommand.register(dispatcher);
			CreepersExplodeObstructionsCommand.register(dispatcher);
			RaidersBreakBlocksCommand.register(dispatcher);
			SkeletonsBreakBlocksCommand.register(dispatcher);
			WitchesBreakBlocksCommand.register(dispatcher);
			ZombiesBreakAndPlaceBlocksCommand.register(dispatcher);
			MobBlockBreakAdditionalPenaltyCommand.register(dispatcher);
			MobBlockPlacementPenaltyCommand.register(dispatcher);
			MobJumpPenaltyCommand.register(dispatcher);
			RenderMobPathingCommand.register(dispatcher);
		});
		ServerEntityEvents.ENTITY_LOAD.register(this::onEntityLoad);
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

		LOGGER.info("Enhanced Mobs Mod has been initialized");
	}

	private void onEntityLoad(Entity entity, ServerWorld world) {
		if (entity instanceof ZombieEntity) {
			((ZombieEntity) entity).setCanPickUpLoot(true);
		}
	}
}