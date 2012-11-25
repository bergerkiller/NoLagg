package com.bergerkiller.bukkit.nolagg.itemstacker;

import java.util.LinkedList;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.utils.ItemUtil;

import net.minecraft.server.Entity;
import net.minecraft.server.EntityExperienceOrb;
import net.minecraft.server.EntityItem;

/**
 * Handles the entity item and orb stacking process
 */
public class WorldStackFormer {
	private boolean isProcessing = false; // Processing state, True for async busy or waiting, False for sync available
	private final double radiusSquared;
	private boolean disabled = false;
	private final LinkedList<EntityItem> syncItems = new LinkedList<EntityItem>();
	private final LinkedList<EntityExperienceOrb> syncOrbs = new LinkedList<EntityExperienceOrb>();
	private LinkedList<StackingTask<EntityExperienceOrb>> orbTasks = new LinkedList<StackingTask<EntityExperienceOrb>>();
	private LinkedList<StackingTask<EntityItem>> itemTasks = new LinkedList<StackingTask<EntityItem>>();

	public WorldStackFormer(World world) {
		this.radiusSquared = Math.pow(NoLaggItemStacker.stackRadius.get(world), 2.0);
	}

	/**
	 * Disables this World Stack Former
	 */
	public void disable() {
		this.disabled = false;
	}

	/**
	 * Checks if this World Stack Former is disabled
	 * 
	 * @return True if it is disabled, False if not
	 */
	public boolean isDisabled() {
		return this.disabled;
	}

	/**
	 * Adds an Entity to the sync lists of this world stacker
	 * 
	 * @param e the entity to add
	 */
	public void addEntity(Entity e) {
		if (e instanceof EntityItem) {
			if (!NoLaggItemStacker.isIgnoredItem(e)) {
				syncItems.add((EntityItem) e);
			}
		} else if (e instanceof EntityExperienceOrb && NoLaggItemStacker.stackOrbs) {
			syncOrbs.add((EntityExperienceOrb) e);
		}
	}

	/**
	 * Removes an Entity from the sync lists of this world stacker
	 * 
	 * @param e the entity to remove
	 */
	public void removeEntity(Entity e) {
		if (e instanceof EntityItem) {
			if (!NoLaggItemStacker.isIgnoredItem(e)) {
				// don't bother doing an 'ignored item' check as it checks in a map or set anyway
				syncItems.remove((EntityItem) e);
			}
		} else if (e instanceof EntityExperienceOrb && NoLaggItemStacker.stackOrbs) {
			syncOrbs.remove((EntityExperienceOrb) e);
		}
	}

	/**
	 * Performs the asynchronous operations
	 */
	public void runAsync() {
		if (!this.isProcessing) {
			return;
		}
		// Generate nearby items
		for (StackingTask<EntityItem> task : itemTasks) {
			if (!task.isValid()) {
				break; // Reached end of data
			}
			task.fillNearby(itemTasks, radiusSquared);
		}
		// Generate nearby orbs
		if (NoLaggItemStacker.stackOrbs) {
			for (StackingTask<EntityExperienceOrb> task : orbTasks) {
				if (!task.isValid()) {
					break; // Reached end of data
				}
				task.fillNearby(orbTasks, radiusSquared);
			}
		}
		// Finished
		this.isProcessing = false;
	}

	/**
	 * Performs the synchronized (main thread) operations
	 */
	public void runSync() {
		if (this.isProcessing) {
			return;
		}

		// Finalize item stacking
		boolean changed;
		for (StackingTask<EntityItem> itemTask : itemTasks) {
			if (!itemTask.isValid()) {
				break; // Reached end of data
			}
			if (itemTask.canProcess()) {
				// Stacking logic
				changed = false;
				for (EntityItem item : itemTask.getNearby()) {
					if (StackingTask.isMaxed(itemTask.getEntity())) {
						break;
					}
					if (!item.dead && !StackingTask.isMaxed(item)) {
						if (ItemUtil.transfer(item.itemStack, itemTask.getEntity().itemStack, Integer.MAX_VALUE) > 0) {
							if (item.itemStack.count == 0) {
								item.dead = true;
							}
							changed = true;
						}
					}
				}
				if (changed) {
					ItemUtil.respawnItem(itemTask.getEntity());
				}
			}
		}

		// Finalize orb stacking
		if (NoLaggItemStacker.stackOrbs) {
			for (StackingTask<EntityExperienceOrb> orbTask : orbTasks) {
				if (!orbTask.isValid()) {
					break; // Reached end of data
				}
				if (orbTask.canProcess()) {
					// Stacking logic
					for (EntityExperienceOrb orb : orbTask.getNearby()) {
						if (!orb.dead) {
							orbTask.getEntity().value += orb.value;
							orb.dead = true;
						}
					}
				}
			}
		}

		// Transfer entities into tasks
		StackingTask.transfer(syncItems, itemTasks);
		StackingTask.transfer(syncOrbs, orbTasks);

		// Start the next run
		this.isProcessing = true;
	}
}
