package net.fabricmc.example.client;

import baritone.api.utils.BetterBlockPos;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.example.client.payload.ClientBlockData;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientPlacingBlockHighlighter implements ClientModInitializer {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BetterBlockPos.class, new BetterBlockPosSerializer())
            .create();
    public static final Identifier MOD_PACKET_ID = Identifier.of("modid", "block_update");
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
        float[] color = {0, 1, 0, 1}; // Green
        float nx = 0.0f, ny = 1.0f, nz = 0.0f;

        double x = blockPosition.getX() - camX + 0.5;
        double y = blockPosition.getY() - camY + 0.5;
        double z = blockPosition.getZ() - camZ + 0.5;

        // Render a cube to highlight the block
        vertexConsumer.vertex(modelMatrix, (float) x - 0.5f, (float) y - 0.5f, (float) z - 0.5f)
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);
        vertexConsumer.vertex(modelMatrix, (float) x + 0.5f, (float) y - 0.5f, (float) z - 0.5f)
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);
        vertexConsumer.vertex(modelMatrix, (float) x + 0.5f, (float) y + 0.5f, (float) z - 0.5f)
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);
        vertexConsumer.vertex(modelMatrix, (float) x - 0.5f, (float) y + 0.5f, (float) z - 0.5f)
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);

        vertexConsumer.vertex(modelMatrix, (float) x - 0.5f, (float) y - 0.5f, (float) z + 0.5f)
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);
        vertexConsumer.vertex(modelMatrix, (float) x + 0.5f, (float) y - 0.5f, (float) z + 0.5f)
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);
        vertexConsumer.vertex(modelMatrix, (float) x + 0.5f, (float) y + 0.5f, (float) z + 0.5f)
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);
        vertexConsumer.vertex(modelMatrix, (float) x - 0.5f, (float) y + 0.5f, (float) z + 0.5f)
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);

        vertexConsumer.vertex(modelMatrix, (float) x - 0.5f, (float) y - 0.5f, (float) z - 0.5f)
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);
        vertexConsumer.vertex(modelMatrix, (float) x - 0.5f, (float) y - 0.5f, (float) z + 0.5f)
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);

        vertexConsumer.vertex(modelMatrix, (float) x + 0.5f, (float) y - 0.5f, (float) z - 0.5f)
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);
        vertexConsumer.vertex(modelMatrix, (float) x + 0.5f, (float) y - 0.5f, (float) z + 0.5f)
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);

        vertexConsumer.vertex(modelMatrix, (float) x + 0.5f, (float) y + 0.5f, (float) z - 0.5f)
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);
        vertexConsumer.vertex(modelMatrix, (float) x + 0.5f, (float) y + 0.5f, (float) z + 0.5f)
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);

        vertexConsumer.vertex(modelMatrix, (float) x - 0.5f, (float) y + 0.5f, (float) z - 0.5f)
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);
        vertexConsumer.vertex(modelMatrix, (float) x - 0.5f, (float) y + 0.5f, (float) z + 0.5f)
                .color(color[0], color[1], color[2], color[3])
                .normal(entry, nx, ny, nz);
    }
}
