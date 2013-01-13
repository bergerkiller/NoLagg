package com.bergerkiller.bukkit.nolagg.spawnlimiter.limit;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;

/**
 * Handles all the Entity limits for certain Entities on the entire server<br>
 * This excludes the mob-spawner spawned and default entity limits.
 */
public class EntitySpawnLimiter {
	public final GroupLimiter GLOBAL = new GroupLimiter();
	public final GroupLimiter WORLDDEFAULT = new GroupLimiter();
	protected final Map<String, WorldGroupLimiter> limiters = new HashMap<String, WorldGroupLimiter>();

	private WorldGroupLimiter getWorldLimiter(String worldname) {
		worldname = worldname.toLowerCase();
		WorldGroupLimiter lim = limiters.get(worldname);
		if (lim == null) {
			lim = new WorldGroupLimiter(this.GLOBAL, WORLDDEFAULT);
			limiters.put(worldname, lim);
		}
		return lim;
	}

	/**
	 * Gets the Entity limits set for the entity in the world specified
	 * 
	 * @param world the entity is in
	 * @param entityname of the Entity
	 * @return Entity limits for that Entity
	 */
	public EntityLimit getEntityLimits(World world, String entityname) {
		return getWorldLimiter(world.getName()).getEntityLimits(entityname);
	}

	/**
	 * Clears all the limits set for this section of Entity limits
	 * 
	 * @return This Entity Spawn Limiter
	 */
	public EntitySpawnLimiter clear() {
		GLOBAL.clear();
		WORLDDEFAULT.clear();
		limiters.clear();
		return this;
	}

	/**
	 * Loads all the Entity limits from the configuration node specified
	 * 
	 * @param limits node to load from
	 */
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
}
