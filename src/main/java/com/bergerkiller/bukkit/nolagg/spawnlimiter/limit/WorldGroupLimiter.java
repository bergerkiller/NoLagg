package com.bergerkiller.bukkit.nolagg.spawnlimiter.limit;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates Entity Limits using a global and world-specific set of spawn limits
 */
public class WorldGroupLimiter extends GroupLimiter {
	// Global limits to apply
	private final GroupLimiter global;
	// Entity limiters generated from group limiters
	private final Map<String, EntityLimit> genEntities = new HashMap<String, EntityLimit>();

	public WorldGroupLimiter(final GroupLimiter global, GroupLimiter limiter) {
		this.global = global;
		this.mobLimiter.limit = limiter.mobLimiter.limit;
		this.animalLimiter.limit = limiter.animalLimiter.limit;
		this.monsterLimiter.limit = limiter.monsterLimiter.limit;
		this.itemLimiter.limit = limiter.itemLimiter.limit;
		this.fallingBlockLimiter.limit = limiter.fallingBlockLimiter.limit;
		this.entityLimiters.clear();
		for (Map.Entry<String, SpawnLimit> elim : limiter.entityLimiters.entrySet()) {
			this.entityLimiters.put(elim.getKey(), new SpawnLimit(elim.getValue().limit));
		}
	}

	/**
	 * Gets the Entity limits for an Entity name
	 * 
	 * @param name of the Entity
	 * @return Entity Limits for that Entity
	 */
	public EntityLimit getEntityLimits(String name) {
		name = name.toLowerCase();
		EntityLimit lim = genEntities.get(name);
		if (lim == null) {
			lim = new EntityLimit(name, this.global, this);
			genEntities.put(name, lim);
		}
		return lim;
	}
}
