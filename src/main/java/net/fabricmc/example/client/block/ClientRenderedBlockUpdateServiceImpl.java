package net.fabricmc.example.client.block;

import baritone.api.BaritoneAPI;
import baritone.api.utils.BetterBlockPos;
import com.google.gson.Gson;
import net.fabricmc.api.EnvType;
import net.fabricmc.example.client.gson.GsonHelper;
import net.fabricmc.example.client.path.ClientBlockData;
import net.fabricmc.example.client.path.PathingData;
import net.fabricmc.example.client.payload.BaritoneCustomPayload;
import net.fabricmc.example.client.payload.ClientPayloadData;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class ClientRenderedBlockUpdateServiceImpl {

    // Use a pre-configured Gson instance that handles BlockPos/BetterBlockPos properly
    private static final Gson GSON = GsonHelper.getGson();

    public static void renderBreakingBlock(int mobId, BlockPos blockPos) {
        // Always send packet - client will handle rendering
        sendPlacingBlockPacket(mobId, blockPos);
    }

    public static void renderPlacingBlock(int mobId, BlockPos blockPos) {
        // Always send packet - client will handle rendering
        sendPlacingBlockPacket(mobId, blockPos);
    }

    public static void renderTargetBlock(int mobId, BlockPos blockPos) {
        // Always send packet - client will handle rendering
        sendTargetBlockPacket(mobId, blockPos);
    }

    public static void clearTargetBlock(int mobId) {
        // Always send packet - client will handle clearing
        sendClearTargetBlockPacket(mobId);
    }

    private static void sendPlacingBlockPacket(int mobId, BlockPos blockPos) {
        ClientBlockData blockData = new ClientBlockData(mobId, blockPos);
        ClientPayloadData payloadData = new ClientPayloadData("placing_block", blockData);

        String json = GSON.toJson(payloadData);
        BaritoneCustomPayload customPayload = new BaritoneCustomPayload(json);

        PacketByteBuf buf = PacketByteBufs.create();
        customPayload.write(buf);

        MinecraftServer server = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().world().getServer();
        if (server != null) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player, customPayload);
            }
        }
    }

    private static void sendTargetBlockPacket(int mobId, BlockPos blockPos) {
        ClientBlockData blockData = new ClientBlockData(mobId, blockPos);
        ClientPayloadData payloadData = new ClientPayloadData("target_block", blockData);

        String json = GSON.toJson(payloadData);
        BaritoneCustomPayload customPayload = new BaritoneCustomPayload(json);

        PacketByteBuf buf = PacketByteBufs.create();
        customPayload.write(buf);

        MinecraftServer server = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().world().getServer();
        if (server != null) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player, customPayload);
            }
        }
    }

    private static void sendClearTargetBlockPacket(int mobId) {
        ClientBlockData blockData = new ClientBlockData(mobId, null);
        ClientPayloadData payloadData = new ClientPayloadData("clear_target_block", blockData);

        String json = GSON.toJson(payloadData);
        BaritoneCustomPayload customPayload = new BaritoneCustomPayload(json);

        PacketByteBuf buf = PacketByteBufs.create();
        customPayload.write(buf);

        MinecraftServer server = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().world().getServer();
        if (server != null) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player, customPayload);
            }
        }
    }
}
