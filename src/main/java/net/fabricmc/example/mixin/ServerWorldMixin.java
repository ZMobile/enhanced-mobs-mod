package net.fabricmc.example.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {
    @Inject(method = "addEntity", at = @At("HEAD"))
    private void onAddEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
    }

    @Inject(method = "addEntity", at = @At("RETURN"), cancellable = true)
    private void afterAddEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
    }
}
