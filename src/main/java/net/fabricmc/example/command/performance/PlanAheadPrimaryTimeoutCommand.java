package net.fabricmc.example.command.performance;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PlanAheadPrimaryTimeoutCommand {
    // Register the command with double input
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("planAheadPrimaryTimeout")
                .requires(source -> source.hasPermissionLevel(2)) // Requires OP level 2 (default OP level)
                .then(argument("value", LongArgumentType.longArg())
                        .executes(context -> {
                            long value = LongArgumentType.getLong(context, "value");
                            BaritoneAPI.getSettings().planAheadPrimaryTimeoutMS.value = value;
                            context.getSource().sendFeedback(() -> Text.of("Setting planAheadPrimaryTimeout to: " + value + "milliseconds"), true);
                            // Add custom logic here
                            return 1;
                        }))
                .executes(context -> {
                    context.getSource().sendFeedback(() -> Text.of("Usage: /planAheadPrimaryTimeout <value>"), true);
                    return 0;
                }));
    }
}