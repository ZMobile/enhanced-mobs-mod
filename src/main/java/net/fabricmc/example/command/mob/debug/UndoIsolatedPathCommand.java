package net.fabricmc.example.command.mob.debug;

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

public class UndoIsolatedPathCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("undoIsolatedPath")
                .requires(source -> source.hasPermissionLevel(2)) // Requires OP level 2 (default OP level)
                .executes(context -> {
                    // Call the method to undo isolated paths
                    ClientPayloadData payloadData = new ClientPayloadData("undoIsolatedPathCommand", null);
                    Gson gson = new Gson();
                    String json = gson.toJson(payloadData);

                    // Encode the payload into a PacketByteBuf
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeString(json); // Write the JSON to the buffer

                    // Send the packet to all online players
                    MinecraftServer server = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().world().getServer();
                    if (server != null) {
                        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                            ServerPlayNetworking.send(player, BaritoneCustomPayload.ID, buf);
                        }
                    }

                    // Send feedback to the command source
                    context.getSource().sendFeedback(() -> Text.of("All isolated paths have been undone."), true);
                    return 1;
                }));
    }
}