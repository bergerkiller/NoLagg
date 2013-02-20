package com.bergerkiller.bukkit.nolagg.tnt;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;

import net.minecraft.server.v1_4_R1.Packet60Explosion;
import net.minecraft.server.v1_4_R1.WorldServer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityExplodeEvent;

import com.bergerkiller.bukkit.common.BlockLocation;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.collections.BlockSet;
import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.NativeUtil;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
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
		return NativeUtil.getNative(w).random.nextInt(n);
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

	/*
	 * Detonates TNT and creates explosions Returns false if it was not possible
	 * to do in any way (Including if the feature is disabled)
	 */
	public static boolean detonate(Block tntBlock) {
		if (added == null)
			return false;
		if (todo == null)
			return false;
		if (interval <= 0)
			return false;
		if (tntBlock != null) { // && tntBlock.getType() == Material.TNT) {
			if (added.add(tntBlock)) {
				todo.offer(tntBlock);
				return true;
			}
		}
		return false;
	}

	private static boolean allowdrops = true;

	public static boolean createExplosion(EntityExplodeEvent event) {
		return createExplosion(event.getLocation(), event.blockList(), event.getYield());
	}

	public static boolean createExplosion(Location at, List<Block> affectedBlocks, float yield) {
		if (interval > 0) {
			if (denyExplosionsCounter == 0) {
				try {
					WorldServer world = NativeUtil.getNative(at.getWorld());
					int id;
					for (Block b : affectedBlocks) {
						id = b.getTypeId();
						if (id == Material.TNT.getId()) {
							detonate(b);
						} else {
							if (id != Material.FIRE.getId()) {
								if (allowdrops) {
									BlockUtil.dropNaturally(b, yield);
								}
								b.setTypeId(0);
							}
						}
					}
					if (sentExplosions < explosionRate) {
						++sentExplosions;
						Packet60Explosion packet = new Packet60Explosion(at.getX(), at.getY(), at.getZ(), yield, Collections.EMPTY_LIST, null);
						PacketUtil.broadcastPacketNearby(at, 64.0, packet);
						world.makeSound(at.getX(), at.getY(), at.getZ(), "random.explode", 4.0f, (1.0f + (world.random.nextFloat() - world.random.nextFloat()) * 0.2f) * 0.7f);
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
