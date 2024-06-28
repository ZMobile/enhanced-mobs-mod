package net.fabricmc.example.command;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.example.config.ConfigManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RenderMobPathingCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("renderMobPathing")
                .requires(source -> source.hasPermissionLevel(2)) // Requires OP level 2 (default OP level)
                .then(argument("state", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean state = BoolArgumentType.getBool(context, "state");
                            BaritoneAPI.getSettings().renderPath.value = state;
                            BaritoneAPI.getSettings().renderSelectionBoxes.value = state;
                            BaritoneAPI.getSettings().renderGoal.value = state;
                            BaritoneAPI.getSettings().renderCachedChunks.value = state;
                            BaritoneAPI.getSettings().renderSelectionCorners.value = state;
                            BaritoneAPI.getSettings().renderGoalAnimated.value = state;
                            BaritoneAPI.getSettings().renderPathAsLine.value = state;
                            BaritoneAPI.getSettings().renderGoalXZBeacon.value = state;
                            ConfigManager.getConfig().setRenderMobPathing(state);
                            ConfigManager.saveConfig();
                            context.getSource().sendFeedback(() -> Text.of("Setting render mob pathing to: " + state), true);
                            // Add custom logic here
                            return 1;
                        }))
                .executes(context -> {
                    context.getSource().sendFeedback(() -> Text.of("Usage: /renderMobPathing <true/false>"), true);
                    return 0;
                }));
    }
}
