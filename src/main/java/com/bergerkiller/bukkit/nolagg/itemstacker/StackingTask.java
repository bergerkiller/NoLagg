package com.bergerkiller.bukkit.nolagg.itemstacker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.common.utils.ItemUtil;

public class StackingTask <T extends Entity> {
	private static Location locationBuffer = new Location(null, 0.0, 0.0, 0.0);
	private static Location selfLocationBuffer = new Location(null, 0.0, 0.0, 0.0);
	private T entity;
	private List<T> nearby = new ArrayList<T>(0);

	/**
	 * Resets this Stacking Task
	 * 
	 * @param entity to set
	 */
	public void reset(T entity) {
		this.entity = entity;
		this.nearby.clear();
	}

	/**
	 * Clears this Stacking Task to ensure minimal memory usage
	 */
	public void clear() {
		this.entity = null;
		this.nearby = new ArrayList<T>(0);
	}

	/**
	 * Gets the nearby entities
	 * 
	 * @return nearby entities
	 */
	public List<T> getNearby() {
		return this.nearby;
	}
	
	/**
	 * Gets the center Entity
	 * 
	 * @return center entity
	 */
	public T getEntity() {
		return this.entity;
	}

	/**
	 * Checks if this Stacking Task contains a valid Entity
	 * 
	 * @return True if this task is Valid, False if not
	 */
	public boolean isValid() {
		return this.entity != null;
	}

	/**
	 * Checks if this Stacking Task can be processed upon
	 * 
	 * @return True if processing is possible, False if not
	 */
	public boolean canProcess() {
		return !this.entity.isDead() && this.nearby.size() >= NoLaggItemStacker.stackThreshold - 1;
	}

	/**
	 * Checks whether a given item reached it's maximum stacking size
	 * 
	 * @return True if the item is maxed, False if not
	 */
	public static boolean isMaxed(Item item) {
		return item.getItemStack().getAmount() >= ItemUtil.getMaxSize(item.getItemStack());
	}

	/**
	 * Checks whether a given item reached it's maximum stacking size
	 * 
	 * @return True if the item is maxed, False if not
	 */
	public static boolean isMaxed(ItemStack item) {
		return item.getAmount() >= ItemUtil.getMaxSize(item);
	}

	/**
	 * Fills the nearby entities of this task
	 * 
	 * @param Entitytasks to use as source for entities
	 * @param radiusSquared for stacking
	 */
	public void fillNearby(List<StackingTask<T>> Entitytasks, final double radiusSquared) {
		if (this.entity.isDead()) {
			return;
		}
		this.entity.getLocation(selfLocationBuffer);
		T entity;
		ItemStack entityItemStack = this.entity instanceof Item ? ((Item) this.entity).getItemStack() : null;
		for (StackingTask<T> task : Entitytasks) {
			if (!task.isValid()) {
				break; // Reached end of data
			}
			entity = task.entity;

			// Same entity, dead entity or out of range?
			if (entity.isDead() || entity == this.entity || entity.getWorld() != this.entity.getWorld() ||
					entity.getLocation(locationBuffer).distanceSquared(selfLocationBuffer) > radiusSquared) {
				continue;
			}

			// Same item type and data? (If item)
			if (entityItemStack != null) {
				Item to = (Item) entity;
				if (isMaxed(entityItemStack) || !ItemUtil.equalsIgnoreAmount(entityItemStack, to.getItemStack())) {
					continue;
				}
			}

			// This item can stack: add the nearby entity
			this.nearby.add(entity);
		}
	}

	/**
	 * Transfers all entities into Stacking Tasks
	 * 
	 * @param entities to transfer
	 * @param tasks to transfer the entities to
	 */
	public static <T extends Entity> void transfer(Collection<T> entities, Collection<StackingTask<T>> tasks) {
		// Ensure required tasks capacity
		if (entities.size() > tasks.size()) {
			for (int i = tasks.size(); i < entities.size(); i++) {
				tasks.add(new StackingTask<T>());
			}
		}
		// Transfer
		Iterator<T> entitiesIter = entities.iterator();
		Iterator<StackingTask<T>> tasksIter = tasks.iterator();
		while (entitiesIter.hasNext()) {
			tasksIter.next().reset(entitiesIter.next());
		}
		// Clear unused elements
		while (tasksIter.hasNext()) {
			tasksIter.next().clear();
		}
	}
}
