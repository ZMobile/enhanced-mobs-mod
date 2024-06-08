package net.fabricmc.example.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public abstract class MobSpawnerMixin {

    // Define maximum number of witches allowed per chunk
    private static final int MAX_WITCHES_PER_CHUNK = 1;

    @Inject(method = "spawnEntity", at = @At("HEAD"), cancellable = true)
    private void onSpawnEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        /*if (entity instanceof WitchEntity) {
            ServerWorld world = (ServerWorld) (Object) this;
            BlockPos pos = entity.getBlockPos();
            ChunkPos chunkPos = new ChunkPos(pos);

            // Count the number of witches in the chunk
            int witchCount = 0;
            for (WitchEntity witch : world.getEntitiesByType(EntityType.WITCH, (e) -> true)) {
                if (new ChunkPos(witch.getBlockPos()).equals(chunkPos)) {
                    witchCount++;
                }
            }

            // If the limit is reached, cancel the spawn
            if (witchCount >= MAX_WITCHES_PER_CHUNK) {
                cir.setReturnValue(false);
            }
        }*/
    }
}