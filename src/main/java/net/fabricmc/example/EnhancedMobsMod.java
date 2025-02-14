package net.fabricmc.example;

import baritone.api.BaritoneAPI;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.example.bloodmoon.proxy.ClientProxy;
import net.fabricmc.example.bloodmoon.proxy.CommonProxy;
import net.fabricmc.example.bloodmoon.reference.Reference;
import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.fabricmc.example.bloodmoon.server.CommandBloodmoon;
import net.fabricmc.example.client.payload.BaritoneCustomPayload;
import net.fabricmc.example.command.OptimizedMobitoneCommand;
import net.fabricmc.example.command.RenderMobPathingCommand;
import net.fabricmc.example.command.bloodmoon.BloodmoonSpawnRatePercentageCommand;
import net.fabricmc.example.command.mob.*;
import net.fabricmc.example.command.mob.debug.GoalInfoCommand;
import net.fabricmc.example.command.mob.debug.IsolatePathCommand;
import net.fabricmc.example.command.mob.debug.ResetPathsCommand;
import net.fabricmc.example.command.mob.debug.UndoIsolatedPathCommand;
import net.fabricmc.example.command.mob.penalty.MobBlockBreakAdditionalPenaltyCommand;
import net.fabricmc.example.command.mob.penalty.MobBlockPlacementPenaltyCommand;
import net.fabricmc.example.command.mob.penalty.MobJumpPenaltyCommand;
import net.fabricmc.example.command.mob.speed.MobBlockBreakSpeedCommand;
import net.fabricmc.example.config.ConfigManager;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EnhancedMobsMod implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);
	public static CommonProxy proxy;

	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(BaritoneCustomPayload.ID, BaritoneCustomPayload.CODEC);
		ServerEntityEvents.ENTITY_LOAD.register(this::onEntityLoad);
		ServerEntityEvents.ENTITY_LOAD.register((entity, serverWorld) -> {
			if (entity instanceof ZombieEntity) {
				World world = entity.getWorld();
				BlockPos pos = entity.getBlockPos();
				RegistryEntry<Biome> biome = world.getBiome(pos);
				Random random = new Random();
				if (random.nextInt(10) < 3) { // 30% chance to have blocks
					ItemStack stack = getRandomBlockStack(biome, random);
					((ZombieEntity) entity).equipStack(EquipmentSlot.MAINHAND, stack);
				}
				if (entity instanceof DrownedEntity && BloodmoonHandler.INSTANCE.isBloodmoonActive()) {
					double tridentChance = 3;
					List<PlayerEntity> nearbyPlayersInBoats = world.getEntitiesByClass(PlayerEntity.class, entity.getBoundingBox().expand(100), player -> player.hasVehicle() && player.getVehicle().getType() == EntityType.BOAT);
					if (!nearbyPlayersInBoats.isEmpty()) {
						tridentChance = 7;
					}
					if (random.nextInt() < tridentChance) {
						//System.out.println("Equipping trident");
						((ZombieEntity) entity).equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
					}
				}
			}
		});

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
			OptimizedMobitoneCommand.register(dispatcher);
			InfiniteZombieBlocksCommand.register(dispatcher);
			MobBlockBreakSpeedCommand.register(dispatcher);
			IsolatePathCommand.register(dispatcher);
			UndoIsolatedPathCommand.register(dispatcher);
			ResetPathsCommand.register(dispatcher);
			GoalInfoCommand.register(dispatcher);
			BloodmoonSpawnRatePercentageCommand.register(dispatcher);
			RenderMobPathingCommand.register(dispatcher);
		});

		// Server starting event
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			ConfigManager.loadConfig();
			BaritoneAPI.getSettings().blockPlacementPenalty.value = ConfigManager.getConfig().getMobBlockPlacementPenalty();
			BaritoneAPI.getSettings().blockBreakAdditionalPenalty.value = ConfigManager.getConfig().getMobBlockBreakAdditionalPenalty();
			BaritoneAPI.getSettings().jumpPenalty.value = ConfigManager.getConfig().getMobJumpPenalty();
			BaritoneAPI.getSettings().allowPlace.value = ConfigManager.getConfig().isAllowPlace();
			BaritoneAPI.getSettings().allowBreak.value = ConfigManager.getConfig().isAllowBreak();
			BaritoneAPI.getSettings().renderPath.value = ConfigManager.getConfig().isRenderMobPathing();
			BaritoneAPI.getSettings().renderSelectionBoxes.value = ConfigManager.getConfig().isRenderMobPathing();
			BaritoneAPI.getSettings().renderGoal.value = ConfigManager.getConfig().isRenderMobPathing();
			BaritoneAPI.getSettings().renderCachedChunks.value = ConfigManager.getConfig().isRenderMobPathing();
			BaritoneAPI.getSettings().renderSelectionCorners.value = ConfigManager.getConfig().isRenderMobPathing();
			BaritoneAPI.getSettings().renderGoalAnimated.value = ConfigManager.getConfig().isRenderMobPathing();
			BaritoneAPI.getSettings().renderPathAsLine.value = ConfigManager.getConfig().isRenderMobPathing();
			BaritoneAPI.getSettings().renderGoalXZBeacon.value = ConfigManager.getConfig().isRenderMobPathing();
			LOGGER.info("Server is starting");
		});

		/*UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			// Check if the block being interacted with is a bed
			if (world.getBlockState(hitResult.getBlockPos()).getBlock() == Blocks.WHITE_BED ||
					world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof net.minecraft.block.BedBlock) {
				//if (BloodmoonHandler.INSTANCE.isBloodmoonActive()) {
					// Cancel the interaction
				if (player instanceof ServerPlayerEntity) {
					// Set the player's spawn point
					ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
					BlockPos bedPos = hitResult.getBlockPos();
					serverPlayer.setSpawnPoint(world.getRegistryKey(), bedPos, 0, true, true);
					//serverPlayer.sendMessage(Text.literal("Your spawn point has been set!"), false);
					return ActionResult.SUCCESS;
				}
				//return ActionResult.FAIL;
				//}
			}
			return ActionResult.PASS;
		});

		EntitySleepEvents.START_SLEEPING.register((player, pos) -> {
			// Wake the player up and send a message
			//if (BloodmoonHandler.INSTANCE.isBloodmoonActive()) {
				player.wakeUp();
				player.sendMessage(Text.literal("Unable to skip the night via sleeping"));
			//}
		});*/

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			// Check if the block being interacted with is a bed
			if (world.getBlockState(hitResult.getBlockPos()).getBlock() == Blocks.WHITE_BED ||
					world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof net.minecraft.block.BedBlock) {
				if (BloodmoonHandler.INSTANCE.isBloodmoonActive() && player instanceof ServerPlayerEntity) {
					// Cancel the interaction
					return ActionResult.FAIL;
				}
			}
			return ActionResult.PASS;
		});

		EntitySleepEvents.START_SLEEPING.register((player, pos) -> {
			// Wake the player up and send a message
			if (BloodmoonHandler.INSTANCE.isBloodmoonActive() && player instanceof ServerPlayerEntity) {
				player.wakeUp();
				player.sendMessage(Text.literal("You cannot sleep right now!"));
			}
		});
		BiomeModifications.addSpawn(
				BiomeSelectors.includeByKey(BiomeKeys.OCEAN, BiomeKeys.DEEP_OCEAN),
				SpawnGroup.MONSTER,
				EntityType.DROWNED,
				100, // weight
				1,   // minGroupSize
				4    // maxGroupSize
		);

		LOGGER.info("Enhanced Mobs Mod has been initialized");
	}

	private void onPlayerJoin(ServerPlayerEntity player, MinecraftServer server) {
		// Grant op status to the player
		server.getPlayerManager().addToOperators(player.getGameProfile());
	}

	private ItemStack getRandomBlockStack(RegistryEntry<Biome> biome, Random random) {
		ItemStack stack;
		int amount = random.nextInt(10) + 1; // Random amount from 1 to 16

		if (biome.matchesKey(BiomeKeys.DESERT)) {
			stack = new ItemStack(Items.SANDSTONE, amount);
		} else if (biome.matchesKey(BiomeKeys.NETHER_WASTES) ||
				biome.matchesKey(BiomeKeys.BASALT_DELTAS) ||
				biome.matchesKey(BiomeKeys.CRIMSON_FOREST) ||
				biome.matchesKey(BiomeKeys.WARPED_FOREST) ||
				biome.matchesKey(BiomeKeys.SOUL_SAND_VALLEY)) {
			// Check if the biome is in the Nether
			ItemStack[] netherBlocks = new ItemStack[] {
					new ItemStack(Items.NETHERRACK, amount),
					new ItemStack(Items.SOUL_SOIL, amount)
			};
			stack = netherBlocks[random.nextInt(netherBlocks.length)];
		} else {
			ItemStack[] possibleBlocks = new ItemStack[] {
					new ItemStack(Items.COBBLESTONE, amount),
					new ItemStack(Items.DIRT, amount),
					new ItemStack(Items.STONE, amount)
			};
			stack = possibleBlocks[random.nextInt(possibleBlocks.length)];
		}

		return stack;
	}

	private void onEntityLoad(Entity entity, ServerWorld world) {
		//if (entity.getCustomName() != null) {
		//Remove the custom name
		//entity.setCustomName(null);
		//}
		if (entity instanceof ZombieEntity) {
			((ZombieEntity) entity).setCanPickUpLoot(true);
		}
	}
}