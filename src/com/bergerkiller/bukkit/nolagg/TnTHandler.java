package com.bergerkiller.bukkit.nolagg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

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
	public static int rate = 1;
	
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
		
}
