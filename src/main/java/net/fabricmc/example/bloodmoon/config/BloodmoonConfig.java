package net.fabricmc.example.bloodmoon.config;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import java.io.File;
import java.util.*;



public class BloodmoonConfig {
	public static General GENERAL = new General();
	public static Appearance APPEARANCE = new Appearance();
	public static Schedule SCHEDULE = new Schedule();
	public static Spawning SPAWNING = new Spawning();

	private static Config config;
	private static final File configFile = new File("config/bloodmoon.conf");

	static {
		loadConfig();
	}

	public static class General {
		public boolean NO_SLEEP = true;
		public boolean VANISH = false;
		public boolean RESPECT_GAMERULE = true;
		public boolean SEND_MESSAGE = true;
	}

	public static class Appearance {
		public boolean RED_MOON = true;
		public boolean RED_SKY = true;
		public boolean RED_LIGHT = true;
		public boolean BLACK_FOG = true;
	}

	public static class Schedule {
		public double CHANCE = 0.05;
		public boolean FULLMOON = false;
		public int NTH_NIGHT = 0;
	}

	public static class Spawning {
		public int SPAWN_SPEED = 4;
		public int SPAWN_LIMIT_MULT = 4;
		public int SPAWN_RANGE = 2;
		public int SPAWN_DISTANCE = 24;
		public List<String> SPAWN_WHITELIST = new ArrayList<>();
		public List<String> SPAWN_BLACKLIST = new ArrayList<>();
	}

	// Cache
	static Map<String, String> classToEntityNameMap = new HashMap<>();

	public static boolean canSpawn(Class<?> entityClass) {
		if (SPAWNING.SPAWN_WHITELIST.isEmpty()) {
			if (SPAWNING.SPAWN_BLACKLIST.isEmpty()) {
				return true;
			} else {
				String entityName = getEntityName(entityClass);
				for (String name : SPAWNING.SPAWN_BLACKLIST) {
					if (name.equals(entityName)) {
						return false;
					}
				}
				return true;
			}
		} else {
			String entityName = getEntityName(entityClass);
			for (String name : SPAWNING.SPAWN_WHITELIST) {
				if (name.equals(entityName)) {
					return true;
				}
			}
			return false;
		}
	}

	public static String getEntityName(Class<?> entityClass) {
		// Implementation for fetching the entity name
		return entityClass.getSimpleName();
	}

	private static void loadConfig() {
		if (configFile.exists()) {
			config = ConfigFactory.parseFile(configFile);
		} else {
			config = ConfigFactory.empty();
			saveConfig();
		}

		GENERAL.NO_SLEEP = config.hasPath("general.no_sleep") ? config.getBoolean("general.no_sleep") : GENERAL.NO_SLEEP;
		GENERAL.VANISH = config.hasPath("general.vanish") ? config.getBoolean("general.vanish") : GENERAL.VANISH;
		GENERAL.RESPECT_GAMERULE = config.hasPath("general.respect_gamerule") ? config.getBoolean("general.respect_gamerule") : GENERAL.RESPECT_GAMERULE;
		GENERAL.SEND_MESSAGE = config.hasPath("general.send_message") ? config.getBoolean("general.send_message") : GENERAL.SEND_MESSAGE;

		APPEARANCE.RED_MOON = config.hasPath("appearance.red_moon") ? config.getBoolean("appearance.red_moon") : APPEARANCE.RED_MOON;
		APPEARANCE.RED_SKY = config.hasPath("appearance.red_sky") ? config.getBoolean("appearance.red_sky") : APPEARANCE.RED_SKY;
		APPEARANCE.RED_LIGHT = config.hasPath("appearance.red_light") ? config.getBoolean("appearance.red_light") : APPEARANCE.RED_LIGHT;
		APPEARANCE.BLACK_FOG = config.hasPath("appearance.black_fog") ? config.getBoolean("appearance.black_fog") : APPEARANCE.BLACK_FOG;

		SCHEDULE.CHANCE = config.hasPath("schedule.chance") ? config.getDouble("schedule.chance") : SCHEDULE.CHANCE;
		SCHEDULE.FULLMOON = config.hasPath("schedule.fullmoon") ? config.getBoolean("schedule.fullmoon") : SCHEDULE.FULLMOON;
		SCHEDULE.NTH_NIGHT = config.hasPath("schedule.nth_night") ? config.getInt("schedule.nth_night") : SCHEDULE.NTH_NIGHT;

		SPAWNING.SPAWN_SPEED = config.hasPath("spawning.spawn_speed") ? config.getInt("spawning.spawn_speed") : SPAWNING.SPAWN_SPEED;
		SPAWNING.SPAWN_LIMIT_MULT = config.hasPath("spawning.spawn_limit_mult") ? config.getInt("spawning.spawn_limit_mult") : SPAWNING.SPAWN_LIMIT_MULT;
		SPAWNING.SPAWN_RANGE = config.hasPath("spawning.spawn_range") ? config.getInt("spawning.spawn_range") : SPAWNING.SPAWN_RANGE;
		SPAWNING.SPAWN_DISTANCE = config.hasPath("spawning.spawn_distance") ? config.getInt("spawning.spawn_distance") : SPAWNING.SPAWN_DISTANCE;
		SPAWNING.SPAWN_WHITELIST = config.hasPath("spawning.spawn_whitelist") ? config.getStringList("spawning.spawn_whitelist") : SPAWNING.SPAWN_WHITELIST;
		SPAWNING.SPAWN_BLACKLIST = config.hasPath("spawning.spawn_blacklist") ? config.getStringList("spawning.spawn_blacklist") : SPAWNING.SPAWN_BLACKLIST;
	}

