package net.fabricmc.example.command.mob.penalty;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.example.config.ConfigManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class MobBlockPlacementPenaltyCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("mobBlockPlacementPenalty")
                .requires(source -> source.hasPermissionLevel(2)) // Requires OP level 2 (default OP level)
                .then(argument("value", DoubleArgumentType.doubleArg())
                        .executes(context -> {
                            double value = DoubleArgumentType.getDouble(context, "value");
                            ConfigManager.getConfig().setMobBlockPlacementPenalty(value);
                            context.getSource().sendFeedback(() -> Text.of("Setting mobBlockPlacementPenalty to: " + value), true);
                            // Add custom logic here
                            return 1;
                        }))
                .executes(context -> {
                    context.getSource().sendFeedback(() -> Text.of("Usage: /mobBlockPlacementPenalty <value>"), true);
                    return 0;
                }));
    }
}
