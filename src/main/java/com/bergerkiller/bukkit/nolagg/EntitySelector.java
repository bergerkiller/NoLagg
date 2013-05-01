package com.bergerkiller.bukkit.nolagg;

import org.bukkit.entity.Entity;

/**
 * Allows selecting entities for another operation
 */
public interface EntitySelector {
	/**
	 * Checks whether the entity specified is selected by this Entity Selector
	 * 
	 * @param entity to check
	 * @return True if selected, False if not
	 */
	public boolean check(Entity entity);
}
