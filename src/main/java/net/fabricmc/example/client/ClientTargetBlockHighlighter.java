package net.fabricmc.example.client;

import baritone.api.utils.BetterBlockPos;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.example.client.path.BetterBlockPosSerializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class ClientTargetBlockHighlighter implements ClientModInitializer {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BetterBlockPos.class, new BetterBlockPosSerializer())
            .create();
    private static final Map<Integer, BlockPos> blocks = new ConcurrentHashMap<>();
    private static final Map<Integer, BlockPos> hiddenBlocks = new ConcurrentHashMap<>();
    private static boolean isolated = false;

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MatrixStack matrixStack = context.matrixStack();
            VertexConsumerProvider vertexConsumerProvider = context.consumers();
            renderBlocks(matrixStack, vertexConsumerProvider);
        });
    }

    public static void updateBlock(ClientBlockData blockData) {
        if (blockData == null || blockData.getBlockPos() == null) {
            return;
        }
        if (isolated) {
            hiddenBlocks.put(blockData.getMobId(), blockData.getBlockPos());
        } else {
            blocks.put(blockData.getMobId(), blockData.getBlockPos());
        }
    }

    public static void removeBlock(int mobId) {
        blocks.remove(mobId);
    }

    public static void isolateBlock(int mobId) {
        if (isolated) {
            undoIsolatedBlock();
        }
        for (Map.Entry<Integer, BlockPos> entry : blocks.entrySet()) {
            if (entry.getKey() != mobId) {
                hiddenBlocks.put(entry.getKey(), entry.getValue());
            }
        }
        blocks.entrySet().removeIf(entry -> entry.getKey() != mobId);
        isolated = true;
    }

    public static void clearBlocks() {
        blocks.clear();
        hiddenBlocks.clear();
    }

    public static void undoIsolatedBlock() {
        blocks.putAll(hiddenBlocks);
        hiddenBlocks.clear();
        isolated = false;
    }

    private void renderBlocks(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider) {
        for (Map.Entry<Integer, BlockPos> entry : blocks.entrySet()) {
            BlockPos blockPosition = entry.getValue();
            if (blockPosition != null) {
                renderBlock(matrixStack, vertexConsumerProvider, blockPosition);
            }
        }
    }

    private void renderBlock(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, BlockPos blockPosition) {
        MinecraftClient client = MinecraftClient.getInstance();
        double camX = client.getEntityRenderDispatcher().camera.getPos().x;
        double camY = client.getEntityRenderDispatcher().camera.getPos().y;
        double camZ = client.getEntityRenderDispatcher().camera.getPos().z;

        renderHighlight(matrixStack, vertexConsumerProvider, blockPosition, camX, camY, camZ);
    }

    private void renderHighlight(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, BlockPos blockPosition, double camX, double camY, double camZ) {
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getLines());

        Matrix4f modelMatrix = matrixStack.peek().getPositionMatrix();
        MatrixStack.Entry entry = matrixStack.peek();
        float[] color = {1, 0, 0, 1}; // Red for target blocks
        float nx = 0.0f, ny = 1.0f, nz = 0.0f;

        // Render box edges
        float x = (float) (blockPosition.getX() - camX);
        float y = (float) (blockPosition.getY() - camY);
        float z = (float) (blockPosition.getZ() - camZ);
        
        // Bottom face
        renderLine(vertexConsumer, modelMatrix, entry, x, y, z, x + 1, y, z, color, nx, ny, nz);
        renderLine(vertexConsumer, modelMatrix, entry, x + 1, y, z, x + 1, y, z + 1, color, nx, ny, nz);
        renderLine(vertexConsumer, modelMatrix, entry, x + 1, y, z + 1, x, y, z + 1, color, nx, ny, nz);
        renderLine(vertexConsumer, modelMatrix, entry, x, y, z + 1, x, y, z, color, nx, ny, nz);
        
        // Top face
        renderLine(vertexConsumer, modelMatrix, entry, x, y + 1, z, x + 1, y + 1, z, color, nx, ny, nz);
        renderLine(vertexConsumer, modelMatrix, entry, x + 1, y + 1, z, x + 1, y + 1, z + 1, color, nx, ny, nz);
        renderLine(vertexConsumer, modelMatrix, entry, x + 1, y + 1, z + 1, x, y + 1, z + 1, color, nx, ny, nz);
        renderLine(vertexConsumer, modelMatrix, entry, x, y + 1, z + 1, x, y + 1, z, color, nx, ny, nz);
        
        // Vertical edges
        renderLine(vertexConsumer, modelMatrix, entry, x, y, z, x, y + 1, z, color, nx, ny, nz);
        renderLine(vertexConsumer, modelMatrix, entry, x + 1, y, z, x + 1, y + 1, z, color, nx, ny, nz);
        renderLine(vertexConsumer, modelMatrix, entry, x + 1, y, z + 1, x + 1, y + 1, z + 1, color, nx, ny, nz);
        renderLine(vertexConsumer, modelMatrix, entry, x, y, z + 1, x, y + 1, z + 1, color, nx, ny, nz);
    }
    
    private void renderLine(VertexConsumer vertexConsumer, Matrix4f modelMatrix, MatrixStack.Entry entry, 
                          float x1, float y1, float z1, float x2, float y2, float z2, 
                          float[] color, float nx, float ny, float nz) {
        vertexConsumer.vertex(modelMatrix, x1, y1, z1)
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);
        vertexConsumer.vertex(modelMatrix, x2, y2, z2)
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);
    }
}