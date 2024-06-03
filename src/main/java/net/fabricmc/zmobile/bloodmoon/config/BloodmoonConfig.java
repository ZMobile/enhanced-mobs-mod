package net.fabricmc.zmobile.bloodmoon.config;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.*;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;


public class BloodmoonConfig {
	public static General GENERAL = new General();
	public static Appearance APPEARANCE = new Appearance();
	public static Schedule SCHEDULE = new Schedule();
	public static Spawning SPAWNING = new Spawning();

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

	public static Screen getConfigScreen(Screen parent) {
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Text.of("Bloodmoon Configuration"));

		ConfigEntryBuilder entryBuilder = builder.entryBuilder();

		builder.getOrCreateCategory(Text.of("General"))
				.addEntry(entryBuilder.startBooleanToggle(Text.of("No Sleep"), GENERAL.NO_SLEEP)
						.setDefaultValue(true)
						.setSaveConsumer(newValue -> GENERAL.NO_SLEEP = newValue)
						.build())
				.addEntry(entryBuilder.startBooleanToggle(Text.of("Vanish"), GENERAL.VANISH)
						.setDefaultValue(false)
						.setSaveConsumer(newValue -> GENERAL.VANISH = newValue)
						.build())
				.addEntry(entryBuilder.startBooleanToggle(Text.of("Respect Gamerule"), GENERAL.RESPECT_GAMERULE)
						.setDefaultValue(true)
						.setSaveConsumer(newValue -> GENERAL.RESPECT_GAMERULE = newValue)
						.build())
				.addEntry(entryBuilder.startBooleanToggle(Text.of("Send Message"), GENERAL.SEND_MESSAGE)
						.setDefaultValue(true)
						.setSaveConsumer(newValue -> GENERAL.SEND_MESSAGE = newValue)
						.build());

		builder.getOrCreateCategory(Text.of("Appearance"))
				.addEntry(entryBuilder.startBooleanToggle(Text.of("Red Moon"), APPEARANCE.RED_MOON)
						.setDefaultValue(true)
						.setSaveConsumer(newValue -> APPEARANCE.RED_MOON = newValue)
						.build())
				.addEntry(entryBuilder.startBooleanToggle(Text.of("Red Sky"), APPEARANCE.RED_SKY)
						.setDefaultValue(true)
						.setSaveConsumer(newValue -> APPEARANCE.RED_SKY = newValue)
						.build())
				.addEntry(entryBuilder.startBooleanToggle(Text.of("Red Light"), APPEARANCE.RED_LIGHT)
						.setDefaultValue(true)
						.setSaveConsumer(newValue -> APPEARANCE.RED_LIGHT = newValue)
						.build())
				.addEntry(entryBuilder.startBooleanToggle(Text.of("Black Fog"), APPEARANCE.BLACK_FOG)
						.setDefaultValue(true)
						.setSaveConsumer(newValue -> APPEARANCE.BLACK_FOG = newValue)
						.build());

		builder.getOrCreateCategory(Text.of("Schedule"))
				.addEntry(entryBuilder.startDoubleField(Text.of("Chance"), SCHEDULE.CHANCE)
						.setDefaultValue(0.05)
						.setSaveConsumer(newValue -> SCHEDULE.CHANCE = newValue)
						.build())
				.addEntry(entryBuilder.startBooleanToggle(Text.of("Full Moon"), SCHEDULE.FULLMOON)
						.setDefaultValue(false)
						.setSaveConsumer(newValue -> SCHEDULE.FULLMOON = newValue)
						.build())
				.addEntry(entryBuilder.startIntField(Text.of("Nth Night"), SCHEDULE.NTH_NIGHT)
						.setDefaultValue(0)
						.setSaveConsumer(newValue -> SCHEDULE.NTH_NIGHT = newValue)
						.build());

		builder.getOrCreateCategory(Text.of("Spawning"))
				.addEntry(entryBuilder.startIntField(Text.of("Spawn Speed"), SPAWNING.SPAWN_SPEED)
						.setDefaultValue(4)
						.setSaveConsumer(newValue -> SPAWNING.SPAWN_SPEED = newValue)
						.build())
				.addEntry(entryBuilder.startIntField(Text.of("Spawn Limit Multiplier"), SPAWNING.SPAWN_LIMIT_MULT)
						.setDefaultValue(4)
						.setSaveConsumer(newValue -> SPAWNING.SPAWN_LIMIT_MULT = newValue)
						.build())
				.addEntry(entryBuilder.startIntField(Text.of("Spawn Range"), SPAWNING.SPAWN_RANGE)
						.setDefaultValue(2)
						.setSaveConsumer(newValue -> SPAWNING.SPAWN_RANGE = newValue)
						.build())
				.addEntry(entryBuilder.startIntField(Text.of("World Spawn Distance"), SPAWNING.SPAWN_DISTANCE)
						.setDefaultValue(24)
						.setSaveConsumer(newValue -> SPAWNING.SPAWN_DISTANCE = newValue)
						.build())
				.addEntry(entryBuilder.startStrList(Text.of("Spawn Whitelist"), SPAWNING.SPAWN_WHITELIST)
						.setDefaultValue(new ArrayList<>())
						.setSaveConsumer(newValue -> SPAWNING.SPAWN_WHITELIST = newValue)
						.build())
				.addEntry(entryBuilder.startStrList(Text.of("Spawn Blacklist"), SPAWNING.SPAWN_BLACKLIST)
						.setDefaultValue(new ArrayList<>())
						.setSaveConsumer(newValue -> SPAWNING.SPAWN_BLACKLIST = newValue)
						.build());

		builder.setSavingRunnable(() -> {
			// Save the config file
		});

		return builder.build();
	}
}
