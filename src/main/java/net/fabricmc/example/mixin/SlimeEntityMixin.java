package net.fabricmc.example.mixin;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.fabricmc.example.config.ConfigManager;
import net.fabricmc.example.mobai.BreakPlaceAndChaseGoal;
import net.fabricmc.example.mobai.CustomTargetGoal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlimeEntity.class)
public abstract class SlimeEntityMixin extends MobEntity {

    protected SlimeEntityMixin(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addCustomGoals(CallbackInfo info) {
        if (BloodmoonHandler.INSTANCE.isBloodmoonActive()) {
            this.discard();
        }
    }
}
