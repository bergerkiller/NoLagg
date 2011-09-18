package com.bergerkiller.bukkit.nolagg;

import java.util.ArrayList;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class StackFormer {
	private static ArrayList<Item> watchedItems = new ArrayList<Item>();
	private static boolean ignorenext = false;
	
	private static Item update(Item item) {
		item.remove();
		ItemHandler.removeSpawnedItem(item);
		item = item.getWorld().dropItem(item.getLocation(), item.getItemStack());
		item.setVelocity(new Vector());
		return item;
	}
			
	public static void add(Item item) {
		if (!ignorenext && ItemHandler.formStacks) {
			watchedItems.add(item);
		}
	}
	
	public static void update() {
		if (!ItemHandler.formStacks) {
			return;
		}
		int i = 0;
		while (i < watchedItems.size()) {
			Item item = watchedItems.get(i);
			if (item.isDead()) {
				watchedItems.remove(i);
				ItemHandler.removeSpawnedItem(item);
			} else {
				//check for nearby items
				ItemStack stack = item.getItemStack();
				int maxsize = stack.getType().getMaxStackSize();
				for (Entity e : item.getNearbyEntities(1, 0.5, 1)) {
					if (e instanceof Item && e != item && !e.isDead()) {
						Item ii = (Item) e;
						ItemStack stack2 = ii.getItemStack();
						if (stack2.getType() == stack.getType()) {
							if (stack.getDurability() == stack2.getDurability()) {
								//Validated!
								int newamount = stack.getAmount() + stack2.getAmount();
								if (newamount <= maxsize) {
									stack.setAmount(newamount);
									ii.remove();
									ItemHandler.removeSpawnedItem(item);
								} else  {
									//set to max
									stack.setAmount(maxsize);
									//set prev. item
									stack2.setAmount(newamount - maxsize);
								}
								ignorenext = true;
								item = update(item);
								stack = item.getItemStack();
								ignorenext = false;
							}
						}
					}
					if (stack.getAmount() == maxsize) break;
				}
				if (stack.getAmount() == maxsize) {
					//no need to watch a full stack
					watchedItems.remove(i);
				} else {
					watchedItems.set(i, item);
					i++;
				}
			}
		}
	}
	
}
