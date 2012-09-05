package com.bergerkiller.bukkit.nolagg.spawnlimiter;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.common.utils.EntityUtil;

public class WorldLimiter extends GroupLimiter {

	private final GroupLimiter global;

	public WorldLimiter(final GroupLimiter global) {
		this.global = global;
	}

	/*
	 * Entity limiters generated from group limiters
	 */
	private final Map<String, EntityLimiter> genEntities = new HashMap<String, EntityLimiter>();

	public EntityLimiter getEntityLimits(Entity entity) {
		return getEntityLimits(EntityUtil.getName(entity));
	}

	public EntityLimiter getEntityLimits(ItemStack item) {
		return getEntityLimits("item" + item.getType());
	}

	public EntityLimiter getEntityLimits(String name) {
		name = name.toLowerCase();
		EntityLimiter lim = genEntities.get(name);
		if (lim == null) {
			lim = new EntityLimiter(name, this.global, this);
			genEntities.put(name, lim);
		}
		return lim;
	}

}
