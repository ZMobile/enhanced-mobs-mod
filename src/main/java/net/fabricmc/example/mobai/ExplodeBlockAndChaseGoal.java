package net.fabricmc.example.mobai;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExplodeBlockAndChaseGoal extends Goal {
    private final CreeperEntity mob;
    private PlayerEntity targetPlayer;
    private IPath currentPath;
    private IPathingBehavior pathingBehavior;
    private BlockPos breakingPos;
    private BlockPos targetPos;
    private final Map<BlockPos, Integer> blockDamageProgress = new HashMap<>();

    public ExplodeBlockAndChaseGoal(CreeperEntity mob) {
        this.mob = mob;
        BaritoneAPI.getSettings().allowBreak.value = true;
        BaritoneAPI.getSettings().allowPlace.value = true;
        BaritoneAPI.getSettings().allowParkour.value = false;
        BaritoneAPI.getSettings().allowJumpAt256.value = false;
        BaritoneAPI.getSettings().allowParkourAscend.value = false;
        BaritoneAPI.getSettings().allowParkourPlace.value = false;
        BaritoneAPI.getSettings().assumeExternalAutoTool.value = true; // Assume tool is externally managed
        BaritoneAPI.getSettings().avoidance.value = false;
        BaritoneAPI.getSettings().renderPath.value = false;
        BaritoneAPI.getSettings().renderSelectionBoxes.value = false;
        BaritoneAPI.getSettings().renderGoal.value = false;
        BaritoneAPI.getSettings().renderCachedChunks.value = false;
        BaritoneAPI.getSettings().renderSelectionCorners.value = false;
        BaritoneAPI.getSettings().renderGoalAnimated.value = false;
        BaritoneAPI.getSettings().renderPathAsLine.value = false;
        BaritoneAPI.getSettings().renderGoalXZBeacon.value = false;
        BaritoneAPI.getSettings().assumeWalkOnWater.value = false;
        BaritoneAPI.getSettings().walkOnWaterOnePenalty.value = 23D;
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
        //System.out.println("#################### GOAL Triggered");
    }

    private void calculatePath() {
        //System.out.println("Calculating path.");
        if (this.targetPlayer != null) {
            targetPos = targetPlayer.getBlockPos();
            GoalBlock goal = new GoalBlock(targetPos.getX(), targetPos.getY(), targetPos.getZ());
            // Check if block underneath player is air and if so set goal to one of the adjacent blocks that's over a solid block.
            if (mob.getEntityWorld().getBlockState(targetPos.down()).isAir()) {
                for (Direction direction : Direction.Type.HORIZONTAL) {
                    BlockPos adjacentPos = targetPos.offset(direction);
                    if (mob.getEntityWorld().getBlockState(adjacentPos.down()).isSolidBlock(mob.getEntityWorld(), adjacentPos.down())) {
                        goal = new GoalBlock(adjacentPos.getX(), adjacentPos.getY(), adjacentPos.getZ());
                        break;
                    }
                }
            }
            if (mob.getEntityWorld().getBlockState(targetPos.down()).isAir()) {
                //System.out.println("Player is standing on air. Cannot calculate path.");
                return;
            }
            IBaritone goalBaritone = BaritoneAPI.getProvider().getBaritoneForEntity(mob);
            if (goalBaritone != null) {
                pathingBehavior = goalBaritone.getPathingBehavior();
                goalBaritone.getCustomGoalProcess().setGoalAndPath(goal);
                if (goalBaritone.getPathingBehavior().getCurrent() != null) {
                    currentPath = goalBaritone.getPathingBehavior().getCurrent().getPath();
                    breakingPos = null;
                    findBreakingBlock();
                } else {
                    currentPath = null;
                    //System.out.println("Failed to calculate path.");
                }
            }
        }
    }

    private void findBreakingBlock() {
        if (currentPath != null) {
            List<BetterBlockPos> positions = currentPath.positions();
            for (int i = 0; i < positions.size(); i++) {
                BetterBlockPos pos = positions.get(i);
                //System.out.println("Checking block at: " + pos);
                BlockPos blockPos = new BlockPos(pos.x, pos.y, pos.z);
                if (isBreakable(blockPos) || isBreakable(blockPos.up())) {
                    breakingPos = isBreakable(blockPos) ? blockPos : blockPos.up();
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
                        // If next block pos x and y, or y and z are different, check if the diagonal block is breakable
                        if (nextPos.x != pos.x && nextPos.z != pos.z) {
                            BlockPos diagonalBlockPos1 = new BlockPos(pos.x, pos.y, nextPos.z);
                            BlockPos diagonalBlockPos2 = new BlockPos(nextPos.x, pos.y, pos.z);
                            if ((isBreakable(diagonalBlockPos1) || isBreakable(diagonalBlockPos1.up()) && isSolidBlock(diagonalBlockPos1.down()))) {
                                if (isSolidBlock(diagonalBlockPos2.down()) && !isSolidBlock(diagonalBlockPos2) && !isSolidBlock(diagonalBlockPos2)) {
                                    // Diagonal path already exists, none needed
                                    continue;
                                }
                                breakingPos = isBreakable(diagonalBlockPos1) ? diagonalBlockPos1 : diagonalBlockPos1.up();
                                //System.out.println("Identified block to break at: " + breakingPos);
                                mob.getNavigation().startMovingTo(pos.getX(), pos.getY(), pos.getZ(), 1.0);
                                return;
                            }
                            if (isBreakable(diagonalBlockPos2) || isBreakable(diagonalBlockPos2.up()) && isSolidBlock(diagonalBlockPos2.down())) {
                                if (isSolidBlock(diagonalBlockPos1.down()) && !isSolidBlock(diagonalBlockPos1) && !isSolidBlock(diagonalBlockPos1)) {
                                    // Diagonal path already exists, none needed
                                    continue;
                                }
                                breakingPos = isBreakable(diagonalBlockPos2) ? diagonalBlockPos2 : diagonalBlockPos2.up();
                                //System.out.println("Identified block to break at: " + breakingPos);
                                mob.getNavigation().startMovingTo(pos.getX(), pos.getY(), pos.getZ(), 1.0);
                                return;
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
                                //System.out.println("Identified block to break at: " + breakingPos);
                                mob.getNavigation().startMovingTo(pos.getX(), pos.getY(), pos.getZ(), 1.0);
                                return;
                            }
                        }
                    }
                }
            }
        }
        //System.out.println("No block to break found in the path.");
    }

    private boolean isBreakable(BlockPos blockPos) {
        //System.out.println("block state: " + mob.getWorld().getBlockState(blockPos));
        if (pathingBehavior != null && pathingBehavior.getCurrent() != null) {
            IPathExecutor current = pathingBehavior.getCurrent(); // this should prevent most race conditions?
            Set<BlockPos> blocksToBreak = current.toBreak();
            //System.out.println("Blocks to break size: " + blocksToBreak.size());
            for (BlockPos pos : blocksToBreak) {
                if (pos.equals(blockPos)) {
                    //System.out.println("Blocks to break contains this block");
                    return true;
                }
                //System.out.println("Block to break: " + pos);
            }
        }
        return mob.getWorld().getBlockState(blockPos).isSolidBlock(mob.getWorld(), blockPos);
    }

    private boolean isSolidBlock(BlockPos blockPos) {
        //System.out.println("block state: " + mob.getWorld().getBlockState(blockPos));
        if (pathingBehavior != null && pathingBehavior.getCurrent() != null) {
            IPathExecutor current = pathingBehavior.getCurrent(); // this should prevent most race conditions?
            Set<BlockPos> blocksToBreak = current.toBreak();
            //System.out.println("Blocks to break size: " + blocksToBreak.size());
            for (BlockPos pos : blocksToBreak) {
                if (pos.equals(blockPos)) {
                    //System.out.println("Blocks to break contains this block");
                    return true;
                }
            }
        }
        return mob.getWorld().getBlockState(blockPos).isSolidBlock(mob.getWorld(), blockPos);
    }

    @Override
    public void tick() {
        if (targetPlayer != null) {
            calculatePath();
            if (breakingPos != null) {
                if (mob.getBlockPos().isWithinDistance(breakingPos, 3)) {
                    //System.out.println("Block is within distance to explode.");
                    explodeBlock();
                } else {
                    //System.out.println("Block is not within distance to explode. Moving to block.");
                    //System.out.println("Distance: " + mob.getBlockPos().getManhattanDistance(breakingPos));
                    mob.getNavigation().startMovingTo(breakingPos.getX(), breakingPos.getY(), breakingPos.getZ(), 1.0);
                }
            }
        }
    }

    private void explodeBlock() {
        //System.out.println("Exploding block at: " + breakingPos);
        mob.ignite(); // Ignite the creeper to make it explode
        breakingPos = null;
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
            if (isBreakable(blockPos) || isBreakable(blockPos.up())) {
                return true;
            } else {
                //System.out.println("Check diagonals between positions.");
                if (i != positions.size() - 1) {
                    BetterBlockPos nextPos = positions.get(i + 1);
                    if (nextPos.x != pos.x && nextPos.z != pos.z) {
                        BlockPos diagonalBlockPos1 = new BlockPos(pos.x, pos.y, nextPos.z);
                        BlockPos diagonalBlockPos2 = new BlockPos(nextPos.x, pos.y, pos.z);
                        if ((isBreakable(diagonalBlockPos1) || isBreakable(diagonalBlockPos1.up())) && isSolidBlock(diagonalBlockPos1.down())) {
                            if (isSolidBlock(diagonalBlockPos2.down()) && !isSolidBlock(diagonalBlockPos2) && !isSolidBlock(diagonalBlockPos2)) {
                                // Diagonal path already exists, none needed
                                continue;
                            }
                            return true;
                        }
                        if ((isBreakable(diagonalBlockPos2) || isBreakable(diagonalBlockPos2.up())) && isSolidBlock(diagonalBlockPos2.down())) {
                            if (isSolidBlock(diagonalBlockPos1.down()) && !isSolidBlock(diagonalBlockPos1) && !isSolidBlock(diagonalBlockPos1)) {
                                // Diagonal path already exists, none needed
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
        //System.out.println("#################### GOAL Stopped");
        resetGoal();
    }

    private void resetGoal() {
        currentPath = null;
        breakingPos = null;
        mob.getNavigation().stop();
    }
}
