package net.fabricmc.example.bloodmoon.server;

import baritone.api.BaritoneAPI;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.example.bloodmoon.config.BloodmoonConfig;
import net.fabricmc.example.bloodmoon.network.PacketHandler;
import net.fabricmc.example.bloodmoon.network.messages.MessageBloodmoonStatus;
import net.fabricmc.example.config.ConfigManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import net.minecraft.world.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class BloodmoonHandler extends PersistentState {
	private static final Logger LOGGER = LogManager.getLogger("bloodmoon");
	public static ServerWorld world;
	public static BloodmoonHandler INSTANCE;
	public static Set<ServerPlayerEntity> logoutQueue = new HashSet<>();
	public static Set<ServerPlayerEntity> joinedPlayers = new HashSet<>();
	private static boolean eventsRegistered = false;

	private final BloodmoonSpawner bloodMoonSpawner;
	//for my server only
	private static int daysElapsed;

	boolean bloodMoon;
	boolean forceBloodMoon;

	int nightCounter;

	public BloodmoonHandler() {
		super();
		bloodMoonSpawner = new BloodmoonSpawner();
		bloodMoon = false;
		forceBloodMoon = false;
		daysElapsed = 0;
	}

	public static final PersistentStateType<BloodmoonHandler> BLOODMOON_HANDLER_TYPE = new PersistentStateType<>(
			"bloodmoon_handler",
			ctx -> new BloodmoonHandler(),
			ctx -> BloodmoonHandler.CODEC,
			DataFixTypes.WORLD_GEN_SETTINGS
	);

	public static final Codec<BloodmoonHandler> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("bloodMoon").forGetter(h -> h.bloodMoon),
			Codec.BOOL.fieldOf("forceBloodMoon").forGetter(h -> h.forceBloodMoon),
			Codec.INT.fieldOf("daysElapsed").forGetter(h -> h.daysElapsed)
	).apply(instance, (bloodMoon, forceBloodMoon, daysElapsed) -> {
		BloodmoonHandler handler = new BloodmoonHandler();
		handler.bloodMoon = bloodMoon;
		handler.forceBloodMoon = forceBloodMoon;
		handler.daysElapsed = daysElapsed;
		return handler;
	}));


	public static void initialize(ServerWorld serverWorld) {
		LOGGER.info("=== BloodmoonHandler.initialize() called for dimension: {} ===", serverWorld.getRegistryKey().getValue());
		// Only initialize for Overworld to avoid duplicate registration
		if (serverWorld.getRegistryKey() != World.OVERWORLD) {
			LOGGER.info("Skipping non-Overworld dimension");
			return;
		}

		LOGGER.info("Loading persistent state...");
		PersistentStateManager persistentStateManager = serverWorld.getPersistentStateManager();
		INSTANCE = persistentStateManager.getOrCreate(BLOODMOON_HANDLER_TYPE);
		world = serverWorld;
		LOGGER.info("Persistent state loaded");

		// Only register events once to prevent duplicate handlers
		if (!eventsRegistered) {
			LOGGER.info("Registering BloodmoonHandler events (first time only)...");
			eventsRegistered = true;
			ServerTickEvents.END_WORLD_TICK.register(BloodmoonHandler::endWorldTick);
			ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
				if (BloodmoonHandler.INSTANCE != null) {
					BloodmoonHandler.INSTANCE.playerJoinedWorld(handler.getPlayer());
				}
			});
			LOGGER.info("BloodmoonHandler events registered");
		} else {
			LOGGER.info("Events already registered, skipping");
		}
		LOGGER.info("=== BloodmoonHandler.initialize() END ===");
	}

	public static BloodmoonHandler getInstance() {
		if (INSTANCE == null) {
			return INSTANCE;
		}
		return INSTANCE;
	}

	public void playerJoinedWorld(ServerPlayerEntity player) {
		if (!joinedPlayers.contains(player)) {
			daysElapsed = 0;
			joinedPlayers.add(player);
		}
		if (bloodMoon) {
			PacketHandler.sendTo(player, new MessageBloodmoonStatus(bloodMoon));
		}
	}

	public static void endWorldTick(ServerWorld world) {
		if (INSTANCE != null && world.getRegistryKey() == World.OVERWORLD) {
			/*if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
				// Client-specific code should not be here
				return;
			}*/

			int time = (int) (world.getTimeOfDay() % 24000);
			if (INSTANCE.isBloodmoonActive()) {
				if (!BloodmoonConfig.GENERAL.RESPECT_GAMERULE || world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)) {
					for (int i = 0; i < BloodmoonConfig.SPAWNING.SPAWN_SPEED; i++) {
						INSTANCE.bloodMoonSpawner.triggerBloodmoonSpawning(world, world.getDifficulty() != Difficulty.PEACEFUL, false);
					}
				}

				if (time >= 0 && time < 12000) {
					//BaritoneAPI.getSettings().slowPath.value = false;
					INSTANCE.setBloodmoon(false);
				}
			} else {
				//System.out.println("Bloodmoon checker called");
				if (time == 12000) {
					//System.out.println("Bloodmoon checker called");
					daysElapsed++;
					if (BloodmoonConfig.SCHEDULE.NTH_NIGHT != 0) {
						INSTANCE.nightCounter--;

						if (INSTANCE.nightCounter < 0) {
							INSTANCE.nightCounter = BloodmoonConfig.SCHEDULE.NTH_NIGHT;
						}

						INSTANCE.markDirty();
					}

					if (INSTANCE.forceBloodMoon || Math.random() < BloodmoonConfig.SCHEDULE.CHANCE || (BloodmoonConfig.SCHEDULE.FULLMOON && world.getMoonPhase() == 0) || (BloodmoonConfig.SCHEDULE.NTH_NIGHT != 0 && INSTANCE.nightCounter == 0)) {
						INSTANCE.forceBloodMoon = false;
						if (ConfigManager.getConfig().isBloodmoonEnabled() && getElapsedDays(world) >= ConfigManager.getConfig().getDaysBeforeBloodmoonPossibility()){
							INSTANCE.setBloodmoon(true);

							if (BloodmoonConfig.GENERAL.SEND_MESSAGE) {
								world.getPlayers().forEach(player -> {
									player.sendMessage(Text.literal("Bloodmoon is rising...").formatted(Formatting.RED), false);
								});
							}
						}

						if (INSTANCE.nightCounter == 0 && BloodmoonConfig.SCHEDULE.NTH_NIGHT != 0) {
							INSTANCE.nightCounter = BloodmoonConfig.SCHEDULE.NTH_NIGHT;
							INSTANCE.markDirty();
						}
					}
				}
			}
		}
	}

	public static int getElapsedDays(ServerWorld world) {
		return (int) (world.getTime() / 24000);
	}

	public void setBloodmoon(boolean bloodMoon) {
		if (this.bloodMoon != bloodMoon) {
			PacketHandler.sendToAll(world, new MessageBloodmoonStatus(bloodMoon));
			this.markDirty();
		}
		this.bloodMoon = bloodMoon;
	}

	public void updateClients() {
		PacketHandler.sendToAll(world, new MessageBloodmoonStatus(bloodMoon));
	}

	public void force() {
		forceBloodMoon = true;
		this.markDirty();
	}

	public boolean isBloodmoonActive() {
		return bloodMoon;
	}

	/*public static BloodmoonHandler readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		BloodmoonHandler handler = new BloodmoonHandler();
		Optional<Boolean> bloodMoonOpt = nbt.getBoolean("bloodMoon");
		handler.bloodMoon = bloodMoonOpt.orElse(false);
		Optional<Boolean> forceBloodMoonOpt = nbt.getBoolean("forceBloodMoon");
		handler.forceBloodMoon = forceBloodMoonOpt.orElse(false);
		Optional<Integer> daysElapsedOpt = nbt.getInt("daysElapsed");
		handler.nightCounter = daysElapsedOpt.orElse(0);
		return handler;
	}

	/*@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		nbt.putBoolean("bloodMoon", bloodMoon);
		nbt.putBoolean("forceBloodMoon", forceBloodMoon);
		nbt.putInt("nightCounter", nightCounter);
		return nbt;
	}

	public boolean isBloodmoonScheduled() {
		return forceBloodMoon;
	}*/

	public void stop() {
		//BaritoneAPI.getSettings().slowPath.value = false;
		setBloodmoon(false);
	}
}