package net.fabricmc.example.command;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;


import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class CustomCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess dedicated) {
        dispatcher.register(literal("something")
                .then(argument("message", StringArgumentType.string())
                        .executes(context -> {
                            GoalBlock goal = new GoalBlock(0, 60, 200);
                            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(goal);
                            String message = StringArgumentType.getString(context, "message");
                            context.getSource().sendFeedback(() -> Text.of("You typed: " + message), false);
                            // Add custom logic here
                            return 1;
                        }))
                .executes(context -> {
                    context.getSource().sendFeedback(() -> Text.of("You triggered /something"), false);
                    // Add custom logic here
                    return 1;
                }));
    }
}