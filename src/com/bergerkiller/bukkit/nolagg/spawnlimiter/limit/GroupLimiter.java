package com.bergerkiller.bukkit.nolagg.spawnlimiter.limit;

import java.util.HashMap;
import java.util.Map;

import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.nolagg.spawnlimiter.EntitySpawnHandler;

/**
 * Contains all the entity limit types and the grouped types
 * Can return spawn limits from that
 */
public class GroupLimiter {
	public final SpawnLimit mobLimiter = new SpawnLimit();
	public final SpawnLimit animalLimiter = new SpawnLimit();
	public final SpawnLimit monsterLimiter = new SpawnLimit();
	public final SpawnLimit itemLimiter = new SpawnLimit();
	public final SpawnLimit fallingBlockLimiter = new SpawnLimit();
	protected final Map<String, SpawnLimit> entityLimiters = new HashMap<String, SpawnLimit>();

	/**
	 * Configures a limit for an Entity
	 * 
	 * @param entityname for the limit
	 * @param limit to set
	 */
	public void setLimit(String entityname, int limit) {
		entityname = entityname.toLowerCase();
		if (entityname.contains("tnt"))
			entityname = "tnt";
		if (entityname.equals("mob") || entityname.equals("mobs")) {
			mobLimiter.limit = limit;
		} else if (entityname.equals("animal") || entityname.equals("animals")) {
			animalLimiter.limit = limit;
		} else if (entityname.equals("monster") || entityname.equals("monsters")) {
			monsterLimiter.limit = limit;
		} else if (entityname.equals("item") || entityname.equals("items")) {
			itemLimiter.limit = limit;
		} else if (entityname.equals("fallingblock") || entityname.equals("fallingblocks")) {
			fallingBlockLimiter.limit = limit;
		} else if (limit >= 0) {
			entityLimiters.put(entityname, new SpawnLimit(limit));
		} else {
			entityLimiters.remove(entityname);
		}
	}

	/**
	 * Clears all set entity spawn limits
	 */
	public void clear() {
		mobLimiter.clear();
		animalLimiter.clear();
		monsterLimiter.clear();
		itemLimiter.clear();
		fallingBlockLimiter.clear();
		entityLimiters.clear();
	}

	/**
	 * Gets all the spawn limits set for the entity name specified
	 * 
	 * @param name of the Entity
	 * @return spawn limits for this Entity
	 */
	protected SpawnLimit[] getLimits(String name) {
		name = name.toLowerCase();
		if (EntitySpawnHandler.isItem(name)) {
			return new SpawnLimit[] { entityLimiters.get(name), itemLimiter };
		} else if (EntityUtil.isAnimal(name)) {
			return new SpawnLimit[] { entityLimiters.get(name), animalLimiter, mobLimiter };
		} else if (EntityUtil.isMonster(name)) {
			return new SpawnLimit[] { entityLimiters.get(name), monsterLimiter, mobLimiter };
		} else if (EntitySpawnHandler.isFalling(name)) {
			return new SpawnLimit[] { entityLimiters.get(name), fallingBlockLimiter };
		} else {
			return new SpawnLimit[] { entityLimiters.get(name) };
		}
	}
}
