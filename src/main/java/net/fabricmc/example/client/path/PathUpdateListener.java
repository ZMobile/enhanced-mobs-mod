package net.fabricmc.example.client.path;

import baritone.api.BaritoneAPI;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.event.events.*;
import baritone.api.event.listener.IGameEventListener;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.calc.IPathFinder;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.utils.BetterBlockPos;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.example.client.payload.BaritoneCustomPayload;
import net.fabricmc.example.client.payload.ClientPayloadData;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PathUpdateListener implements IGameEventListener {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(BetterBlockPos.class, new BetterBlockPosSerializer())
            .create();
    private static final Identifier BARITONE_PACKET_ID = Identifier.of("modid", "baritone_packet");

    private final int mobId;
    private final IPathingBehavior behavior;

    public PathUpdateListener(int mobId, IPathingBehavior behavior) {
        this.mobId = mobId;
        this.behavior = behavior;
    }

    @Override
    public void onTick(TickEvent tickEvent) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return;
        }
        IPathExecutor current = behavior.getCurrent();
        IPathExecutor next = behavior.getNext();
        if (behavior.getInProgress().isPresent()) {
            IPathFinder pathFinder = behavior.getInProgress().get();
            Optional<IPath> bestSoFar = pathFinder.bestPathSoFar();
            if (bestSoFar.isPresent() && bestSoFar.get().positions() != null) {
                List<BetterBlockPos> pathPositions = bestSoFar.get().positions();
                PathingData pathingData = new PathingData(mobId, "bestSoFar", pathPositions);
                ClientPayloadData payloadData = new ClientPayloadData("path", pathingData);

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
            Optional<IPath> pathToMostRecentNodeConsidered = pathFinder.pathToMostRecentNodeConsidered();
            if (pathToMostRecentNodeConsidered.isPresent() && pathToMostRecentNodeConsidered.get().positions() != null) {
                List<BetterBlockPos> pathPositions = pathToMostRecentNodeConsidered.get().positions();
                PathingData pathingData = new PathingData(mobId, "mostRecentConsidered", pathPositions);
                ClientPayloadData payloadData = new ClientPayloadData("path", pathingData);

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

        if (current != null && current.getPath() != null) {
            List<BetterBlockPos> pathPositions = current.getPath().positions();
            PathingData pathingData = new PathingData(mobId, "current", pathPositions);
            ClientPayloadData payloadData = new ClientPayloadData("path", pathingData);

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

        if (next != null && next.getPath() != null) {
            List<BetterBlockPos> pathPositions = next.getPath().positions();
            PathingData pathingData = new PathingData(mobId, "next", pathPositions);
            ClientPayloadData payloadData = new ClientPayloadData("path", pathingData);

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

    @Override
    public void onPostTick(TickEvent tickEvent) {

    }

    @Override
    public void onPlayerUpdate(PlayerUpdateEvent playerUpdateEvent) {

    }

    @Override
    public void onSendChatMessage(ChatEvent chatEvent) {

    }

    @Override
    public void onPreTabComplete(TabCompleteEvent tabCompleteEvent) {

    }

    @Override
    public void onChunkEvent(ChunkEvent chunkEvent) {

    }

    @Override
    public void onBlockChange(BlockChangeEvent blockChangeEvent) {

    }

    @Override
    public void onRenderPass(RenderEvent renderEvent) {

    }

    @Override
    public void onWorldEvent(WorldEvent worldEvent) {

    }

    @Override
    public void onSendPacket(PacketEvent packetEvent) {

    }

    @Override
    public void onReceivePacket(PacketEvent packetEvent) {

    }

    @Override
    public void onPlayerRotationMove(RotationMoveEvent rotationMoveEvent) {

    }

    @Override
    public void onPlayerSprintState(SprintStateEvent sprintStateEvent) {

    }

    @Override
    public void onBlockInteract(BlockInteractEvent blockInteractEvent) {

    }

    @Override
    public void onPlayerDeath() {

    }

    @Override
    public void onPathEvent(PathEvent pathEvent) {
        /*IPathExecutor current = behavior.getCurrent();
        IPathExecutor next = behavior.getNext();
        if (behavior.getInProgress().isPresent()) {
            IPathFinder pathFinder = behavior.getInProgress().get();
            Optional<IPath> bestSoFar = pathFinder.bestPathSoFar();
            if (bestSoFar.isPresent() && bestSoFar.get().positions() != null) {
                List<BetterBlockPos> pathPositions = bestSoFar.get().positions();
                PathingData pathingData = new PathingData(mobId, "bestSoFar", pathPositions);
                ClientPayloadData payloadData = new ClientPayloadData("path", pathingData);

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
            Optional<IPath> pathToMostRecentNodeConsidered = pathFinder.pathToMostRecentNodeConsidered();
            if (pathToMostRecentNodeConsidered.isPresent() && pathToMostRecentNodeConsidered.get().positions() != null) {
                List<BetterBlockPos> pathPositions = pathToMostRecentNodeConsidered.get().positions();
                PathingData pathingData = new PathingData(mobId, "mostRecentConsidered", pathPositions);
                ClientPayloadData payloadData = new ClientPayloadData("path", pathingData);

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

        if (current != null && current.getPath() != null) {
            List<BetterBlockPos> pathPositions = current.getPath().positions();
            PathingData pathingData = new PathingData(mobId, "current", pathPositions);
            ClientPayloadData payloadData = new ClientPayloadData("path", pathingData);

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

        if (next != null && next.getPath() != null) {
            List<BetterBlockPos> pathPositions = next.getPath().positions();
            PathingData pathingData = new PathingData(mobId, "next", pathPositions);
            ClientPayloadData payloadData = new ClientPayloadData("path", pathingData);

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
        }*/
    }
}
