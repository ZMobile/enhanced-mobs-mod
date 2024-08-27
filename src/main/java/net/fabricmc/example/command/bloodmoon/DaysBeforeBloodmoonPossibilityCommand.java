package net.fabricmc.example.command.bloodmoon;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.example.config.ConfigManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class DaysBeforeBloodmoonPossibilityCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("daysBeforeBloodmoonPossibility")
                .requires(source -> source.hasPermissionLevel(2)) // Requires OP level 2 (default OP level)
                .then(argument("value", IntegerArgumentType.integer())
                        .executes(context -> {
                            int value = IntegerArgumentType.getInteger(context, "value");
                            ConfigManager.getConfig().setDaysBeforeBloodmoonPossibility(value);
                            ConfigManager.saveConfig();
                            context.getSource().sendFeedback(() -> Text.of("Days before bloodmoon possibility set to " + value), true);
                            return 1;
                        }))
                .executes(context -> {
                    context.getSource().sendFeedback(() -> Text.of("Usage: /daysBeforeBloodmoonPossibility <value>"), true);
                    return 0;
                }));
    }
}
