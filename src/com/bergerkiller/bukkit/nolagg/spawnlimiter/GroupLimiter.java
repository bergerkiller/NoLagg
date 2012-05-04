package com.bergerkiller.bukkit.nolagg.spawnlimiter;

import java.util.HashMap;
import java.util.Map;

import com.bergerkiller.bukkit.common.utils.EntityUtil;

/*
 * Contains all the entity limit types and the grouped types
 * Can return an entity limiter based on that
 */
public class GroupLimiter {
		
	public final SpawnLimiter mobLimiter = new SpawnLimiter();
	public final SpawnLimiter animalLimiter = new SpawnLimiter();
	public final SpawnLimiter monsterLimiter = new SpawnLimiter();
	public final SpawnLimiter itemLimiter = new SpawnLimiter();
	public final SpawnLimiter fallingBlockLimiter = new SpawnLimiter();
	private final Map<String, SpawnLimiter> entityLimiters = new HashMap<String, SpawnLimiter>();
	
	public void setLimit(String entityname, int limit) {
		if (entityname.contains("tnt")) entityname = "tnt";
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
			entityLimiters.put(entityname, new SpawnLimiter(limit));
		} else {
			entityLimiters.remove(entityname);
		}
	}
	public void reset() {
		mobLimiter.reset();
		animalLimiter.reset();
		monsterLimiter.reset();
		itemLimiter.reset();
		fallingBlockLimiter.reset();
		for (SpawnLimiter lim : entityLimiters.values()) lim.reset();
	}
	public void clear() {
		mobLimiter.clear();
		animalLimiter.clear();
		monsterLimiter.clear();
		itemLimiter.clear();
		fallingBlockLimiter.clear();
		entityLimiters.clear();
	}
	
	public SpawnLimiter[] getLimits(String name) {
		if (SpawnHandler.isItem(name)) {
			return new SpawnLimiter[] {entityLimiters.get(name), itemLimiter};
		} else if (EntityUtil.isAnimal(name)) {
			return new SpawnLimiter[] {entityLimiters.get(name), animalLimiter, mobLimiter};
		} else if (EntityUtil.isMonster(name)) {
			return new SpawnLimiter[] {entityLimiters.get(name), monsterLimiter, mobLimiter};
		} else if (SpawnHandler.isFalling(name)) {
			return new SpawnLimiter[] {entityLimiters.get(name), fallingBlockLimiter};
		} else {
			return new SpawnLimiter[] {entityLimiters.get(name)};
		}
	}
	
	public void load(GroupLimiter limiter) {
		this.mobLimiter.limit = limiter.mobLimiter.limit;
		this.animalLimiter.limit = limiter.animalLimiter.limit;
		this.monsterLimiter.limit = limiter.monsterLimiter.limit;
		this.itemLimiter.limit = limiter.itemLimiter.limit;
		this.fallingBlockLimiter.limit = limiter.fallingBlockLimiter.limit;
		this.entityLimiters.clear();
		for (Map.Entry<String, SpawnLimiter> elim : limiter.entityLimiters.entrySet()) {
			this.entityLimiters.put(elim.getKey(), new SpawnLimiter(elim.getValue().limit));
		}
	}
			
}
