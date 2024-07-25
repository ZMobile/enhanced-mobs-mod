package net.fabricmc.example.client.block;

import baritone.api.BaritoneAPI;
import baritone.api.utils.BetterBlockPos;
import com.google.gson.Gson;
import net.fabricmc.example.client.path.ClientBlockData;
import net.fabricmc.example.client.path.PathingData;
import net.fabricmc.example.client.payload.BaritoneCustomPayload;
import net.fabricmc.example.client.payload.ClientPayloadData;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class ClientRenderedBlockUpdateServiceImpl {
    public static void renderPlacingBlock(int mobId, BlockPos blockPos) {
        Gson gson = new Gson();
        ClientBlockData blockData = new ClientBlockData(mobId, blockPos);
        ClientPayloadData payloadData = new ClientPayloadData("placing_block", blockData);

        String json = gson.toJson(payloadData);
        BaritoneCustomPayload customPayload = new BaritoneCustomPayload(json);

        // Encode the custom payload into a PacketByteBuf
        PacketByteBuf buf = PacketByteBufs.create();
        customPayload.write(buf);

        // Send the packet to all online players
        MinecraftServer server = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().world().getServer();
        if (server != null) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player, customPayload);
            }
        }
    }

    public static void renderTargetBlock(int mobId, BlockPos blockPos) {
        BlockPos newBlockPos = new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        Gson gson = new Gson();
        ClientBlockData blockData = new ClientBlockData(mobId, newBlockPos);
        ClientPayloadData payloadData = new ClientPayloadData("target_block", blockData);

        String json = gson.toJson(payloadData);
        BaritoneCustomPayload customPayload = new BaritoneCustomPayload(json);

        // Encode the custom payload into a PacketByteBuf
        PacketByteBuf buf = PacketByteBufs.create();
        customPayload.write(buf);

        // Send the packet to all online players
        MinecraftServer server = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().world().getServer();
        if (server != null) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player, customPayload);
            }
        }
    }
}
