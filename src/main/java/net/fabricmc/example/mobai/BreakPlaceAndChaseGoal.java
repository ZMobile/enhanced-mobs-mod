package net.fabricmc.example.mobai;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.calc.IPathFinder;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.MinecraftServerUtil;
import net.fabricmc.example.bloodmoon.server.BloodmoonHandler;
import net.fabricmc.example.client.block.ClientRenderedBlockUpdateServiceImpl;
import net.fabricmc.example.config.ConfigManager;
import net.fabricmc.example.mobai.tracker.MobPathTracker;
import net.fabricmc.example.service.MobitoneService;
import net.fabricmc.example.service.MobitoneServiceImpl;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.fabricmc.example.mixin.MobEntityAccessor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class BreakPlaceAndChaseGoal extends Goal {
    private static final Logger LOGGER = LogManager.getLogger("enhancedmobs");
    private final PathAwareEntity mob;
    private BlockPos previousPos;
    private Entity targetEntity;
    private IPathingBehavior pathingBehavior;
    private List<BetterBlockPos> currentPath;
    private int breakingTicks;
    private int standingStillTicks = 0;
    private int generalStandingStillTicks = 0; // For tracking when not breaking/placing
    private BlockPos breakingPos;
    private BlockPos placingPos;
    private BlockPos placingTargetPos;
    private static final int BREAKING_TIME = 50; // Faster breaking time in ticks (2.5 seconds)
    private final Map<BlockPos, Integer> blockDamageProgress = new HashMap<>();
    private List<BetterBlockPos> savedPath;
    private IBaritone baritone;
    private int pathRecalculationCooldown = 0;
    private Set<PrioritizedGoal> temporarilyDisabledGoals = new HashSet<>();

    // Performance optimization fields
    private int pathCheckCooldown = 0;
    private static final int PATH_CHECK_INTERVAL = 20; // Check every 20 ticks (1 second)
    private Boolean cachedCanReachDirectly = null;
    private int lastPathCheckTick = 0;

    public BreakPlaceAndChaseGoal(PathAwareEntity mob) {
        LOGGER.info("BreakPlaceAndChaseGoal constructor START for mob {}", mob.getId());
        this.mob = mob;
        LOGGER.info("BreakPlaceAndChaseGoal: Setting Baritone settings");
        BaritoneAPI.getSettings().allowParkour.value = false;
        BaritoneAPI.getSettings().allowJumpAt256.value = false;
        BaritoneAPI.getSettings().allowParkourAscend.value = false;
        BaritoneAPI.getSettings().allowParkourPlace.value = false;
        BaritoneAPI.getSettings().avoidance.value = false;
        BaritoneAPI.getSettings().assumeExternalAutoTool.value = true; // Assume tool is externally managed
        BaritoneAPI.getSettings().assumeWalkOnWater.value = false;
        BaritoneAPI.getSettings().walkOnWaterOnePenalty.value = 5.0D;
        savedPath = null;
        LOGGER.info("BreakPlaceAndChaseGoal: Updating MobPathTracker");
        MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
        LOGGER.info("BreakPlaceAndChaseGoal: Adding to MobitoneService");
        MobitoneServiceImpl.addMobitone(mob);
        LOGGER.info("BreakPlaceAndChaseGoal: Getting Baritone for entity");
        this.baritone = BaritoneAPI.getProvider().getBaritoneForEntity(mob);
        LOGGER.info("BreakPlaceAndChaseGoal: Getting pathingBehavior");
        this.pathingBehavior = baritone.getPathingBehavior();
        LOGGER.info("BreakPlaceAndChaseGoal constructor END for mob {}", mob.getId());
    }

    @Override
    public boolean canStart() {
        if (mob.getTarget() != null && mob.getTarget() instanceof LivingEntity) {
            targetEntity = mob.getTarget();
            boolean withinRange = mob.getBlockPos().isWithinDistance(targetEntity.getBlockPos(), 100)
                    && Math.abs(mob.getBlockPos().getY() - targetEntity.getBlockPos().getY()) < 50;
            boolean skeletonSpecificTrigger = false;
            if ((!mob.isNavigating() || isEntityStuckInDesignatedGlitchBlock(mob) || isNavigationTargetFarFromActualTarget()) && withinRange) {
                if (mob instanceof SkeletonEntity) {
                    Path path = mob.getNavigation().findPathTo(mob.getTarget(), 0);
                    if (path == null || path.isFinished()) {
                        skeletonSpecificTrigger = true;
                    }
                }
                boolean canStart = (!mob.isAttacking() || skeletonSpecificTrigger || isNavigationTargetFarFromActualTarget());
                if (mob instanceof SkeletonEntity && canStart) {
                }
                return canStart;
            }
        }
        return false;
    }

    private boolean isNavigationTargetFarFromActualTarget() {
        if (targetEntity == null || !mob.isNavigating()) {
            return false;
        }

        // Get the mob's current navigation path
        Path currentPath = mob.getNavigation().getCurrentPath();
        if (currentPath == null || currentPath.isFinished()) {
            return false;
        }

        // Get the target position of the navigation (where mob is trying to go)
        BlockPos navTarget = currentPath.getTarget();
        if (navTarget == null) {
            return false;
        }

        // Compare navigation target to actual target position
        BlockPos actualTargetPos = targetEntity.getBlockPos();
        double distance = navTarget.getSquaredDistance(actualTargetPos);

        // If navigation target is more than 5 blocks away from actual target,
        // the mob is probably stuck trying to reach an unreachable position
        return distance > 25; // 5 blocks squared
    }

    @Override
    public void start() {
        //baritone.getPathingBehavior().setCanPath(true);
        //calculatePath();
        // Take control of movement
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
        // Disable other goals when this goal starts
        disableOtherGoals();
        // Clear cached path data when starting
        cachedCanReachDirectly = null;
        pathCheckCooldown = 0;
        // //System.out.println("BreakPlaceAndChaseGoal starting for mob " + mob.getId());
    }

    @Override
    public boolean canStop() {
        // Don't allow this goal to stop unless player is truly reachable
        if (targetEntity != null) {
            return canReachTargetDirectly() && !isStuckBelowTarget();
        }
        return true;
    }

    @Override
    public void stop() {
        resetGoal(true);
        // Release all controls
        this.setControls(EnumSet.noneOf(Control.class));
        // Re-enable any disabled goals
        enableOtherGoals();
        // Allow vanilla navigation to resume
        // //System.out.println("BreakPlaceAndChaseGoal releasing control of mob " + mob.getId());
        super.stop();
    }

    private boolean canReachTargetDirectly() {
        if (targetEntity == null) return false;

        // Quick distance check first - if too far, don't bother with pathfinding
        double distance = mob.squaredDistanceTo(targetEntity);
        if (distance > 400) { // More than 20 blocks away
            cachedCanReachDirectly = false;
            return false;
        }

        // Use cached result if available and recent
        if (pathCheckCooldown > 0) {
            pathCheckCooldown--;
            return cachedCanReachDirectly != null ? cachedCanReachDirectly : false;
        }

        // Reset cooldown for next check
        pathCheckCooldown = PATH_CHECK_INTERVAL;

        // Simple line-of-sight check first
        Vec3d mobEyes = mob.getEyePos();
        Vec3d targetEyes = targetEntity.getEyePos();
        HitResult hitResult = mob.getEntityWorld().raycast(new RaycastContext(
                mobEyes, targetEyes,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mob
        ));

        // If we have direct line of sight and are close, assume we can reach
        if (hitResult.getType() == HitResult.Type.MISS && distance < 49) { // Within 7 blocks
            cachedCanReachDirectly = true;
            return true;
        }

        // Only do expensive pathfinding check occasionally
        Path directPath = mob.getNavigation().findPathTo(targetEntity, 0);
        if (directPath == null || directPath.isFinished()) {
            cachedCanReachDirectly = false;
            return false;
        }

        // Get the final position of the path
        PathNode pathEnd = directPath.getEnd();
        if (pathEnd == null) {
            cachedCanReachDirectly = false;
            return false;
        }

        // Check if the path actually reaches the player's position or adjacent
        BlockPos targetPos = targetEntity.getBlockPos();
        BlockPos pathEndPos = pathEnd.getBlockPos();

        // Must reach exact position or 1 block adjacent (including diagonal)
        int xDiff = Math.abs(pathEndPos.getX() - targetPos.getX());
        int yDiff = Math.abs(pathEndPos.getY() - targetPos.getY());
        int zDiff = Math.abs(pathEndPos.getZ() - targetPos.getZ());

        // Path must end at player position or immediately adjacent
        if (xDiff > 1 || yDiff > 1 || zDiff > 1) {
            //System.out.println("Mob " + mob.getId() + " - Path doesn't reach player position (diff: " + xDiff + "," + yDiff + "," + zDiff + ")");
            cachedCanReachDirectly = false;
            return false;
        }

        // For all mobs (including skeletons that might melee when close)
        // Verify the mob can actually walk to this position
        if (!canWalkToPosition(pathEndPos)) {
            cachedCanReachDirectly = false;
            return false;
        }

        // For ranged mobs like skeletons
        if (mob instanceof SkeletonEntity) {
            // Check if the skeleton can actually reach a position to shoot from
            BlockPos currentPos = mob.getBlockPos();

            // First, check if we're at the path end position (actually reached it)
            if (!currentPos.isWithinDistance(pathEnd.getBlockPos(), 1.5)) {
                // We haven't reached the path end yet
                cachedCanReachDirectly = false;
                return false;
            }

            // Check line of sight from CURRENT position, not path end
            if (!hasLineOfSight(currentPos, targetPos)) {
                cachedCanReachDirectly = false;
                return false;
            }

            // Check if skeleton is at a good shooting height
            int heightDiff = currentPos.getY() - targetPos.getY();

            // Skeleton needs to be at similar height or above to shoot effectively
            // If skeleton is more than 1 block below, it probably can't shoot well
            if (heightDiff < -1) {
                //System.out.println("Skeleton " + mob.getId() + " - Too low to shoot effectively (heightDiff: " + heightDiff + ")");
                cachedCanReachDirectly = false;
                return false;
            }

            // If skeleton is in a hole (surrounded by blocks), it can't maneuver to shoot
            if (isInHole(currentPos)) {
                //System.out.println("Skeleton " + mob.getId() + " - Still in a hole, can't shoot effectively");
                cachedCanReachDirectly = false;
                return false;
            }
        }

        // Additional check: Can the mob actually walk the entire path without obstacles?
        // This catches cases where there's a path but blocks are in the way
        boolean canReach = isPathWalkable(directPath);
        cachedCanReachDirectly = canReach;
        return canReach;
    }

    private boolean hasLineOfSight(BlockPos from, BlockPos to) {
        World world = getWorld(mob);
        // Simple check - is there a clear line between positions at eye level
        return world.raycast(new RaycastContext(
                Vec3d.ofCenter(from).add(0, 1.5, 0), // Eye level
                Vec3d.ofCenter(to).add(0, 1.5, 0),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mob
        )).getType() == HitResult.Type.MISS;
    }

    private boolean isPathWalkable(Path path) {
        if (path == null) return false;

        // Check each step of the path to ensure mob can actually walk it
        for (int i = 0; i < path.getLength() - 1; i++) {
            PathNode current = path.getNode(i);
            PathNode next = path.getNode(i + 1);

            BlockPos currentPos = new BlockPos(current.x, current.y, current.z);
            BlockPos nextPos = new BlockPos(next.x, next.y, next.z);

            // Check if there's a block preventing movement
            if (Math.abs(currentPos.getY() - nextPos.getY()) > 1) {
                // Need to jump more than 1 block - check if path is clear
                BlockPos checkPos = currentPos.up(2);
                if (isSolidBlock(checkPos)) {
                    return false; // Can't jump, head would hit block
                }
            }

            // Check if next position is actually walkable
            if (isSolidBlock(nextPos) || isSolidBlock(nextPos.up())) {
                return false; // Path goes through solid blocks
            }
        }

        return true;
    }

    private boolean isStuckBelowTarget() {
        if (targetEntity == null) return false;

        // Check if mob is directly below target
        int xDiff = Math.abs(mob.getBlockPos().getX() - targetEntity.getBlockPos().getX());
        int zDiff = Math.abs(mob.getBlockPos().getZ() - targetEntity.getBlockPos().getZ());
        int yDiff = targetEntity.getBlockPos().getY() - mob.getBlockPos().getY();

        // If mob is within 3 blocks horizontally and more than 2 blocks below
        if (xDiff <= 3 && zDiff <= 3 && yDiff > 2) {
            // Check if there's solid blocks above preventing direct path
            BlockPos checkPos = mob.getBlockPos().up(2);
            for (int i = 0; i < yDiff - 2; i++) {
                if (isSolidBlock(checkPos)) {
                    return true; // There's obstruction above, mob is stuck below
                }
                checkPos = checkPos.up();
            }
        }

        return false;
    }

    private boolean isInHole(BlockPos pos) {
        // Check if mob is surrounded by blocks at its level and above
        int solidSides = 0;
        int solidAbove = 0;

        for (Direction dir : Direction.Type.HORIZONTAL) {
            // Check at mob level
            if (isSolidBlock(pos.offset(dir))) {
                solidSides++;
            }
            // Check one block up
            if (isSolidBlock(pos.up().offset(dir))) {
                solidAbove++;
            }
        }

        // If surrounded by 3+ blocks at ground level or 2+ blocks above, it's in a hole
        return solidSides >= 3 || solidAbove >= 2;
    }

    private boolean canWalkToPosition(BlockPos pos) {
        // Check if position is walkable (not solid at feet and body level)
        if (isSolidBlock(pos) || isSolidBlock(pos.up())) {
            return false;
        }

        // Check if there's ground to stand on
        if (!isSolidBlock(pos.down())) {
            // Check if it's a valid non-solid ground (like water)
            BlockState below = getWorld(mob).getBlockState(pos.down());
            if (!below.getFluidState().isEmpty()) {
                return true; // Can walk in water
            }
            return false; // Can't walk on air
        }

        return true;
    }

    private void calculatePath() {
        // //System.out.println("Mob " + mob.getId() + " - Calculating path to target");
        if (this.targetEntity != null) {
            GoalBlock goal = getTargetGoal();
            // //System.out.println("Mob " + mob.getId() + " - Target goal: " + goal.getGoalPos());
            if (baritone != null) {
                pathingBehavior = baritone.getPathingBehavior();
                baritone.getCustomGoalProcess().setGoalAndPath(goal);
                if (baritone.getPathingBehavior().getCurrent() != null) {
                    currentPath = baritone.getPathingBehavior().getCurrent().getPath().positions();
                    breakingPos = null;
                    placingPos = null;
                    placingTargetPos = null;
                    findBreakingOrPlacingBlock();
                } else {
                    if (baritone.getPathingBehavior().getInProgress().isPresent()) {
                        IPathFinder pathFinder = baritone.getPathingBehavior().getInProgress().get();
                        Optional<IPath> bestSoFar = pathFinder.bestPathSoFar();
                        if (bestSoFar.isPresent() && bestSoFar.get().positions() != null) {
                            currentPath = new ArrayList<>(bestSoFar.get().positions());
                            breakingPos = null;
                            placingPos = null;
                            placingTargetPos = null;
                            findBreakingOrPlacingBlock();
                        } else if (savedPath != null) {
                            // No new path yet - use savedPath to maintain current breaking/placing action
                            currentPath = new ArrayList<>(savedPath);
                            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
                            findBreakingOrPlacingBlock();
                        } else {
                            currentPath = null;
                        }
                    } else if (savedPath != null) {
                        // No path in progress - use savedPath to maintain current breaking/placing action
                        currentPath = new ArrayList<>(savedPath);
                        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
                        findBreakingOrPlacingBlock();
                    } else {
                        currentPath = null;
                    }
                    ////System.out.println("Failed to calculate path.");
                }
            } else if (savedPath != null) {
                currentPath = new ArrayList<>(savedPath);
                this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
                breakingPos = null;
                placingPos = null;
                placingTargetPos = null;
                findBreakingOrPlacingBlock();
            }
        }
    }

    /*private boolean isPlacementNeeded(BlockPos blockPos, int pathIndex) {
        if (getWorld(mob).getBlockState(blockPos).isAir()) {
            return true;
        }
        if (getWorld(mob).getBlockState(blockPos).isOf(Blocks.WATER)) {
            if (!getWorld(mob).getBlockState(blockPos.up()).isAir()) {
                return false;
            }
            if (pathIndex == -1 || pathIndex == currentPath.size() - 1) {
                return false;
            }
            boolean isNextPosHigher = currentPath.get(pathIndex + 1).getY() > currentPath.get(pathIndex).getY();
            BlockPos pos = currentPath.get(pathIndex);
            BlockPos nextPos = currentPath.get(pathIndex + 1);
            return blockPos.equals(pos.down()) && nextPos.getY() > pos.getY() && !getWorld(mob).getBlockState(nextPos.down()).isOf(Blocks.WATER);
        }
        return false;
    }*/

    private boolean isPlacementNeeded(BlockPos blockPos, int pathIndex) {
        BlockPos pos = null;
        BlockPos nextPos = null;
        if (pathIndex != -1 && pathIndex != currentPath.size() - 1) {
            pos = currentPath.get(pathIndex);
            nextPos = currentPath.get(pathIndex + 1);
        }
        if (!isSolidBlock(blockPos) || getWorld(mob).getBlockState(blockPos).isOf(Blocks.LAVA)) {
            if (nextPos != null && pos.equals(blockPos)) {
                return !nextPos.equals(pos.down());
            }
            return true;
        }
        if (getWorld(mob).getBlockState(blockPos).isOf(Blocks.WATER)  || !getWorld(mob).getFluidState(blockPos).isEmpty()) {
            if (!getWorld(mob).getBlockState(blockPos.up()).isAir()) {
                return false;
            }
            if (pathIndex == -1 || pathIndex == currentPath.size() - 1) {
                return false;
            }
            //boolean isNextPosHigher = currentPath.get(pathIndex + 1).getY() > currentPath.get(pathIndex).getY();
            return !(nextPos.equals(pos.down()));
        }
        return false;
    }

    private void findBreakingOrPlacingBlock() {
        if (currentPath != null && !currentPath.isEmpty()) {
            BetterBlockPos destination = currentPath.get(currentPath.size() - 1);
            List<BetterBlockPos> positions = currentPath;

            // Find where the mob currently is on the path
            int mobPathIndex = -1;
            BlockPos mobPos = mob.getBlockPos();


            // If we can look a bit ahead for placement opportunities (within reach)

            // Start searching from the mob's position on the path

            // Search within walkable range plus a bit ahead for placement opportunities
            int lastWalkableIndex = 0;
            for (int i = 0; i <= positions.size() - 1; i++) {
                BlockPos targetBlockPos;
                if (i == 0) {
                    targetBlockPos = mob.getBlockPos();
                } else {
                    targetBlockPos = new BlockPos(positions.get(i - 1).x, positions.get(i - 1).y, positions.get(i - 1).z);
                }
                BetterBlockPos pos = positions.get(i);
                ////System.out.println("Checking block at: " + pos);
                if (isBreakable(pos) || isBreakable(pos.up())) {
                    breakingPos = isBreakable(pos) ? pos : pos.up();
                    BlockPos adjacentPos;
                    if (i == 0) {
                        adjacentPos = mob.getBlockPos();
                    } else {
                        adjacentPos = positions.get(i - 1);
                    }
                    savedPath = new ArrayList<>(currentPath);
                    MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
                    navigateMobToTargetPos(currentPath.get(lastWalkableIndex));
                    return;
                } else {
                    ////System.out.println("Check diagonals between positions.");
                    if (i != positions.size() - 1) {
                        BetterBlockPos nextPos = positions.get(i + 1);
                        //If next block pos x and y, or y and z are different, check if the diagonal block is breakable
                        if (nextPos.x != pos.x && nextPos.z != pos.z) {
                            BlockPos diagonalBlockPos1 = new BlockPos(pos.x, pos.y, nextPos.z);
                            BlockPos diagonalBlockPos2 = new BlockPos(nextPos.x, pos.y, pos.z);
                            if ((isBreakable(diagonalBlockPos1) || isBreakable(diagonalBlockPos1.up()) && !isPlacementNeeded(diagonalBlockPos1.down(), i))) {
                                if ((!isPlacementNeeded(diagonalBlockPos2.down(), i) || !isPlacementNeeded(diagonalBlockPos2.down(2), i)) && !isSolidBlock(diagonalBlockPos2) && !isSolidBlock(diagonalBlockPos2)) {
                                    //Diagonal path already exists, none needed
                                    //continue;
                                } else {
                                    breakingPos = isBreakable(diagonalBlockPos1) ? diagonalBlockPos1 : diagonalBlockPos1.up();
                                    savedPath = new ArrayList<>(currentPath);
                                    MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
                                    navigateMobToTargetPos(currentPath.get(lastWalkableIndex));
                                    return;
                                }
                            }
                            if (isBreakable(diagonalBlockPos2) || isBreakable(diagonalBlockPos2.up()) && !isPlacementNeeded(diagonalBlockPos2.down(), i)) {
                                if ((!isPlacementNeeded(diagonalBlockPos1.down(), i) || !isPlacementNeeded(diagonalBlockPos2.down(2), i)) && !isSolidBlock(diagonalBlockPos1) && !isSolidBlock(diagonalBlockPos1)) {
                                    //Diagonal path already exists, none needed
                                    //continue;
                                } else {
                                    breakingPos = isBreakable(diagonalBlockPos2) ? diagonalBlockPos2 : diagonalBlockPos2.up();
                                    savedPath = new ArrayList<>(currentPath);
                                    MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
                                    navigateMobToTargetPos(currentPath.get(lastWalkableIndex));
                                    return;
                                }
                            }
                        }
                        if (nextPos.y != pos.y) {
                            BlockPos twoBlocksUp = null;
                            if (nextPos.y < pos.y) {
                                for (int j = pos.y + 1; j >= nextPos.y + 1; j--) {
                                    twoBlocksUp = new BlockPos(nextPos.x, j, nextPos.z);
                                    if (isBreakable(twoBlocksUp)) {
                                        breakingPos = twoBlocksUp;
                                        navigateMobToTargetPos(pos);
                                        savedPath = new ArrayList<>(currentPath);
                                        MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
                                        navigateMobToTargetPos(currentPath.get(lastWalkableIndex));
                                        return;
                                    }
                                }
                            } else {
                                twoBlocksUp = new BlockPos(pos.x, pos.y + 2, pos.z);
                            }
                            if (isBreakable(twoBlocksUp)) {
                                breakingPos = twoBlocksUp;
                                navigateMobToTargetPos(pos);
                                savedPath = new ArrayList<>(currentPath);
                                MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
                                navigateMobToTargetPos(currentPath.get(lastWalkableIndex));
                                return;
                            }
                        }

                        //Now to check if placement needed:
                        //System.out.println("Checking placement opportunities.");
                        BetterBlockPos floorUnderBlockPos = new BetterBlockPos(pos.x, pos.y - 1, pos.z);
                        if (mob.getMainHandStack().getItem() instanceof BlockItem) {
                            if (isPlacementNeeded(floorUnderBlockPos, i) && !isSolidBlock(floorUnderBlockPos) && !nextPos.equals(floorUnderBlockPos)) {
                                //System.out.println("  Placement needed at 1: " + floorUnderBlockPos);
                              
                                // Check if we can place directly at target position
                                BlockPos placingPosToCheck = null;
                                if (canPlaceBlockAt(floorUnderBlockPos)) {
                                    placingPosToCheck = floorUnderBlockPos;
                                    //System.out.println(" Placement needed at 2");
                                } else {
                                    // Need to find a support block to place first
                                    //System.out.println("  Cannot place directly, looking for support position");
                                    BlockPos supportPos = findSupportBlockPosition(floorUnderBlockPos);
                                    if (supportPos != null) {
                                        placingPosToCheck = supportPos;
                                        //System.out.println("  Found support position: " + supportPos);
                                    } else {
                                        // Can't find a valid placement, skip
                                        //System.out.println("  No valid support position found, skipping");
                                        continue;
                                    }
                                }

                                // Only place if it won't block the path
                                if (!wouldBlockPath(placingPosToCheck)) {
                                    //System.out.println("  Placement valid at 3: " + placingPosToCheck);
                                    placingPos = placingPosToCheck;
                                    //System.out.println("  Final placing position: " + placingPos);
                                    BlockPos placingPosRender = new BlockPos(placingPos.getX(), placingPos.getY(), placingPos.getZ());
                                    ClientRenderedBlockUpdateServiceImpl.renderPlacingBlock(mob.getId(), placingPosRender);
                                    breakingPos = null; // Ensure breakingPos is null
                                    //System.out.println("  Navigating to position that can reach placement: " + placingTargetPos);
                                    savedPath = new ArrayList<>(currentPath);
                                    MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
                                    navigateMobToTargetPos(currentPath.get(lastWalkableIndex));
                                    return;
                                } else {
                                    //System.out.println("  Placement would block path, skipping");
                                }
                            } else {
                                if (nextPos.x != pos.x && nextPos.z != pos.z) {
                                    BlockPos diagonalBlockPos1 = new BlockPos(pos.x, pos.y, nextPos.z);
                                    BlockPos diagonalBlockPos2 = new BlockPos(nextPos.x, pos.y, pos.z);
                                    if ((!isSolidBlock(diagonalBlockPos1) && !isSolidBlock(diagonalBlockPos1.up()) && isPlacementNeeded(diagonalBlockPos1.down(), i))) {
                                        if ((!isSolidBlock(diagonalBlockPos2) || !isSolidBlock(diagonalBlockPos2.up())) && isSolidBlock(diagonalBlockPos2.down())) {
                                            //Diagonal path already exists, none needed
                                            //continue;
                                        } else {
                                            BlockPos potentialPlacingPos = diagonalBlockPos1.down();
                                            // Only place if it won't block the path and can be placed
                                            if (canPlaceBlockAt(potentialPlacingPos) && !wouldBlockPath(potentialPlacingPos)) {
                                                placingPos = potentialPlacingPos;
                                                BlockPos placingPosRender = new BlockPos(placingPos.getX(), placingPos.getY(), placingPos.getZ());
                                                ClientRenderedBlockUpdateServiceImpl.renderPlacingBlock(mob.getId(), placingPosRender);
                                                breakingPos = null; // Ensure breakingPos is null
                                                placingTargetPos = findFurthestWalkablePositionThatCanReach(placingPos);
                                                if (placingTargetPos != null) {
                                                } else {
                                                    //System.out.println("  ERROR: No position found that can reach placement target!");
                                                    continue;
                                                }
                                                savedPath = new ArrayList<>(currentPath);
                                                MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
                                                navigateMobToTargetPos(currentPath.get(lastWalkableIndex));
                                                return;
                                            }
                                        }
                                    }
                                    if (!isSolidBlock(diagonalBlockPos2) || !isSolidBlock(diagonalBlockPos2.up()) && isPlacementNeeded(diagonalBlockPos2.down(), i)) {
                                        if ((!isSolidBlock(diagonalBlockPos1) || !isSolidBlock(diagonalBlockPos1.down())) && isSolidBlock(diagonalBlockPos1.down())) {
                                            //Diagonal path already exists, none needed
                                            //continue;
                                        } else {
                                            BlockPos potentialPlacingPos = diagonalBlockPos2.down();
                                            // Only place if it won't block the path and can be placed
                                            if (canPlaceBlockAt(potentialPlacingPos) && !wouldBlockPath(potentialPlacingPos)) {
                                                placingPos = potentialPlacingPos;
                                                BlockPos placingPosRender = new BlockPos(placingPos.getX(), placingPos.getY(), placingPos.getZ());
                                                ClientRenderedBlockUpdateServiceImpl.renderPlacingBlock(mob.getId(), placingPosRender);
                                                breakingPos = null; // Ensure breakingPos is null
                                                savedPath = new ArrayList<>(currentPath);
                                                MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
                                                navigateMobToTargetPos(currentPath.get(lastWalkableIndex));
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        lastWalkableIndex = i;
                    }
                    //
                }
            }
            if (baritone != null) {
                pathingBehavior = baritone.getPathingBehavior();
                baritone.getCustomGoalProcess().setGoalAndPath(getTargetGoal());
            }
        }
        ////System.out.println("No block to break found in the path.");
    }

    private BlockPos findSuitableAdjacentBlock(BlockPos blockPos) {
        // First, find the closest position on the path to the zombie that we can navigate to
        BlockPos closestReachablePos = findClosestReachablePositionOnPath(blockPos);
        if (closestReachablePos == null) {
            return null;
        }
        return closestReachablePos;
    }

    private BlockPos findClosestReachablePositionOnPath(BlockPos targetPlacePos) {
        if (currentPath == null || currentPath.isEmpty()) {
            //System.out.println("    findClosestReachablePositionOnPath: No current path");
            return null;
        }

        BlockPos mobPos = mob.getBlockPos();
        //System.out.println("    findClosestReachablePositionOnPath: Looking for position to place block at " + targetPlacePos);
        //System.out.println("    Mob position: " + mobPos);

        // Debug: Check path ordering
        //System.out.println("    Path start (index 0): " + currentPath.get(0) + " - distance from mob: " + Math.sqrt(mobPos.getSquaredDistance(currentPath.get(0))));
        //System.out.println("    Path end (index " + (currentPath.size()-1) + "): " + currentPath.get(currentPath.size()-1) + " - distance from mob: " + Math.sqrt(mobPos.getSquaredDistance(currentPath.get(currentPath.size()-1))));

        // First, check if we can reach from our current position
        if (canReachToPlace(mobPos, targetPlacePos)) {
            //System.out.println("    Can place from current position!");
            return mobPos;
        }

        // Find where we are on the path
        int mobPathIndex = -1;
        for (int i = 0; i < currentPath.size(); i++) {
            if (mobPos.isWithinDistance(currentPath.get(i), 2.0)) {
                mobPathIndex = i;
                break;
            }
        }
        if (mobPathIndex == -1) mobPathIndex = 0;

        //System.out.println("    Mob is at path index: " + mobPathIndex + " out of " + currentPath.size());

        // Find the last walkable position on the path (where the zombie can actually stand)
        int lastWalkableIndex = mobPathIndex;
        for (int i = mobPathIndex; i < currentPath.size(); i++) {
            BetterBlockPos pathPos = currentPath.get(i);
            BlockPos checkPos = new BlockPos(pathPos.x, pathPos.y, pathPos.z);
            if (canStandAt(checkPos)) {
                lastWalkableIndex = i;
            } else {
                //System.out.println("    Path becomes unwalkable at index " + i + ": " + checkPos);
                break;
            }
        }

        //System.out.println("    Last walkable position is at index " + lastWalkableIndex + ": " + currentPath.get(lastWalkableIndex));

        // Now search BACKWARDS from the last walkable position to find where we can place
        BlockPos bestPosition = null;

        for (int i = lastWalkableIndex; i >= mobPathIndex; i--) {
            BetterBlockPos pathPos = currentPath.get(i);
            BlockPos checkPos = new BlockPos(pathPos.x, pathPos.y, pathPos.z);

            // Can we reach to place from this path position?
            if (canStandAt(checkPos) && canReachToPlace(checkPos, targetPlacePos)) {
                //System.out.println("    Found reachable position at path index " + i + ": " + checkPos);
                bestPosition = checkPos;
                break; // Take the position closest to the end of walkable path
            }
        }

        if (bestPosition != null) {
            //System.out.println("    Final best position: " + bestPosition + " (distance from mob: " + Math.sqrt(mobPos.getSquaredDistance(bestPosition)) + ")");
        } else {
            //System.out.println("    No suitable position found!");
        }

        return bestPosition;
    }

    private BlockPos findFurthestWalkablePositionThatCanReach(BlockPos targetPlacePos) {
        if (currentPath == null || currentPath.isEmpty()) {
            return null;
        }

        BlockPos mobPos = mob.getBlockPos();

        // Find where we are on the path
        int mobPathIndex = -1;
        for (int i = 0; i < currentPath.size(); i++) {
            if (mobPos.isWithinDistance(currentPath.get(i), 2.0)) {
                mobPathIndex = i;
                break;
            }
        }
        if (mobPathIndex == -1) mobPathIndex = 0;

        // First, find the last continuously walkable position from where we are
        int lastWalkableIndex = mobPathIndex;
        //System.out.println("    Starting from mob path index: " + mobPathIndex + " (total path size: " + currentPath.size() + ")");

        for (int i = mobPathIndex; i < currentPath.size(); i++) {
            BetterBlockPos pathPos = currentPath.get(i);
            BlockPos checkPos = new BlockPos(pathPos.x, pathPos.y, pathPos.z);

            // Check if this position is walkable
            if (!canStandAt(checkPos)) {
                //System.out.println("    Path becomes unwalkable at index " + i + ": " + checkPos);
                break;
            }

            // If this is not the first position, check if we can walk from the previous position
            if (i > mobPathIndex) {
                BetterBlockPos prevPos = currentPath.get(i - 1);
                BlockPos prevCheckPos = new BlockPos(prevPos.x, prevPos.y, prevPos.z);
                if (!canWalkBetween(prevCheckPos, checkPos)) {
                    //System.out.println("    Cannot walk from index " + (i-1) + " to " + i);
                    break;
                }
            }

            lastWalkableIndex = i;
        }

        //System.out.println("    Last continuously walkable index: " + lastWalkableIndex);

        // Now find the furthest position within our walkable range that can reach the target
        BlockPos furthestReachable = null;
        for (int i = lastWalkableIndex; i >= mobPathIndex; i--) {
            BetterBlockPos pathPos = currentPath.get(i);
            BlockPos checkPos = new BlockPos(pathPos.x, pathPos.y, pathPos.z);

            if (canReachToPlace(checkPos, targetPlacePos)) {
                furthestReachable = checkPos;
                //System.out.println("    Found furthest position that can reach target at index " + i + ": " + checkPos);
                break;
            }
        }

        // If we can't find any position on the path that can reach, check adjacent positions within walkable range
        if (furthestReachable == null) {
            //System.out.println("    No path position can reach target, checking adjacent positions...");
            for (int i = lastWalkableIndex; i >= mobPathIndex; i--) {
                BetterBlockPos pathPos = currentPath.get(i);
                BlockPos checkPos = new BlockPos(pathPos.x, pathPos.y, pathPos.z);

                // Check adjacent positions
                for (Direction dir : Direction.Type.HORIZONTAL) {
                    BlockPos adjacent = checkPos.offset(dir);
                    if (canStandAt(adjacent) && canWalkBetween(checkPos, adjacent) && canReachToPlace(adjacent, targetPlacePos)) {
                        //System.out.println("    Found adjacent position that can reach at index " + i + ": " + adjacent);
                        return adjacent;
                    }
                }
            }
        }

        return furthestReachable;
    }

    /*private BlockPos findFurthestWalkablePosition() {
        if (currentPath == null || currentPath.isEmpty()) {
            return null;
        }

        BlockPos mobPos = mob.getBlockPos();

        // Find where we are on the path
        int mobPathIndex = -1;
        for (int i = 0; i < currentPath.size(); i++) {
            if (mobPos.isWithinDistance(currentPath.get(i), 2.0)) {
                mobPathIndex = i;
                break;
            }
        }
        if (mobPathIndex == -1) mobPathIndex = 0;

        // Find the furthest walkable position from where we are
        BlockPos furthestWalkable = null;
        for (int i = currentPath.size() - 1; i >= mobPathIndex; i--) {
            BetterBlockPos pathPos = currentPath.get(i);
            BlockPos checkPos = new BlockPos(pathPos.x, pathPos.y, pathPos.z);

            if (canStandAt(checkPos)) {
                furthestWalkable = checkPos;
                //System.out.println("    Found furthest walkable at index " + i + ": " + checkPos);
                break;
            }
        }

        return furthestWalkable;
    }*/

    private boolean canWalkBetween(BlockPos from, BlockPos to) {
        // Check if we can walk from one position to another
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();

        // Must be adjacent (including diagonally)
        if (Math.abs(dx) > 1 || Math.abs(dz) > 1 || Math.abs(dy) > 1) {
            return false;
        }

        // Check if target position is walkable
        if (!canStandAt(to)) {
            return false;
        }

        // If moving up, check headroom at current position
        if (dy > 0 && isSolidBlock(from.up(2))) {
            return false;
        }

        // If moving down, must be able to fall safely
        if (dy < 0 && Math.abs(dy) > 1) {
            return false; // Can't fall more than 1 block
        }

        // If moving diagonally, check corners aren't blocked
        if (dx != 0 && dz != 0) {
            BlockPos corner1 = new BlockPos(from.getX(), from.getY(), to.getZ());
            BlockPos corner2 = new BlockPos(to.getX(), from.getY(), from.getZ());
            if (isSolidBlock(corner1) || isSolidBlock(corner1.up()) ||
                isSolidBlock(corner2) || isSolidBlock(corner2.up())) {
                return false;
            }
        }

        return true;
    }

    private boolean canWalkTo(BlockPos from, BlockPos to) {
        // Check if we can walk from one position to another
        int dx = Math.abs(to.getX() - from.getX());
        int dy = to.getY() - from.getY();
        int dz = Math.abs(to.getZ() - from.getZ());

        // Must be adjacent (including diagonally)
        if (dx > 1 || dz > 1 || Math.abs(dy) > 1) {
            return false;
        }

        // Check if target position is walkable
        if (!canStandAt(to)) {
            return false;
        }

        // If moving up, check headroom at current position
        if (dy > 0 && isSolidBlock(from.up(2))) {
            return false;
        }

        // If moving diagonally, check corners aren't blocked
        if (dx > 0 && dz > 0) {
            BlockPos corner1 = new BlockPos(from.getX(), from.getY(), to.getZ());
            BlockPos corner2 = new BlockPos(to.getX(), from.getY(), from.getZ());
            if (isSolidBlock(corner1) || isSolidBlock(corner1.up()) ||
                isSolidBlock(corner2) || isSolidBlock(corner2.up())) {
                return false;
            }
        }

        return true;
    }

    private boolean canStandAt(BlockPos pos) {
        // Check if the mob can stand at this position
        return (isSolidBlock(pos.down()) || getWorld(mob).getBlockState(pos.down()).isOf(Blocks.WATER))
                && !isSolidBlock(pos)
                && !isSolidBlock(pos.up());
    }

    private boolean canReachToPlace(BlockPos standingPos, BlockPos targetPlacePos) {
        // Check if we can reach to place a block at targetPlacePos from standingPos
        double distance = standingPos.getSquaredDistance(targetPlacePos);
        // Minecraft reach distance is about 4.5 blocks
        boolean canReach = distance <= 20.25; // 4.5 squared

        if (canReach) {
            //System.out.println("      Can reach from " + standingPos + " to place at " + targetPlacePos + " (dist: " + Math.sqrt(distance) + ")");
        }

        return canReach;
    }

    private boolean canPlaceBlockAt(BlockPos pos) {
        // Check if a block can be placed at this position
        // Must be air/water and have at least one adjacent solid block
        if (isSolidBlock(pos)) {
            return false;
        }

        // Check if there's at least one adjacent solid block (including below)
        // Check below
        if (isSolidBlock(pos.down())) {
            return true;
        }

        // Check all horizontal directions
        for (Direction dir : Direction.Type.HORIZONTAL) {
            if (isSolidBlock(pos.offset(dir))) {
                return true;
            }
        }

        // Check above (for hanging blocks)
        if (isSolidBlock(pos.up())) {
            return true;
        }

        return false;
    }

    private BlockPos findSupportBlockPosition(BlockPos targetPos) {
        // Find a support block position that:
        // 1. Can be placed (has adjacent solid blocks)
        // 2. Will eventually allow placing at targetPos
        // 3. Is as close as possible to the current path
        // 4. Must not block the path

        BlockPos mobPos = mob.getBlockPos();
        BlockPos bestSupportPos = null;
        double closestDistance = Double.MAX_VALUE;

        // First priority: Check position directly below target
        BlockPos belowTarget = targetPos.down();
        if (!isSolidBlock(belowTarget) && canPlaceBlockAt(belowTarget) && !wouldBlockPath(belowTarget)) {
            double distance = mobPos.getSquaredDistance(belowTarget);
            if (isOnOrNearPath(belowTarget)) {
                bestSupportPos = belowTarget;
                closestDistance = distance;
            }
        }

        // Check positions that would provide support for the target
        // Only look at positions at or below the target to avoid placing on the path
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos supportPos = targetPos.offset(dir);

            // Only consider positions at the same level as target if they won't block path
            if (!isSolidBlock(supportPos) && canPlaceBlockAt(supportPos) && supportPos.getY() <= targetPos.getY()) {
                if (!wouldBlockPath(supportPos)) {
                    double distance = mobPos.getSquaredDistance(supportPos);
                    if (distance < closestDistance && isOnOrNearPath(supportPos)) {
                        closestDistance = distance;
                        bestSupportPos = supportPos;
                    }
                }
            }

            // Check one level down (always safe as it's below the target)
            BlockPos supportPosDown = supportPos.down();
            if (!isSolidBlock(supportPosDown) && canPlaceBlockAt(supportPosDown) && !wouldBlockPath(supportPosDown)) {
                double distance = mobPos.getSquaredDistance(supportPosDown);
                if (distance < closestDistance && isOnOrNearPath(supportPosDown)) {
                    closestDistance = distance;
                    bestSupportPos = supportPosDown;
                }
            }
        }

        return bestSupportPos;
    }

    private boolean isOnOrNearPath(BlockPos pos) {
        if (currentPath == null) return false;

        // Check if position is on the path or very close to it
        for (BetterBlockPos pathPos : currentPath) {
            int dx = Math.abs(pos.getX() - pathPos.getX());
            int dy = Math.abs(pos.getY() - pathPos.getY());
            int dz = Math.abs(pos.getZ() - pathPos.getZ());

            // Within 2 blocks horizontally and 1 block vertically
            if (dx <= 2 && dy <= 1 && dz <= 2) {
                return true;
            }
        }
        return false;
    }

    private BlockPos findSuitableAdjacentBlockNextToWater(BlockPos blockPos) {
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos adjacentPos = blockPos.offset(direction);
            if (getWorld(mob).getBlockState(adjacentPos.down()).isSolidBlock(getWorld(mob), adjacentPos) && !isSolidBlock(adjacentPos) && !isSolidBlock(adjacentPos.up())) {
                for (Direction direction2 : Direction.Type.HORIZONTAL) {
                    //Find adjacent water at the same level as the solid block
                    BlockPos adjacentWaterPos = adjacentPos.offset(direction2).down();
                    if (getWorld(mob).getBlockState(adjacentWaterPos).isOf(Blocks.WATER)) {
                        return adjacentPos;
                    }
                }
            }
        }
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos downAdjacentPos = blockPos.offset(direction).down();
            if (getWorld(mob).getBlockState(downAdjacentPos.down()).isSolidBlock(getWorld(mob), downAdjacentPos) && !isSolidBlock(downAdjacentPos) && !isSolidBlock(downAdjacentPos.up())) {
                for (Direction direction2 : Direction.Type.HORIZONTAL) {
                    //Find adjacent water at the same level as the solid block
                    BlockPos adjacentWaterPos = downAdjacentPos.offset(direction2).down();
                    if (getWorld(mob).getBlockState(adjacentWaterPos).isOf(Blocks.WATER)) {
                        return downAdjacentPos;
                    }
                }
            }
        }
        return null;
    }

    private World getWorld(PathAwareEntity mob) {
        return MinecraftServerUtil.getMinecraftServer().getWorld(mob.getEntityWorld().getRegistryKey());
    }

    private boolean isBreakable(BlockPos blockPos) {
        BlockState blockState = getWorld(mob).getBlockState(blockPos);
        if (blockState.isOf(Blocks.BEDROCK) || blockState.isOf(Blocks.OBSIDIAN) || blockState.isOf(Blocks.SPAWNER)) {
            return false;
        }
        ////System.out.println("block state: " + getWorld(mob).getBlockState(blockPos));
        /*if (currentPath != null) {
            IPathExecutor current = pathingBehavior.getCurrent(); // this should prevent most race conditions?
            Set<BlockPos> blocksToBreak = current.toBreak();
            ////System.out.println("Blocks to break size: " + blocksToBreak.size());
            for (BlockPos pos : blocksToBreak) {
                if (pos.equals(blockPos)) {
                    for (BlockPos pos2 : current.toPlace()) {
                        if (pos2.equals(blockPos)){
                            return false;
                        }
                    }
                    ////System.out.println("Blocks to break contains this block");
                    return true;
                }
                ////System.out.println("Block to break: " + pos);
            }
        }*/
        return blockState.isSolidBlock(getWorld(mob), blockPos) || willObstructPlayer(getWorld(mob), blockPos) || isObstructiveNonQualifyingSolidBlock(blockPos);
    }

    private boolean isSolidBlock(BlockPos blockPos) {
        ////System.out.println("block state: " + getWorld(mob).getBlockState(blockPos));
        /*if (pathingBehavior != null && pathingBehavior.getCurrent() != null) {
            IPathExecutor current = pathingBehavior.getCurrent(); // this should prevent most race conditions?
            Set<BlockPos> blocksToBreak = current.toBreak();
            Set<BlockPos> blocksToWalkInto = current.toWalkInto();
            ////System.out.println("Blocks to break size: " + blocksToBreak.size());
            for (BlockPos pos : blocksToBreak) {
                if (pos.equals(blockPos)) {
                    ////System.out.println("Blocks to break contains this block");
                    return true;
                }
            }
            /*for (BlockPos pos : blocksToWalkInto) {
                if (pos.equals(blockPos)) {
                if (pos.equals(blockPos)) {
                    return true;
                }
            }*
        }*/
        return getWorld(mob).getBlockState(blockPos).isSolidBlock(getWorld(mob), blockPos) || isObstructiveNonQualifyingSolidBlock(blockPos) || willObstructPlayer(getWorld(mob), blockPos);
    }

    private boolean isObstructiveNonQualifyingSolidBlock(Block block) {
        return block instanceof LadderBlock
                || block instanceof VineBlock
                || block instanceof FenceBlock
                || block instanceof WallBlock
                || block instanceof PaneBlock
                || block instanceof TransparentBlock
                || block instanceof DoorBlock
                || block instanceof TrapdoorBlock
                || block instanceof BedBlock
                || block instanceof ChainBlock
                || block == Blocks.IRON_BARS
                || block == Blocks.IRON_CHAIN
                || block == Blocks.POINTED_DRIPSTONE
                || block == Blocks.END_ROD
                || block instanceof AzaleaBlock
                || block instanceof BigDripleafBlock
                || block instanceof SmallDripleafBlock;
    }

    private boolean isObstructiveNonQualifyingSolidBlock(BlockPos blockPos) {
        BlockState blockState = getWorld(mob).getBlockState(blockPos);
        Block block = blockState.getBlock();

        return isObstructiveNonQualifyingSolidBlock(block);
    }

    private boolean isAdjacentOrDiagonal(BlockPos pos1, BlockPos pos2) {
        int dx = Math.abs(pos1.getX() - pos2.getX());
        int dy = Math.abs(pos1.getY() - pos2.getY());
        int dz = Math.abs(pos1.getZ() - pos2.getZ());
        return dx <= 1 && dy <= 1 && dz <= 1;
    }


    @Override
    public void tick() {
        if (BloodmoonHandler.INSTANCE.isBloodmoonActive()) {
            //BaritoneAPI.getSettings().slowPath.value = true;
            if (mob instanceof ZombieEntity) {
                ItemStack mainHandItem = mob.getMainHandStack(); // Get the item in the zombie's main hand
                //if (mainHandItem.getItem() instanceof BlockItem) { // Check if the item is a BlockItem
                baritone.getPathingBehavior().getInProgress().ifPresent(pathFinder -> {
                    pathFinder.setSlowPathBypass(true);
                });
                /*} else {
                    baritone.getPathingBehavior().getInProgress().ifPresent(pathFinder -> {
                        pathFinder.setSlowPathBypass(false);
                    });
                }*/
            } else {
                baritone.getPathingBehavior().getInProgress().ifPresent(pathFinder -> {
                    pathFinder.setSlowPathBypass(false);
                });
            }
        } else {
            //BaritoneAPI.getSettings().slowPath.value = false;
            baritone.getPathingBehavior().getInProgress().ifPresent(pathFinder -> {
                pathFinder.setSlowPathBypass(false);
            });
        }
        //baritone.getPathingBehavior().setCanPath(breakingPos == null && placingPos == null);
        if (savedPath != null && (mob.getBlockPos().equals(savedPath.get(savedPath.size() - 1)) || mob.getBlockPos().up().equals(savedPath.get(savedPath.size() - 1)))) {
            resetGoal(true);
            return;
        }
        if (currentPath != null && (mob.getBlockPos().equals(currentPath.get(currentPath.size() - 1)) || mob.getBlockPos().up().equals(currentPath.get(currentPath.size() - 1)))) {
            resetGoal(true);
            return;
        }
        targetEntity = mob.getTarget();
        if (targetEntity != null) {
            // FIRST: Check if player is directly reachable - if so, immediately stop
            if (canReachTargetDirectly() && !isStuckBelowTarget()) {
                //System.out.println("Mob " + mob.getId() + " - Player is directly reachable, stopping goal immediately");
                this.stop();
                return;
            }
            if (isEntityStuckInDesignatedGlitchBlock(mob)) {
                float yaw = mob.getYaw();
                yaw = yaw % 360;
                if (yaw < 0) {
                    yaw += 360;
                }
                Direction facing;
                if (yaw >= 45 && yaw < 135) {
                    facing = Direction.WEST;
                } else if (yaw >= 135 && yaw < 225) {
                    facing = Direction.NORTH;
                } else if (yaw >= 225 && yaw < 315) {
                    facing = Direction.EAST;
                } else {
                    facing = Direction.SOUTH;
                }
                BlockPos feetPos = mob.getBlockPos();
                BlockPos headPos = feetPos.up();
                BlockPos facingFeetPos = feetPos.offset(facing);
                BlockPos facingHeadPos = facingFeetPos.up();

                // Check if the block at the entity's feet, head, or in front is a stalagmite
                if (isADesignatedGlitchBlock(feetPos, mob.getEntityWorld())) {
                    breakingPos = feetPos;
                } else if (isADesignatedGlitchBlock(headPos, mob.getEntityWorld())) {
                    breakingPos = headPos;
                } else if (isADesignatedGlitchBlock(facingFeetPos, mob.getEntityWorld())) {
                    breakingPos = facingFeetPos;
                } else if (isADesignatedGlitchBlock(facingHeadPos, mob.getEntityWorld())) {
                    breakingPos = facingHeadPos;
                }
            }
            if (previousPos == null) {
                previousPos = mob.getBlockPos();
            }
            // Proactively check if navigation target is blocked and fix it
            validateAndUpdateNavigationTarget();
            if (breakingPos != null) {
                // Keep full control
                this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
                // Render the breaking block as orange
                ClientRenderedBlockUpdateServiceImpl.renderBreakingBlock(mob.getId(), breakingPos);
                //System.out.println("Mob " + mob.getId() + " has breaking position: " + breakingPos);
                if (!isSolidBlock(breakingPos)) {
                    resetGoal(true);
                    return;
                }
                if (mob.getBlockPos() != null) {
                    if (mob.getBlockPos().getManhattanDistance(previousPos) > 2) {
                        previousPos = mob.getBlockPos();
                        standingStillTicks = 0;
                    } else {
                        standingStillTicks++;
                    }

                    // 10 seconds for breaking blocks
                    if (standingStillTicks > 200) {
                        System.out.println("Mob: " + mob.getId() + " Standing still for too long while trying to break block at " + breakingPos);
                        resetGoal(true);
                        return;
                    }
                }
                if (mob.getBlockPos().isWithinDistance(breakingPos, 4.5)) {
                    ////System.out.println("Block is within distance to break.");
                    continueBreakingBlock();
                } else {
                    if (previousPos == null) {
                        previousPos = mob.getBlockPos();
                    }
                    //System.out.println("Mob " + mob.getId() + " too far from breaking pos: " + breakingPos + ", distance: " + mob.getBlockPos().getSquaredDistance(breakingPos));

                    // Ensure we maintain full control
                    this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));

                    // Simply navigate to the breaking position
                    //mob.getNavigation().startMovingTo(breakingPos.getX(), breakingPos.getY(), breakingPos.getZ(), 1.0);
                    /*if (mob.getBlockPos().getManhattanDistance(previousPos) > 2) {
                        previousPos = mob.getBlockPos();
                        standingStillTicks = 0;
                    } else {
                        //System.out.println("Mob: " + mob.getId() + "Standing still ticks: " + standingStillTicks);
                        standingStillTicks++;
                    }
                    if (standingStillTicks> 200 || !isSolidBlock(breakingPos))
                    {
                        //System.out.println("Mob: " + mob.getId() + "Standing still for too long.");
                        resetGoal(true);
                    }*/
                }
                ////System.out.println("Block is not within distance to break. Moving to block.");
                ////System.out.println("Distance: " + mob.getBlockPos().getManhattanDistance(breakingPos));
            } else if (placingPos != null) {
                // Keep full control
                this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
                if (isSolidBlock(placingPos)) {
                    resetGoal(true);
                    return;
                }
                if (mob.getBlockPos() != null) {
                    if (mob.getBlockPos().getManhattanDistance(previousPos) > 2) {
                        previousPos = mob.getBlockPos();
                        standingStillTicks = 0;
                    } else {
                        standingStillTicks++;
                    }

                    // 8 seconds for placing blocks
                    if (standingStillTicks > 160) {
                        System.out.println("Mob: " + mob.getId() + " Standing still for too long while trying to place at " + placingPos);
                        resetGoal(true);
                        return;
                    }
                }
                if (mob.getBlockPos().isWithinDistance(placingPos, 5.5)) {
                    // //System.out.println("Block is within distance to place.");
                    placeBlock();
                } else {
                    // Remove the end of walkable path check since we're using blanket 8 seconds

                    if (previousPos == null) {
                        previousPos = mob.getBlockPos();
                    }
                    navigateMobToTargetPos(placingPos);
                    /*If mob moved more than 2 blocks away from previous pos:
                    if (mob.getBlockPos().getManhattanDistance(previousPos) > 2) {
                        previousPos = mob.getBlockPos();
                        standingStillTicks = 0;
                    } else {
                        //System.out.println("Mob: " + mob.getId() + "Standing still ticks: " + standingStillTicks);
                        standingStillTicks++;
                    }
                    if (standingStillTicks > 200 || isSolidBlock(placingPos)) {
                        //System.out.println("Mob: " + mob.getId() + "Standing still for too long.");
                        resetGoal(true);
                    }*/
                }
                //If mob moved more than 2 blocks away from previous pos:
            } else {
                // If we have a currentPath with a savedPath, stay on it - don't let vanilla AI interfere
                if (currentPath != null && savedPath != null) {
                    this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
                    // Stop vanilla navigation from interfering
                    mob.getNavigation().stop();
                } else {
                    // Check if we should let vanilla AI take over
                    boolean canReachDirectly = canReachTargetDirectly();
                    boolean stuckBelow = isStuckBelowTarget();

                    // Simple logic: if mob can't reach target OR is stuck, maintain control
                    if (!canReachDirectly || stuckBelow) {
                        // We need to dig/build to reach target
                        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));

                        // Let Baritone handle the movement
                    } else {
                        // Can reach directly and not stuck - let vanilla AI handle it
                        //System.out.println("Mob " + mob.getId() + " releasing control to vanilla AI");
                        mob.getNavigation().findPathTo(targetEntity, 0);
                        return;
                    }
                }

                if (pathRecalculationCooldown <= 0) {
                    calculatePath();
                    pathRecalculationCooldown = 20; // 1 second cooldown
                } else {
                    pathRecalculationCooldown--;
                    // Force Baritone to control movement
                    if (baritone != null) {

                        // Check Baritone status
                        boolean isPathing = baritone.getPathingBehavior().isPathing();

                        if (mob instanceof SkeletonEntity) {
                        }

                        // If Baritone isn't actively pathing, force it to start
                        if (!isPathing) {
                            //System.out.println("Mob " + mob.getId() + " - Forcing Baritone to start pathing");
                            GoalBlock goal = getTargetGoal();
                            if (goal != null) {
                                baritone.getCustomGoalProcess().setGoalAndPath(goal);
                            }
                        }

                        // We have a path from Baritone, now we need to move the mob along it
                        if (currentPath != null && !currentPath.isEmpty()) {
                            // Find the next position the mob should move to
                            BlockPos mobPos = mob.getBlockPos();
                            BetterBlockPos targetPos = null;

                            // Find where we are in the path and get next position
                            for (int i = 0; i < currentPath.size(); i++) {
                                BetterBlockPos pathPos = currentPath.get(i);
                                if (mobPos.equals(pathPos) || mobPos.isWithinDistance(pathPos, 1.5)) {
                                    // We're at this position, get the next one
                                    if (i + 1 < currentPath.size()) {
                                        targetPos = currentPath.get(i + 1);
                                        break;
                                    }
                                } else if (i == 0) {
                                    // We're not on the path yet, go to first position
                                    targetPos = pathPos;
                                    break;
                                }
                            }

                            if (targetPos != null) {
                                // Let Baritone handle the pathing
                                //mob.getNavigation().startMovingTo(targetPos.x, targetPos.y, targetPos.z, 1.0);

                                if (mob instanceof SkeletonEntity) {
                                    //System.out.println("Skeleton " + mob.getId() + " - Moving to path position: " + targetPos);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isEntityStuckInDesignatedGlitchBlock(LivingEntity entity) {
        float yaw = entity.getYaw();
        yaw = yaw % 360;
        if (yaw < 0) {
            yaw += 360;
        }
        Direction facing;
        if (yaw >= 45 && yaw < 135) {
            facing = Direction.WEST;
        } else if (yaw >= 135 && yaw < 225) {
            facing = Direction.NORTH;
        } else if (yaw >= 225 && yaw < 315) {
            facing = Direction.EAST;
        } else {
            facing = Direction.SOUTH;
        }
        BlockPos feetPos = entity.getBlockPos();
        BlockPos headPos = feetPos.up();
        BlockPos facingPos = feetPos.offset(facing);
        BlockPos facingHeadPos = facingPos.up();

        boolean isFeetStalagmite = isADesignatedGlitchBlock(feetPos, entity.getEntityWorld());
        boolean isHeadStalagmite = isADesignatedGlitchBlock(headPos, entity.getEntityWorld());
        boolean isFeetFacingStalagmite = isADesignatedGlitchBlock(facingPos, entity.getEntityWorld());
        boolean isHeadFacingStalagmite = isADesignatedGlitchBlock(facingHeadPos, entity.getEntityWorld());

        return (isFeetStalagmite || isHeadStalagmite || isFeetFacingStalagmite || isHeadFacingStalagmite) && isEntityNotMoving(entity);
    }


    private boolean isADesignatedGlitchBlock(BlockPos pos, World world) {
        BlockState blockState = world.getBlockState(pos);
        return blockState.isOf(Blocks.POINTED_DRIPSTONE) // Stalagmite
                || blockState.isOf(Blocks.END_ROD) // End Rod
                || blockState.isOf(Blocks.IRON_CHAIN) // Chain
                || blockState.isOf(Blocks.AZALEA) // Azalea Block
                || blockState.isOf(Blocks.FLOWERING_AZALEA) // Flowering Azalea Block
                || blockState.isOf(Blocks.BIG_DRIPLEAF)
                || blockState.isOf(Blocks.SMALL_DRIPLEAF)
                || blockState.isOf(Blocks.DECORATED_POT)
                || blockState.isOf(Blocks.DAYLIGHT_DETECTOR)
                || blockState.isOf(Blocks.SLIME_BLOCK)
                || blockState.isOf(Blocks.HONEY_BLOCK)
                || blockState.isOf(Blocks.TARGET);
    }

    private boolean isEntityNotMoving(LivingEntity entity) {
        // Check if the entity's movement is minimal
        return entity.getVelocity().lengthSquared() < 0.01;
    }

    private boolean isAtEndOfWalkablePath() {
        if (currentPath == null || currentPath.isEmpty()) {
            return false;
        }

        BlockPos mobPos = mob.getBlockPos();

        // Check if we're at one of the last few positions in the path
        int pathSize = currentPath.size();
        for (int i = Math.max(0, pathSize - 5); i < pathSize; i++) {
            BetterBlockPos pathPos = currentPath.get(i);
            if (mobPos.isWithinDistance(pathPos, 1.5)) {
                // We're at one of the last positions
                // Check if we can walk further toward the target
                for (int j = i + 1; j < pathSize; j++) {
                    BetterBlockPos nextPos = currentPath.get(j);
                    if (canWalkBetween(mobPos, new BlockPos(nextPos.x, nextPos.y, nextPos.z))) {
                        return false; // We can still walk further
                    }
                }
                return true; // Can't walk to any further positions
            }
        }

        return false;
    }

    private boolean isPlaceableBlock(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof BlockItem)) {
            return false;
        }
        BlockState blockState = ((BlockItem) itemStack.getItem()).getBlock().getDefaultState();
        if (blockState.isOf(Blocks.POINTED_DRIPSTONE)) {
            return false;
        }
        BlockItem blockItem = (BlockItem) itemStack.getItem();

        if (isObstructiveNonQualifyingSolidBlock(blockItem.getBlock())) {
            return false;
        }
        return !isSeed(itemStack.getItem());
    }

    public boolean isSeed(Item item) {
        return item == Items.WHEAT_SEEDS || item == Items.BEETROOT_SEEDS || item == Items.MELON_SEEDS || item == Items.PUMPKIN_SEEDS;
    }


    private void placeBlock() {
        if (placingPos != null && mob.getMainHandStack().getItem() instanceof BlockItem) {
            World world = getWorld(mob);
            ItemStack itemStack = mob.getMainHandStack();
            BlockItem blockItem = (BlockItem) itemStack.getItem();
            //Make sure it isnt a half slab, door, trapdoor, or bed:
            if (isPlaceableBlock(itemStack)) {
                boolean success = world.setBlockState(placingPos, blockItem.getBlock().getDefaultState(), 3);
                if (success) {
                    // Decrement the amount of blocks in the mob's hand by 1
                    //if not infinite blocks or block isnt cobblestone, dirt, stone, deepslate or deepslate cobble
                    if (!ConfigManager.getConfig().isInfiniteZombieBlocks() || (blockItem.getBlock() != Blocks.COBBLESTONE
                            && blockItem.getBlock() != Blocks.DIRT
                            && blockItem.getBlock() != Blocks.STONE
                            && blockItem.getBlock() != Blocks.DEEPSLATE
                            && blockItem.getBlock() != Blocks.COBBLED_DEEPSLATE
                            && blockItem.getBlock() != Blocks.NETHERRACK
                            && blockItem.getBlock() != Blocks.SOUL_SOIL)) {
                        itemStack.decrement(1);
                    }
                }
            }
            boolean resetSavedPath = true;
            if (savedPath != null) {
                for (BetterBlockPos betterBlockPos : savedPath) {
                    if (betterBlockPos.equals(placingPos)) {
                        int index = savedPath.indexOf(betterBlockPos);
                        if (index >= 0) {
                            savedPath.subList(0, index + 1).clear();
                        }
                        break;
                    }
                }
                resetSavedPath = savedPath.isEmpty();
            }
            if (MobPathTracker.placingPosLiesInPath(placingPos)) {
                if (baritone != null) {
                    pathingBehavior = baritone.getPathingBehavior();
                    baritone.getCustomGoalProcess().setGoalAndPath(getTargetGoal());
                }
            }
            // Validate navigation target after placing - it may now be inside the placed block
            validateAndUpdateNavigationTarget();
            resetGoal(resetSavedPath);
        }
    }

    private boolean hasAdjacentBlockIncludingBelow(BlockPos blockPos) {
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos adjacentPos = blockPos.offset(direction);
            if (isSolidBlock(adjacentPos)) {
                return true;
            }
        }
        return isSolidBlock(blockPos.down());
    }

    private boolean wouldBlockPath(BlockPos placementPos) {
        if (currentPath == null) return false;

        // Check if the placement position would interfere with the path
        for (BetterBlockPos pathPos : currentPath) {
            // If we're trying to place a block where the mob needs to stand (feet position)
            if (pathPos.equals(placementPos)) {
                return true;
            }

            // If we're trying to place a block where the mob's head would be (one above feet)
            if (pathPos.equals(placementPos.down())) {
                return true;
            }

            // If placing below the path position, that's generally okay (building floor)
            if (pathPos.up().equals(placementPos)) {
                // This would place a block above where the mob needs to walk
                return true;
            }
        }

        // Also check if placement would block movement between consecutive path positions
        for (int i = 0; i < currentPath.size() - 1; i++) {
            BetterBlockPos current = currentPath.get(i);
            BetterBlockPos next = currentPath.get(i + 1);

            // If the placement is between two path positions at head height
            if (placementPos.getY() == current.getY() + 1 || placementPos.getY() == next.getY() + 1) {
                // Check if it would block diagonal movement
                if (isBlockingDiagonalMovement(current, next, placementPos)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isBlockingDiagonalMovement(BetterBlockPos from, BetterBlockPos to, BlockPos placementPos) {
        // Check if placement would block diagonal movement between two positions
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();

        // If moving diagonally
        if (Math.abs(dx) == 1 && Math.abs(dz) == 1) {
            // Check the two possible paths for diagonal movement
            BlockPos corner1 = new BlockPos(from.getX() + dx, from.getY(), from.getZ());
            BlockPos corner2 = new BlockPos(from.getX(), from.getY(), from.getZ() + dz);

            // If placement blocks either corner at foot or head level
            if (placementPos.equals(corner1) || placementPos.equals(corner1.up()) ||
                placementPos.equals(corner2) || placementPos.equals(corner2.up())) {
                return true;
            }
        }

        return false;
    }

    private boolean hasPathConnection(BlockPos blockPos) {
        if (currentPath == null) return false;

        // Check if this position is adjacent (including diagonally) to any path position
        for (BetterBlockPos pathPos : currentPath) {
            int dx = Math.abs(blockPos.getX() - pathPos.getX());
            int dy = Math.abs(blockPos.getY() - pathPos.getY());
            int dz = Math.abs(blockPos.getZ() - pathPos.getZ());

            // Adjacent horizontally or one block below/above
            if (dx <= 1 && dy <= 1 && dz <= 1) {
                return true;
            }
        }
        return false;
    }



    private void continueBreakingBlock() {
        BlockState blockState = getWorld(mob).getBlockState(breakingPos);
        if (blockState.isOf(Blocks.OBSIDIAN) || blockState.isOf(Blocks.BEDROCK) || blockState.isOf(Blocks.SPAWNER)) {
            resetGoal(true);
            return;
        }
        World world = getWorld(mob);
        breakingTicks++;
        int originalProgress = blockDamageProgress.getOrDefault(breakingPos, 0);
        double progress;

        // Retrieve block hardness
        float blockHardness = getWorld(mob).getBlockState(breakingPos).getHardness(getWorld(mob), breakingPos);
        int adjustedBreakingTime = (int) (BREAKING_TIME * blockHardness);

        // Increase progress incrementally
        ////System.out.println("Breaking ticks: " + breakingTicks);
        progress = originalProgress + (int) ((breakingTicks / (float) adjustedBreakingTime) * 10);
        ////System.out.println("Progress: " + progress);
        world.setBlockBreakingInfo(mob.getId(), breakingPos, (int)progress);
        blockDamageProgress.put(breakingPos, (int)progress);
        if (blockState.getBlock() instanceof BlockEntityProvider) {
            BlockEntity blockEntity = world.getBlockEntity(breakingPos);
            //System.out.println("Block entity: " + blockEntity);
            if (blockEntity instanceof LootableContainerBlockEntity container) {
                // Drop all items from the container
                for (int i = 0; i < container.size(); i++) {
                    ItemStack stack = container.getStack(i);
                    //System.out.println("Checking stack: " + stack);
                    if (!stack.isEmpty()) {
                        //Block.dropStack(world, breakingPos, stack);
                    }
                }
                // Clear the container to ensure no residual items
                //container.clear();
            }
        }

        if (progress >= 10 / ConfigManager.getConfig().getMobBlockBreakSpeed()) {
            ////System.out.println("Breaking block at: " + breakingPos);
            if (blockState.getBlock() instanceof BlockEntityProvider) {
                BlockEntity blockEntity = world.getBlockEntity(breakingPos);
                //System.out.println("Block entity: " + blockEntity);
                if (blockEntity instanceof LootableContainerBlockEntity container) {
                    // Drop all items from the container
                    container.generateLoot(null); // Use the correct PlayerEntity if needed

                    for (int i = 0; i < container.size(); i++) {
                        ItemStack stack = container.getStack(i);
                        //System.out.println("Dropping stack: " + stack);
                        if (!stack.isEmpty()) {
                            Block.dropStack(world, breakingPos, stack);
                        }
                    }
                    // Clear the container to ensure no residual items
                    container.clear();
                }
            }

            boolean success = world.breakBlock(breakingPos, true, mob);
            ////System.out.println("Block broken: " + success);
            ////System.out.println("Is air: " + world.getBlockState(breakingPos).isAir());
            // Drop the block's items and container contents manually

            if (!success) {
                world.setBlockState(breakingPos, Blocks.AIR.getDefaultState(), 3);
            }
            blockDamageProgress.remove(breakingPos);
            breakingTicks = 0;
            if (MobPathTracker.breakingPosLiesInPath(breakingPos)) {
                if (baritone != null) {
                    pathingBehavior = baritone.getPathingBehavior();
                    baritone.getCustomGoalProcess().setGoalAndPath(getTargetGoal());
                }
            }
            if (savedPath != null) {
                for (BetterBlockPos betterBlockPos : savedPath) {
                    if (betterBlockPos.equals(placingPos)) {
                        int index = savedPath.indexOf(betterBlockPos);
                        if (index >= 0) {
                            savedPath.subList(0, index + 1).clear();
                        }
                        break;
                    }
                }
                if (savedPath.isEmpty()) {
                    savedPath = null;
                    MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
                }
            }
            resetGoal(false);
        }
    }

    @Override
    public boolean shouldContinue() {
        // Always continue if we have no target
        if (targetEntity == null) {
            return false;
        }

        // Continue if navigation is stuck far from target
        if (isNavigationTargetFarFromActualTarget()) {
            return true;
        }

        // Continue if we're actively breaking or placing
        if (breakingPos != null || placingPos != null) {
            return true;
        }

        // Continue if we can't reach the target directly
        if (!canReachTargetDirectly()) {
            return true;
        }

        // Continue if there are still blocks to break or place
        if (currentPath != null || savedPath != null) {
            if (currentPath == null) {
                currentPath = new ArrayList<>(savedPath);
                this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
            }
            return areSolidBlocksSeparatingPlayerFromMob();
        }

        return false;
    }

    public boolean areSolidBlocksSeparatingPlayerFromMob() {
        for (int i = 0; i < currentPath.size(); i++) {
            BetterBlockPos pos = currentPath.get(i);
            ////System.out.println("Checking block at: " + pos);
            BlockPos blockPos = new BlockPos(pos.x, pos.y, pos.z);
            if (isBreakable(blockPos) || isBreakable(blockPos.up()) || isPlacementNeeded(blockPos.down(), i)) {
                return true;
            } else {
                ////System.out.println("Check diagonals between positions.");
                if (i != currentPath.size() - 1) {
                    BetterBlockPos nextPos = currentPath.get(i + 1);
                    if (nextPos.x != pos.x && nextPos.z != pos.z) {
                        BlockPos diagonalBlockPos1 = new BlockPos(pos.x, pos.y, nextPos.z);
                        BlockPos diagonalBlockPos2 = new BlockPos(nextPos.x, pos.y, pos.z);
                        if ((isBreakable(diagonalBlockPos1) || isBreakable(diagonalBlockPos1.up())) && isSolidBlock(diagonalBlockPos1.down())) {
                            if (!isPlacementNeeded(diagonalBlockPos2.down(), i) && !isSolidBlock(diagonalBlockPos2) && !isSolidBlock(diagonalBlockPos2)) {
                                //Diagonal path already exists, none needed
                                continue;
                            }
                            return true;
                        }
                        if ((isBreakable(diagonalBlockPos2) || isBreakable(diagonalBlockPos2.up())) && isSolidBlock(diagonalBlockPos2.down())) {
                            if (!isPlacementNeeded(diagonalBlockPos1.down(), i) && !isSolidBlock(diagonalBlockPos1) && !isSolidBlock(diagonalBlockPos1)) {
                                //Diagonal path already exists, none needed
                                continue;
                            }
                            return true;
                        }
                    }
                    if (nextPos.y != pos.y) {
                        BlockPos twoBlocksUp;
                        if (nextPos.y < pos.y) {
                            for (int j = pos.y + 1; j >= nextPos.y + 1; j--) {
                                twoBlocksUp = new BlockPos(nextPos.x, j, nextPos.z);
                                if (isBreakable(twoBlocksUp)) {
                                    return true;
                                }
                            }
                        } else {
                            twoBlocksUp = new BlockPos(pos.x, pos.y + 2, pos.z);
                            if (isBreakable(twoBlocksUp)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean willObstructPlayer(BlockView world, BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }

        try {
            BlockState state = world.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(world, pos);
            return !shape.isEmpty() && !state.isOf(Blocks.COBWEB);
        } catch (NullPointerException e) {
            return isSolidBlock(pos); // Handle specific null-related issues
        } catch (Exception e) {
            e.printStackTrace(); // Log other exceptions for debugging purposes
            return false; // Return false if any unexpected exception occurs
        }
    }

    private void resetGoal(boolean removePath) {
        breakingTicks = 0;
        standingStillTicks = 0;
        generalStandingStillTicks = 0;
        placingPos = null;
        placingTargetPos = null;
        breakingPos = null;
        previousPos = null;
        pathRecalculationCooldown = 0;
        blockDamageProgress.clear();

        if (removePath) {
            // Full reset - release controls and clear all paths
            this.setControls(EnumSet.noneOf(Control.class));
            currentPath = null;
            savedPath = null;
            MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
        } else if (savedPath != null) {
            // Partial reset (e.g., after breaking a block) - keep the path and controls
            // Restore currentPath from savedPath to continue following it
            currentPath = new ArrayList<>(savedPath);
            // Re-find the next breaking/placing block on the path
            findBreakingOrPlacingBlock();
        } else {
            currentPath = null;
        }
    }

    public boolean hasBreakingPos() {
        return breakingPos != null;
    }

    public boolean hasPlacingPos() {
        return placingPos != null;
    }

    public double getDistanceToPlacingPos() {
        if (placingPos != null) {
            return mob.getBlockPos().getManhattanDistance(new BlockPos(placingPos.getX(), placingPos.getY(), placingPos.getZ()));
        }
        return -1;
    }

    public GoalBlock getTargetGoal() {
        if (this.targetEntity != null) {
            BlockPos targetPos = targetEntity.getBlockPos();
            GoalBlock goal = new GoalBlock(targetPos.getX(), targetPos.getY(), targetPos.getZ());
            //Check if block underneath player is air and if so set goal to one of the adjacent blocks thats over a solid block.
            if (mob.getEntityWorld().getBlockState(targetPos.down()).isAir()) {
                for (Direction direction : Direction.Type.HORIZONTAL) {
                    BlockPos adjacentPos = targetPos.offset(direction);
                    if (isSolidBlock(adjacentPos.down())) {
                        goal = new GoalBlock(adjacentPos.getX(), adjacentPos.getY(), adjacentPos.getZ());
                        break;
                    }
                }
            }
            return goal;
        }
        return null;
    }

    public void navigateMobToTargetPos(BlockPos targetPos) {
        this.setControls(EnumSet.of(Control.MOVE));

        // Clear previous target block before setting new one
        ClientRenderedBlockUpdateServiceImpl.clearTargetBlock(mob.getId());

        if (getWorld(mob).getBlockState(targetPos).isOf(Blocks.WATER)) {
            mob.getNavigation().startMovingTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.0);
            ClientRenderedBlockUpdateServiceImpl.renderTargetBlock(mob.getId(), targetPos);
        }
        if (!isSolidBlock(targetPos) && !isSolidBlock(targetPos.up()) && !isPlacementNeeded(targetPos.down(), -1)) {
            mob.getNavigation().startMovingTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.0);
            ClientRenderedBlockUpdateServiceImpl.renderTargetBlock(mob.getId(), targetPos);
            return;
        } else {
            if (isSolidBlock(targetPos)) {
                if (!isSolidBlock(targetPos.up())) {
                    BlockPos adjacentPos = targetPos.up();
                    mob.getNavigation().startMovingTo(targetPos.getX(), targetPos.getY() + 1, targetPos.getZ(), 1.0);
                    ClientRenderedBlockUpdateServiceImpl.renderTargetBlock(mob.getId(), adjacentPos);
                    return;
                }
            }
            if (isPlacementNeeded(targetPos.down(), -1)) {
                for (Direction direction : Direction.Type.HORIZONTAL) {
                    BlockPos adjacentPos = targetPos.up().offset(direction);
                    if (isSolidBlock(adjacentPos.down()) && !isSolidBlock(adjacentPos) && !isSolidBlock(adjacentPos.up())) {
                        mob.getNavigation().startMovingTo(adjacentPos.getX(), adjacentPos.getY(), adjacentPos.getZ(), 1.0);
                        ClientRenderedBlockUpdateServiceImpl.renderTargetBlock(mob.getId(), adjacentPos);
                        return;
                    }
                }
                for (Direction direction : Direction.Type.HORIZONTAL) {
                    BlockPos adjacentPos = targetPos.offset(direction);
                    if (isSolidBlock(adjacentPos.down()) && !isSolidBlock(adjacentPos) && !isSolidBlock(adjacentPos.up())) {
                        mob.getNavigation().startMovingTo(adjacentPos.getX(), adjacentPos.getY(), adjacentPos.getZ(), 1.0);
                        ClientRenderedBlockUpdateServiceImpl.renderTargetBlock(mob.getId(), adjacentPos);
                        return;
                    }
                }
                for (Direction direction : Direction.Type.HORIZONTAL) {
                    BlockPos downAdjacentPos = targetPos.offset(direction).down();
                    if (isSolidBlock(downAdjacentPos.down()) && !isSolidBlock(downAdjacentPos) && !isSolidBlock(downAdjacentPos.up())) {
                        mob.getNavigation().startMovingTo(downAdjacentPos.getX(), downAdjacentPos.getY(), downAdjacentPos.getZ(), 1.0);
                        ClientRenderedBlockUpdateServiceImpl.renderTargetBlock(mob.getId(), downAdjacentPos);
                        return;
                    }
                }
            }
        }
        mob.getNavigation().startMovingTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.0);
        ClientRenderedBlockUpdateServiceImpl.renderTargetBlock(mob.getId(), targetPos);
    }

    /**
     * Validates the current navigation target and updates it if blocked.
     * Called after placing a block to ensure the mob doesn't get stuck navigating to a now-solid position.
     */
    private void validateAndUpdateNavigationTarget() {
        Path currentNavPath = mob.getNavigation().getCurrentPath();
        if (currentNavPath == null || currentNavPath.isFinished()) {
            return;
        }

        BlockPos navTarget = currentNavPath.getTarget();
        if (navTarget == null) {
            return;
        }

        // Check if the navigation target is now inside a solid block
        if (isSolidBlock(navTarget)) {
            BlockPos validPos = findValidAdjacentPosition(navTarget);
            if (validPos != null) {
                mob.getNavigation().startMovingTo(validPos.getX(), validPos.getY(), validPos.getZ(), 1.0);
                ClientRenderedBlockUpdateServiceImpl.renderTargetBlock(mob.getId(), validPos);
            }
        }
    }

    /**
     * Finds a valid position adjacent to or above a blocked position.
     * Returns a position that is not solid and has space above it.
     */
    private BlockPos findValidAdjacentPosition(BlockPos blockedPos) {
        // First try directly above
        BlockPos abovePos = blockedPos.up();
        if (!isSolidBlock(abovePos) && !isSolidBlock(abovePos.up())) {
            return abovePos;
        }

        // Try horizontal adjacent positions at same level
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos adjacentPos = blockedPos.offset(direction);
            if (!isSolidBlock(adjacentPos) && !isSolidBlock(adjacentPos.up()) && isSolidBlock(adjacentPos.down())) {
                return adjacentPos;
            }
        }

        // Try horizontal adjacent positions one level up
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos adjacentPos = blockedPos.up().offset(direction);
            if (!isSolidBlock(adjacentPos) && !isSolidBlock(adjacentPos.up()) && isSolidBlock(adjacentPos.down())) {
                return adjacentPos;
            }
        }

        return null;
    }

    private void disableOtherGoals() {
        // Get the goal selector using the accessor
        MobEntityAccessor accessor = (MobEntityAccessor) mob;
        GoalSelector goalSelector = accessor.getGoalSelector();

        //System.out.println("Disabling other goals for mob " + mob.getId() + " (type: " + mob.getClass().getSimpleName() + ")");
        //System.out.println("Total goals found: " + goalSelector.getGoals().size());

        // Disable all goals except this one
        for (PrioritizedGoal prioritizedGoal : goalSelector.getGoals()) {
            Goal goal = prioritizedGoal.getGoal();

            // Skip if this is our own goal
            if (goal == this) {
                //System.out.println("Skipping own goal: " + goal.getClass().getSimpleName());
                continue;
            }

            // Check if goal has any controls
            if (prioritizedGoal.getControls().isEmpty()) {
                //System.out.println("Goal " + goal.getClass().getSimpleName() + " has no controls to disable");
                continue;
            }

            // Disable all controls for other goals
            //System.out.println("Disabling controls for goal: " + goal.getClass().getSimpleName() + ", controls: " + prioritizedGoal.getControls());
            for (Goal.Control control : prioritizedGoal.getControls()) {
                goalSelector.disableControl(control);
            }
            temporarilyDisabledGoals.add(prioritizedGoal);
            //System.out.println("Disabled goal: " + goal.getClass().getSimpleName() + " for mob " + mob.getId());
        }

        //System.out.println("Total goals disabled: " + temporarilyDisabledGoals.size());
    }

    private void enableOtherGoals() {
        // Get the goal selector using the accessor
        MobEntityAccessor accessor = (MobEntityAccessor) mob;
        GoalSelector goalSelector = accessor.getGoalSelector();

        // Re-enable all temporarily disabled goals
        for (PrioritizedGoal prioritizedGoal : temporarilyDisabledGoals) {
            for (Goal.Control control : prioritizedGoal.getControls()) {
                goalSelector.enableControl(control);
            }
            //System.out.println("Re-enabled attack goal: " + prioritizedGoal.getGoal().getClass().getSimpleName() + " for mob " + mob.getId());
        }
        temporarilyDisabledGoals.clear();
    }
}