	public static void saveConfig() {
		config = ConfigFactory.empty()
				.withValue("general.no_sleep", ConfigValueFactory.fromAnyRef(GENERAL.NO_SLEEP))
				.withValue("general.vanish", ConfigValueFactory.fromAnyRef(GENERAL.VANISH))
				.withValue("general.respect_gamerule", ConfigValueFactory.fromAnyRef(GENERAL.RESPECT_GAMERULE))
				.withValue("general.send_message", ConfigValueFactory.fromAnyRef(GENERAL.SEND_MESSAGE))
				.withValue("appearance.red_moon", ConfigValueFactory.fromAnyRef(APPEARANCE.RED_MOON))
				.withValue("appearance.red_sky", ConfigValueFactory.fromAnyRef(APPEARANCE.RED_SKY))
				.withValue("appearance.red_light", ConfigValueFactory.fromAnyRef(APPEARANCE.RED_LIGHT))
				.withValue("appearance.black_fog", ConfigValueFactory.fromAnyRef(APPEARANCE.BLACK_FOG))
				.withValue("schedule.chance", ConfigValueFactory.fromAnyRef(SCHEDULE.CHANCE))
				.withValue("schedule.fullmoon", ConfigValueFactory.fromAnyRef(SCHEDULE.FULLMOON))
				.withValue("schedule.nth_night", ConfigValueFactory.fromAnyRef(SCHEDULE.NTH_NIGHT))
				.withValue("spawning.spawn_speed", ConfigValueFactory.fromAnyRef(SPAWNING.SPAWN_SPEED))
				.withValue("spawning.spawn_limit_mult", ConfigValueFactory.fromAnyRef(SPAWNING.SPAWN_LIMIT_MULT))
				.withValue("spawning.spawn_range", ConfigValueFactory.fromAnyRef(SPAWNING.SPAWN_RANGE))
				.withValue("spawning.spawn_distance", ConfigValueFactory.fromAnyRef(SPAWNING.SPAWN_DISTANCE))
				.withValue("spawning.spawn_whitelist", ConfigValueFactory.fromAnyRef(SPAWNING.SPAWN_WHITELIST))
				.withValue("spawning.spawn_blacklist", ConfigValueFactory.fromAnyRef(SPAWNING.SPAWN_BLACKLIST));

		ConfigFactory.parseFile(configFile).root().withValue("general.no_sleep", ConfigValueFactory.fromAnyRef(GENERAL.NO_SLEEP));
		config = config.withFallback(ConfigFactory.parseFile(configFile));
		try {
			com.typesafe.config.ConfigRenderOptions options = com.typesafe.config.ConfigRenderOptions.defaults().setComments(true);
			com.typesafe.config.ConfigFactory.parseFile(configFile).withFallback(config).root().render(options);
			ConfigFactory.parseFile(configFile).root().withFallback(config).render(options);
			com.typesafe.config.ConfigFactory.parseFile(configFile).withFallback(config).root().render(options);
			ConfigFactory.parseFile(configFile).root().render(options);
			com.typesafe.config.ConfigFactory.parseFile(configFile).root().render(options);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
