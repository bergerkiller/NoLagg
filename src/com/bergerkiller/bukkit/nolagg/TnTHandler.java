package com.bergerkiller.bukkit.nolagg;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import net.minecraft.server.ChunkPosition;
import net.minecraft.server.Packet60Explosion;
import net.minecraft.server.ServerConfigurationManager;
import net.minecraft.server.WorldServer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityExplodeEvent;

public class TnTHandler {

	private static Queue<Block> todo = new LinkedList<Block>();
	private static HashSet<Location> added = new HashSet<Location>();
	private static int taskId = -1;
	public static int interval = 1;
	public static int rate = 10;
	public static int explosionRate = 5;
	private static long sentExplosions = 0;
	private static long intervalCounter = 0;
		
	public static void init() {
		//start the task
		if (NoLagg.bufferTNT && interval > 0) {
			taskId = NoLagg.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(NoLagg.plugin, new Runnable() {
				public void run() {
					if (denyExplosionsCounter > 0) {
						--denyExplosionsCounter;
					}
					if (intervalCounter == interval) {
						intervalCounter = 1;
						for (int i = 0; i < rate; i++) {
							if (todo.size() == 0) {
								return;
							} else {
								Block next = todo.poll();
								Location l = next.getLocation();
								added.remove(l);
								if (next.getType() == Material.TNT) {
									next.setTypeId(0);
									TNTPrimed tnt = l.getWorld().spawn(l.add(0.5, 0.5, 0.5), TNTPrimed.class);
									int fuse = tnt.getFuseTicks();
									fuse = nextRandom(tnt.getWorld(), fuse / 4) + fuse / 8;
									//entitytntprimed.fuseTicks = world.random.nextInt(entitytntprimed.fuseTicks / 4) + entitytntprimed.fuseTicks / 8;
									tnt.setFuseTicks(fuse);
								}
								sentExplosions = 0;
							}
						}
					} else {
						intervalCounter++;
					}
				}
			}, 0, 1);
		}
	}
	public static void deinit() {
		if (taskId != -1) {
			NoLagg.plugin.getServer().getScheduler().cancelTask(taskId);
		}
	}
	
	private static int nextRandom(World w, int n) {
		net.minecraft.server.World world = ((CraftWorld) w).getHandle();
		return world.random.nextInt(n);
	}
		
	private static int denyExplosionsCounter = 0;
	public static void clear() {
		todo.clear();
		added.clear();
		denyExplosionsCounter = 5;
	}
	
	/*
	 * Detonates TNT and creates explosions
	 * Returns false if it was not possible to do in any way
	 * (Including if the feature is disabled)
	 */
	public static boolean detonate(Block tntBlock) {
		if (interval <= 0) return false;
		if (tntBlock != null && tntBlock.getType() == Material.TNT) {
			if (!added.contains(tntBlock.getLocation())) {
				todo.offer(tntBlock);
				added.add(tntBlock.getLocation());
				return true;
			}
		}
		return false;
	}
			
	public static boolean createExplosion(EntityExplodeEvent event) {
		return createExplosion(event.getLocation(), event.blockList(), event.getYield());
	}
	public static boolean createExplosion(Location at, List<Block> affectedBlocks, float yield) {
		if (interval > 0 && NoLagg.bufferTNT) {
			if (denyExplosionsCounter == 0) {
				try {
					WorldServer world = ((CraftWorld) at.getWorld()).getHandle();
					for (Block b : affectedBlocks) {
						 
						int id = b.getTypeId();
						 
			            if (id == Material.TNT.getId()) {
							TnTHandler.detonate(b);
			            } else if (id != Material.BEDROCK.getId() && id != Material.OBSIDIAN.getId()) {
			    			int x = b.getLocation().getBlockX();
			    			int y = b.getLocation().getBlockY();
			    			int z = b.getLocation().getBlockZ();
			            	if (id > 0 && id != Material.FIRE.getId()) {
			            		net.minecraft.server.Block bb = net.minecraft.server.Block.byId[id];
			            		if (bb != null) {
			            			bb.dropNaturally(world, x, y, z, world.getData(x, y, z), yield);
			            		}
			            	}
			                world.setTypeId(x, y, z, 0);
			            }
					}
					if (sentExplosions < explosionRate) {
						++sentExplosions;
						Packet60Explosion packet = new Packet60Explosion(at.getX(), at.getY(), at.getZ(), yield, new HashSet<ChunkPosition>());
						ServerConfigurationManager manager = world.server.serverConfigurationManager;
						manager.sendPacketNearby(at.getX(), at.getY(), at.getZ(), 64.0D, world.dimension, packet);
					}
				} catch (Throwable t) {
					System.out.println("[NoLagg] Warning: explosion did not go as planned!");
					t.printStackTrace();
				}
			}
			return true;
		} else {
			return false;
		}
	}
		
}
