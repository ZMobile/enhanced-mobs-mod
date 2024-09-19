package net.fabricmc.example.command.logout;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.literal;

public class LogoutQueueCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("logoutQueue")
                 .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (BloodmoonHandler.INSTANCE.isBloodmoonActive()) {
                        // Add player to logout queue
                        BloodmoonHandler.logoutQueue.add(player);
                        context.getSource().sendFeedback(() -> Text.of("You will be logged out when the Bloodmoon ends."), true);
                    } else {
                        // No Bloodmoon, log out player immediately
                        player.networkHandler.disconnect(Text.of("Logged out."));
                    }
                    return 1;
                }));
    }
}
