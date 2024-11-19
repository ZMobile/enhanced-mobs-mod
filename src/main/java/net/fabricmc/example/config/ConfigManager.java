package net.fabricmc.example.config;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;

public class ConfigManager {
    private static final String CONFIG_FILE_NAME = "enhanced_mobs_mod_config.json";
    private static ModConfig config;

    public static void loadConfig() {
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            // Copy default config from resources
            try (InputStream in = ConfigManager.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Load the config
        try (FileReader reader = new FileReader(configFile)) {
            config = new Gson().fromJson(reader, ModConfig.class);
            if (config.isInfiniteZombieBlocks() == null) {
                config.setInfiniteZombieBlocks(true);
            }
            if (config.isOptimizedMobitone() == null) {
                config.setOptimizedMobitone(true);
            }
            if (config.getMobBlockBreakSpeed() == null) {
                config.setMobBlockBreakSpeed(1.0);
            }
            if (config.getBloodmoonSpawnPercentage() == null) {
                config.setBloodmoonSpawnPercentage(
                        0.00001);
            }
            if (config.isSkeletonsBreakBlocksDuringBloodmoon() == null) {
                config.setSkeletonsBreakBlocksDuringBloodmoon(true);
            }
            if (config.getDaysBeforeBloodmoonPossibility() == null) {
                config.setDaysBeforeBloodmoonPossibility(3);
            }
            if (config.isBuildingMiningMobsDuringBloodmoonOnly() == null) {
                config.setBuildingMiningMobsDuringBloodmoonOnly(false);

                config.setSkeletonsBreakBlocksDuringBloodmoon(true);
            }
            if (config.isCreeperHiss() == null) {
                config.setCreeperHiss(false);
            }
            if (config.isSpiderSpeed() == null) {
                config.setSpiderSpeed(true);
            }
            if (config.isSlowPath() == null) {
                config.setSlowPath(true);
            }
            if (config.getSlowPathDelay() == null) {
                config.setSlowPathDelay(4L);
            }
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            config = new ModConfig();
            saveConfig();
        }
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(getConfigFile())) {
            new Gson().toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ModConfig getConfig() {
        return config;
    }

    private static File getConfigFile() {
        return new File(FabricLoader.getInstance().getConfigDir().toFile(), CONFIG_FILE_NAME);
    }
}