package net.fabricmc.example;

import baritone.api.BaritoneAPI;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.example.bloodmoon.proxy.ClientProxy;
import net.fabricmc.example.bloodmoon.proxy.CommonProxy;
import net.fabricmc.example.bloodmoon.reference.Reference;
import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.fabricmc.example.bloodmoon.server.CommandBloodmoon;
import net.fabricmc.example.client.payload.BaritoneCustomPayload;
import net.fabricmc.example.command.OptimizedMobitoneCommand;
import net.fabricmc.example.command.TrueDarknessEnforcedCommand;
import net.fabricmc.example.command.bloodmoon.BloodmoonChancePercentageCommand;
import net.fabricmc.example.command.bloodmoon.BloodmoonSpawnRatePercentageCommand;
import net.fabricmc.example.command.bloodmoon.BuildingMiningMobsDuringBloodmoonOnly;
import net.fabricmc.example.command.bloodmoon.DaysBeforeBloodmoonPossibilityCommand;
import net.fabricmc.example.command.logout.LogoutQueueCommand;
import net.fabricmc.example.command.mob.*;
import net.fabricmc.example.command.mob.debug.GoalInfoCommand;
import net.fabricmc.example.command.mob.debug.IsolatePathCommand;
import net.fabricmc.example.command.mob.debug.ResetPathsCommand;
import net.fabricmc.example.command.mob.debug.UndoIsolatedPathCommand;
import net.fabricmc.example.command.mob.penalty.MobBlockBreakAdditionalPenaltyCommand;
import net.fabricmc.example.command.mob.penalty.MobBlockPlacementPenaltyCommand;
import net.fabricmc.example.command.mob.penalty.MobJumpPenaltyCommand;
import net.fabricmc.example.command.mob.speed.MobBlockBreakSpeedCommand;
import net.fabricmc.example.command.performance.*;
import net.fabricmc.example.config.ConfigManager;
import net.fabricmc.example.client.darkness.ModPlayerData;
import net.fabricmc.example.util.MinecraftServerUtil;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
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
		System.out.println("Initializing mod...");
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
			PayloadTypeRegistry.playS2C().register(BaritoneCustomPayload.ID, BaritoneCustomPayload.CODEC);
		}
		ServerEntityEvents.ENTITY_LOAD.register(this::onEntityLoad);
		ServerEntityEvents.ENTITY_LOAD.register((entity, serverWorld) -> {
			if (entity instanceof ZombieEntity) {
				World world = entity.getWorld();
				BlockPos pos = entity.getBlockPos();
				RegistryEntry<Biome> biome = world.getBiome(pos);
				Random random = new Random();
				//if (ConfigManager.getConfig().zombiesSpawnWithBlocks()) {
				if (random.nextInt(10) < 3) { // 30% chance to have blocks
					ItemStack stack = getRandomBlockStack(biome, random);
					((ZombieEntity) entity).equipStack(EquipmentSlot.MAINHAND, stack);
				}
				if (entity instanceof DrownedEntity && BloodmoonHandler.INSTANCE.isBloodmoonActive()) {
					double tridentChance = 3;

					// Create a set of all boat entity types
					Set<EntityType<?>> boatTypes = new HashSet<>();
					boatTypes.add(EntityType.ACACIA_BOAT);
					boatTypes.add(EntityType.ACACIA_CHEST_BOAT);
					boatTypes.add(EntityType.BIRCH_BOAT);
					boatTypes.add(EntityType.BIRCH_CHEST_BOAT);
					boatTypes.add(EntityType.DARK_OAK_BOAT);
					boatTypes.add(EntityType.DARK_OAK_CHEST_BOAT);
					boatTypes.add(EntityType.JUNGLE_BOAT);
					boatTypes.add(EntityType.JUNGLE_CHEST_BOAT);
					boatTypes.add(EntityType.MANGROVE_BOAT);
					boatTypes.add(EntityType.MANGROVE_CHEST_BOAT);
					boatTypes.add(EntityType.CHERRY_BOAT);
					boatTypes.add(EntityType.CHERRY_CHEST_BOAT);
					boatTypes.add(EntityType.SPRUCE_BOAT);
					boatTypes.add(EntityType.SPRUCE_CHEST_BOAT);
					boatTypes.add(EntityType.OAK_BOAT);
					boatTypes.add(EntityType.OAK_CHEST_BOAT);
					boatTypes.add(EntityType.BAMBOO_RAFT);
					boatTypes.add(EntityType.BAMBOO_CHEST_RAFT);

					// Check for nearby players in boats
					List<PlayerEntity> nearbyPlayersInBoats = world.getEntitiesByClass(
							PlayerEntity.class,
							entity.getBoundingBox().expand(100),
							player -> player.hasVehicle() && boatTypes.contains(player.getVehicle().getType())
					);

					if (!nearbyPlayersInBoats.isEmpty()) {
						tridentChance = 7;
					}

					if (random.nextDouble() < tridentChance / 10.0) { // Adjusted for proper percentage calculation
						// Equip the drowned with a trident
						((ZombieEntity) entity).equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
					}
				}

				//}
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
			TrueDarknessEnforcedCommand.register(dispatcher);
			OptimizedMobitoneCommand.register(dispatcher);
			InfiniteZombieBlocksCommand.register(dispatcher);
			MobBlockBreakSpeedCommand.register(dispatcher);
			if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
				IsolatePathCommand.register(dispatcher);
				UndoIsolatedPathCommand.register(dispatcher);
				ResetPathsCommand.register(dispatcher);
			}
			//GoalInfoCommand.register(dispatcher);
			BloodmoonSpawnRatePercentageCommand.register(dispatcher);
			BloodmoonChancePercentageCommand.register(dispatcher);
			DaysBeforeBloodmoonPossibilityCommand.register(dispatcher);
			BuildingMiningMobsDuringBloodmoonOnly.register(dispatcher);
			//LogoutQueueCommand.register(dispatcher);
			CreeperHissCommand.register(dispatcher);
			SpiderSpeedCommand.register(dispatcher);
			SlowPathDelayCommand.register(dispatcher);
			SlowPathCommand.register(dispatcher);
			PrimaryTimeoutCommand.register(dispatcher);
			FailureTimeoutCommand.register(dispatcher);
			PlanAheadPrimaryTimeoutCommand.register(dispatcher);
			PlanAheadFailureTimeoutCommand.register(dispatcher);
		});

		// Server starting event
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			MinecraftServerUtil.setMinecraftServer(server);
			ConfigManager.loadConfig();
			BaritoneAPI.getSettings().blockPlacementPenalty.value = ConfigManager.getConfig().getMobBlockPlacementPenalty();
			BaritoneAPI.getSettings().blockBreakAdditionalPenalty.value = ConfigManager.getConfig().getMobBlockBreakAdditionalPenalty();
			BaritoneAPI.getSettings().jumpPenalty.value = ConfigManager.getConfig().getMobJumpPenalty();
			BaritoneAPI.getSettings().allowPlace.value = ConfigManager.getConfig().isAllowPlace();
			BaritoneAPI.getSettings().allowBreak.value = ConfigManager.getConfig().isAllowBreak();
			BaritoneAPI.getSettings().slowPath.value = ConfigManager.getConfig().isSlowPath();
			BaritoneAPI.getSettings().slowPathTimeDelayMS.value = ConfigManager.getConfig().getSlowPathDelay();
			LOGGER.info("Server is starting");
		});

		//ClientModPacket.register();
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			((ModPlayerData) handler.getPlayer()).setHasMod(false);

			// Schedule the check after a delay (e.g., 5 seconds)
			scheduler.schedule(() -> server.execute(() -> {
				if (!((ModPlayerData) handler.getPlayer()).hasMod() && ConfigManager.getConfig().isTrueDarknessEnforced()) {
					handler.disconnect(Text.literal("You must have Zante's True Darkness mod installed to join this server."));
				}
			}), 5, TimeUnit.SECONDS);
		});

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
				((ServerPlayerEntity) player).sendMessage(Text.literal("You cannot sleep right now!"));
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

		//LOGGER.info("Enhanced Mobs Mod has been initialized");
		System.out.println("Initializing mod complete");
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