package net.fabricmc.example.command.mob.debug;

import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.EnvType;
import net.fabricmc.example.client.ClientPathManager;
import net.fabricmc.example.client.payload.BaritoneCustomPayload;
import net.fabricmc.example.client.payload.ClientPayloadData;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class TogglePathRenderingCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("togglePathRendering")
                .requires(source -> source.hasPermissionLevel(2)) // Requires OP level 2
                .executes(context -> {
                    boolean isSinglePlayer = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;

                    if (isSinglePlayer) {
                        // In single-player, toggle directly
                        boolean newState = !ClientPathManager.isRenderingEnabled();
                        ClientPathManager.setRenderingEnabled(newState);
                        context.getSource().sendFeedback(() -> Text.of("Path rendering " + (newState ? "enabled" : "disabled")), true);
                    } else {
                        // In multiplayer, send payload to all clients
                        ClientPayloadData payloadData = new ClientPayloadData("togglePathRendering", null);
                        Gson gson = new Gson();
                        String json = gson.toJson(payloadData);
                        BaritoneCustomPayload customPayload = new BaritoneCustomPayload(json);

                        PacketByteBuf buf = PacketByteBufs.create();
                        customPayload.write(buf);

                        MinecraftServer server = context.getSource().getServer();
                        if (server != null) {
                            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                                ServerPlayNetworking.send(player, customPayload);
                            }
                        }

                        context.getSource().sendFeedback(() -> Text.of("Path rendering toggled for all clients"), true);
                    }
                    return 1;
                }));
    }
}
