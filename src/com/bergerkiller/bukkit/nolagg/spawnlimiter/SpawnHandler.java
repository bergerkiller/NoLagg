package com.bergerkiller.bukkit.nolagg.spawnlimiter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.bukkit.World;
import org.bukkit.entity.Entity;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;

public class SpawnHandler {
	private static WeakHashMap<Entity, Boolean> ignoredEntities = new WeakHashMap<Entity, Boolean>();
	private static WeakHashMap<Entity, Boolean> mobSpawnedEntities = new WeakHashMap<Entity, Boolean>();

	public static void ignoreSpawn(Entity entity) {
		ignoredEntities.put(entity, true);
	}

	public static boolean isIgnored(Entity entity) {
		return ignoredEntities.containsKey(entity);
	}

	public static void mobSpawnerSpawned(Entity entity) {
		mobSpawnedEntities.put(entity, true);
	}

	public static boolean isMobSpawnerSpawned(Entity entity) {
		return mobSpawnedEntities.containsKey(entity);
	}

	public static SpawnHandler MOBSPAWNERHANDLER = new SpawnHandler();
	public static SpawnHandler GENERALHANDLER = new SpawnHandler();

	public final GroupLimiter GLOBAL = new GroupLimiter();
	public final GroupLimiter WORLDDEFAULT = new GroupLimiter();
	private final Map<String, WorldLimiter> limiters = new HashMap<String, WorldLimiter>();

	public WorldLimiter getWorldLimiter(World world) {
		return getWorldLimiter(world.getName());
	}

	public WorldLimiter getWorldLimiter(String worldname) {
		worldname = worldname.toLowerCase();
		WorldLimiter lim = limiters.get(worldname);
		if (lim == null) {
			lim = new WorldLimiter(this.GLOBAL);
			lim.load(WORLDDEFAULT);
			limiters.put(worldname, lim);
		}
		return lim;
	}

	public static boolean isItem(String name) {
		return name.toLowerCase().startsWith("item");
	}

	public static boolean isFalling(String name) {
		return name.toLowerCase().startsWith("falling");
	}

	public static EntityLimiter getEntityLimits(Entity entity) {
		if (isIgnored(entity))
			return new EntityLimiter();
		if (isMobSpawnerSpawned(entity)) {
			return MOBSPAWNERHANDLER.getWorldLimiter(entity.getWorld()).getEntityLimits(entity);
		} else {
			return GENERALHANDLER.getWorldLimiter(entity.getWorld()).getEntityLimits(entity);
		}
	}

	public EntityLimiter getEntityLimits(World world, String entityname) {
		return getWorldLimiter(world).getEntityLimits(entityname);
	}

	public static void handleDespawn(Entity entity) {
		if (ignoredEntities.remove(entity) != null)
			return;
		getEntityLimits(entity).despawn();
		mobSpawnedEntities.remove(entity);
	}

	public static void reset() {
		MOBSPAWNERHANDLER.resetHandler();
		GENERALHANDLER.resetHandler();
	}

	public static void clear() {
		MOBSPAWNERHANDLER.clearHandler();
		GENERALHANDLER.clearHandler();
	}

	public void load(ConfigurationNode limits) {
		ConfigurationNode node = limits.getNode("default");
		for (String key : node.getKeys()) {
			WORLDDEFAULT.setLimit(key, node.get(key, -1));
		}
		node = limits.getNode("global");
		for (String key : node.getKeys()) {
			GLOBAL.setLimit(key, node.get(key, -1));
		}
		node = limits.getNode("worlds");
		for (ConfigurationNode cn : node.getNodes()) {
			String world = cn.getName();
			for (String key : cn.getKeys()) {
				getWorldLimiter(world).setLimit(key, cn.get(key, -1));
			}
		}
	}

	private void resetHandler() {
		GLOBAL.reset();
		for (WorldLimiter lim : limiters.values())
			lim.reset();
	}

	private void clearHandler() {
		GLOBAL.clear();
		WORLDDEFAULT.clear();
		limiters.clear();
	}

	public static void update(World world, List<Entity> entities) {
		// general entities
		WorldLimiter limmob = MOBSPAWNERHANDLER.getWorldLimiter(world);
		WorldLimiter limgen = GENERALHANDLER.getWorldLimiter(world);
		for (Entity e : entities) {
			if (ignoredEntities.containsKey(e))
				continue;
			if (isMobSpawnerSpawned(e)) {
				if (limmob.getEntityLimits(e).handleSpawn())
					continue;
			} else {
				if (limgen.getEntityLimits(e).handleSpawn())
					continue;
			}
			e.remove();
		}
	}
}
