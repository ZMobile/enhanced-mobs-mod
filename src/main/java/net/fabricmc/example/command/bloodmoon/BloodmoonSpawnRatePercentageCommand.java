package net.fabricmc.example.command.bloodmoon;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.example.bloodmoon.server.BloodmoonSpawner;
import net.fabricmc.example.config.ConfigManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BloodmoonSpawnRatePercentageCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("bloodmoonSpawnRatePercentage")
                .requires(source -> source.hasPermissionLevel(2)) // Requires OP level 2 (default OP level)
                .then(argument("value", DoubleArgumentType.doubleArg())
                        .executes(context -> {
                            double value = DoubleArgumentType.getDouble(context, "value");
                            ConfigManager.getConfig().setBloodmoonSpawnPercentage(value);
                            ConfigManager.saveConfig();
                            context.getSource().sendFeedback(() -> Text.of("Bloodmoon spawn rate percentage set to " + value), true);
                            return 1;
                        }))
                .executes(context -> {
                    context.getSource().sendFeedback(() -> Text.of("Usage: /bloodmoonSpawnRatePercentage <value>"), true);
                    return 0;
                }));
    }
}
