package net.fabricmc.example.bloodmoon.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class CommandBloodmoon {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("bloodmoon")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("force")
                        .executes(CommandBloodmoon::forceBloodmoon))
                .then(CommandManager.literal("stop")
                        .executes(CommandBloodmoon::stopBloodmoon))
                .executes(context -> {
                    context.getSource().sendError(Text.literal("Usage: /bloodmoon <force|stop|entitynames>"));
                    return 0;
                })
        );
    }

    private static int forceBloodmoon(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        BloodmoonHandler handler = BloodmoonHandler.INSTANCE;
        if (handler != null) {
            handler.force();
            source.sendFeedback(() -> Text.literal("Bloodmoon forced."), true);
        } else {
            source.sendError(Text.literal("Bloodmoon handler not initialized."));
        }
        return 1;
    }

    private static int stopBloodmoon(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        BloodmoonHandler handler = BloodmoonHandler.INSTANCE;
        if (handler != null) {
            handler.stop();
            source.sendFeedback(() -> Text.literal("Bloodmoon stopped."), true);
        } else {
            source.sendError(Text.literal("Bloodmoon handler not initialized."));
        }
        return 1;
    }
}
    /*private static int listEntityNames(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(literal("This command can only be used by a player."));
            return 0;
        }

        Set<String> names = new HashSet<>();
        Class<Monster> monsterClass = Monster.class;

        List<Entity> monstersNearby = player.getWorld().getEntitiesByClass(Monster.class, player.getBoundingBox().expand(10, 10, 10), entity -> entity instanceof Monster);

        for (Entity entity : monsterNearby) {
            names.add(BloodmoonConfig.getEntityName(entity.getClass()));
        }

        source.sendFeedback(() -> literal("Nearby entities:"), false);
        for (String name : names) {
            source.sendFeedback(literal(" - " + name), false);
        }
        return 1;
    }*/