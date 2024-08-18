package net.fabricmc.example.command.mob.penalty;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.example.config.ConfigManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class MobJumpPenaltyCommand {
    // Register the command with double input
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("mobJumpPenalty")
                .requires(source -> source.hasPermissionLevel(2)) // Requires OP level 2 (default OP level)
                .then(argument("value", DoubleArgumentType.doubleArg())
                        .executes(context -> {
                            double value = DoubleArgumentType.getDouble(context, "value");
                            BaritoneAPI.getSettings().jumpPenalty.value = value;
                            ConfigManager.getConfig().setMobJumpPenalty(value);
                            ConfigManager.saveConfig();
                            context.getSource().sendFeedback(() -> Text.of("Setting mobJumpPenalty to: " + value), true);
                            // Add custom logic here
                            return 1;
                        }))
                .executes(context -> {
                    context.getSource().sendFeedback(() -> Text.of("Usage: /mobJumpPenalty <value>"), true);
                    return 0;
                }));
    }
}
