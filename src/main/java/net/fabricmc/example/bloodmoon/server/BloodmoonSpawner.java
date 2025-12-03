package net.fabricmc.example.bloodmoon.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.fabricmc.example.bloodmoon.config.BloodmoonConfig;
import net.fabricmc.example.config.ConfigManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.Spawner;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.chunk.WorldChunk;


import java.util.*;


import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.minecraft.entity.EntityType.*;

public final class BloodmoonSpawner implements Spawner {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final int MOB_COUNT_DIV = (int) Math.pow(17.0D, 2.0D);
	private final Set<ChunkPos> eligibleChunksForSpawning = Sets.newHashSet();
	int witchCount = 0; // Counter for witches

	// Cache for player positions to avoid repeated calculations
	private List<PlayerEntity> cachedPlayers = null;
	private List<Vec3d> cachedPlayerPositions = null;

	public void triggerBloodmoonSpawning(ServerWorld world, boolean spawnHostileMobs, boolean spawnPeacefulMobs) {
		if (!spawnHostileMobs && !spawnPeacefulMobs) {
			return;
		}

		this.eligibleChunksForSpawning.clear();
		int eligibleChunkCount = 0;

		// Cache player data to avoid repeated lookups
		cachedPlayers = new ArrayList<>(world.getPlayers());
		cachedPlayerPositions = new ArrayList<>();

		for (PlayerEntity player : cachedPlayers) {
			if (!player.isSpectator()) {
				cachedPlayerPositions.add(player.getEntityPos());

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
		BlockPos worldSpawnPos = world.getSpawnPoint().getPos();

		for (SpawnGroup spawnGroup : SpawnGroup.values()) {
			if ((!spawnGroup.isPeaceful() || spawnPeacefulMobs) && (spawnGroup.isPeaceful() || spawnHostileMobs)) {
				int existingEntityCount = countEntities(world, spawnGroup);
				int maxSpawnCount = spawnGroup.getCapacity() * eligibleChunkCount / MOB_COUNT_DIV;

				maxSpawnCount *= BloodmoonConfig.SPAWNING.SPAWN_LIMIT_MULT;

				if (existingEntityCount <= maxSpawnCount) {
					List<ChunkPos> shuffledChunks = Lists.newArrayList(this.eligibleChunksForSpawning);
					Collections.shuffle(shuffledChunks);
					BlockPos.Mutable mutablePos = new BlockPos.Mutable();

					// Limit spawn attempts to prevent excessive processing
					int maxSpawnAttempts = Math.min(shuffledChunks.size(), 50); // Limit to 50 chunks per spawn group

					for (int i = 0; i < maxSpawnAttempts; i++) {
						ChunkPos chunkPos = shuffledChunks.get(i);
						BlockPos spawnPos = getRandomChunkPosition(world, chunkPos.x, chunkPos.z);
						int x = spawnPos.getX();
						int y = spawnPos.getY();
						int z = spawnPos.getZ();
						BlockState blockState = world.getBlockState(spawnPos);

						if (!blockState.isOpaque()) {
							int groupSize = MathHelper.ceil(Math.random() * 4.0D);

							for (int j = 0; j < groupSize; ++j) {
								x += world.random.nextInt(6) - world.random.nextInt(6);
								z += world.random.nextInt(6) - world.random.nextInt(6);

								// Find the surface at the new X,Z position
								BlockPos surfacePos = world.getTopPosition(Heightmap.Type.WORLD_SURFACE, new BlockPos(x, 0, z));
								y = surfacePos.getY();

								mutablePos.set(x, y, z);
								float spawnX = (float) x + 0.5F;
								float spawnZ = (float) z + 0.5F;
								int blockLightLevel = world.getLightLevel(LightType.BLOCK, mutablePos);

								// Check if the block below is opaque
								BlockPos belowPos = mutablePos.down();
								BlockState belowState = world.getBlockState(belowPos);

								// Skip if block below is not opaque/solid
								if (!belowState.isOpaque() || !belowState.isSolidBlock(world, belowPos)) {
									continue;
								}

								// During bloodmoon, we need to prevent spawning in direct sunlight
								int skyLightLevel = world.getLightLevel(LightType.SKY, mutablePos);
								boolean isDaytime = world.isDay();

								// If it's daytime and the position has high sky light, check if it's actually exposed to sun
								if (isDaytime && skyLightLevel > 10 && world.isSkyVisible(mutablePos)) {
									// This position is in direct sunlight, skip it
									continue;
								}

								int waterLightThreshold = 7;
								// Check spawning conditions
								boolean canSpawn = false;

								// For water spawning (drowned)
								if (world.getBlockState(mutablePos).isOf(Blocks.WATER) && blockLightLevel <= waterLightThreshold) {
									canSpawn = true;
								}
								// For regular mob spawning on land
								else if (!world.getBlockState(mutablePos).isOf(Blocks.WATER) && world.isAir(mutablePos)) {
									canSpawn = true;
								}

								if (canSpawn) {
									// Use optimized player check
									if (isPlayerNearbyOptimized(mutablePos, 200)) {
										RegistryEntry<Biome> biome = world.getBiome(mutablePos);
										Pool<SpawnSettings.SpawnEntry> spawnList = world.getChunkManager().getChunkGenerator().getEntitySpawnList(biome, world.getStructureAccessor(), spawnGroup, mutablePos);

										if (!spawnList.isEmpty()) {
											int spawnIndex = world.random.nextInt(spawnList.getEntries().size());
											SpawnSettings.SpawnEntry spawnEntry = spawnList.getEntries().get(spawnIndex).value();

											double bloodmoonSpawnChance = ConfigManager.getConfig().getBloodmoonSpawnPercentage();
											if (mutablePos.getY() < 45) {
												bloodmoonSpawnChance = bloodmoonSpawnChance * 10;
											}

											// Optimize entity checks for specific types only
											if (spawnEntry.type() == EntityType.DROWNED) {
												// Only check for drowned-specific conditions
												if (cachedPlayers.size() > 0) {
													// Simple check instead of searching all entities
													bloodmoonSpawnChance = Math.min(1, ConfigManager.getConfig().getBloodmoonSpawnPercentage() * 100);
												}
											}

											if (Math.random() < bloodmoonSpawnChance) {
												if (spawnEntry.type() == ENDERMAN) {
													continue;
												}

												if (spawnEntry.type() == WITCH && Math.random() > 0.1) { // 10% chance to spawn a witch
													continue;
												}

												if (spawnEntry.type() == EntityType.DROWNED) {
													if (!(world.getBlockState(mutablePos).isOf(Blocks.WATER) &&
															world.getBlockState(mutablePos.up()).isOf(Blocks.WATER) &&
															world.getBlockState(mutablePos.up(2)).isOf(Blocks.WATER))) {
														continue;
													}
												}

												if (spawnEntry.type() == WITCH) {
													witchCount++;
													if (witchCount > 5) { // Limit to 5 witches
														continue;
													}
												}
												if (BloodmoonConfig.canSpawn(spawnEntry.type().getBaseClass())) {
													MobEntity mobEntity;

													try {
														mobEntity = (MobEntity) spawnEntry.type().create(world, SpawnReason.NATURAL);
													} catch (Exception e) {
														e.printStackTrace();
														return;
													}

													mobEntity.refreshPositionAndAngles(spawnX, y, spawnZ, world.random.nextFloat() * 360.0F, 0.0F);

													if (world.tryLoadEntity(mobEntity) && mobEntity.canSpawn(world, SpawnReason.NATURAL) && mobEntity.canSpawn(world)) {
														++spawnCount;
														mobEntity.setPersistent();

														if (mobEntity.getType() == EntityType.DROWNED) {
															LOGGER.info("Successfully spawned drowned at position: " + mutablePos);
														}
													} else {
														if (mobEntity.getType() == EntityType.DROWNED) {
															LOGGER.warn("Failed to spawn drowned at position: " + mutablePos);
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
			}
		}

		// Clear cache after spawning
		cachedPlayers = null;
		cachedPlayerPositions = null;
	}

	// Optimized player proximity check using cached positions
	private boolean isPlayerNearbyOptimized(BlockPos pos, double distance) {
		if (cachedPlayerPositions == null) {
			return false;
		}

		double distanceSq = distance * distance;
		for (Vec3d playerPos : cachedPlayerPositions) {
			double dx = pos.getX() - playerPos.x;
			double dy = pos.getY() - playerPos.y;
			double dz = pos.getZ() - playerPos.z;
			if (dx * dx + dy * dy + dz * dz <= distanceSq) {
				return true;
			}
		}
		return false;
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
		// Avoid getChunk call if possible - use already loaded chunk
		WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkX, chunkZ, false);
		if (chunk == null) {
			// Fallback if chunk not loaded - estimate surface height
			return new BlockPos(chunkX * 16 + world.random.nextInt(16), 64, chunkZ * 16 + world.random.nextInt(16));
		}

		int x = chunkX * 16 + world.random.nextInt(16);
		int z = chunkZ * 16 + world.random.nextInt(16);

		// Get the surface Y position (highest solid block)
		int y = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x & 15, z & 15);

		// Ensure Y is valid
		if (y <= 0) {
			// Fallback: search from top down for first solid block
			for (y = world.getHeight() - 1; y > 0; y--) {
				BlockPos pos = new BlockPos(x, y, z);
				if (world.getBlockState(pos).isSolidBlock(world, pos)) {
					break;
				}
			}
		}

		// Return position one block above the surface for spawning
		return new BlockPos(x, y + 1, z);
	}

	public static boolean canSpawnAtLocation(EntityType<?> entityType, World world, BlockPos pos) {
		if (!world.getWorldBorder().contains(pos)) {
			return false;
		} else {
			BlockState blockState = world.getBlockState(pos);
			if (entityType == EntityType.DROWNED) {
				// Allow drowned to spawn in water
				return blockState.isOf(Blocks.WATER);
			}
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
				SpawnSettings.SpawnEntry spawnEntry = spawnEntries.getEntries().get(spawnEntryIndex).value();
				if (spawnEntry.type() == DROWNED) {
					LOGGER.info("Attempting to spawn drowned at position: " + x + ", " + z);
				}
				if (spawnEntry == null) {
					continue;
				}
				int count = spawnEntry.minGroupSize() + random.nextInt(1 + spawnEntry.maxGroupSize() - spawnEntry.minGroupSize());
				int posX = x + random.nextInt(16);
				int posZ = z + random.nextInt(16);
				int posY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, posX, posZ);

				for (int i = 0; i < count; ++i) {
					boolean spawned = false;

					for (int attempt = 0; !spawned && attempt < 4; ++attempt) {
						BlockPos spawnPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, new BlockPos(posX, 0, posZ));

						if (canSpawnAtLocation(spawnEntry.type(), world, spawnPos)) {
							MobEntity mobEntity;

							try {
								mobEntity = (MobEntity) spawnEntry.type().create(world, SpawnReason.NATURAL);
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