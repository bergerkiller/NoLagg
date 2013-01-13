package com.bergerkiller.bukkit.nolagg.spawnlimiter;

import java.util.HashMap;
import java.util.Map;

import com.bergerkiller.bukkit.common.utils.EntityUtil;

/**
 * Contains the entity names mapped to a Boolean, checking if they can be deleted from loading chunks
 */
public class ExistingRemovalMap {
	private static boolean removeMobs = true;
	private static boolean removeAnimals = true;
	private static boolean removeMonsters = true;
	private static boolean removeItems = true;
	private static boolean removeFallingBlocks = true;
	private static final Map<String, Boolean> canRemove = new HashMap<String, Boolean>();

	public static void clear() {
		removeMobs = true;
		removeAnimals = true;
		removeMonsters = true;
		removeItems = true;
		removeFallingBlocks = true;
		canRemove.clear();
	}

	/**
	 * Adds an entity type or group as a force-remove cancelled type<br>
	 * This type will not be removed from loaded chunks
	 * 
	 * @param entityname of the entity type of group
	 */
	public static void addNotRemovable(String entityname) {
		entityname = entityname.toLowerCase();
		if (entityname.contains("tnt"))
			entityname = "tnt";
		if (entityname.equals("mob") || entityname.equals("mobs")) {
			removeMobs = false;
		} else if (entityname.equals("animal") || entityname.equals("animals")) {
			removeAnimals = false;
		} else if (entityname.equals("monster") || entityname.equals("monsters")) {
			removeMonsters = false;
		} else if (entityname.equals("item") || entityname.equals("items")) {
			removeItems = false;
		} else if (entityname.equals("fallingblock") || entityname.equals("fallingblocks")) {
			removeFallingBlocks = false;
		} else {
			canRemove.put(entityname, false);
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
