package net.fabricmc.example.bloodmoon.server;

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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class BloodmoonHandler extends PersistentState {
	public static ServerWorld world;
	public static BloodmoonHandler INSTANCE;

	private final BloodmoonSpawner bloodMoonSpawner;

	boolean bloodMoon;
	boolean forceBloodMoon;

	int nightCounter;

	public BloodmoonHandler() {
		super();
		bloodMoonSpawner = new BloodmoonSpawner();
		bloodMoon = false;
		forceBloodMoon = false;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt.putBoolean("bloodMoon", bloodMoon);
		nbt.putBoolean("forceBloodMoon", forceBloodMoon);
		nbt.putInt("nightCounter", nightCounter);
		return nbt;
	}

	public static void initialize(ServerWorld serverWorld) {
		PersistentStateManager persistentStateManager = serverWorld.getPersistentStateManager();
		INSTANCE = persistentStateManager.getOrCreate(
				BloodmoonHandler::readNbt, // This is the read function that reads from NBT and returns a BloodmoonHandler
				BloodmoonHandler::new, // This is the supplier that provides a new instance if one doesn't already exist
				"bloodmoon" // This is the id under which the state is stored
		);
		world = serverWorld;

		ServerTickEvents.END_WORLD_TICK.register(BloodmoonHandler::endWorldTick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			BloodmoonHandler.INSTANCE.playerJoinedWorld(handler.getPlayer());
		});
	}

	public static BloodmoonHandler getInstance() {
		if (INSTANCE == null) {
			return INSTANCE;
		}
		return INSTANCE;
	}

	public void playerJoinedWorld(ServerPlayerEntity player) {
		if (bloodMoon) {
			PacketHandler.sendTo(player, new MessageBloodmoonStatus(bloodMoon));
		}
	}

	public static void endWorldTick(ServerWorld world) {
		if (INSTANCE != null && world.getRegistryKey() == World.OVERWORLD) {
			if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
				// Client-specific code should not be here
				return;
			}

			int time = (int) (world.getTimeOfDay() % 24000);
			if (INSTANCE.isBloodmoonActive()) {
				if (!BloodmoonConfig.GENERAL.RESPECT_GAMERULE || world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)) {
					for (int i = 0; i < BloodmoonConfig.SPAWNING.SPAWN_SPEED; i++) {
						INSTANCE.bloodMoonSpawner.spawn(world, world.getDifficulty() != Difficulty.PEACEFUL, false);
					}
				}

				if (time >= 0 && time < 12000) {
					INSTANCE.setBloodmoon(false);
				}
			} else {
				if (time == 12000) {
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

	public static BloodmoonHandler readNbt(NbtCompound nbt) {
		BloodmoonHandler handler = new BloodmoonHandler();
		handler.bloodMoon = nbt.getBoolean("bloodMoon");
		handler.forceBloodMoon = nbt.getBoolean("forceBloodMoon");
		handler.nightCounter = nbt.getInt("nightCounter");
		return handler;
	}

	public boolean isBloodmoonScheduled() {
		return forceBloodMoon;
	}

	public void stop() {
		setBloodmoon(false);
	}
}