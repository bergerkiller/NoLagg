package com.bergerkiller.bukkit.nolagg.spawnlimiter;

import java.util.HashMap;
import java.util.Map;

import com.bergerkiller.bukkit.common.utils.EntityUtil;

/**
 * Contains the entity names mapped to a Boolean, checking if they can be deleted from loading chunks
 */
public class ExistingRemovalMap {
	private static boolean removeMobs = false;
	private static boolean removeAnimals = false;
	private static boolean removeMonsters = false;
	private static boolean removeItems = false;
	private static boolean removeFallingBlocks = false;
	private static final Map<String, Boolean> canRemove = new HashMap<String, Boolean>();

	public static void clear() {
		removeMobs = false;
		removeAnimals = false;
		removeMonsters = false;
		removeItems = false;
		removeFallingBlocks = false;
		canRemove.clear();
	}

	/**
	 * Adds an entity type or group as removable
	 * 
	 * @param entityname of the entity type of group
	 */
	public static void addRemovable(String entityname) {
		entityname = entityname.toLowerCase();
		if (entityname.contains("tnt"))
			entityname = "tnt";
		if (entityname.equals("mob") || entityname.equals("mobs")) {
			removeMobs = true;
		} else if (entityname.equals("animal") || entityname.equals("animals")) {
			removeAnimals = true;
		} else if (entityname.equals("monster") || entityname.equals("monsters")) {
			removeMonsters = true;
		} else if (entityname.equals("item") || entityname.equals("items")) {
			removeItems = true;
		} else if (entityname.equals("fallingblock") || entityname.equals("fallingblocks")) {
			removeFallingBlocks = true;
		} else {
			canRemove.put(entityname, true);
		}
	}

	/**
	 * Checks if a certain Entity name or type is removable
	 * 
	 * @param name of the entity
	 * @return True if removable, False if not
	 */
	public static boolean isRemovable(String name) {
		name = name.toLowerCase();
		Boolean val = canRemove.get(name);
		if (val == null) {
			val = false;
			// Check possible categories
			if (EntitySpawnHandler.isItem(name)) {
				val = removeItems;
			} else if (EntityUtil.isAnimal(name)) {
				val = removeAnimals || removeMobs;
			} else if (EntityUtil.isMonster(name)) {
				val = removeMonsters || removeMobs;
			} else if (EntitySpawnHandler.isFalling(name)) {
				val = removeFallingBlocks;
			}
			// Put the resulting setting
			canRemove.put(name, val);
		}
		return val;
	}
}
