package com.bergerkiller.bukkit.nolagg;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

import net.minecraft.server.WorldServer;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;

public class SpawnHandler {
	private static WeakHashMap<Entity, Boolean> spawnIgnore = new WeakHashMap<Entity, Boolean>();
	
	private static HashMap<String, SpawnLimiter> worldLimits = new HashMap<String, SpawnLimiter>();
	private static SpawnLimiter defaultLimits = new SpawnLimiter();
	private static SpawnLimiter globalLimits = new SpawnLimiter();

	public static void deinit() {
		worldLimits.clear();
		worldLimits = null;
		defaultLimits = null;
		globalLimits = null;
		spawnIgnore.clear();
		spawnIgnore = null;
	}
	
	private static SpawnLimiter getWorldLimits(String worldname) {
		if (worldname == null || worldLimits == null) {
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
		if (globalLimits == null) return true;
		if (worldLimits == null) return true;
		return globalLimits.canSpawn(object) && getWorldLimits(worldname).canSpawn(object);
	}
	public static boolean canSpawn(World world, Object object) {
		return canSpawn(world.getName(), object);
	}
	public static boolean canSpawn(Entity entity) {
		if (spawnIgnore != null && spawnIgnore.containsKey(entity)) {
		    return true;
		} else {
			return canSpawn(entity.getWorld(), entity);
		}
	}
	
	public static void addSpawn(String worldname, Object object) {
		if (worldLimits == null || globalLimits == null) return;
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
		if (spawnIgnore == null) return;
		spawnIgnore.put(entity, true);
	}
	
	public static void removeSpawn(String worldname, Object object) {
		if (worldLimits == null || globalLimits == null) return;
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
		
	public static void reset() {
		if (!NoLagg.useSpawnLimits) return;
		if (worldLimits == null || globalLimits == null) return;
		for (SpawnLimiter limiter : worldLimits.values()) {
			limiter.reset();
		}
		globalLimits.reset();
	}
		
	public static void update(WorldServer ws, List<Entity> entities) {
		if (!NoLagg.useSpawnLimits) return;
		if (worldLimits == null || globalLimits == null) return;
		SpawnLimiter limiter = getWorldLimits(ws.getWorld().getName());
		for (Entity e : entities) {
			if (e instanceof Player) continue;
			if (e instanceof LivingEntity) {
				if (spawnIgnore.containsKey(e)) continue;
				if (globalLimits.canSpawn(e)) {
					if (limiter.canSpawn(e)) {
						limiter.addSpawn(e);
						globalLimits.addSpawn(e);
					} else {
						e.remove();
					}
				}
			}
		}
		Collections.reverse(entities);
		for (Entity e : entities) {
			if (!(e instanceof LivingEntity)) {
				if (spawnIgnore.containsKey(e)) continue;
				if (globalLimits.canSpawn(e)) {
					if (limiter.canSpawn(e)) {
						limiter.addSpawn(e);
						globalLimits.addSpawn(e);
					} else {
						e.remove();
					}
				}
			}
		}
	}
	
}
