package net.fabricmc.example.mobai;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.MinecraftServerUtil;
import net.fabricmc.example.client.block.ClientRenderedBlockUpdateServiceImpl;
import net.fabricmc.example.config.ConfigManager;
import net.fabricmc.example.mobai.tracker.MobPathTracker;
import net.fabricmc.example.service.MobitoneServiceImpl;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.*;

public class BreakPlaceAndChaseGoal extends Goal {
    private final PathAwareEntity mob;
    private BlockPos previousPos;
    private Entity targetEntity;
    private IPathingBehavior pathingBehavior;
    private List<BetterBlockPos> currentPath;
    private int breakingTicks;
    private int standingStillTicks = 0;
    private BlockPos breakingPos;
    private BlockPos placingPos;
    private BlockPos placingTargetPos;
    private static final int BREAKING_TIME = 50; // Faster breaking time in ticks (2.5 seconds)
    private final Map<BlockPos, Integer> blockDamageProgress = new HashMap<>();
    private List<BetterBlockPos> savedPath;

    public BreakPlaceAndChaseGoal(PathAwareEntity mob) {
        this.mob = mob;
        BaritoneAPI.getSettings().allowParkour.value = false;
        BaritoneAPI.getSettings().allowJumpAt256.value = false;
        BaritoneAPI.getSettings().allowParkourAscend.value = false;
        BaritoneAPI.getSettings().allowParkourPlace.value = false;
        BaritoneAPI.getSettings().avoidance.value = false;
        BaritoneAPI.getSettings().assumeExternalAutoTool.value = true; // Assume tool is externally managed
        BaritoneAPI.getSettings().assumeWalkOnWater.value = false;
        BaritoneAPI.getSettings().walkOnWaterOnePenalty.value = 5.0D;
        savedPath = null;
        MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
    }

    @Override
    public boolean canStart() {
        if (mob.getTarget() != null && mob.getTarget() instanceof PlayerEntity) {
            targetEntity = mob.getTarget();
            boolean withinRange = mob.getBlockPos().isWithinDistance(targetEntity.getBlockPos(), 100)
                    && Math.abs(mob.getBlockPos().getY() - targetEntity.getBlockPos().getY()) < 50;
            boolean skeletonSpecificTrigger = false;
            if ((!mob.isNavigating() || isEntityStuckInDesignatedGlitchBlock(mob)) && withinRange) {
                if (mob instanceof SkeletonEntity) {
                    Path path = mob.getNavigation().findPathTo(mob.getTarget(), 0);
                    if (path == null || path.isFinished()) {
                        skeletonSpecificTrigger = true;
                    }
                }
                boolean canStart = (!mob.isAttacking() || skeletonSpecificTrigger);
                return canStart;
            }
        }
        return false;
    }

    @Override
    public void start() {
        //calculatePath();
    }

