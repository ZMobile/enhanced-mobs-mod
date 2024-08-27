package net.fabricmc.example.mobai.tracker;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.MinecraftServerUtil;
import net.fabricmc.example.client.block.ClientRenderedBlockUpdateServiceImpl;
import net.fabricmc.example.config.ConfigManager;
import net.fabricmc.example.service.MobitoneServiceImpl;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class MobPathTracker {
   private static Map<String, List<BetterBlockPos>> mobIdToPathMap = new HashMap<>();

   public static void updatePath(String mobId, List<BetterBlockPos> path) {
      mobIdToPathMap.put(mobId, path);
   }

   public static void removePath(String mobId) {
      mobIdToPathMap.remove(mobId);
   }

   public static List<BetterBlockPos> getPath(String mobId) {
      return mobIdToPathMap.get(mobId);
   }

   public static boolean breakingPosLiesInPath(BlockPos breakingPos) {
       //Checks if this breaking block is another mobs breaking block.
       for (List<BetterBlockPos> positions : mobIdToPathMap.values()) {
              if (positions == null) {
                continue;
              }
           for (BetterBlockPos pos : positions) {
               if (breakingPos.equals(pos.down())) {
                   return true;
               }
           }
       }
       return false;
   }

   public static boolean placingPosLiesInPath(BlockPos placingPos) {
       //Checks if this placing block is another mobs breaking block.
       for (List<BetterBlockPos> positions : mobIdToPathMap.values()) {
           if (positions == null) {
                continue;
           }
           for (int i = 0; i < positions.size(); i++) {
               BetterBlockPos pos = positions.get(i);
               if (placingPos.equals(pos) || placingPos.equals(pos.up())) {
                   return true;
               }
               if (i != positions.size() - 1) {
                   BetterBlockPos nextPos = positions.get(i + 1);
                   if (nextPos.y != pos.y) {
                       BlockPos twoBlocksUp;
                       if (nextPos.y < pos.y) {
                           twoBlocksUp = new BlockPos(nextPos.x, nextPos.y + 2, nextPos.z);
                       } else {
                           twoBlocksUp = new BlockPos(pos.x, pos.y + 2, pos.z);
                       }
                       if (placingPos.equals(twoBlocksUp)) {
                           return true;
                       }
                   }
               }
           }
       }
        return false;
   }
}
