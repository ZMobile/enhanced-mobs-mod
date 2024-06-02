package net.fabricmc.example.bloodmoon.server;

import java.util.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.google.common.collect.Sets;
import net.fabricmc.example.bloodmoon.config.BloodmoonConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.Spawner;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;


import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Sets;
import net.fabricmc.example.bloodmoon.config.BloodmoonConfig;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class BloodmoonSpawner implements Spawner {
	private static final int MOB_COUNT_DIV = (int) Math.pow(17.0D, 2.0D);
	private final Set<ChunkPos> eligibleChunksForSpawning = Sets.newHashSet();



	public void triggerBloodmoonSpawning(ServerWorld world, boolean spawnHostileMobs, boolean spawnPeacefulMobs) {
		if (!spawnHostileMobs && !spawnPeacefulMobs) {
			return;
		}

		this.eligibleChunksForSpawning.clear();
		int eligibleChunkCount = 0;

		for (PlayerEntity player : world.getPlayers()) {
			if (!player.isSpectator()) {
				int playerChunkX = MathHelper.floor(player.getX() / 16.0D);
				int playerChunkZ = MathHelper.floor(player.getZ() / 16.0D);
				int radius = 8;

				for (int dx = -radius; dx <= radius; ++dx) {
					for (int dz = -radius; dz <= radius; ++dz) {
						boolean isEdge = dx == -radius || dx == radius || dz == -radius || dz == radius;
						ChunkPos chunkPos = new ChunkPos(dx + playerChunkX, dz + playerChunkZ);

						if (!this.eligibleChunksForSpawning.contains(chunkPos)) {
							++eligibleChunkCount;

							if (!isEdge && world.getWorldBorder().contains(chunkPos)) {
								WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkPos.x, chunkPos.z, false);
								if (chunk != null && world.isChunkLoaded(chunkPos.x, chunkPos.z)) {
									this.eligibleChunksForSpawning.add(chunkPos);
								}
							}
						}
					}
				}
			}
		}

		int spawnCount = 0;
		BlockPos worldSpawnPos = world.getSpawnPos();

		for (SpawnGroup spawnGroup : SpawnGroup.values()) {
			if ((!spawnGroup.isPeaceful() || spawnPeacefulMobs) && (spawnGroup.isPeaceful() || spawnHostileMobs)) {
				int existingEntityCount = countEntities(world, spawnGroup);
				int maxSpawnCount = spawnGroup.getCapacity() * eligibleChunkCount / MOB_COUNT_DIV;

				maxSpawnCount *= BloodmoonConfig.SPAWNING.SPAWN_LIMIT_MULT;

				if (existingEntityCount <= maxSpawnCount) {
					List<ChunkPos> shuffledChunks = Lists.newArrayList(this.eligibleChunksForSpawning);
					Collections.shuffle(shuffledChunks);
					BlockPos.Mutable mutablePos = new BlockPos.Mutable();

					for (ChunkPos chunkPos : shuffledChunks) {
						BlockPos spawnPos = getRandomChunkPosition(world, chunkPos.x, chunkPos.z);
						int x = spawnPos.getX();
						int y = spawnPos.getY();
						int z = spawnPos.getZ();
						BlockState blockState = world.getBlockState(spawnPos);

						if (!blockState.isOpaque()) {
							int groupSize = MathHelper.ceil(Math.random() * 4.0D);

							for (int i = 0; i < groupSize; ++i) {
								x += world.random.nextInt(6) - world.random.nextInt(6);
								y += world.random.nextInt(1) - world.random.nextInt(1);
								z += world.random.nextInt(6) - world.random.nextInt(6);
								mutablePos.set(x, y, z);
								float spawnX = (float) x + 0.5F;
								float spawnZ = (float) z + 0.5F;

								if (world.isSkyVisible(mutablePos) && world.getClosestPlayer(spawnX, y, spawnZ, BloodmoonConfig.SPAWNING.SPAWN_RANGE, false) == null && worldSpawnPos.isWithinDistance(new Vec3d(spawnX, y, spawnZ), BloodmoonConfig.SPAWNING.SPAWN_DISTANCE)) {
									RegistryEntry<Biome> biome = world.getBiome(mutablePos);
									Pool<SpawnSettings.SpawnEntry> spawnList = world.getChunkManager().getChunkGenerator().getEntitySpawnList(biome, world.getStructureAccessor(), spawnGroup, mutablePos);

									if (!spawnList.isEmpty()) {
										int spawnIndex = world.random.nextInt(spawnList.getEntries().size());
										SpawnSettings.SpawnEntry spawnEntry = spawnList.getEntries().get(spawnIndex);
										if (BloodmoonConfig.canSpawn(spawnEntry.type.getBaseClass())) {
											MobEntity mobEntity;

											try {
												mobEntity = (MobEntity) spawnEntry.type.create(world);
											} catch (Exception e) {
												e.printStackTrace();
												return;
											}

											mobEntity.refreshPositionAndAngles(spawnX, y, spawnZ, world.random.nextFloat() * 360.0F, 0.0F);

											if (world.tryLoadEntity(mobEntity) && mobEntity.canSpawn(world, SpawnReason.NATURAL) && mobEntity.canSpawn(world)) {
												++spawnCount;
												mobEntity.setPersistent();
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private int countEntities(ServerWorld world, SpawnGroup spawnGroup) {
		int count = 0;
		for (Entity entity : world.iterateEntities()) {
			if (entity instanceof MobEntity && entity.getType().getSpawnGroup() == spawnGroup) {
				count++;
			}
		}
		return count;
	}

	private static BlockPos getRandomChunkPosition(World world, int chunkX, int chunkZ) {
		WorldChunk chunk = world.getChunk(chunkX, chunkZ);
		int x = chunkX * 16 + world.random.nextInt(16);
		int z = chunkZ * 16 + world.random.nextInt(16);
		int maxY = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x, z) + 1;
		int y = world.random.nextInt(maxY > 0 ? maxY : chunk.getHighestNonEmptySection() * 16 - 1);
		return new BlockPos(x, y, z);
	}

	public static boolean canSpawnAtLocation(EntityType<?> entityType, World world, BlockPos pos) {
		if (!world.getWorldBorder().contains(pos)) {
			return false;
		} else {
			BlockState blockState = world.getBlockState(pos);

			if (entityType.getSpawnGroup() == SpawnGroup.WATER_CREATURE) {
				return blockState.getFluidState().isStill() && world.getBlockState(pos.down()).getFluidState().isStill() && !world.getBlockState(pos.up()).isSolidBlock(world, pos.up());
			} else {
				BlockPos blockBelow = pos.down();
				BlockState stateBelow = world.getBlockState(blockBelow);

				if (!stateBelow.allowsSpawning(world, blockBelow, entityType)) {
					return false;
				} else {
					boolean isNotBedrockOrBarrier = stateBelow.getBlock() != Blocks.BEDROCK && stateBelow.getBlock() != Blocks.BARRIER;
					return isNotBedrockOrBarrier && Block.isFaceFullSquare(blockState.getCollisionShape(world, pos), Direction.UP) && world.getLightLevel(LightType.BLOCK, pos) < 7;
				}
			}
		}
	}


	public static void performWorldGenSpawning(ServerWorld world, Biome biome, int x, int z, Random random) {
		Pool<SpawnSettings.SpawnEntry> spawnEntries = biome.getSpawnSettings().getSpawnEntries(SpawnGroup.CREATURE);

		if (!spawnEntries.isEmpty()) {
			while (random.nextFloat() < biome.getSpawnSettings().getCreatureSpawnProbability()) {
				int spawnEntryIndex = random.nextInt(spawnEntries.getEntries().size());
				SpawnSettings.SpawnEntry spawnEntry = spawnEntries.getEntries().get(spawnEntryIndex);
				if (spawnEntry == null) {
					continue;
				}
				int count = spawnEntry.minGroupSize + random.nextInt(1 + spawnEntry.maxGroupSize - spawnEntry.minGroupSize);
				int posX = x + random.nextInt(16);
				int posZ = z + random.nextInt(16);
				int posY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, posX, posZ);

				for (int i = 0; i < count; ++i) {
					boolean spawned = false;

					for (int attempt = 0; !spawned && attempt < 4; ++attempt) {
						BlockPos spawnPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, new BlockPos(posX, 0, posZ));

						if (canSpawnAtLocation(spawnEntry.type, world, spawnPos)) {
							MobEntity mobEntity;

							try {
								mobEntity = (MobEntity) spawnEntry.type.create(world);
							} catch (Exception e) {
								e.printStackTrace();
								continue;
							}

							mobEntity.refreshPositionAndAngles((double) posX + 0.5D, (double) spawnPos.getY(), (double) posZ + 0.5D, random.nextFloat() * 360.0F, 0.0F);
							world.spawnEntity(mobEntity);
							spawned = true;
						}

						posX += random.nextInt(5) - random.nextInt(5);
						posZ += random.nextInt(5) - random.nextInt(5);
					}
				}
			}
		}
	}


	@Override
	public void setEntityType(EntityType<?> type, net.minecraft.util.math.random.Random random) {
		// TODO Auto-generated method stub
	}
}