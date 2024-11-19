package net.fabricmc.example.mixin;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.fabricmc.example.config.ConfigManager;
import net.fabricmc.example.mobai.CustomCreeperTargetGoal;
import net.fabricmc.example.mobai.CustomTargetGoal;
import net.fabricmc.example.mobai.ExplodeBlockAndChaseGoal;
import net.fabricmc.example.service.MobitoneServiceImpl;
import net.fabricmc.example.util.MinecraftServerUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperEntity.class)
public abstract class CreeperEntityMixin extends PathAwareEntity {
    protected CreeperEntityMixin(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addCustomGoals(CallbackInfo info) {
        CreeperEntity creeperEntity = (CreeperEntity) (Object) this;
        //if (!BloodmoonHandler.INSTANCE.isBloodmoonActive()) {
        if (ConfigManager.getConfig().isCreepersExplodeObstructions()) {
            //if (!ConfigManager.getConfig().isOptimizedMobitone()) {
                MobitoneServiceImpl.addMobitone(this);
                MobitoneServiceImpl.fillInQueue();
            //}

            //}
            //BaritoneAPI.getProvider().createBaritone(MinecraftServerUtil.getMinecraftServer(), creeperEntity);
            this.goalSelector.add(1, new ExplodeBlockAndChaseGoal(creeperEntity));
        }
        this.goalSelector.add(6, new CustomCreeperTargetGoal(this));
        //System.out.println("Baritone goal successfully added to CreeperEntity");
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onCreeperDespawn(CallbackInfo info) {
        if (!this.isAlive()) {
            /*IBaritone goalBaritone = BaritoneAPI.getProvider().getBaritoneForEntity(this);
            if (goalBaritone != null) {
                //BaritoneAPI.getProvider().destroyBaritone(goalBaritone);
                //System.out.println("Baritone instance successfully removed for CreeperEntity on despawn");
            }*/
            MobitoneServiceImpl.removeMobitone(this);
        }
    }

    @Inject(method = "explode", at = @At("TAIL"))
    private void explode(CallbackInfo info) {
        //if (!this.isAlive()) {
            /*IBaritone goalBaritone = BaritoneAPI.getProvider().getBaritoneForEntity(this);
            if (goalBaritone != null) {
                //BaritoneAPI.getProvider().destroyBaritone(goalBaritone);
                //System.out.println("Baritone instance successfully removed for CreeperEntity on despawn");
            }*/
            MobitoneServiceImpl.removeMobitone(this);//
    }
}