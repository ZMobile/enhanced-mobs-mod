package net.fabricmc.example.command.mob;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.example.config.ConfigManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SpiderSpeedCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("spiderSpeed")
                .requires(source -> source.hasPermissionLevel(2)) // Requires OP level 2 (default OP level)
                .then(argument("state", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean state = BoolArgumentType.getBool(context, "state");
                            ConfigManager.getConfig().setSpiderSpeed(state);
                            ConfigManager.saveConfig();
                            context.getSource().sendFeedback(() -> Text.of("Setting spider speed to: " + state), true);
                            return 1;
                        }))
                .executes(context -> {
                    context.getSource().sendFeedback(() -> Text.of("Usage: /spiderSpeed <true/false>"), true);
                    return 0;
                }));
    }
}
