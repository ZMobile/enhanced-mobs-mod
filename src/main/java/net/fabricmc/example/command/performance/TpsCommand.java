package net.fabricmc.example.command.performance;

import baritone.api.BaritoneAPI;
import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.example.client.payload.BaritoneCustomPayload;
import net.fabricmc.example.client.payload.ClientPayloadData;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class TpsCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("tps")
                .requires(source -> source.hasPermissionLevel(2)) // Requires OP level 2 (default OP level)
                .executes(context -> {
                    // Send the packet to all online players
                    MinecraftServer server = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().world().getServer();
                    if (server != null) {
                        context.getSource().sendFeedback(() -> Text.of("Tps: " + getTps(server)), true);
                    }
                    return 1;
                }));
    }

    private static double getTps(MinecraftServer server) {
        double averageTickTime = server.getAverageTickTime();
        return Math.min(1000.0 / averageTickTime, 20.0);
    }
}
