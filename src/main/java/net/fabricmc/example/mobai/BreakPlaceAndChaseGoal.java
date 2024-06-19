package net.fabricmc.example.mobai;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.MinecraftServerUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;

public class BreakPlaceAndChaseGoal extends Goal {
    private final PathAwareEntity mob;
    private BlockPos previousPos;
    private PlayerEntity targetPlayer;
    private IPathingBehavior pathingBehavior;
    private IPath currentPath;
    private int breakingTicks;
    private int standingStillTicks = 0;
    private BlockPos breakingPos;
    private BlockPos placingPos;
    private BlockPos placingTargetPos;
    private BlockPos walkingTargetPos;
    private static final int BREAKING_TIME = 50; // Faster breaking time in ticks (2.5 seconds)
    private final Map<BlockPos, Integer> blockDamageProgress = new HashMap<>();

    public BreakPlaceAndChaseGoal(PathAwareEntity mob) {
        this.mob = mob;
        BaritoneAPI.getSettings().allowBreak.value = true;
        BaritoneAPI.getSettings().allowPlace.value = true;
        BaritoneAPI.getSettings().allowParkour.value = false;
        BaritoneAPI.getSettings().allowJumpAt256.value = false;
        BaritoneAPI.getSettings().allowParkourAscend.value = false;
        BaritoneAPI.getSettings().allowParkourPlace.value = false;
        BaritoneAPI.getSettings().avoidance.value = false;
        BaritoneAPI.getSettings().assumeExternalAutoTool.value = true; // Assume tool is externally managed
        BaritoneAPI.getSettings().renderPath.value = true;
        BaritoneAPI.getSettings().renderSelectionBoxes.value = true;
        BaritoneAPI.getSettings().renderGoal.value = true;
        BaritoneAPI.getSettings().renderCachedChunks.value = true;
        BaritoneAPI.getSettings().renderSelectionCorners.value = true;
        BaritoneAPI.getSettings().renderGoalAnimated.value = true;
        BaritoneAPI.getSettings().renderPathAsLine.value = true;
        BaritoneAPI.getSettings().renderGoalXZBeacon.value = false;
        BaritoneAPI.getSettings().assumeWalkOnWater.value = false;
        BaritoneAPI.getSettings().walkOnWaterOnePenalty.value = 23D;
        BaritoneAPI.getSettings().blockPlacementPenalty.value = 3.0D;
    }

    @Override
    public boolean canStart() {
        if (mob.getTarget() != null && mob.getTarget() instanceof PlayerEntity) {
            targetPlayer = (PlayerEntity) mob.getTarget();
            boolean withinRange = mob.getBlockPos().isWithinDistance(targetPlayer.getBlockPos(), 100)
                    && Math.abs(mob.getBlockPos().getY() - targetPlayer.getBlockPos().getY()) < 50;
            return !mob.isAttacking() && !mob.isNavigating() && withinRange;
        }
        return false;
    }

    @Override
    public void start() {
        System.out.println("#################### GOAL Triggered: + " + mob.getUuid());
        //calculatePath();
    }

    private void calculatePath() {
        System.out.println("Calculating path.");
        GoalBlock goal = null;
        if (this.targetPlayer != null) {
            System.out.println("Target player isnt null.");
            BlockPos targetPos = targetPlayer.getBlockPos();
            goal = new GoalBlock(targetPos.getX(), targetPos.getY(), targetPos.getZ());
            //Check if block underneath player is air and if so set goal to one of the adjacent blocks thats over a solid block.
            if (mob.getEntityWorld().getBlockState(targetPos.down()).isAir()) {
                for (Direction direction : Direction.Type.HORIZONTAL) {
                    BlockPos adjacentPos = targetPos.offset(direction);
                    if (mob.getEntityWorld().getBlockState(adjacentPos.down()).isSolidBlock(mob.getEntityWorld(), adjacentPos.down())) {
                        goal = new GoalBlock(adjacentPos.getX(), adjacentPos.getY(), adjacentPos.getZ());
                        break;
                    }
                }
            }
            System.out.println("Goal: " + goal);
        }
        IBaritone goalBaritone = BaritoneAPI.getProvider().getBaritoneForEntity(mob);
        if (goalBaritone != null) {
            System.out.println("Goal baritone isnt null.");
            pathingBehavior = goalBaritone.getPathingBehavior();
            if (goalBaritone.getPathingBehavior().getCurrent() == null) {
                goalBaritone.getCustomGoalProcess().setGoalAndPath(goal);
                currentPath = null;
            } else {
                System.out.println("Current path set");
                currentPath = goalBaritone.getPathingBehavior().getCurrent().getPath();
                breakingPos = null;
                placingPos = null;
                placingTargetPos = null;
                //System.out.println("Failed to calculate path.");
            }
        }
        System.out.println("Current path: " + currentPath);
        if (currentPath != null) {
            findBreakingOrPlacingBlock();
        }
    }

