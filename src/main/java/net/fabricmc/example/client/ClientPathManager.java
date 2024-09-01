package net.fabricmc.example.client;

import baritone.api.utils.BetterBlockPos;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.example.client.payload.BaritoneCustomPayload;
import net.fabricmc.example.client.payload.ClientBlockData;
import net.fabricmc.example.client.payload.ClientPathingData;
import net.fabricmc.example.client.payload.ClientPayloadData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.mojang.text2speech.Narrator.LOGGER;

public class ClientPathManager implements ClientModInitializer {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BetterBlockPos.class, new BetterBlockPosSerializer())
            .create();
    public static final Identifier MOD_PACKET_ID = Identifier.of("modid","path_update");
    private static final Map<Integer, List<BetterBlockPos>> paths = new ConcurrentHashMap<>();
    private static final Map<Integer, List<BetterBlockPos>> hiddenPaths = new ConcurrentHashMap<>();
    private static boolean isolated = false;

    @Override
    public void onInitializeClient() {
        /*CustomPayload.Type<PacketByteBuf, BaritoneCustomPayload> modPacketType = new CustomPayload.Type<>(
                BaritoneCustomPayload.ID, ModPacketCodec.CODEC
        );*/

        // Assuming you have a registry for custom payload types
        //PayloadTypeRegistry.playS2C().register(BaritoneCustomPayload.ID, BaritoneCustomPayload.CODEC);

        // Register the packet receiver
        ClientPlayNetworking.registerGlobalReceiver(BaritoneCustomPayload.ID, (payload, context) -> {
            String json = payload.getJson();
            ClientPayloadData payloadData = GSON.fromJson(json, ClientPayloadData.class);
            if (payloadData.getType().equals("path")) {
                String pathDataJson = GSON.toJson(payloadData.getData());
                ClientPathingData pathingData = GSON.fromJson(pathDataJson, ClientPathingData.class);
                updatePath(pathingData);
            } else if (payloadData.getType().equals("placing_block")) {
                String blockDataJson = GSON.toJson(payloadData.getData());
                ClientBlockData blockData = GSON.fromJson(blockDataJson, ClientBlockData.class);
                ClientPlacingBlockHighlighter.updateBlock(blockData);
            } else if (payloadData.getType().equals("target_block")) {
                String blockDataJson = GSON.toJson(payloadData.getData());
                ClientBlockData blockData = GSON.fromJson(blockDataJson, ClientBlockData.class);
                ClientTargetBlockHighlighter.updateBlock(blockData);
            } else if (payloadData.getType().equals("isolatePathCommand")) {
                String data = (String) payloadData.getData();
                ClientPathManager.isolatePath(Integer.parseInt(data));
                ClientPlacingBlockHighlighter.isolateBlock(Integer.parseInt(data));
            } else if (payloadData.getType().equals("undoIsolatedPathCommand")) {
                ClientPathManager.undoIsolatedPath();
                ClientPlacingBlockHighlighter.undoIsolatedBlock();
            } else if (payloadData.getType().equals("resetPathCommand")) {
                ClientPathManager.clearPaths();
                ClientPlacingBlockHighlighter.clearBlocks();
            }
            LOGGER.info("Received data: {}", json);
        });
        // Register the render event
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MatrixStack matrixStack = context.matrixStack();
            VertexConsumerProvider vertexConsumerProvider = context.consumers();
            renderPaths(matrixStack, vertexConsumerProvider);
        });
    }

    public static void updatePath(ClientPathingData pathingData) {
        if (isolated) {
            hiddenPaths.put(pathingData.getMobId(), pathingData.getPathPositions());
        } else {
            paths.put(pathingData.getMobId(), pathingData.getPathPositions());
        }
    }

    public static void removePath(int mobId) {
        paths.remove(mobId);
    }

    public static void isolatePath(int mobId) {
        if (isolated) {
            undoIsolatedPath();
        }
        //Remove all from paths from paths that arent mob uuid and put them in hiddenPaths
        for (Map.Entry<Integer, List<BetterBlockPos>> entry : paths.entrySet()) {
            if (entry.getKey() != mobId) {
                hiddenPaths.put(entry.getKey(), entry.getValue());
            }
        }
        paths.entrySet().removeIf(entry -> entry.getKey() != mobId);
        isolated = true;
    }

    public static void clearPaths() {
        paths.clear();
        hiddenPaths.clear();
        ClientTargetBlockHighlighter.clearBlocks();
        ClientPlacingBlockHighlighter.clearBlocks();
    }

    public static void undoIsolatedPath() {
        paths.putAll(hiddenPaths);
        hiddenPaths.clear();
        isolated = false;
    }

    private void renderPaths(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider) {
        for (Map.Entry<Integer, List<BetterBlockPos>> entry : paths.entrySet()) {
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
        MatrixStack.Entry entry = matrixStack.peek();
        // Define the color and normal vector
        float[] color = {0, 0, 1, 1}; // Blue color
        float nx = 0.0f, ny = 1.0f, nz = 0.0f; // Normal vector, typically used for lighting calculations

        vertexConsumer.vertex(modelMatrix, (float) (start.getX() - camX + 0.5), (float) (start.getY() - camY + 0.5), (float) (start.getZ() - camZ + 0.5))
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);
        vertexConsumer.vertex(modelMatrix, (float) (end.getX() - camX + 0.5), (float) (end.getY() - camY + 0.5), (float) (end.getZ() - camZ + 0.5))
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);
    }
}