package com.bergerkiller.bukkit.nolagg;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class StackFormer {
	public static double stackRadiusSquared;
	public static int stackThreshold = 2;
	
	private static boolean addSameItemsNear(List<Entity> rval, List<Item> from, Item around) {
		if (around.isDead()) return false;
		Location m = around.getLocation();
		ItemStack stack = around.getItemStack();
		boolean added = false;
		for (Item item : from) {
			if (item.isDead()) continue;
			if (item == around) continue;
			if (canStack(m, item.getLocation())) {
				ItemStack instack = item.getItemStack();
				if (instack.getTypeId() != stack.getTypeId()) {
					continue;
				}
				if (instack.getDurability() != stack.getDurability()) {
					continue;
				}
				added = true;
				rval.add(item);
			}
		}
		return added;
	}
	private static boolean addSameOrbsNear(List<Entity> rval, List<ExperienceOrb> from, ExperienceOrb around) {
		if (around.isDead()) return false;
		Location m = around.getLocation();
		boolean added = true;
		for (ExperienceOrb orb : from) {
			if (orb.isDead()) continue;
			if (orb == around) continue;
			if (canStack(m, orb.getLocation())) {
				added = true;
				rval.add(orb);
			}
		}
		return added;
	}
	
	private static boolean canStack(Location l1, Location l2) {
		double d = distance(l1.getX(), l2.getX());
		if (d > stackRadiusSquared) return false;
		d += distance(l1.getZ(), l2.getZ());
		if (d > stackRadiusSquared) return false;
		d += distance(l1.getY(), l2.getY());
		if (d > stackRadiusSquared) return false;
		return true;
	}	
	private static double distance(double d1, double d2) {
		d1 = Math.abs(d1 - d2);
		return d1 * d1;
	}
   	
	public static void update(List<Item> items, List<ExperienceOrb> orbs) {
		if (!ItemHandler.formStacks) return;
		if (stackThreshold < 2) return;
		List<Entity> near = new ArrayList<Entity>(stackThreshold - 1);
		updateItems(near, items);
		updateOrbs(near, orbs);
	}
	public static void updateOrbs(List<Entity> near, List<ExperienceOrb> orbs) {
		if (!ItemHandler.formStacks) return;
		for (ExperienceOrb orb : orbs) {
			if (orb.isDead()) continue;
			if (addSameOrbsNear(near, orbs, orb)) {
				if (near.size() > stackThreshold - 2) {
					for (Entity e : near) {
						if (e.isDead()) continue;
						//add the experience
						ExperienceOrb to = (ExperienceOrb) e;
						for (Entity ee : near) {
							if (ee.isDead()) continue;
							ExperienceOrb from = (ExperienceOrb) ee;
							to.setExperience(to.getExperience() + from.getExperience());
							ee.remove();
						}
					}						
				}
				near.clear();
			}
		}
	}
	public static void updateItems(List<Entity> near, List<Item> items) {
		if (!ItemHandler.formStacks) return;
		for (Item item : items) {
			if (item.isDead()) continue;
			ItemStack stack = item.getItemStack();
			int maxsize = stack.getType().getMaxStackSize();
			if (stack.getAmount() >= maxsize) continue;
			if (addSameItemsNear(near, items, item)) {
				if (near.size() > stackThreshold - 2) {
					//addition the items
					for (Entity e : near) {
						if (e.isDead()) continue;
						Item nearitem = (Item) e;
						ItemStack stack2 = nearitem.getItemStack();
						if (stack2.getAmount() >= maxsize) continue;
						int newamount = stack.getAmount() + stack2.getAmount();
						if (newamount <= maxsize) {
							stack.setAmount(newamount);
							nearitem.remove();
							ItemHandler.removeSpawnedItem(nearitem);
						} else {
							//set to max
							stack.setAmount(maxsize);
							//set prev. item
							stack2.setAmount(newamount - maxsize);
						}
						item = ItemHandler.respawnItem(item, new Vector());
						stack = item.getItemStack();
					}
				}
				near.clear();
			}
		}
	}
	
}
