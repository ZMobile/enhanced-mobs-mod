package net.fabricmc.example.command.bloodmoon;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.example.bloodmoon.config.BloodmoonConfig;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BloodmoonChancePercentageCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("bloodmoonChancePercentage")
                .requires(source -> source.hasPermissionLevel(2)) // Requires OP level 2 (default OP level)
                .then(argument("value", DoubleArgumentType.doubleArg())
                        .executes(context -> {
                            double value = DoubleArgumentType.getDouble(context, "value");
                            BloodmoonConfig.SCHEDULE.CHANCE = value;
                            BloodmoonConfig.saveConfig();
                            context.getSource().sendFeedback(() -> Text.of("Bloodmoon chance percentage set to " + value), true);
                            return 1;
                        }))
                .executes(context -> {
                    context.getSource().sendFeedback(() -> Text.of("Usage: /bloodmoonChancePercentage <value>"), true);
                    return 0;
                }));
    }
}