    private boolean isPlacementNeeded(BlockPos blockPos, int pathIndex) {
        System.out.println("Checking if placement needed: " + blockPos);
        if (getWorld(mob).getBlockState(blockPos).isAir()) {
            return true;
        }
       if (getWorld(mob).getBlockState(blockPos).isOf(Blocks.WATER)) {
           if (!getWorld(mob).getBlockState(blockPos.up()).isAir()) {
               return false;
           }
           System.out.println("Block is water");
            if (pathIndex == currentPath.length() - 1) {
                return false;
            }
            System.out.println("Checking if block is water and next block is higher");
            System.out.println("Current path: " + currentPath.positions().get(pathIndex));
            System.out.println("Next path: " + currentPath.positions().get(pathIndex + 1));
            System.out.println("Block pos equals pos down: " + blockPos.equals(currentPath.positions().get(pathIndex).down()));
            boolean isNextPosHigher = currentPath.positions().get(pathIndex + 1).getY() > currentPath.positions().get(pathIndex).getY();
            System.out.println("Next pos is higher: " + isNextPosHigher);
            System.out.println("Block below next pos is not water: " + !getWorld(mob).getBlockState(currentPath.positions().get(pathIndex + 1).down()).isOf(Blocks.WATER));
            BlockPos pos = currentPath.positions().get(pathIndex);
            BlockPos nextPos = currentPath.positions().get(pathIndex + 1);
            return blockPos.equals(pos.down()) && nextPos.getY() > pos.getY() && !getWorld(mob).getBlockState(nextPos.down()).isOf(Blocks.WATER);
        }
        return false;
    }

