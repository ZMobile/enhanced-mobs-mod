package net.fabricmc.example.mobai;

import baritone.BaritoneProvider;
import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.movement.IMovement;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.List;

public class BreakBlockAndChaseGoal extends Goal {
    private final PathAwareEntity mob;
    private final GoalSelector goalSelector;
    private final BaritoneProvider baritoneProvider;
    private PlayerEntity targetPlayer;
    private IPath currentPath;
    private int currentIndex;
    private int breakingTicks;
    private BlockPos breakingPos;
    private static final int BREAKING_TIME = 100; // Example breaking time in ticks (5 seconds)

    public BreakBlockAndChaseGoal(PathAwareEntity mob,
                                  GoalSelector goalSelector) {
        this.mob = mob;
        this.goalSelector = goalSelector;
        this.baritoneProvider = new BaritoneProvider();
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        Goal goal = goalSelector.getGoals().stream()
                .filter(g -> g != null && g.getGoal() instanceof ZombieAttackGoal)
                .findFirst()
                .orElse(null);
        System.out.println("Goal:" + goal);
        if (goal != null) {
            System.out.println("#################### ZOMBIE ATTACK" +
                    "  GOAL should continue: " + goal.shouldContinue());
        }
        return goal == null || !goal.canStart();
    }

    @Override
    public void start() {
        System.out.println("#################### GOAL Triggered");
        /*Settings settings = BaritoneAPI.getSettings();

        // Configure Baritone to allow block breaking but not placing
        settings.allowBreak.value = true;
        settings.allowPlace.value = false;

        calculatePath();*/
    }


    private void calculatePath() {
        /*System.out.println("Calculating path.");
        if (this.targetPlayer != null) {
            System.out.println("Target player: " + this.targetPlayer.getName());
            BlockPos targetPos = targetPlayer.getBlockPos();
            GoalBlock goal = new GoalBlock(targetPos.getX(), targetPos.getY(), targetPos.getZ());

            // Get the current position of the mob (zombie)
            BlockPos mobPos = mob.getBlockPos();

            // Cancel any existing path to start fresh
            baritoneProvider.getPrimaryBaritone().getPathingBehavior().forceCancel();

            // Synchronize Baritone's state with the mob's position
            BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext().player().setPosition(mobPos.getX(), mobPos.getY(), mobPos.getZ());

            // Set goal and path
            baritoneProvider.getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(goal);

            // Check if the path is calculated
            if (baritoneProvider.getPrimaryBaritone().getPathingBehavior().getCurrent() != null) {
                System.out.println("Path calculated.");
                currentPath = baritoneProvider.getPrimaryBaritone().getPathingBehavior().getCurrent().getPath();
                currentIndex = 0;
                breakingTicks = 0;
                breakingPos = null;
            } else {
                currentPath = null;
            }
        }*/
    }


    @Override
    public void tick() {
        System.out.println("Injecting custom goals into ZombieEntity");
        System.out.println("Info about zombie goals:");
        for (Goal goal : this.goalSelector.getGoals()) {
            if (goal instanceof PrioritizedGoal) {
                PrioritizedGoal prioritizedGoal = (PrioritizedGoal) goal;
                System.out.println("Goal: " + prioritizedGoal.getGoal().getClass().getName() + ", Priority: " + prioritizedGoal.getPriority());
            } else {
                System.out.println("Goal: " + goal.getClass().getName());
            }
            ZombieEntity zombieEntity = (ZombieEntity) mob;
            System.out.println("Mob target: " + zombieEntity.getTarget());
            if (zombieEntity.getTarget() != null) {
                System.out.println("Mob can reach player: " + zombieEntity.isInWalkTargetRange(mob.getTarget().getBlockPos()));
            }
            System.out.println("Mob is navigating: " + mob.isNavigating());
            System.out.println("Mob is attacking: " + mob.isAttacking());
        }
        /*if (currentPath != null) {
            List<BetterBlockPos> positions = currentPath.positions();
            List<IMovement> movements = currentPath.movements();

            if (breakingPos != null) {
                // Continue breaking the current block
                World world = mob.getWorld();
                breakingTicks++;
                world.setBlockBreakingInfo(mob.getId(), breakingPos, (int) (breakingTicks / (float) BREAKING_TIME * 10));

                if (breakingTicks >= BREAKING_TIME) {
                    System.out.println("Breaking block at: " + breakingPos);
                    world.breakBlock(breakingPos, true, mob);
                    breakingTicks = 0;
                    breakingPos = null;
                }

                // Check if the mob is too far from the breaking block
                if (breakingPos != null && !mob.getBlockPos().isWithinDistance(breakingPos, 2.0)) {
                    System.out.println("Interrupted breaking block at: " + breakingPos);
                    breakingTicks = 0;
                    breakingPos = null;
                }

                return; // Exit early to keep breaking the block
            }

            if (currentIndex < positions.size() - 1) {
                BetterBlockPos nextPos = positions.get(currentIndex + 1);

                // Move the mob to the next position
                mob.getNavigation().startMovingTo(nextPos.x, nextPos.y, nextPos.z, 1.0);

                // Check if the mob has reached the next position
                if (mob.getBlockPos().isWithinDistance(new BlockPos(nextPos.x, nextPos.y, nextPos.z), 1.0)) {
                    currentIndex++;
                }

                // Handle block actions
                if (currentIndex < movements.size()) {
                    IMovement movement = movements.get(currentIndex);

                    // Check if the movement involves breaking a block
                    BetterBlockPos dest = movement.getDest();
                    BlockPos blockPos = new BlockPos(dest.x, dest.y, dest.z);

                    // Set the breaking position to the block position and start breaking
                    breakingPos = blockPos;
                    breakingTicks = 0;
                    System.out.println("Started breaking block at: " + breakingPos);
                }
            }
        } else {
            System.out.println("No current path.");
        }*/
    }

    @Override
    public boolean shouldContinue() {
        /*
        // Continue if there are more positions in the path and the player is still within range
        boolean continueGoal = currentPath != null && currentIndex < currentPath.positions().size() - 1 && this.targetPlayer.isAlive() && this.targetPlayer.squaredDistanceTo(this.mob) < 400;
        if (!continueGoal) {
            System.out.println("Current path doesnt equal null: " + (currentPath != null));
            System.out.println("Stopping goal: Conditions not met.");
        }
        return continueGoal;
         */ return false;
    }

    @Override
    public void stop() {
        /*// Cleanup or reset when the goal stops
        currentPath = null;
        currentIndex = 0;
        breakingTicks = 0;
        breakingPos = null;
        System.out.println("Goal stopped.");

        // Let other goals take over
        this.mob.getNavigation().stop();*/
    }
}
