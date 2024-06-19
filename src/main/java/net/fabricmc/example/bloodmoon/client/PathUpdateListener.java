package net.fabricmc.example.bloodmoon.client;

import baritone.api.BaritoneAPI;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.event.events.*;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.event.listener.IGameEventListener;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.utils.BetterBlockPos;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import io.netty.buffer.Unpooled;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.UUID;

public class PathUpdateListener implements IGameEventListener {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BetterBlockPos.class, new BetterBlockPosSerializer())
            .create();
    private static final Identifier BARITONE_PACKET_ID = new Identifier("modid", "baritone_packet");

    private final UUID mobUuid;
    private final IPathingBehavior behavior;

    public PathUpdateListener(UUID mobUuid, IPathingBehavior behavior) {
        this.mobUuid = mobUuid;
        this.behavior = behavior;
    }

    @Override
    public void onTick(TickEvent tickEvent) {

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
        System.out.println("Custom on path event executed");
        IPathExecutor executor = behavior.getCurrent();
        if (executor != null && executor.getPath() != null) {
            List<BetterBlockPos> pathPositions = executor.getPath().positions();
            PathingData pathingData = new PathingData(mobUuid, pathPositions);

            String json = GSON.toJson(pathingData);

            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeString(json);

            // Send the packet to all online players
            MinecraftServer server = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().world().getServer();
            if (server != null) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    System.out.println("Sending packet to player: " + player.getName());
                    ServerPlayNetworking.send(player, BARITONE_PACKET_ID, buf);
                }
            }
        }
    }
}
