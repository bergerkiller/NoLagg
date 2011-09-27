package com.bergerkiller.bukkit.nolagg;

import java.util.ArrayList;

import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class StackFormer {
	private static ArrayList<Item> watchedItems = new ArrayList<Item>();
	private static boolean ignorenext = false;
				
	public static void add(Item item) {
		if (!ignorenext && ItemHandler.formStacks) {
			watchedItems.add(item);
		}
	}
	
	public static void loadChunk(Chunk chunk) {
		if (!ItemHandler.formStacks) return;
		for (Entity e : chunk.getEntities()) {
			if (e instanceof Item) {
				watchedItems.add((Item) e);
			}
		}
	}
	public static void unloadChunk(Chunk chunk) {
		if (!ItemHandler.formStacks) return;
		int i = 0;
		while (i < watchedItems.size()) {
			Item item = watchedItems.get(i);
			if (item.isDead()) {
		    	watchedItems.remove(i);
		    	continue;
			}
			int cx = item.getLocation().getBlockX() >> 4;
			if (cx == chunk.getX()) {
			    int cz = item.getLocation().getBlockZ() >> 4;
			    if (cz == chunk.getZ()) {
			    	watchedItems.remove(i);
			    	continue;
			    }
			}
			i++;
		}
	}
	
	public static void update() {
		if (!ItemHandler.formStacks) return;
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
								} else if (stack2.getAmount() < maxsize) {
									//set to max
									stack.setAmount(maxsize);
									//set prev. item
									stack2.setAmount(newamount - maxsize);
								} else {
									continue;
								}
								ignorenext = true;
								item = ItemHandler.respawnItem(item, new Vector());
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
