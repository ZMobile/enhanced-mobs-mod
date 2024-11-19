package net.fabricmc.example.mixin;

import net.fabricmc.example.config.ConfigManager;
import net.fabricmc.example.mobai.CustomTargetGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpiderEntity.class)
public class SpiderEntityMixin extends PathAwareEntity {
    protected SpiderEntityMixin(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addCustomGoals(CallbackInfo info) {
        this.goalSelector.add(6, new CustomTargetGoal(this));
        EntityAttributeInstance speedAttribute = this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);

        if (speedAttribute != null && ConfigManager.getConfig().isSpiderSpeed()) {
            speedAttribute.setBaseValue(0.75);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void checkZombieState(CallbackInfo info) {

    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onZombieDespawn(CallbackInfo info) {
        if (!this.isAlive()) {
        }
    }
}
