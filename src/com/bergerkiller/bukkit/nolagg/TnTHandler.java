package com.bergerkiller.bukkit.nolagg;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import net.minecraft.server.Packet60Explosion;
import net.minecraft.server.ServerConfigurationManager;
import net.minecraft.server.WorldServer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.TNTPrimed;

public class TnTHandler {

	private static Queue<Block> todo = new LinkedList<Block>();
	private static HashSet<Location> added = new HashSet<Location>();
	private static int taskId = -1;
	public static int interval = 1;
	public static int rate = 10;
	private static long explosionInterval = 20;
	private static long lastExplosionTime = 0;
	
	public static void setExplosionRate(double ratePerSecond) {
		explosionInterval = (long) (1000 / ratePerSecond);
	}
	
	private static int nextRandom(World w, int n) {
		net.minecraft.server.World world = ((CraftWorld) w).getHandle();
		return world.random.nextInt(n);
	}
	
	private static void startTask() {
		if (added.size() == 0 || taskId == -1) {
			//start the task
			taskId = NoLagg.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(NoLagg.plugin, new Runnable() {
				public void run() {
					for (int i = 0; i < rate; i++) {
						if (todo.size() == 0) {
							NoLagg.plugin.getServer().getScheduler().cancelTask(taskId);
							taskId = -1;
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
						}
					}
				}
			}, 0, interval);
		}
	}
	
	public static void clear() {
		todo.clear();
		added.clear();
	}
	
	public static boolean detonate(Block tntBlock) {
		if (tntBlock != null && tntBlock.getType() == Material.TNT) {
			if (!added.contains(tntBlock.getLocation())) {
				todo.offer(tntBlock);
				added.add(tntBlock.getLocation());
				startTask();
				return true;
			}
		}
		return false;
	}
		
	@SuppressWarnings("rawtypes")
	public static void createExplosion(Location at, List<Block> affectedBlocks, float yield) {
		try {
			WorldServer world = ((CraftWorld) at.getWorld()).getHandle();
			
			for (Block b : affectedBlocks) {
				 
				int id = b.getTypeId();
				 
	            if (id == Material.TNT.getId()) {
					TnTHandler.detonate(b);
	            } else {
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
			long time = System.currentTimeMillis();
			if ((time - lastExplosionTime) > explosionInterval) {
				lastExplosionTime = time;
				Packet60Explosion packet = new Packet60Explosion(at.getX(), at.getY(), at.getZ(), yield, new HashSet());
				ServerConfigurationManager manager = world.server.serverConfigurationManager;
				manager.sendPacketNearby(at.getX(), at.getY(), at.getZ(), 64.0D, world.dimension, packet);
			}
		} catch (Throwable t) {
			System.out.println("[NoLagg] Warning: explosion did not go as planned!");
			t.printStackTrace();
		}
	}
		
}
