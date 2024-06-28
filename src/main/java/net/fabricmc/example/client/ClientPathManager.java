package net.fabricmc.example.client;

import baritone.api.utils.BetterBlockPos;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientPathManager implements ClientModInitializer {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BetterBlockPos.class, new BetterBlockPosSerializer())
            .create();
    private static final Identifier BARITONE_PACKET_ID = new Identifier("modid", "baritone_packet");
    private final Map<UUID, List<BetterBlockPos>> paths = new ConcurrentHashMap<>();

    @Override
    public void onInitializeClient() {
        // Register the packet receiver
        ClientPlayNetworking.registerGlobalReceiver(BARITONE_PACKET_ID, (client, handler, buf, responseSender) -> {
            String json = buf.readString(32767);
            System.out.println("Packet received: " + json);
            ClientPathingData pathingData = GSON.fromJson(json, ClientPathingData.class);
            client.execute(() -> {
                updatePath(pathingData);
            });
        });

        // Register the render event
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MatrixStack matrixStack = context.matrixStack();
            VertexConsumerProvider vertexConsumerProvider = context.consumers();
            renderPaths(matrixStack, vertexConsumerProvider);
        });
    }

    private void updatePath(ClientPathingData pathingData) {
        paths.put(pathingData.getMobUuid(), pathingData.getPathPositions());
    }

    public void removePath(UUID mobUuid) {
        paths.remove(mobUuid);
    }

    private void renderPaths(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider) {
        for (Map.Entry<UUID, List<BetterBlockPos>> entry : paths.entrySet()) {
            List<BetterBlockPos> pathPositions = entry.getValue();
            if (pathPositions != null && !pathPositions.isEmpty()) {
                renderPath(matrixStack, vertexConsumerProvider, pathPositions);
            }
        }
    }

    private void renderPath(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, List<BetterBlockPos> pathPositions) {
        MinecraftClient client = MinecraftClient.getInstance();
        double camX = client.getEntityRenderDispatcher().camera.getPos().x;
        double camY = client.getEntityRenderDispatcher().camera.getPos().y;
        double camZ = client.getEntityRenderDispatcher().camera.getPos().z;

        for (int i = 0; i < pathPositions.size() - 1; i++) {
            BetterBlockPos start = pathPositions.get(i);
            BetterBlockPos end = pathPositions.get(i + 1);

            renderLine(matrixStack, vertexConsumerProvider, start, end, camX, camY, camZ);
        }
    }

    private void renderLine(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, BetterBlockPos start, BetterBlockPos end, double camX, double camY, double camZ) {
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getLines());

        Matrix4f modelMatrix = matrixStack.peek().getPositionMatrix();

        // Define the color and normal vector
        float[] color = {0, 0, 1, 1}; // Blue color
        float nx = 0.0f, ny = 1.0f, nz = 0.0f; // Normal vector, typically used for lighting calculations

        vertexConsumer.vertex(modelMatrix, (float) (start.getX() - camX + 0.5), (float) (start.getY() - camY + 0.5), (float) (start.getZ() - camZ + 0.5))
                .color(color[0], color[1], color[2], color[3])
                .normal(matrixStack.peek().getNormalMatrix(), nx, ny, nz)
                .next();
        vertexConsumer.vertex(modelMatrix, (float) (end.getX() - camX + 0.5), (float) (end.getY() - camY + 0.5), (float) (end.getZ() - camZ + 0.5))
                .color(color[0], color[1], color[2], color[3])
                .normal(matrixStack.peek().getNormalMatrix(), nx, ny, nz)
                .next();
    }
}