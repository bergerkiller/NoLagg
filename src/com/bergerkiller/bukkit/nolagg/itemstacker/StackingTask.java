package com.bergerkiller.bukkit.nolagg.itemstacker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.minecraft.server.Entity;

import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.common.utils.ItemUtil;
import com.bergerkiller.bukkit.common.utils.NativeUtil;

public class StackingTask <T extends org.bukkit.entity.Entity> {
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
	 * Fills the nearby entities of this task
	 * 
	 * @param Entitytasks to use as source for entities
	 * @param radiusSquared for stacking
	 */
	public void fillNearby(List<StackingTask<T>> Entitytasks, final double radiusSquared) {
		if (this.entity.isDead()) {
			return;
		}
		T entity;
		double d;
		Entity selfEntity = NativeUtil.getNative(this.entity);
		for (StackingTask<T> task : Entitytasks) {
			if (!task.isValid()) {
				break; // Reached end of data
			}
			entity = task.entity;
			if (!entity.isDead() && entity != this.entity) {
				// Distance check
				Entity e = NativeUtil.getNative(entity);
				d = distance(selfEntity.locX, e.locX);
				if (d > radiusSquared) {
					continue;
				}
				d += distance(selfEntity.locZ, e.locZ);
				if (d > radiusSquared) {
					continue;
				}
				d += distance(selfEntity.locY, e.locY);
				if (d > radiusSquared) {
					continue;
				}
				// Add to nearby entities (performs possible more checks)
				addNearby(entity);
			}
		}
	}

	private void addNearby(T entity) {
		if (this.entity instanceof Item) {
			// Do a compatibility check
			Item from = (Item) this.entity;
			Item to = (Item) entity;
			if (isMaxed(from) || !itemEquals(from, to)) {
				return;
			}
		}
		this.nearby.add(entity);
	}

	private static boolean itemEquals(Item item1, Item item2) {
		ItemStack stack1 = item1.getItemStack();
		ItemStack stack2 = item2.getItemStack();
		return stack1.getTypeId() == stack2.getTypeId() && stack1.getDurability() == stack2.getDurability();
	}

	private static double distance(final double d1, final double d2) {
		return Math.abs((d1 - d2) * (d1 - d2));
	}

	/**
	 * Transfers all entities into Stacking Tasks
	 * 
	 * @param entities to transfer
	 * @param tasks to transfer the entities to
	 */
	public static <T extends org.bukkit.entity.Entity> void transfer(Collection<T> entities, Collection<StackingTask<T>> tasks) {
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
