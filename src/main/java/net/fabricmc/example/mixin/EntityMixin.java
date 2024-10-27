package net.fabricmc.example.mixin;

import net.fabricmc.example.config.ConfigManager;
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
        if (sound == SoundEvents.ENTITY_CREEPER_PRIMED && !ConfigManager.getConfig().isCreeperHiss()) {
            ci.cancel();
        }
    }
}
