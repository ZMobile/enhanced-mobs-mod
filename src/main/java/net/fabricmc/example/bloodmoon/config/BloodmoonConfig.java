package net.fabricmc.example.bloodmoon.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;




import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class BloodmoonConfig {
	public static General GENERAL = new General();
	public static Appearance APPEARANCE = new Appearance();
	public static Schedule SCHEDULE = new Schedule();
	public static Spawning SPAWNING = new Spawning();

	private static final String CONFIG_FILE_PATH = "config/bloodmoon.properties";

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
		return entityClass.getSimpleName();
	}

	private static void loadConfig() {
		Properties properties = new Properties();
		try {
			File configFile = new File(CONFIG_FILE_PATH);
			if (!configFile.exists()) {
				saveDefaultConfig();
			}

			try (FileInputStream fis = new FileInputStream(CONFIG_FILE_PATH)) {
				properties.load(fis);
			}

			GENERAL.NO_SLEEP = Boolean.parseBoolean(properties.getProperty("general.no_sleep", "true"));
			GENERAL.VANISH = Boolean.parseBoolean(properties.getProperty("general.vanish", "false"));
			GENERAL.RESPECT_GAMERULE = Boolean.parseBoolean(properties.getProperty("general.respect_gamerule", "true"));
			GENERAL.SEND_MESSAGE = Boolean.parseBoolean(properties.getProperty("general.send_message", "true"));

			APPEARANCE.RED_MOON = Boolean.parseBoolean(properties.getProperty("appearance.red_moon", "true"));
			APPEARANCE.RED_SKY = Boolean.parseBoolean(properties.getProperty("appearance.red_sky", "true"));
			APPEARANCE.RED_LIGHT = Boolean.parseBoolean(properties.getProperty("appearance.red_light", "true"));
			APPEARANCE.BLACK_FOG = Boolean.parseBoolean(properties.getProperty("appearance.black_fog", "true"));

			SCHEDULE.CHANCE = Double.parseDouble(properties.getProperty("schedule.chance", "0.05"));
			SCHEDULE.FULLMOON = Boolean.parseBoolean(properties.getProperty("schedule.fullmoon", "false"));
			SCHEDULE.NTH_NIGHT = Integer.parseInt(properties.getProperty("schedule.nth_night", "0"));

			SPAWNING.SPAWN_SPEED = Integer.parseInt(properties.getProperty("spawning.spawn_speed", "4"));
			SPAWNING.SPAWN_LIMIT_MULT = Integer.parseInt(properties.getProperty("spawning.spawn_limit_mult", "4"));
			SPAWNING.SPAWN_RANGE = Integer.parseInt(properties.getProperty("spawning.spawn_range", "2"));
			SPAWNING.SPAWN_DISTANCE = Integer.parseInt(properties.getProperty("spawning.spawn_distance", "24"));
			SPAWNING.SPAWN_WHITELIST = parseList(properties.getProperty("spawning.spawn_whitelist", ""));
			SPAWNING.SPAWN_BLACKLIST = parseList(properties.getProperty("spawning.spawn_blacklist", ""));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void saveDefaultConfig() {
		Properties properties = new Properties();
		properties.setProperty("general.no_sleep", String.valueOf(GENERAL.NO_SLEEP));
		properties.setProperty("general.vanish", String.valueOf(GENERAL.VANISH));
		properties.setProperty("general.respect_gamerule", String.valueOf(GENERAL.RESPECT_GAMERULE));
		properties.setProperty("general.send_message", String.valueOf(GENERAL.SEND_MESSAGE));

		properties.setProperty("appearance.red_moon", String.valueOf(APPEARANCE.RED_MOON));
		properties.setProperty("appearance.red_sky", String.valueOf(APPEARANCE.RED_SKY));
		properties.setProperty("appearance.red_light", String.valueOf(APPEARANCE.RED_LIGHT));
		properties.setProperty("appearance.black_fog", String.valueOf(APPEARANCE.BLACK_FOG));

		properties.setProperty("schedule.chance", String.valueOf(SCHEDULE.CHANCE));
		properties.setProperty("schedule.fullmoon", String.valueOf(SCHEDULE.FULLMOON));
		properties.setProperty("schedule.nth_night", String.valueOf(SCHEDULE.NTH_NIGHT));

		properties.setProperty("spawning.spawn_speed", String.valueOf(SPAWNING.SPAWN_SPEED));
		properties.setProperty("spawning.spawn_limit_mult", String.valueOf(SPAWNING.SPAWN_LIMIT_MULT));
		properties.setProperty("spawning.spawn_range", String.valueOf(SPAWNING.SPAWN_RANGE));
		properties.setProperty("spawning.spawn_distance", String.valueOf(SPAWNING.SPAWN_DISTANCE));
		properties.setProperty("spawning.spawn_whitelist", String.join(",", SPAWNING.SPAWN_WHITELIST));
		properties.setProperty("spawning.spawn_blacklist", String.join(",", SPAWNING.SPAWN_BLACKLIST));

		try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE_PATH)) {
			properties.store(fos, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void saveConfig() {
		Properties properties = new Properties();
		properties.setProperty("general.no_sleep", String.valueOf(GENERAL.NO_SLEEP));
		properties.setProperty("general.vanish", String.valueOf(GENERAL.VANISH));
		properties.setProperty("general.respect_gamerule", String.valueOf(GENERAL.RESPECT_GAMERULE));
		properties.setProperty("general.send_message", String.valueOf(GENERAL.SEND_MESSAGE));

		properties.setProperty("appearance.red_moon", String.valueOf(APPEARANCE.RED_MOON));
		properties.setProperty("appearance.red_sky", String.valueOf(APPEARANCE.RED_SKY));
		properties.setProperty("appearance.red_light", String.valueOf(APPEARANCE.RED_LIGHT));
		properties.setProperty("appearance.black_fog", String.valueOf(APPEARANCE.BLACK_FOG));

		properties.setProperty("schedule.chance", String.valueOf(SCHEDULE.CHANCE));
		properties.setProperty("schedule.fullmoon", String.valueOf(SCHEDULE.FULLMOON));
		properties.setProperty("schedule.nth_night", String.valueOf(SCHEDULE.NTH_NIGHT));

		properties.setProperty("spawning.spawn_speed", String.valueOf(SPAWNING.SPAWN_SPEED));
		properties.setProperty("spawning.spawn_limit_mult", String.valueOf(SPAWNING.SPAWN_LIMIT_MULT));
		properties.setProperty("spawning.spawn_range", String.valueOf(SPAWNING.SPAWN_RANGE));
		properties.setProperty("spawning.spawn_distance", String.valueOf(SPAWNING.SPAWN_DISTANCE));
		properties.setProperty("spawning.spawn_whitelist", String.join(",", SPAWNING.SPAWN_WHITELIST));
		properties.setProperty("spawning.spawn_blacklist", String.join(",", SPAWNING.SPAWN_BLACKLIST));

		try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE_PATH)) {
			properties.store(fos, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static List<String> parseList(String property) {
		List<String> list = new ArrayList<>();
		if (!property.isEmpty()) {
			String[] items = property.split(",");
			for (String item : items) {
				list.add(item.trim());
			}
		}
		return list;
	}
}