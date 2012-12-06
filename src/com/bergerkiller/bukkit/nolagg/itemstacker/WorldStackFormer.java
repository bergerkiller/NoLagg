package com.bergerkiller.bukkit.nolagg.itemstacker;

import java.util.LinkedList;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;

import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.ItemUtil;

/**
 * Handles the entity item and orb stacking process
 */
public class WorldStackFormer {
	private boolean isProcessing = false; // Processing state, True for async busy or waiting, False for sync available
	private final double radiusSquared;
	private boolean disabled = false;
	private final LinkedList<Item> syncItems = new LinkedList<Item>();
	private final LinkedList<ExperienceOrb> syncOrbs = new LinkedList<ExperienceOrb>();
	private LinkedList<StackingTask<ExperienceOrb>> orbTasks = new LinkedList<StackingTask<ExperienceOrb>>();
	private LinkedList<StackingTask<Item>> itemTasks = new LinkedList<StackingTask<Item>>();

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
		if (e instanceof Item) {
			if (!NoLaggItemStacker.isIgnoredItem(e)) {
				syncItems.add((Item) e);
			}
		} else if (e instanceof ExperienceOrb && NoLaggItemStacker.stackOrbs) {
			syncOrbs.add((ExperienceOrb) e);
		}
	}

	/**
	 * Removes an Entity from the sync lists of this world stacker
	 * 
	 * @param e the entity to remove
	 */
	public void removeEntity(Entity e) {
		if (e instanceof Item) {
			if (!NoLaggItemStacker.isIgnoredItem(e)) {
				// don't bother doing an 'ignored item' check as it checks in a map or set anyway
				syncItems.remove((Item) e);
			}
		} else if (e instanceof ExperienceOrb && NoLaggItemStacker.stackOrbs) {
			syncOrbs.remove((ExperienceOrb) e);
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
		for (StackingTask<Item> task : itemTasks) {
			if (!task.isValid()) {
				break; // Reached end of data
			}
			task.fillNearby(itemTasks, radiusSquared);
		}
		// Generate nearby orbs
		if (NoLaggItemStacker.stackOrbs) {
			for (StackingTask<ExperienceOrb> task : orbTasks) {
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
		for (StackingTask<Item> itemTask : itemTasks) {
			if (!itemTask.isValid()) {
				break; // Reached end of data
			}
			if (itemTask.canProcess()) {
				// Stacking logic
				changed = false;
				for (Item item : itemTask.getNearby()) {
					if (StackingTask.isMaxed(itemTask.getEntity())) {
						break;
					}
					if (!item.isDead() && !StackingTask.isMaxed(item)) {
						org.bukkit.inventory.ItemStack stack = item.getItemStack();
						if (ItemUtil.transfer(stack, itemTask.getEntity().getItemStack(), Integer.MAX_VALUE) > 0) {
							if (stack.getAmount() == 0) {
								item.remove();
							} else {
								item.setItemStack(stack);
							}
							changed = true;
						}
					}
				}
				if (changed) {
					ItemUtil.respawnItem(EntityUtil.getNative(itemTask.getEntity()));
				}
			}
		}

		// Finalize orb stacking
		if (NoLaggItemStacker.stackOrbs) {
			for (StackingTask<ExperienceOrb> orbTask : orbTasks) {
				if (!orbTask.isValid()) {
					break; // Reached end of data
				}
				if (orbTask.canProcess()) {
					// Stacking logic
					for (ExperienceOrb orb : orbTask.getNearby()) {
						if (!orb.isDead()) {
							ExperienceOrb e = orbTask.getEntity();
							e.setExperience(e.getExperience() + orb.getExperience());
							orb.remove();
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
