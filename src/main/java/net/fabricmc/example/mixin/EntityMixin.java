package net.fabricmc.example.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.example.config.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
    private void playSound(SoundEvent sound, float volume, float pitch, CallbackInfo ci) {
        // Only run this on the server side
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            if (sound == SoundEvents.ENTITY_CREEPER_PRIMED && ConfigManager.getConfig() != null && !ConfigManager.getConfig().isCreeperHiss()) {
                ci.cancel();
            }
        }
    }
}
