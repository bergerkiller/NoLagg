package com.bergerkiller.bukkit.nolagg.tnt;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityExplodeEvent;

import com.bergerkiller.bukkit.common.BlockLocation;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.collections.BlockSet;
import com.bergerkiller.bukkit.common.protocol.PacketFields;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.wrappers.BlockInfo;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class TNTHandler {
	private static Queue<Block> todo = new LinkedList<Block>();
	private static BlockSet added = new BlockSet();
	private static Task task;
	public static int interval;
	public static int rate;
	public static int explosionRate;
	private static long sentExplosions = 0;
	private static long intervalCounter = 0;
	public static boolean changeBlocks = true;

	public static int getBufferCount() {
		return todo.size();
	}

	public static void init() {
		// start the task
		if (interval > 0) {
			task = new Task(NoLagg.plugin) {
				public void run() {
					if (added == null)
						return;
					if (todo == null)
						return;
					if (denyExplosionsCounter > 0) {
						--denyExplosionsCounter;
					}
					sentExplosions = 0;
					if (intervalCounter == interval) {
						intervalCounter = 1;
						CustomExplosion.useQuickDamageMode = todo.size() > 500;
						if (todo.isEmpty()) {
							return;
						}
						for (int i = 0; i < rate; i++) {
							Block next = todo.poll();
							if (next == null)
								break;
							added.remove(next);
							int x = next.getX();
							int y = next.getY();
							int z = next.getZ();
							int cx = x >> 4;
							int cz = z >> 4;
							int dcx, dcz;
							boolean isLoaded = true;
							for (dcx = -2; dcx <= 5 && isLoaded; dcx++) {
								for (dcz = -2; dcz <= 5 && isLoaded; dcz++) {
									if (!WorldUtil.isLoaded(next.getWorld(), cx + dcx, cz+ dcz)) {
										isLoaded = false;
									}
								}
							}
							if (isLoaded) {
								org.bukkit.Chunk chunk = next.getChunk();
								if (WorldUtil.getBlockTypeId(chunk, x, y, z) == Material.TNT.getId()) {
									WorldUtil.setBlock(chunk, x, y, z, 0, 0);

									TNTPrimed tnt = next.getWorld().spawn(next.getLocation().add(0.5, 0.5, 0.5), TNTPrimed.class);
									int fuse = tnt.getFuseTicks();
									fuse = nextRandom(tnt.getWorld(), fuse >> 2) + fuse >> 3;
									tnt.setFuseTicks(fuse);
								}
							}
						}
					} else {
						intervalCounter++;
					}
				}
			}.start(1, 1);
		}
	}

	public static void deinit() {
		Task.stop(task);
		added = null;
		todo = null;
	}

	private static int nextRandom(World w, int n) {
		return WorldUtil.getRandom(w).nextInt(n);
	}

	private static int denyExplosionsCounter = 0; // tick countdown to deny
													// explosions

	public static void clear(World world) {
		Iterator<BlockLocation> iter = added.iterator();
		while (iter.hasNext()) {
			if (iter.next().world.equals(world.getName())) {
				iter.remove();
			}
		}
		Iterator<Block> iter2 = todo.iterator();
		while (iter2.hasNext()) {
			if (iter2.next().getWorld() == world) {
				iter.remove();
			}
		}
		denyExplosionsCounter = 5;
	}

	public static void clear() {
		todo.clear();
		added.clear();
		denyExplosionsCounter = 5;
	}

	public static boolean isScheduledForDetonation(Block block) {
		return added.contains(block);
	}

	/**
	 * Detonates TNT and creates explosions Returns false if it was not possible
	 * to do in any way (Including if the feature is disabled)
	 */
	public static boolean detonate(Block tntBlock) {
		if (added == null || todo == null || interval <= 0 || tntBlock == null || !added.add(tntBlock)) {
			return false;
		}
		todo.offer(tntBlock);
		return true;
	}

	public static boolean createExplosion(EntityExplodeEvent event) {
		return createExplosion(event.getLocation(), event.blockList(), event.getYield());
	}

	public static boolean createExplosion(Location at, List<Block> affectedBlocks, float yield) {
		if (interval > 0) {
			if (denyExplosionsCounter == 0) {
				try {
					int id;
					for (Block b : affectedBlocks) {
						id = b.getTypeId();
						if (id == Material.TNT.getId()) {
							detonate(b);
						} else if (id != Material.FIRE.getId()) {
							BlockInfo.get(b).destroy(b, yield);
						}
					}
					if (sentExplosions < explosionRate) {
						sentExplosions++;

						// Explosion effect
						Object explosionPacket = PacketFields.EXPLOSION.newInstance(at.getX(), at.getY(), at.getZ(), yield);
						PacketUtil.broadcastPacketNearby(at, 64.0, explosionPacket);

						// Sound (with random pitch)
						final Random random = WorldUtil.getRandom(at.getWorld());
						final float pitch = (1.0f + (random.nextFloat() - random.nextFloat()) * 0.2f) * 0.7f;
						at.getWorld().playSound(at, Sound.EXPLODE, 4.0f, pitch);
					}
				} catch (Throwable t) {
					NoLaggTNT.plugin.log(Level.WARNING, "Explosion did not go as planned!");
					t.printStackTrace();
				}
			}
			return true;
		} else {
			return false;
		}
	}
}
