package net.fabricmc.example.command.mob.debug;

import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.example.mobai.BreakPlaceAndChaseGoal;
import net.fabricmc.example.mobai.tracker.BreakPlaceAndChaseGoalTracker;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class GoalInfoCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("goalInfo")
                .requires(source -> source.hasPermissionLevel(1)) // Requires OP level 2 (default OP level)
                .then(argument("value", StringArgumentType.string())
                        .executes(context -> {
                            String value = StringArgumentType.getString(context, "value");
                            Gson gson = new Gson();
                            BreakPlaceAndChaseGoal goal = BreakPlaceAndChaseGoalTracker.getGoal(Integer.parseInt(value));
                            String response;
                            if (goal != null) {
                                response = "Has breaking block: " + goal.hasBreakingPos() + ", Has placing block: " + goal.hasPlacingPos();
                                if (goal.hasPlacingPos()) {
                                    response += ", Distance to placing block: " + goal.getDistanceToPlacingPos();
                                }
                            } else {
                                response = "Goal not found";
                            }
                            // v System.out.println(gson.toJson(goal));
                            String finalResponse = response;
                            context.getSource().sendFeedback(() -> Text.of(finalResponse), true);
                            // Add custom logic here
                            return 1;
                        }))
                .executes(context -> {
                    context.getSource().sendFeedback(() -> Text.of("Usage: /goalInfo <value>"), true);
                    return 0;
                }));
    }
}
