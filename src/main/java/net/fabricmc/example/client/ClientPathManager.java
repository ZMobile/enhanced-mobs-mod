package net.fabricmc.example.client;

import baritone.api.utils.BetterBlockPos;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.example.client.path.BetterBlockPosSerializer;
import net.fabricmc.example.client.path.PathingData;
import net.fabricmc.example.client.payload.BaritoneCustomPayload;
import net.fabricmc.example.client.payload.ClientPayloadData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class ClientPathManager implements ClientModInitializer {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BetterBlockPos.class, new BetterBlockPosSerializer())
            .create();

    private static final Map<Integer, List<BetterBlockPos>> currentPaths = new ConcurrentHashMap<>();
    private static final Map<Integer, List<BetterBlockPos>> nextPaths = new ConcurrentHashMap<>();
    private static final Map<Integer, List<BetterBlockPos>> bestPathsSoFar = new ConcurrentHashMap<>();
    private static final Map<Integer, List<BetterBlockPos>> mostRecentPathsConsidered = new ConcurrentHashMap<>();

    private static final Map<Integer, List<BetterBlockPos>> hiddenCurrentPaths = new ConcurrentHashMap<>();
    private static final Map<Integer, List<BetterBlockPos>> hiddenNextPaths = new ConcurrentHashMap<>();
    private static final Map<Integer, List<BetterBlockPos>> hiddenBestPathsSoFar = new ConcurrentHashMap<>();
    private static final Map<Integer, List<BetterBlockPos>> hiddenMostRecentPathsConsidered = new ConcurrentHashMap<>();

    private static boolean isolated = false;
    private static boolean renderingEnabled = true;

    public static void setRenderingEnabled(boolean enabled) {
        renderingEnabled = enabled;
    }

    public static boolean isRenderingEnabled() {
        return renderingEnabled;
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(BaritoneCustomPayload.ID, (payload, context) -> {
            String json = payload.getJson();
            ClientPayloadData payloadData = GSON.fromJson(json, ClientPayloadData.class);
            if (payloadData.getType().equals("path")) {
                String pathDataJson = GSON.toJson(payloadData.getData());
                PathingData pathingData = GSON.fromJson(pathDataJson, PathingData.class);
                updatePath(pathingData);
            } else if (payloadData.getType().equals("placing_block")) {
                String blockDataJson = GSON.toJson(payloadData.getData());
                net.fabricmc.example.client.path.ClientBlockData blockData = GSON.fromJson(blockDataJson, net.fabricmc.example.client.path.ClientBlockData.class);
                if (blockData != null && blockData.getBlockPos() != null) {
                    ClientPlacingBlockHighlighter.updateBlock(new ClientBlockData(blockData.getMobId(), blockData.getBlockPos()));
                }
            } else if (payloadData.getType().equals("target_block")) {
                String blockDataJson = GSON.toJson(payloadData.getData());
                net.fabricmc.example.client.path.ClientBlockData blockData = GSON.fromJson(blockDataJson, net.fabricmc.example.client.path.ClientBlockData.class);
                if (blockData != null && blockData.getBlockPos() != null) {
                    ClientTargetBlockHighlighter.updateBlock(new ClientBlockData(blockData.getMobId(), blockData.getBlockPos()));
                }
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
            } else if (payloadData.getType().equals("togglePathRendering")) {
                renderingEnabled = !renderingEnabled;
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MatrixStack matrixStack = context.matrixStack();
            VertexConsumerProvider vertexConsumerProvider = context.consumers();
            renderPaths(matrixStack, vertexConsumerProvider);
        });
    }

    public static void isolatePath(int mobId) {
        if (isolated) {
            undoIsolatedPath();
        }

        // Move non-matching current paths to hiddenCurrentPaths
        for (Map.Entry<Integer, List<BetterBlockPos>> entry : currentPaths.entrySet()) {
            if (entry.getKey() != mobId) {
                hiddenCurrentPaths.put(entry.getKey(), entry.getValue());
            }
        }
        currentPaths.entrySet().removeIf(entry -> entry.getKey() != mobId);

        // Move non-matching next paths to hiddenNextPaths
        for (Map.Entry<Integer, List<BetterBlockPos>> entry : nextPaths.entrySet()) {
            if (entry.getKey() != mobId) {
                hiddenNextPaths.put(entry.getKey(), entry.getValue());
            }
        }
        nextPaths.entrySet().removeIf(entry -> entry.getKey() != mobId);

        // Move non-matching best paths so far to hiddenBestPathsSoFar
        for (Map.Entry<Integer, List<BetterBlockPos>> entry : bestPathsSoFar.entrySet()) {
            if (entry.getKey() != mobId) {
                hiddenBestPathsSoFar.put(entry.getKey(), entry.getValue());
            }
        }
        bestPathsSoFar.entrySet().removeIf(entry -> entry.getKey() != mobId);

        // Move non-matching most recent paths considered to hiddenMostRecentPathsConsidered
        for (Map.Entry<Integer, List<BetterBlockPos>> entry : mostRecentPathsConsidered.entrySet()) {
            if (entry.getKey() != mobId) {
                hiddenMostRecentPathsConsidered.put(entry.getKey(), entry.getValue());
            }
        }
        mostRecentPathsConsidered.entrySet().removeIf(entry -> entry.getKey() != mobId);

        isolated = true;
    }

    public static void clearPaths() {
        currentPaths.clear();
        nextPaths.clear();
        bestPathsSoFar.clear();
        mostRecentPathsConsidered.clear();
        hiddenCurrentPaths.clear();
        hiddenNextPaths.clear();
        hiddenBestPathsSoFar.clear();
        hiddenMostRecentPathsConsidered.clear();
    }

    public static void undoIsolatedPath() {
        currentPaths.putAll(hiddenCurrentPaths);
        nextPaths.putAll(hiddenNextPaths);
        bestPathsSoFar.putAll(hiddenBestPathsSoFar);
        mostRecentPathsConsidered.putAll(hiddenMostRecentPathsConsidered);
        hiddenCurrentPaths.clear();
        hiddenNextPaths.clear();
        hiddenBestPathsSoFar.clear();
        hiddenMostRecentPathsConsidered.clear();
        isolated = false;
    }

    public static void updatePath(PathingData pathingData) {
        Map<Integer, List<BetterBlockPos>> targetMap;

        switch (pathingData.getType()) {
            case "current":
                targetMap = isolated ? hiddenCurrentPaths : currentPaths;
                break;
            case "next":
                targetMap = isolated ? hiddenNextPaths : nextPaths;
                break;
            case "bestSoFar":
                targetMap = isolated ? hiddenBestPathsSoFar : bestPathsSoFar;
                break;
            case "mostRecentConsidered":
                targetMap = isolated ? hiddenMostRecentPathsConsidered : mostRecentPathsConsidered;
                break;
            default:
                return; // Unknown path type
        }

        targetMap.put(pathingData.getMobId(), pathingData.getPathPositions());
    }

    private void renderPaths(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider) {
        if (!renderingEnabled) {
            return;
        }
        renderPathsByType(matrixStack, vertexConsumerProvider, mostRecentPathsConsidered, new float[]{1, 1, 0, 1}); // Yellow for mostRecentConsidered
        renderPathsByType(matrixStack, vertexConsumerProvider, bestPathsSoFar, new float[]{0, 1, 0, 1}); // Green for bestSoFar
        renderPathsByType(matrixStack, vertexConsumerProvider, nextPaths, new float[]{1, 0, 0, 1}); // Red for next paths
        renderPathsByType(matrixStack, vertexConsumerProvider, currentPaths, new float[]{0, 0, 1, 1}); // Blue for current paths - rendered last so it's on top
    }

    private void renderPathsByType(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, Map<Integer, List<BetterBlockPos>> paths, float[] color) {
        for (List<BetterBlockPos> pathPositions : paths.values()) {
            if (pathPositions != null && !pathPositions.isEmpty()) {
                renderPath(matrixStack, vertexConsumerProvider, pathPositions, color);
            }
        }
    }

    private void renderPath(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, List<BetterBlockPos> pathPositions, float[] color) {
        MinecraftClient client = MinecraftClient.getInstance();
        double camX = client.getEntityRenderDispatcher().camera.getPos().x;
        double camY = client.getEntityRenderDispatcher().camera.getPos().y;
        double camZ = client.getEntityRenderDispatcher().camera.getPos().z;

        for (int i = 0; i < pathPositions.size() - 1; i++) {
            BetterBlockPos start = pathPositions.get(i);
            BetterBlockPos end = pathPositions.get(i + 1);
            renderLine(matrixStack, vertexConsumerProvider, start, end, camX, camY, camZ, color);
        }
    }

    private void renderLine(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, BetterBlockPos start, BetterBlockPos end, double camX, double camY, double camZ, float[] color) {
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getLines());
        Matrix4f modelMatrix = matrixStack.peek().getPositionMatrix();

        vertexConsumer.vertex(modelMatrix, (float) (start.getX() - camX + 0.5), (float) (start.getY() - camY + 0.5), (float) (start.getZ() - camZ + 0.5))
                .color(color[0], color[1], color[2], color[3])
                .normal(0.0f, 1.0f, 0.0f);
        vertexConsumer.vertex(modelMatrix, (float) (end.getX() - camX + 0.5), (float) (end.getY() - camY + 0.5), (float) (end.getZ() - camZ + 0.5))
                .color(color[0], color[1], color[2], color[3])
                .normal(0.0f, 1.0f, 0.0f);
    }
}