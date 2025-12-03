package net.fabricmc.example.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.fabricmc.example.config.ConfigManager;
import net.fabricmc.example.mobai.BreakPlaceAndChaseGoal;
import net.fabricmc.example.mobai.tracker.BreakPlaceAndChaseGoalTracker;
import net.fabricmc.example.mobai.CustomTargetGoal;
import net.fabricmc.example.mobai.tracker.MobPathTracker;
import net.fabricmc.example.service.MobitoneServiceImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.AzaleaBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombieEntity.class)
public abstract class ZombieEntityMixin extends PathAwareEntity {
    private static final Logger LOGGER = LogManager.getLogger("enhancedmobs");

    protected ZombieEntityMixin(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addCustomGoals(CallbackInfo info) {
        LOGGER.info("ZombieEntityMixin.initGoals() START for entity {}", this.getId());
        //GoalBlock goal = new GoalBlock(0, 60, 200);
        //BaritoneAPI.getProvider().createBaritone(MinecraftServerUtil.getMinecraftServer(),  this);v
        if (ConfigManager.getConfig().isZombiesBreakAndPlaceBlocks()) {
            boolean bloodmoonActive = BloodmoonHandler.INSTANCE != null && BloodmoonHandler.INSTANCE.isBloodmoonActive();
            if (!bloodmoonActive) {
                if (!ConfigManager.getConfig().isBuildingMiningMobsDuringBloodmoonOnly()) {
                    LOGGER.info("ZombieEntityMixin: Provisioning mobitone goal (non-bloodmoon)");
                    provisionMobitoneGoal();
                }
            } else {
                LOGGER.info("ZombieEntityMixin: Provisioning mobitone goal (bloodmoon active)");
                provisionMobitoneGoal();
            }
        }
        this.goalSelector.add(6, new CustomTargetGoal(this));
        LOGGER.info("ZombieEntityMixin.initGoals() END for entity {}", this.getId());

        // BaritoneAPI.getProvider().getBaritoneForEntity(this).getCustomGoalProcess().setGoalAndPath(goal);
    }

    private void provisionMobitoneGoal() {
        LOGGER.info("ZombieEntityMixin.provisionMobitoneGoal() START");
        //if (!ConfigManager.getConfig().isOptimizedMobitone()) {
            LOGGER.info("ZombieEntityMixin: Adding to MobitoneService");
            MobitoneServiceImpl.addMobitone(this);
            MobitoneServiceImpl.fillInQueue();
        //}
        //}
        LOGGER.info("ZombieEntityMixin: Creating BreakPlaceAndChaseGoal");
        BreakPlaceAndChaseGoal goal = new BreakPlaceAndChaseGoal(this);
        LOGGER.info("ZombieEntityMixin: Adding goal to selector");
        this.goalSelector.add(1, goal);
        BreakPlaceAndChaseGoalTracker.addGoal(this.getId(), goal);
        LOGGER.info("ZombieEntityMixin.provisionMobitoneGoal() END");
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void checkZombieState(CallbackInfo info) {
        /*ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            Vec3d playerPosition = MinecraftClient.getInstance().player.getEntityPos();
            GoalBlock goal = new GoalBlock((int) playerPosition.x, (int) playerPosition.y, (int) playerPosition.z);
            IBaritone goalBaritone = BaritoneAPI.getProvider().getBaritoneForEntity(this);
            if (goalBaritone != null) {
                goalBaritone.getCustomGoalProcess().setGoalAndPath(goal);
            }
        }*/
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onZombieDespawn(CallbackInfo info) {
        if (!this.isAlive()) {
            /*IBaritone goalBaritone = BaritoneAPI.getProvider().getBaritoneForEntity(this);
            if (goalBaritone != null) {
                // Clean up Baritone instance for this entity
                BaritoneAPI.getProvider().destroyBaritone(goalBaritone);
                // Debug log to verify cleanup
                //System.out.println("Baritone instance successfully removed for ZombieEntity on despawn");
            }*/
            MobitoneServiceImpl.removeMobitone(this);
            BreakPlaceAndChaseGoalTracker.removeGoal(this.getId());
            MobPathTracker.removePath(this.getUuidAsString());
        }
    }


    @Override
    public boolean isPushable() {
        // Prevent other entities from pushing this zombie
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            return BloodmoonHandler.INSTANCE == null || !BloodmoonHandler.INSTANCE.isBloodmoonActive();
        } else {
            return true;
        }
    }

    @Override
    public boolean isPushedByFluids() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            return BloodmoonHandler.INSTANCE == null || !BloodmoonHandler.INSTANCE.isBloodmoonActive();
        } else {
            return true;
        }
    }
}
