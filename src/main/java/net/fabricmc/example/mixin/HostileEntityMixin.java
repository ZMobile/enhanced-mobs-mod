package net.fabricmc.example.mixin;

import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HostileEntity.class)
public abstract class HostileEntityMixin extends PathAwareEntity {

    // Cache for light level checks to reduce calculations
    @Unique
    private int lastLightCheckTick = 0;

    @Unique
    private float cachedBrightness = 0.0f;

    @Unique
    private static final int LIGHT_CHECK_INTERVAL = 20; // Check every second instead of every tick

    protected HostileEntityMixin(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * Optimize despawn counter updates to reduce expensive light calculations.
     * During bloodmoon, we can skip most despawn checks since we want mobs to persist.
     */
    @Inject(method = "updateDespawnCounter", at = @At("HEAD"), cancellable = true)
    private void optimizeDespawnCounter(CallbackInfo ci) {
        // Only run on server side
        if (!this.getEntityWorld().isClient()) {
            // During bloodmoon, mobs should persist - skip expensive despawn checks
            if (BloodmoonHandler.INSTANCE != null && BloodmoonHandler.INSTANCE.isBloodmoonActive()) {
                ci.cancel();
                return;
            }
        }

        // For non-bloodmoon times, rate limit light level calculations
        World world = this.getEntityWorld();
        int currentTick = (int) (world.getTime() % Integer.MAX_VALUE);

        // Only do full update every second instead of every tick
        if (currentTick - lastLightCheckTick < LIGHT_CHECK_INTERVAL) {
            ci.cancel(); // Skip this tick's update
            return;
        }

        lastLightCheckTick = currentTick;
        // Let original method run but only once per second
    }
}