package com.bergerkiller.bukkit.nolagg;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;

public class SpawnHandler {
	private static WeakHashMap<Entity, Boolean> spawnIgnore = new WeakHashMap<Entity, Boolean>();
	
	private static HashMap<String, SpawnLimiter> worldLimits = new HashMap<String, SpawnLimiter>();
	private static SpawnLimiter defaultLimits = new SpawnLimiter();
	private static SpawnLimiter globalLimits = new SpawnLimiter();

	private static SpawnLimiter getWorldLimits(String worldname) {
		if (worldname == null) {
			return null;
		} else {
			worldname = worldname.toLowerCase();
			SpawnLimiter l = worldLimits.get(worldname);
			if (l == null) {
				l = defaultLimits.clone();
				worldLimits.put(worldname, l);
			}
			return l;
		}
	}
	
	public static void setDefaultLimit(String name, int limit) {
		defaultLimits.setLimit(name, limit);
		if (worldLimits.size() > 0) {
			for (SpawnLimiter l : worldLimits.values()) {
				if (!l.hasLimit(name)) {
					l.setLimit(name, limit);
				}
			}
		}
	}
	public static void setGlobalLimit(String name, int limit) {
		globalLimits.setLimit(name, limit);
	}
	public static void setWorldLimit(String worldname, String name, int limit) {
		getWorldLimits(worldname).setLimit(name, limit);
	}
	
	public static boolean canSpawn(String worldname, Object object) {
		return globalLimits.canSpawn(object) && getWorldLimits(worldname).canSpawn(object);
	}
	public static boolean canSpawn(World world, Object object) {
		return canSpawn(world.getName(), object);
	}
	public static boolean canSpawn(Entity entity) {
		if (spawnIgnore.containsKey(entity)) {
		    return true;
		} else {
			return canSpawn(entity.getWorld(), entity);
		}
	}
	
	public static void addSpawn(String worldname, Object object) {
		getWorldLimits(worldname).addSpawn(object);
		globalLimits.addSpawn(object);
	}
	public static void addSpawn(World world, Object object) {
		addSpawn(world.getName(), object);
	}
	public static void addSpawn(Entity entity) {
		addSpawn(entity.getWorld(), entity);
	}
	
	public static void ignoreSpawn(Entity entity) {
		spawnIgnore.put(entity, true);
	}
	
	public static void removeSpawn(String worldname, Object object) {
		getWorldLimits(worldname).removeSpawn(object);
		globalLimits.removeSpawn(object);
	}
	public static void removeSpawn(World world, Object object) {
		removeSpawn(world.getName(), object);
	}
	public static void removeSpawn(Entity entity) {
		removeSpawn(entity.getWorld(), entity);
	}
	
	public static boolean handleSpawn(ItemSpawnEvent event) {
		if (NoLagg.useSpawnLimits) {
			if (canSpawn(event.getEntity())) {
				addSpawn(event.getEntity());
				return true;
			} else {
				event.setCancelled(true);
				return false;
			}
		} else {
			return true;
		}
	}
	public static boolean handleSpawn(CreatureSpawnEvent event) {
		if (NoLagg.useSpawnLimits) {
			if (canSpawn(event.getEntity())) {
				addSpawn(event.getEntity());
				return true;
			} else {
				event.setCancelled(true);
				return false;
			}
		} else {
			return true;
		}
	}
	public static boolean handleSpawn(Entity entity) {
		if (!NoLagg.useSpawnLimits) return true;
		if (!entity.isDead()) {
			if (canSpawn(entity)) {
				addSpawn(entity);
				return true;
			} else {
				entity.remove();
			}
		}
		return false;
	}
	public static boolean handleSpawn(VehicleCreateEvent event) {
		return handleSpawn(event.getVehicle());
	}
		
	public static void update() {
		//Clear old limit counts
		for (SpawnLimiter limiter : worldLimits.values()) {
			limiter.reset();
		}
		globalLimits.reset();
		//Update spawned creatures
		for (World w : Bukkit.getServer().getWorlds()) {
			List<Entity> entities = w.getEntities();
			Collections.reverse(entities);
			for (Entity e : entities) {
				handleSpawn(e);
			}
		}
	}
}