    private void findBreakingOrPlacingBlock() {
        System.out.println("Finding breaking or placing block for mob: " + mob.getUuid());
        System.out.println("Goal: " + pathingBehavior.getGoal());
        if (currentPath != null) {
            List<BetterBlockPos> positions = currentPath.positions();
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
                    System.out.println("Breaking pos identified 1 for mob: " + mob.getUuid() +  ": " + placingPos);
                    //System.out.println("Identified block to break at: " + breakingPos);
                    BlockPos adjacentPos;
                    if (i == 0) {
                        adjacentPos = mob.getBlockPos();
                    } else {
                        adjacentPos = positions.get(i - 1);
                    }
                    if (adjacentPos != null) {
                        mob.getNavigation().startMovingTo(adjacentPos.getX(), adjacentPos.getY(), adjacentPos.getZ(), 1.0);
                        return;
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
                                if (!isPlacementNeeded(diagonalBlockPos2.down(), i) && !isSolidBlock(diagonalBlockPos2) && !isSolidBlock(diagonalBlockPos2)) {
                                    //Diagonal path already exists, none needed
                                    //continue;
                                } else {
                                    breakingPos = isBreakable(diagonalBlockPos1) ? diagonalBlockPos1 : diagonalBlockPos1.up();
                                    System.out.println("Breaking pos identified 2 for mob: " + mob.getUuid() +  ": " + placingPos);
                                    //System.out.println("Identified block to break at: " + breakingPos);
                                    mob.getNavigation().startMovingTo(pos.getX(), pos.getY(), pos.getZ(), 1.0);
                                    return;
                                }
                            }
                            if (isBreakable(diagonalBlockPos2) || isBreakable(diagonalBlockPos2.up()) && !isPlacementNeeded(diagonalBlockPos2.down(), i)) {
                                if (!isPlacementNeeded(diagonalBlockPos1.down(), i) && !isSolidBlock(diagonalBlockPos1) && !isSolidBlock(diagonalBlockPos1)) {
                                    //Diagonal path already exists, none needed
                                    //continue;
                                } else {
                                    breakingPos = isBreakable(diagonalBlockPos2) ? diagonalBlockPos2 : diagonalBlockPos2.up();
                                    System.out.println("Breaking pos identified 3 for mob: " + mob.getUuid() + ": " + placingPos);
                                    //System.out.println("Identified block to break at: " + breakingPos);
                                    mob.getNavigation().startMovingTo(pos.getX(), pos.getY(), pos.getZ(), 1.0);
                                    return;
                                }
                            }
                        }
                        if (nextPos.y != pos.y) {
                            BlockPos twoBlocksUp;
                            if (nextPos.y < pos.y) {
                                twoBlocksUp = new BlockPos(nextPos.x, nextPos.y + 2, nextPos.z);
                            } else {
                                twoBlocksUp = new BlockPos(pos.x, pos.y + 2, pos.z);
                            }
                            if (isBreakable(twoBlocksUp)) {
                                breakingPos = twoBlocksUp;
                                System.out.println("Breaking pos identified 4 for mob: " + mob.getUuid() +  ": " + placingPos);
                                //System.out.println("Identified block to break at: " + breakingPos);
                                mob.getNavigation().startMovingTo(pos.getX(), pos.getY(), pos.getZ(), 1.0);
                                return;
                            }
                        }

                        //Now to check if placement needed:
                        System.out.println("Checking if placement blocks are needed");
                        System.out.println("i: " + i);
                        System.out.println("Current path length: " + currentPath.length());
                        BetterBlockPos floorUnderBlockPos = new BetterBlockPos(pos.x, pos.y - 1, pos.z);
                        if (isPlacementNeeded(floorUnderBlockPos, i) && !isSolidBlock(floorUnderBlockPos) && mob.getMainHandStack().getItem() instanceof BlockItem) {
                            System.out.println("Placement block set");
                            // Ensure breakingPos is null
                            if (!hasAdjacentBlockIncludingBelow(floorUnderBlockPos)) {
                                placingPos = floorUnderBlockPos.down();
                                System.out.println("Placing pos identified 1 for mob: " + mob.getUuid() +  ": " + placingPos);
                            } else {
                                placingPos = floorUnderBlockPos;
                                System.out.println("Placing pos identified 2 for mob: " + mob.getUuid() +  ": " + placingPos);
                            }
                            System.out.println("Placing target pos: " + placingTargetPos);
                            breakingPos = null; // Ensure breakingPos is null
                            placingTargetPos = findSuitableAdjacentBlock(placingPos);
                            if (placingTargetPos == null) {
                                mob.getNavigation().startMovingTo(placingPos.getX(), placingPos.getY(), placingPos.getZ(), 1.0);
                            } else {
                                mob.getNavigation().startMovingTo(placingTargetPos.getX(), placingTargetPos.getY(), placingTargetPos.getZ(), 1.0);
                            }
                            return;
                        } else {
                    /*if (mob.getMainHandStack().getItem() instanceof BlockItem) {
                        System.out.println("Checking: " + pos);
                        System.out.println("Has a block but still found nowhere to place block");
                    }*/
                        }
                    }
                    //
                }
            }
            walkingTargetPos = positions.get(positions.size() - 1);
        } else {
            System.out.println("Current path is null, voiding");
        }
        //System.out.println("No block to break found in the path.");
    }

    private BlockPos findSuitableAdjacentBlock(BlockPos blockPos) {
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos adjacentPos = blockPos.offset(direction);
            if (getWorld(mob).getBlockState(adjacentPos.down()).isSolidBlock(getWorld(mob), adjacentPos) && !isSolidBlock(adjacentPos) && !isSolidBlock(adjacentPos.up())) {
                return adjacentPos;
            }
        }
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos downAdjacentPos = blockPos.offset(direction).down();
            if (getWorld(mob).getBlockState(downAdjacentPos.down()).isSolidBlock(getWorld(mob), downAdjacentPos) && !isSolidBlock(downAdjacentPos) && !isSolidBlock(downAdjacentPos.up())) {
                return downAdjacentPos;
            }
        }
        return null;
    }

    private World getWorld(PathAwareEntity mob) {
        return MinecraftServerUtil.getMinecraftServer().getWorld(mob.getWorld().getRegistryKey());
    }

    private boolean isBreakable(BlockPos blockPos) {
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
        boolean isLadderVineOrDoor = getWorld(mob).getBlockState(blockPos).isOf(Blocks.LADDER)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.VINE)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.IRON_DOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.OAK_DOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.SPRUCE_DOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.BIRCH_DOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.JUNGLE_DOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.ACACIA_DOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.DARK_OAK_DOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.IRON_TRAPDOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.OAK_TRAPDOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.SPRUCE_TRAPDOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.BIRCH_TRAPDOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.JUNGLE_TRAPDOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.ACACIA_TRAPDOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.DARK_OAK_TRAPDOOR);
        return getWorld(mob).getBlockState(blockPos).isSolidBlock(getWorld(mob), blockPos) || isLadderVineOrDoor ;
    }

    private boolean isSolidBlock(BlockPos blockPos) {
        //System.out.println("block state: " + getWorld(mob).getBlockState(blockPos));
        if (pathingBehavior != null && pathingBehavior.getCurrent() != null) {
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
            }*/
        }
        boolean isLadderVineOrDoor = getWorld(mob).getBlockState(blockPos).isOf(Blocks.LADDER)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.VINE)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.IRON_DOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.OAK_DOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.SPRUCE_DOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.BIRCH_DOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.JUNGLE_DOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.ACACIA_DOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.DARK_OAK_DOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.IRON_TRAPDOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.OAK_TRAPDOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.SPRUCE_TRAPDOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.BIRCH_TRAPDOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.JUNGLE_TRAPDOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.ACACIA_TRAPDOOR)
                || getWorld(mob).getBlockState(blockPos).isOf(Blocks.DARK_OAK_TRAPDOOR);
        return getWorld(mob).getBlockState(blockPos).isSolidBlock(getWorld(mob), blockPos) || isLadderVineOrDoor;
    }

    @Override
    public void tick() {
        if (mob.getTarget() != null && mob.getTarget() instanceof PlayerEntity) {
            targetPlayer = (PlayerEntity) mob.getTarget();
        }
        if (mob instanceof ZombieEntity && mob.getMainHandStack().getItem() instanceof BlockItem) {
            System.out.println("Tick zombie " + mob.getUuid() + " with block item in hand");
        }
        if (targetPlayer != null) {
            System.out.println("target player is set");
            if (breakingPos != null) {
                System.out.println("Breaking pos active");
                System.out.println("Is attacking: " + mob.isAttacking());
                if (mob.getBlockPos().isWithinDistance(breakingPos, 6)) {
                    //System.out.println("Block is within distance to break.");
                    continueBreakingBlock();
                } else {
                    if (previousPos == null) {
                        previousPos = mob.getBlockPos();
                    }
                    mob.getNavigation().startMovingTo(breakingPos.getX(), breakingPos.getY(), breakingPos.getZ(), 1.0);
                    if (mob.getBlockPos().getManhattanDistance(previousPos) > 2) {
                        previousPos = mob.getBlockPos();
                        standingStillTicks = 0;
                    } else {
                        standingStillTicks++;
                    }
                    if (standingStillTicks> 200) {
                        System.out.println("Standing still for too long");
                        resetGoal();
                    }
                }
                //System.out.println("Block is not within distance to break. Moving to block.");
                //System.out.println("Distance: " + mob.getBlockPos().getManhattanDistance(breakingPos));
            } else if (placingPos != null) {
                System.out.println("Placing pos active");
                System.out.println("Is attacking: " + mob.isAttacking());
                System.out.println("Squared distance: " + mob.getBlockPos().getSquaredDistance(placingPos));
                System.out.println(mob.getUuid() + " Placing pos: " + placingPos);
                System.out.println("Placing target pos: " + placingTargetPos);
                if (mob.getBlockPos().isWithinDistance(placingPos, 5)) {
                    // System.out.println("Block is within distance to place.");
                    System.out.println("Is within distance");
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
                    if (placingTargetPos != null) {
                        mob.getNavigation().startMovingTo(placingTargetPos.getX(), placingTargetPos.getY(), placingTargetPos.getZ(), 1.0);
                    } else {
                        mob.getNavigation().startMovingTo(placingPos.getX(), placingPos.getY(), placingPos.getZ(), 1.0);
                    }
                    //If mob moved more than 2 blocks away from previous pos:
                    if (mob.getBlockPos().getManhattanDistance(previousPos) > 2) {
                        previousPos = mob.getBlockPos();
                        standingStillTicks = 0;
                    } else {
                        standingStillTicks++;
                    }
                    if (standingStillTicks > 200 || isSolidBlock(placingPos)) {
                        System.out.println("Stopping goal");
                        resetGoal();
                    }
                }
            } else if (walkingTargetPos != null) {
                if (mob.getBlockPos().isWithinDistance(walkingTargetPos, 2)) {
                    resetGoal();
                } else {
                    mob.getNavigation().startMovingTo(walkingTargetPos.getX(), walkingTargetPos.getY(), walkingTargetPos.getZ(), 1.0);
                }
            } else {
                System.out.println("No block to break or place found. Calculating path.");
                calculatePath();
            }
        }
    }

    private void placeBlock() {
        if (placingPos != null && mob.getMainHandStack().getItem() instanceof BlockItem) {
            World world = getWorld(mob);
            ItemStack itemStack = mob.getMainHandStack();
            BlockItem blockItem = (BlockItem) itemStack.getItem();

            // Make sure it isn't a half slab, door, trapdoor, or bed:
            boolean isLadderVineDoorOrBed = blockItem.getBlock() == Blocks.LADDER
                    || blockItem.getBlock() == Blocks.VINE
                    || blockItem.getBlock() == Blocks.IRON_DOOR
                    || blockItem.getBlock() == Blocks.OAK_DOOR
                    || blockItem.getBlock() == Blocks.SPRUCE_DOOR
                    || blockItem.getBlock() == Blocks.BIRCH_DOOR
                    || blockItem.getBlock() == Blocks.JUNGLE_DOOR
                    || blockItem.getBlock() == Blocks.ACACIA_DOOR
                    || blockItem.getBlock() == Blocks.DARK_OAK_DOOR
                    || blockItem.getBlock() == Blocks.IRON_TRAPDOOR
                    || blockItem.getBlock() == Blocks.OAK_TRAPDOOR
                    || blockItem.getBlock() == Blocks.SPRUCE_TRAPDOOR
                    || blockItem.getBlock() == Blocks.BIRCH_TRAPDOOR
                    || blockItem.getBlock() == Blocks.JUNGLE_TRAPDOOR
                    || blockItem.getBlock() == Blocks.ACACIA_TRAPDOOR
                    || blockItem.getBlock() == Blocks.DARK_OAK_TRAPDOOR
                    || blockItem.getBlock() == Blocks.WHITE_BED
                    || blockItem.getBlock() == Blocks.ORANGE_BED
                    || blockItem.getBlock() == Blocks.MAGENTA_BED
                    || blockItem.getBlock() == Blocks.LIGHT_BLUE_BED
                    || blockItem.getBlock() == Blocks.YELLOW_BED
                    || blockItem.getBlock() == Blocks.LIME_BED
                    || blockItem.getBlock() == Blocks.PINK_BED
                    || blockItem.getBlock() == Blocks.GRAY_BED
                    || blockItem.getBlock() == Blocks.LIGHT_GRAY_BED
                    || blockItem.getBlock() == Blocks.CYAN_BED
                    || blockItem.getBlock() == Blocks.PURPLE_BED
                    || blockItem.getBlock() == Blocks.BLUE_BED
                    || blockItem.getBlock() == Blocks.BROWN_BED
                    || blockItem.getBlock() == Blocks.GREEN_BED
                    || blockItem.getBlock() == Blocks.RED_BED
                    || blockItem.getBlock() == Blocks.BLACK_BED;

            if (!isLadderVineDoorOrBed) {
                boolean success = world.setBlockState(placingPos, blockItem.getBlock().getDefaultState(), 3);
                if (success) {
                    // Decrement the amount of blocks in the mob's hand by 1
                    itemStack.decrement(1);
                    System.out.println("Block placed: " + success);
                }
            }
            resetGoal();
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
        World world = getWorld(mob);
        breakingTicks++;
        int originalProgress = blockDamageProgress.getOrDefault(breakingPos, 0);
        int progress;

        // Retrieve block hardness
        float blockHardness = getWorld(mob).getBlockState(breakingPos).getHardness(getWorld(mob), breakingPos);
        int adjustedBreakingTime = (int) (BREAKING_TIME * blockHardness);

        // Increase progress incrementally
        //System.out.println("Breaking ticks: " + breakingTicks);
        progress = originalProgress + (int) ((breakingTicks / (float) adjustedBreakingTime) * 10);
        //System.out.println("Progress: " + progress);
        world.setBlockBreakingInfo(mob.getId(), breakingPos, progress);
        blockDamageProgress.put(breakingPos, progress);

        if (progress >= 10) {
            //System.out.println("Breaking block at: " + breakingPos);
            boolean success = world.breakBlock(breakingPos, true, mob);
            //System.out.println("Block broken: " + success);
            //System.out.println("Is air: " + world.getBlockState(breakingPos).isAir());
            if (!success) {
                world.setBlockState(breakingPos, Blocks.AIR.getDefaultState(), 3);
            }
            blockDamageProgress.remove(breakingPos);
            breakingTicks = 0;
            breakingPos = null;
        }
    }

    @Override
    public boolean shouldContinue() {
        if (currentPath != null) {
            return areSolidBlocksSeparatingPlayerFromMob();
        }
        return true;
    }

    public boolean areSolidBlocksSeparatingPlayerFromMob() {
        List<BetterBlockPos> positions = currentPath.positions();
        for (int i = 0; i < positions.size(); i++) {
            BetterBlockPos pos = positions.get(i);
            //System.out.println("Checking block at: " + pos);
            BlockPos blockPos = new BlockPos(pos.x, pos.y, pos.z);
            if (isBreakable(blockPos) || isBreakable(blockPos.up()) || isPlacementNeeded(blockPos.down(), i)) {
                return true;
            } else {
                //System.out.println("Check diagonals between positions.");
                if (i != positions.size() - 1) {
                    BetterBlockPos nextPos = positions.get(i + 1);
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
                            twoBlocksUp = new BlockPos(nextPos.x, nextPos.y + 2, nextPos.z);
                        } else {
                            twoBlocksUp = new BlockPos(pos.x, pos.y + 2, pos.z);
                        }
                        if (isBreakable(twoBlocksUp)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void stop() {
        System.out.println("#################### GOAL Stopped: " + mob.getUuid());
        resetGoal();
    }

    private void resetGoal() {
        currentPath = null;
        //calculating = false;
        breakingTicks = 0;
        standingStillTicks = 0;
        placingPos = null;
        placingTargetPos = null;
        walkingTargetPos = null;
        breakingPos = null;
        previousPos = null;
        mob.getNavigation().stop();
    }
}
