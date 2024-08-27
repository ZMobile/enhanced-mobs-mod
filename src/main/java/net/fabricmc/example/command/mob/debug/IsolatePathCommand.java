package net.fabricmc.example.command.mob.debug;

import baritone.api.BaritoneAPI;
import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.example.client.payload.BaritoneCustomPayload;
import net.fabricmc.example.client.payload.ClientPayloadData;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class IsolatePathCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("isolatePath")
                .requires(source -> source.hasPermissionLevel(1)) // Requires OP level 2 (default OP level)
                .then(argument("value", StringArgumentType.string())
                        .executes(context -> {
                            String value = StringArgumentType.getString(context, "value");
                            ClientPayloadData payloadData = new ClientPayloadData("isolatePathCommand", value);
                            Gson gson = new Gson();
                            String json = gson.toJson(payloadData);

                            // Encode the custom payload into a PacketByteBuf
                            PacketByteBuf buf = PacketByteBufs.create();
                            buf.writeString(json);

                            // Send the packet to all online players
                            MinecraftServer server = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().world().getServer();
                            if (server != null) {
                                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                                    ServerPlayNetworking.send(player, BaritoneCustomPayload.ID, buf);
                                }
                            }
                            context.getSource().sendFeedback(Text.of("Isolating path of mob: " + value), true);
                            // Add custom logic here
                            return 1;
                        }))
                .executes(context -> {
                    context.getSource().sendFeedback(Text.of("Usage: /isolatePath <value>"), true);
                    return 0;
                }));
    }
}