    private void calculatePath() {
        //System.out.println("Calculating path.");
        if (this.targetEntity != null) {
            GoalBlock goal = getTargetGoal();
            if (ConfigManager.getConfig().isOptimizedMobitone()) {
                if (savedPath == null) {
                    MobitoneServiceImpl.addMobitone(mob);
                }
            }
            IBaritone goalBaritone = BaritoneAPI.getProvider().getBaritoneForEntity(mob);
            if (goalBaritone != null) {
                pathingBehavior = goalBaritone.getPathingBehavior();
                goalBaritone.getCustomGoalProcess().setGoalAndPath(goal);
                if (goalBaritone.getPathingBehavior().getCurrent() != null) {
                    currentPath = goalBaritone.getPathingBehavior().getCurrent().getPath().positions();
                    breakingPos = null;
                    placingPos = null;
                    placingTargetPos = null;
                    findBreakingOrPlacingBlock();
                } else {
                    currentPath = null;
                    //System.out.println("Failed to calculate path.");
                }
            } else if (ConfigManager.getConfig().isOptimizedMobitone() && savedPath != null) {
                currentPath = new ArrayList<>(savedPath);
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
        if (getWorld(mob).getBlockState(blockPos).isOf(Blocks.WATER)) {
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
        if (currentPath != null) {
            BetterBlockPos destination = currentPath.get(currentPath.size() - 1);
            List<BetterBlockPos> positions = currentPath;
            for (int i = 0; i < positions.size(); i++) {
                BlockPos targetBlockPos;
                if (i == 0) {
                    targetBlockPos = mob.getBlockPos();
                } else {
                    targetBlockPos = new BlockPos(positions.get(i - 1).x, positions.get(i - 1).y, positions.get(i - 1).z);
                }
                BetterBlockPos pos = positions.get(i);
                //System.out.println("Checking block at: " + pos);
                if (isBreakable(pos) || isBreakable(pos.up())) {
                    breakingPos = isBreakable(pos) ? pos : pos.up();
                    BlockPos adjacentPos;
                    if (i == 0) {
                        adjacentPos = mob.getBlockPos();
                    } else {
                        adjacentPos = positions.get(i - 1);
                    }
                    if (adjacentPos != null) {
                        navigateMobToTargetPos(adjacentPos);
                        return;
                    }
                    savedPath = new ArrayList<>(currentPath);
                    MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
                    MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
                    if (ConfigManager.getConfig().isOptimizedMobitone()) {
                        MobitoneServiceImpl.removeMobitone(mob);
                    }
                } else {
                    //System.out.println("Check diagonals between positions.");
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
                                    navigateMobToTargetPos(pos);
                                    savedPath = new ArrayList<>(currentPath);
                                    MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
                                    if (ConfigManager.getConfig().isOptimizedMobitone()) {
                                        MobitoneServiceImpl.removeMobitone(mob);
                                    }
                                    return;
                                }
                            }
                            if (isBreakable(diagonalBlockPos2) || isBreakable(diagonalBlockPos2.up()) && !isPlacementNeeded(diagonalBlockPos2.down(), i)) {
                                if ((!isPlacementNeeded(diagonalBlockPos1.down(), i) || !isPlacementNeeded(diagonalBlockPos2.down(2), i)) && !isSolidBlock(diagonalBlockPos1) && !isSolidBlock(diagonalBlockPos1)) {
                                    //Diagonal path already exists, none needed
                                    //continue;
                                } else {
                                    breakingPos = isBreakable(diagonalBlockPos2) ? diagonalBlockPos2 : diagonalBlockPos2.up();
                                    navigateMobToTargetPos(pos);
                                    savedPath = new ArrayList<>(currentPath);
                                    MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
                                    if (ConfigManager.getConfig().isOptimizedMobitone()) {
                                        MobitoneServiceImpl.removeMobitone(mob);
                                    }
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
                                        break;
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
                                if (ConfigManager.getConfig().isOptimizedMobitone()) {
                                    MobitoneServiceImpl.removeMobitone(mob);
                                }
                                return;
                            }
                        }

                        //Now to check if placement needed:
                        BetterBlockPos floorUnderBlockPos = new BetterBlockPos(pos.x, pos.y - 1, pos.z);
                        if (mob.getMainHandStack().getItem() instanceof BlockItem) {
                            if (isPlacementNeeded(floorUnderBlockPos, i) && !isSolidBlock(floorUnderBlockPos) && !nextPos.equals(floorUnderBlockPos)) {
                                // Ensure breakingPos is null
                                if (!hasAdjacentBlockIncludingBelow(floorUnderBlockPos)) {
                                    placingPos = floorUnderBlockPos.down();
                                } else {
                                    placingPos = floorUnderBlockPos;
                                }
                                BlockPos placingPosRender = new BlockPos(placingPos.getX(), placingPos.getY(), placingPos.getZ());
                                ClientRenderedBlockUpdateServiceImpl.renderPlacingBlock(mob.getId(), placingPosRender);
                                breakingPos = null; // Ensure breakingPos is null
                                placingTargetPos = findSuitableAdjacentBlock(placingPos);
                                navigateMobToTargetPos(placingPos);
                                savedPath = new ArrayList<>(currentPath);
                                MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
                                if (ConfigManager.getConfig().isOptimizedMobitone()) {
                                    MobitoneServiceImpl.removeMobitone(mob);
                                }
                                return;
                            } else {
                                if (nextPos.x != pos.x && nextPos.z != pos.z) {
                                    BlockPos diagonalBlockPos1 = new BlockPos(pos.x, pos.y, nextPos.z);
                                    BlockPos diagonalBlockPos2 = new BlockPos(nextPos.x, pos.y, pos.z);
                                    if ((!isSolidBlock(diagonalBlockPos1) && !isSolidBlock(diagonalBlockPos1.up()) && isPlacementNeeded(diagonalBlockPos1.down(), i))) {
                                        if ((!isSolidBlock(diagonalBlockPos2) || !isSolidBlock(diagonalBlockPos2.up())) && isSolidBlock(diagonalBlockPos2.down())) {
                                            //Diagonal path already exists, none needed
                                            //continue;
                                        } else {
                                            placingPos = diagonalBlockPos1.down();
                                            BlockPos placingPosRender = new BlockPos(placingPos.getX(), placingPos.getY(), placingPos.getZ());
                                            ClientRenderedBlockUpdateServiceImpl.renderPlacingBlock(mob.getId(), placingPosRender);
                                            breakingPos = null; // Ensure breakingPos is null
                                            placingTargetPos = findSuitableAdjacentBlock(placingPos);
                                            navigateMobToTargetPos(placingPos);
                                            savedPath = new ArrayList<>(currentPath);
                                            MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
                                            if (ConfigManager.getConfig().isOptimizedMobitone()) {
                                                MobitoneServiceImpl.removeMobitone(mob);
                                            }
                                        }
                                    }
                                    if (!isSolidBlock(diagonalBlockPos2) || !isSolidBlock(diagonalBlockPos2.up()) && isPlacementNeeded(diagonalBlockPos2.down(), i)) {
                                        if ((!isSolidBlock(diagonalBlockPos1) || !isSolidBlock(diagonalBlockPos1.down())) && isSolidBlock(diagonalBlockPos1.down())) {
                                            //Diagonal path already exists, none needed
                                            //continue;
                                        } else {
                                            placingPos = diagonalBlockPos2.down();
                                            BlockPos placingPosRender = new BlockPos(placingPos.getX(), placingPos.getY(), placingPos.getZ());
                                            ClientRenderedBlockUpdateServiceImpl.renderPlacingBlock(mob.getId(), placingPosRender);
                                            breakingPos = null; // Ensure breakingPos is null
                                            placingTargetPos = findSuitableAdjacentBlock(placingPos);
                                            navigateMobToTargetPos(placingPos);
                                            savedPath = new ArrayList<>(currentPath);
                                            MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
                                            if (ConfigManager.getConfig().isOptimizedMobitone()) {
                                                MobitoneServiceImpl.removeMobitone(mob);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    //
                }
            }
            if (ConfigManager.getConfig().isOptimizedMobitone()) {
                MobitoneServiceImpl.addMobitone(mob);
                IBaritone goalBaritone = BaritoneAPI.getProvider().getBaritoneForEntity(mob);
                if (goalBaritone != null) {
                    pathingBehavior = goalBaritone.getPathingBehavior();
                    goalBaritone.getCustomGoalProcess().setGoalAndPath(getTargetGoal());
                }
            }
        }
        //System.out.println("No block to break found in the path.");
    }

    private BlockPos findSuitableAdjacentBlock(BlockPos blockPos) {
        if (getWorld(mob).getBlockState(blockPos).isSolidBlock(getWorld(mob), blockPos) && !getWorld(mob).getBlockState(blockPos.up()).isSolidBlock(getWorld(mob), blockPos.up())) {
            return blockPos.up();
        }
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos adjacentPos = blockPos.offset(direction);
            if (getWorld(mob).getBlockState(adjacentPos.down()).isSolidBlock(getWorld(mob), adjacentPos) && !isSolidBlock(adjacentPos) && !isSolidBlock(adjacentPos.up())) {
                if (!getWorld(mob).getBlockState(adjacentPos).isOf(Blocks.WATER)) {
                    return adjacentPos;
                }
            }
        }
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos downAdjacentPos = blockPos.offset(direction).down();
            if (getWorld(mob).getBlockState(downAdjacentPos.down()).isSolidBlock(getWorld(mob), downAdjacentPos) && !isSolidBlock(downAdjacentPos) && !isSolidBlock(downAdjacentPos.up())) {
                if (!getWorld(mob).getBlockState(downAdjacentPos).isOf(Blocks.WATER)) {
                    return downAdjacentPos;
                }
            }
        }
        return null;
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
        return MinecraftServerUtil.getMinecraftServer().getWorld(mob.getWorld().getRegistryKey());
    }

    private boolean isBreakable(BlockPos blockPos) {
        BlockState blockState = getWorld(mob).getBlockState(blockPos);
        if (blockState.isOf(Blocks.BEDROCK) || blockState.isOf(Blocks.OBSIDIAN) || blockState.isOf(Blocks.SPAWNER)) {
            return false;
        }
        //System.out.println("block state: " + getWorld(mob).getBlockState(blockPos));
        if (pathingBehavior != null && pathingBehavior.getCurrent() != null) {
            IPathExecutor current = pathingBehavior.getCurrent(); // this should prevent most race conditions?
            Set<BlockPos> blocksToBreak = current.toBreak();
            //System.out.println("Blocks to break size: " + blocksToBreak.size());
            for (BlockPos pos : blocksToBreak) {
                if (pos.equals(blockPos)) {
                    for (BlockPos pos2 : current.toPlace()) {
                        if (pos2.equals(blockPos)){
                            return false;
                        }
                    }
                    //System.out.println("Blocks to break contains this block");
                    return true;
                }
                //System.out.println("Block to break: " + pos);
            }
        }
        return blockState.isSolidBlock(getWorld(mob), blockPos) || willObstructPlayer(getWorld(mob), blockPos) || isObstructiveNonQualifyingSolidBlock(blockPos);
    }

    private boolean isSolidBlock(BlockPos blockPos) {
        //System.out.println("block state: " + getWorld(mob).getBlockState(blockPos));
        /*if (pathingBehavior != null && pathingBehavior.getCurrent() != null) {
            IPathExecutor current = pathingBehavior.getCurrent(); // this should prevent most race conditions?
            Set<BlockPos> blocksToBreak = current.toBreak();
            Set<BlockPos> blocksToWalkInto = current.toWalkInto();
            //System.out.println("Blocks to break size: " + blocksToBreak.size());
            for (BlockPos pos : blocksToBreak) {
                if (pos.equals(blockPos)) {
                    //System.out.println("Blocks to break contains this block");
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
                || block == Blocks.CHAIN
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

    private void moveToBlock(MobEntity mob, BlockPos targetPos) {
        Vec3d mobPos = mob.getPos();
        Vec3d targetVec = new Vec3d(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        Vec3d direction = targetVec.subtract(mobPos).normalize().multiply(0.1); // Adjust the multiplier for smoothness

        mob.setVelocity(direction);
    }

    @Override
    public void tick() {
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
                if (isADesignatedGlitchBlock(feetPos, mob.getWorld())) {
                    breakingPos = feetPos;
                } else if (isADesignatedGlitchBlock(headPos, mob.getWorld())) {
                    breakingPos = headPos;
                } else if (isADesignatedGlitchBlock(facingFeetPos, mob.getWorld())) {
                    breakingPos = facingFeetPos;
                } else if (isADesignatedGlitchBlock(facingHeadPos, mob.getWorld())) {
                    breakingPos = facingHeadPos;
                }
            }
            if (previousPos == null) {
                previousPos = mob.getBlockPos();
            }
            if (breakingPos != null) {
                this.setControls(EnumSet.of(Control.MOVE));
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
                    if (standingStillTicks > 200) {
                        System.out.println("Mob: " + mob.getId() + "Standing still for too long while breaking block.");
                        resetGoal(true);
                        return;
                    }
                }
                if (mob.getBlockPos().isWithinDistance(breakingPos, 4.5)) {
                    //System.out.println("Block is within distance to break.");
                    continueBreakingBlock();
                } else {
                    if (previousPos == null) {
                        previousPos = mob.getBlockPos();
                    }
                    navigateMobToTargetPos(breakingPos);
                    /*if (mob.getBlockPos().getManhattanDistance(previousPos) > 2) {
                        previousPos = mob.getBlockPos();
                        standingStillTicks = 0;
                    } else {
                        System.out.println("Mob: " + mob.getId() + "Standing still ticks: " + standingStillTicks);
                        standingStillTicks++;
                    }
                    if (standingStillTicks> 200 || !isSolidBlock(breakingPos))
                    {
                        System.out.println("Mob: " + mob.getId() + "Standing still for too long.");
                        resetGoal(true);
                    }*/
                }
                //System.out.println("Block is not within distance to break. Moving to block.");
                //System.out.println("Distance: " + mob.getBlockPos().getManhattanDistance(breakingPos));
            } else if (placingPos != null) {
                this.setControls(EnumSet.of(Control.MOVE));
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
                    if (standingStillTicks > 200) {
                        System.out.println("Mob: " + mob.getId() + "Standing still for too long while placing block.");
                        resetGoal(true);
                        return;
                    }
                }
                if (mob.getBlockPos().isWithinDistance(placingPos, 5.5)) {
                    // System.out.println("Block is within distance to place.");
                    placeBlock();
                } else {
                    //mob.getNavigation().startMovingTo(placingPos
                    //.getX(), placingPos.getY(), placingPos.getZ(), 1.0);
                    // System.out.println("Block is not within distance to place. Moving to block.");
                    // System.out.println("Distance: " + mob.getBlockPos().getManhattanDistance(placingPos));
                    //find the nearest block 1 block adjacent or 1 block adjacent and lower with a solid ground:
                    // is placing pos down
                    if (previousPos == null) {
                        previousPos = mob.getBlockPos();
                    }
                    navigateMobToTargetPos(placingPos);
                    /*If mob moved more than 2 blocks away from previous pos:
                    if (mob.getBlockPos().getManhattanDistance(previousPos) > 2) {
                        previousPos = mob.getBlockPos();
                        standingStillTicks = 0;
                    } else {
                        System.out.println("Mob: " + mob.getId() + "Standing still ticks: " + standingStillTicks);
                        standingStillTicks++;
                    }
                    if (standingStillTicks > 200 || isSolidBlock(placingPos)) {
                        System.out.println("Mob: " + mob.getId() + "Standing still for too long.");
                        resetGoal(true);
                    }*/
                }
                //If mob moved more than 2 blocks away from previous pos:
            } else {
                calculatePath();
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

        boolean isFeetStalagmite = isADesignatedGlitchBlock(feetPos, entity.getWorld());
        boolean isHeadStalagmite = isADesignatedGlitchBlock(headPos, entity.getWorld());
        boolean isFeetFacingStalagmite = isADesignatedGlitchBlock(facingPos, entity.getWorld());
        boolean isHeadFacingStalagmite = isADesignatedGlitchBlock(facingHeadPos, entity.getWorld());

        return (isFeetStalagmite || isHeadStalagmite || isFeetFacingStalagmite || isHeadFacingStalagmite) && isEntityNotMoving(entity);
    }


    private boolean isADesignatedGlitchBlock(BlockPos pos, World world) {
        BlockState blockState = world.getBlockState(pos);
        return blockState.isOf(Blocks.POINTED_DRIPSTONE) // Stalagmite
                || blockState.isOf(Blocks.END_ROD) // End Rod
                || blockState.isOf(Blocks.CHAIN) // Chain
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
            if (ConfigManager.getConfig().isOptimizedMobitone() && savedPath != null) {
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
                MobitoneServiceImpl.addMobitone(mob);
                IBaritone goalBaritone = BaritoneAPI.getProvider().getBaritoneForEntity(mob);
                if (goalBaritone != null) {
                    pathingBehavior = goalBaritone.getPathingBehavior();
                    goalBaritone.getCustomGoalProcess().setGoalAndPath(getTargetGoal());
                }
            }
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
        //System.out.println("Breaking ticks: " + breakingTicks);
        progress = originalProgress + (int) ((breakingTicks / (float) adjustedBreakingTime) * 10);
        //System.out.println("Progress: " + progress);
        world.setBlockBreakingInfo(mob.getId(), breakingPos, (int)progress);
        blockDamageProgress.put(breakingPos, (int)progress);

        if (progress >= 10 / ConfigManager.getConfig().getMobBlockBreakSpeed()) {
            //System.out.println("Breaking block at: " + breakingPos);
            boolean success = world.breakBlock(breakingPos, true, mob);
            //System.out.println("Block broken: " + success);
            //System.out.println("Is air: " + world.getBlockState(breakingPos).isAir());
            if (!success) {
                world.setBlockState(breakingPos, Blocks.AIR.getDefaultState(), 3);
            }
            blockDamageProgress.remove(breakingPos);
            breakingTicks = 0;
            if (MobPathTracker.breakingPosLiesInPath(breakingPos)) {
                MobitoneServiceImpl.addMobitone(mob);
                IBaritone goalBaritone = BaritoneAPI.getProvider().getBaritoneForEntity(mob);
                if (goalBaritone != null) {
                    pathingBehavior = goalBaritone.getPathingBehavior();
                    goalBaritone.getCustomGoalProcess().setGoalAndPath(getTargetGoal());
                }
            }
            if (ConfigManager.getConfig().isOptimizedMobitone() && savedPath != null) {
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
        if (currentPath != null || savedPath != null) {
            if (currentPath == null) {
                currentPath = new ArrayList<>(savedPath);
            }
            return areSolidBlocksSeparatingPlayerFromMob();
        }
        return true;
    }

    public boolean areSolidBlocksSeparatingPlayerFromMob() {
        for (int i = 0; i < currentPath.size(); i++) {
            BetterBlockPos pos = currentPath.get(i);
            //System.out.println("Checking block at: " + pos);
            BlockPos blockPos = new BlockPos(pos.x, pos.y, pos.z);
            if (isBreakable(blockPos) || isBreakable(blockPos.up()) || isPlacementNeeded(blockPos.down(), i)) {
                return true;
            } else {
                //System.out.println("Check diagonals between positions.");
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

    @Override
    public void stop() {
        resetGoal(true);
    }

    private void resetGoal(boolean removePath) {
        this.setControls(EnumSet.noneOf(Control.class));
        currentPath = null;
        breakingTicks = 0;
        standingStillTicks = 0;
        placingPos = null;
        placingTargetPos = null;
        breakingPos = null;
        previousPos = null;
        mob.getNavigation().stop();
        if (ConfigManager.getConfig().isOptimizedMobitone()) {
            //MobitoneServiceImpl.removeMobitone(mob);
            if (removePath) {
                savedPath = null;
                MobPathTracker.updatePath(mob.getUuidAsString(), savedPath);
            }
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
}
