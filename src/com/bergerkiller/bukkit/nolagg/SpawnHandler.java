package com.bergerkiller.bukkit.nolagg;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;

public class SpawnHandler {
	
	private static HashMap<String, SpawnLimiter> worldLimits = new HashMap<String, SpawnLimiter>();
	private static SpawnLimiter defaultLimits = new SpawnLimiter();
	
	private static SpawnLimiter getLimiter(String worldname) {
		SpawnLimiter limiter = worldLimits.get(worldname.toLowerCase());
		if (limiter == null) return defaultLimits;
		return limiter;
	}
	
	public static void setLimit(String worldname, String typename, int amount) {
		SpawnLimiter limiter = null;
		if (worldname != null && !worldname.equals("")) {
			worldname = worldname.toLowerCase();
			limiter = worldLimits.get(worldname);
			if (limiter == null) {
				limiter = new SpawnLimiter();
				worldLimits.put(worldname, limiter);
			}
		} else {
			limiter = defaultLimits;
		}
		limiter.addLimit(typename, amount);
	}
	
	public static boolean canSpawn(String worldname, Object object) {
		if (getLimiter(worldname).canSpawn(object)) {
			return true;
		} else {
			return false;
		}
	}
	public static boolean canSpawn(World world, Object object) {
		return canSpawn(world.getName(), object);
	}
	public static boolean canSpawn(Entity entity) {
		return canSpawn(entity.getWorld(), entity);
	}
	
	public static boolean handleSpawn(Entity entity) {
		if (!entity.isDead()) {
			if (!canSpawn(entity)) {
				entity.remove();
			} else {
				return true;
			}
		}
		return false;
	}
	public static boolean handleSpawn(ItemSpawnEvent event) {
		if (!event.isCancelled()) {
			if (!canSpawn(event.getEntity())) {
				event.setCancelled(true);
			} else {
				return true;
			}
		}
		return false;
	}
	public static boolean handleSpawn(VehicleCreateEvent event) {
		return handleSpawn(event.getVehicle());
	}
	public static boolean handleSpawn(ExplosionPrimeEvent event) {
		if (!event.isCancelled()) {
			if (!canSpawn(event.getEntity())) {
				event.setCancelled(true);
			} else {
				return true;
			}
		}
		return false;
	}
	public static boolean handleSpawn(CreatureSpawnEvent event) {
		if (!event.isCancelled()) {
			if (!canSpawn(event.getEntity())) {
				event.setCancelled(true);
			} else {
				return true;
			}
		}
		return false;
	}
	
	public static void remove(World world, Object object) {
		getLimiter(world.getName()).remove(object);
	}
	public static void remove(Entity entity) {
		remove(entity.getWorld(), entity);
	}
	public static void remove(Object object) {
		if (object instanceof Entity) {
			remove((Entity) object);
		}
	}
	
	private static int updateTask = -1;
	public static void update() {
		clear();
		//Update spawned creatures
		for (World w : Bukkit.getServer().getWorlds()) {
			for (Entity e : w.getEntities().toArray(new Entity[0])) {
				handleSpawn(e);
			}
		}
	}
	
	public static void init() {
		updateTask = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(NoLagg.plugin, new Runnable() {
			public void run() {
				update();
			}
		}, 0, 20);
	}
	private static void clear() {
		for (SpawnLimiter limiter : worldLimits.values()) {
			limiter.deinit();
		}
		defaultLimits.deinit();
	}
	public static void deinit() {
		if (updateTask != -1) {
			Bukkit.getServer().getScheduler().cancelTask(updateTask);
		}
		clear();
	}
}
