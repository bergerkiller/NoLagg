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
import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.protocol.PacketType;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.wrappers.BlockInfo;
import com.bergerkiller.bukkit.nolagg.NoLagg;

/**
 * Deals with all NoLagg TNT routines to handle and schedule TNT explosions
 */
public class TNTHandler {
	private Queue<Block> todo = new LinkedList<Block>();
	private BlockSet added = new BlockSet();
	private Task task;
	private int interval = 1;
	private int rate = 10;
	private int explosionRate = 5;
	private long sentExplosions = 0;
	private long intervalCounter = 0;
	private boolean changeBlocks = true;
	private int denyExplosionsCounter = 0; // tick countdown to deny explosions

	/**
	 * Gets the amount of TNT currently buffered for detonation
	 * 
	 * @return tnt block count
	 */
	public int getBufferCount() {
		return todo.size();
	}

	/**
	 * Initializes the TNT buffering and starts the detonation task.
	 * 
	 * @param node - ConfigurationNode to load the settings from
	 */
	public void init(ConfigurationNode node) {
		// Load settings
		node.setHeader("detonationInterval", "The interval (in ticks) at which TNT is detonated by explosions");
		node.setHeader("detonationRate", "How many TNT is detonated by explosions per interval");
		node.setHeader("explosionRate", "The amount of explosion packets to send to the clients per tick");
		node.setHeader("changeBlocks", "If TNT explosions can change non-TNT blocks");
		interval = node.get("detonationInterval", interval);
		rate = node.get("detonationRate", rate);
		explosionRate = node.get("explosionRate", explosionRate);
		changeBlocks = node.get("changeBlocks", changeBlocks);

		// Stop the old task
		if (task != null) {
			Task.stop(task);
			task = null;
		}
		// Start the new task (if needed)
		if (interval > 0) {
			task = new Task(NoLagg.plugin) {
				public void run() {
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
							if (next == null) {
								break;
							}
							added.remove(next);
							// This extra getting is needed to avoid chunk unloads bugging blocks (thanks BUKKIT!)
							next = next.getWorld().getBlockAt(next.getX(), next.getY(), next.getZ());

							// Chunk loaded check
							if (!WorldUtil.areChunksLoaded(next.getWorld(), next.getChunk().getX(), next.getChunk().getZ(), 2)) {
								continue;
							}

							// Detonate the block if TNT
							if (MaterialUtil.isType(next, Material.TNT)) {
								next.setType(Material.AIR);

								// Spawn a mobile TNT entity
								TNTPrimed tnt = next.getWorld().spawn(next.getLocation().add(0.5, 0.5, 0.5), TNTPrimed.class);
								int fuse = tnt.getFuseTicks();
								fuse = WorldUtil.getRandom(tnt.getWorld()).nextInt(fuse >> 2) + fuse >> 3;
								tnt.setFuseTicks(fuse);
							}
						}
					} else {
						intervalCounter++;
					}
				}
			}.start(1, 1);
		}
	}

	/**
	 * De-initializes the TNT buffering, disabling the entire Class
	 */
	public void deinit() {
		Task.stop(task);
		task = null;
	}

	/**
	 * Clears all scheduled TNT detonations for a world
	 * 
	 * @param world to clear for
	 */
	public void clear(World world) {
		Iterator<BlockLocation> iter = added.iterator();
		while (iter.hasNext()) {
			if (iter.next().world.equals(world.getName())) {
				iter.remove();
			}
		}
		Iterator<Block> iter2 = todo.iterator();
		while (iter2.hasNext()) {
			if (iter2.next().getWorld() == world) {
				iter2.remove();
			}
		}
		denyExplosionsCounter = 5;
	}

	/**
	 * Clears all scheduled TNT detonations on the server
	 */
	public void clear() {
		todo.clear();
		added.clear();
		denyExplosionsCounter = 5;
	}

	/**
	 * Checks whether a given block is allowed to be exploded by TNT
	 * 
	 * @param block that exploded
	 * @return True if allowed, False if not
	 */
	public boolean isExplosionAllowed(Block block) {
		return !isScheduledForDetonation(block) && (changeBlocks || MaterialUtil.isType(block, Material.TNT));
	}

	/**
	 * Checks whether a certain TNT block is scheduled for detonation
	 * 
	 * @param block to check
	 * @return True if scheduled for detonation, False if not
	 */
	public boolean isScheduledForDetonation(Block block) {
		return added.contains(block);
	}

	/**
	 * Detonates TNT and creates explosions Returns false if it was not possible
	 * to do in any way (Including if the feature is disabled)
	 */
	public boolean detonate(Block tntBlock) {
		if (interval <= 0 || tntBlock == null || !added.add(tntBlock)) {
			return false;
		}
		todo.offer(tntBlock);
		return true;
	}

	public boolean createExplosion(EntityExplodeEvent event) {
		return createExplosion(event.getLocation(), event.blockList(), event.getYield());
	}

	public boolean createExplosion(Location at, List<Block> affectedBlocks, float yield) {
		if (interval > 0) {
			if (denyExplosionsCounter == 0) {
				try {
					for (Block b : affectedBlocks) {
						Material type = b.getType();
						if (type == Material.TNT) {
							detonate(b);
						} else if (type != Material.FIRE) {
							BlockInfo.get(type).destroy(b, yield);
						}
					}
					if (sentExplosions < explosionRate) {
						sentExplosions++;

						// Explosion effect
						Object explosionPacket = PacketType.OUT_EXPLOSION.newInstance(at.getX(), at.getY(), at.getZ(), yield);
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
