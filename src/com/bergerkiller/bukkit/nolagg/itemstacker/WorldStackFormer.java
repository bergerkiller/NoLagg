package com.bergerkiller.bukkit.nolagg.itemstacker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.server.Entity;
import net.minecraft.server.EntityExperienceOrb;
import net.minecraft.server.EntityItem;
import net.minecraft.server.EntityTracker;
import net.minecraft.server.World;

import com.bergerkiller.bukkit.common.utils.ItemUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

public class WorldStackFormer implements Runnable {
	private Boolean isProcessing = true;
	public final LinkedHashSet<EntityItem> syncItems = new LinkedHashSet<EntityItem>();
	public final LinkedHashSet<EntityExperienceOrb> syncOrbs = new LinkedHashSet<EntityExperienceOrb>();
	private final List<EntityItem> items = new ArrayList<EntityItem>();
	private final List<EntityExperienceOrb> orbs = new ArrayList<EntityExperienceOrb>();
	private final Set<EntityItem> itemsToRespawn = new HashSet<EntityItem>();
	private final Set<Entity> entitiesToKill = new HashSet<Entity>();
	public final EntityTracker tracker;
	private boolean disabled = false;
	public double stackRadiusSquared = 2.0;

	public boolean isDisabled() {
		return this.disabled;
	}

	public void disable() {
		this.disabled = true;
	}

	public WorldStackFormer(World world) {
		this.tracker = WorldUtil.getTracker(world);
		for (Entity e : WorldUtil.getEntities(world)) {
			addEntity(e);
		}
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

	public void update() {
		synchronized (isProcessing) {
			if (isProcessing)
				return;

			// re-spawn previously stacked items
			for (EntityItem item : itemsToRespawn) {
				ItemUtil.respawnItem(item);
			}

			// get rid of trackers of killed entities
			for (Entity entity : entitiesToKill) {
				if (entity.world.entityList.contains(entity)) {
					entity.world.removeEntity(entity);
				}
			}

			// fill the collections with new items and orbs again
			entitiesToKill.clear();
			items.clear();
			orbs.clear();
			itemsToRespawn.clear();
			items.addAll(this.syncItems);
			orbs.addAll(this.syncOrbs);
		}
	}

	@SuppressWarnings("unchecked")
	private void updateOrbs() {
		for (EntityExperienceOrb orb : orbs) {
			if (isDead(orb))
				continue;
			if (addSameOrbsNear(orbs, orb)) {
				if (near.size() > NoLaggItemStacker.stackThreshold - 2) {
					for (EntityExperienceOrb to : (List<EntityExperienceOrb>) near) {
						if (isDead(to))
							continue;
						// add the experience
						orb.value += to.value;
						kill(to);
					}
				}
				near.clear();
			}
		}
	}

	private void kill(Entity entity) {
		entitiesToKill.add(entity);
	}

	private boolean isDead(Entity entity) {
		return entity.dead || entitiesToKill.contains(entity);
	}

	@SuppressWarnings("unchecked")
	private void updateItems() {
		int maxsize;
		for (EntityItem item : items) {
			if (isDead(item))
				continue;
			maxsize = item.itemStack.getMaxStackSize();
			if (item.itemStack.count >= maxsize)
				continue;
			if (addSameItemsNear(items, item)) {
				if (near.size() > NoLaggItemStacker.stackThreshold - 2) {
					// addition the items
					for (EntityItem nearitem : (List<EntityItem>) near) {
						if (isDead(nearitem))
							continue;
						if (nearitem.itemStack == null)
							continue;
						if (ItemUtil.transfer(nearitem.itemStack, item.itemStack, Integer.MAX_VALUE) > 0) {
							if (nearitem.itemStack.count == 0) {
								kill(nearitem);
								// respawn item
								itemsToRespawn.add(item);
							}
						}
					}
				}
				near.clear();
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private static List near = new ArrayList(NoLaggItemStacker.stackThreshold - 1);

	@SuppressWarnings("unchecked")
	private boolean addSameItemsNear(List<EntityItem> from, EntityItem around) {
		if (around.dead)
			return false;
		for (EntityItem item : from) {
			if (item.dead)
				continue;
			if (item == around)
				continue;
			if (item.itemStack.id == around.itemStack.id && item.itemStack.getData() == around.itemStack.getData()) {
				if (canStack(around, item)) {
					near.add(item);
				}
			}
		}
		return !near.isEmpty();
	}

	@SuppressWarnings("unchecked")
	private boolean addSameOrbsNear(List<EntityExperienceOrb> from, EntityExperienceOrb around) {
		if (around.dead)
			return false;
		for (EntityExperienceOrb orb : from) {
			if (orb.dead)
				continue;
			if (orb == around)
				continue;
			if (canStack(around, orb)) {
				near.add(orb);
			}
		}
		return !near.isEmpty();
	}

	private boolean canStack(Entity e1, Entity e2) {
		double d = distance(e1.locX, e2.locX);
		if (d > stackRadiusSquared)
			return false;
		d += distance(e1.locZ, e2.locZ);
		if (d > stackRadiusSquared)
			return false;
		d += distance(e1.locY, e2.locY);
		if (d > stackRadiusSquared)
			return false;
		return true;
	}

	private static double distance(double d1, double d2) {
		d1 = Math.abs(d1 - d2);
		return d1 * d1;
	}

	public void run() {
		synchronized (this.isProcessing) {
			this.isProcessing = true;
		}

		updateItems();
		if (NoLaggItemStacker.stackOrbs) {
			updateOrbs();
		}

		synchronized (this.isProcessing) {
			this.isProcessing = false;
		}
	}

}
