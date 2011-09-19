package com.bergerkiller.bukkit.nolagg;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public class ItemHandler {
	public static int maxItemsPerChunk = 30;
	public static boolean formStacks = true;
	private static boolean ignoreSpawn = false;
	private static HashMap<Chunk, ArrayList<Item>> spawnedItems = new HashMap<Chunk, ArrayList<Item>>();
	private static HashMap<Chunk, ArrayList<Item>> hiddenItems = new HashMap<Chunk, ArrayList<Item>>();
	public static ArrayList<Item> getSpawnedItems(Chunk c) {
		ArrayList<Item> items = spawnedItems.get(c);
		if (items == null) {
			items = new ArrayList<Item>();
			spawnedItems.put(c, items);
		}
		return items;	
	}
	public static ArrayList<Item> getHiddenItems(Chunk c) {
		ArrayList<Item> items = hiddenItems.get(c);
		if (items == null) {
			items = new ArrayList<Item>();
			hiddenItems.put(c, items);
		}
		return items;
	}
	public static int getSpawnedItemCount(Chunk c) {
		return getSpawnedItems(c).size();
	}
	public static void spawnHiddenItem(Chunk c, Item item) {
		if (getHiddenItems(c).remove(item)) {
			getSpawnedItems(c).add(spawnItem(item));
		}
	}
	public static Item spawnItem(Item item) {
		return item.getWorld().dropItemNaturally(item.getLocation(), item.getItemStack());
	}
	public static void loadChunk(Chunk c) {
		unloadChunk(c);
		for (Entity e : c.getEntities().clone()) {
			if (e instanceof Item) {
				Item item = (Item) e;
				if (!handleItemSpawn(item)) {
					item.remove();
				}
			}
		}
	}
	public static void unloadChunk(Chunk c) {
		ignoreSpawn = true;
		for (Item item : getHiddenItems(c)) {
			getSpawnedItems(c).clear();
			spawnItem(item);
		}
		getHiddenItems(c).clear();
		getSpawnedItems(c).clear();
		ignoreSpawn = false;
	}
	public static void unloadAll() {
		for (Chunk c : hiddenItems.keySet()) {
			unloadChunk(c);
		}
	}
	public static void loadAll() {
		for (World w : Bukkit.getServer().getWorlds()) {
			for (Chunk c : w.getLoadedChunks()) {
				loadChunk(c);
			}
		}
	}
	public static void spawnInChunk(Chunk c) {
		ArrayList<Item> items = getHiddenItems(c);
		for (int i = getSpawnedItemCount(c); i <= maxItemsPerChunk && items.size() > 0; i++) {
			spawnHiddenItem(c, items.get(0));
		}
	}
	public static void spawnInAllChunks() {
		for (Chunk c : hiddenItems.keySet()) {
			spawnInChunk(c);
		}
	}
	public static boolean handleItemSpawn(Item item) {
		if (ignoreSpawn) return true;
		if (maxItemsPerChunk == 0) return false;
		if (maxItemsPerChunk < 0) return true;
		Chunk c = item.getLocation().getBlock().getChunk();
		int currentcount = ItemHandler.getSpawnedItemCount(c);
		if (currentcount > ItemHandler.maxItemsPerChunk) {
			if (formStacks) {
				//can we add to an existing stack or is a new item required?
				ItemStack main = item.getItemStack();
				int amount = main.getAmount();
				int max = main.getType().getMaxStackSize();
				boolean add = true;
				for (Item hiddenitem : getHiddenItems(c)) {
					ItemStack hiddenstack = hiddenitem.getItemStack();
					if (hiddenstack.getType() == main.getType() && hiddenstack.getDurability() == main.getDurability()) {
						int hidamount = hiddenstack.getAmount();
						if (hidamount < max) {
							if ((hidamount + amount) <= max) {
								hiddenstack.setAmount(hidamount + amount);
								add = false;
								break;
							} else {
								amount -= max - hidamount;
								hiddenstack.setAmount(max);
							}
						}
					}
				}
				if (add) {
					main.setAmount(amount);
					ItemHandler.getHiddenItems(c).add(item);
				}
			} else {
				ItemHandler.getHiddenItems(c).add(item);
			}
			return false;
		} else {
			ItemHandler.getSpawnedItems(c).add(item);
			return true;
		}
	}
	
	public static void setItem(Item item, ItemStack data) {
		boolean prev = ignoreSpawn;
		ignoreSpawn = true;
		item.remove();
		item.getWorld().dropItem(item.getLocation(), data);
		ignoreSpawn = prev;
	}
	
	public static void removeSpawnedItem(Item item) {
		if (maxItemsPerChunk <= 0) return;
		Chunk c = item.getLocation().getBlock().getChunk();
		if (getSpawnedItems(c).remove(item)) {
			spawnInChunk(c);
		} else {
			//what chunk is it?!
			for (Chunk cc : spawnedItems.keySet()) {
				if (getSpawnedItems(cc).contains(item)) {
					if (getSpawnedItems(cc).remove(item)) {
						spawnInChunk(cc);
					}
					return;
				}
			}
		}
	}

	public static void clear() {
		spawnedItems.clear();
		hiddenItems.clear();
	}
	
	public static void update() {
		if (maxItemsPerChunk == -1) return;
		for (ArrayList<Item> list : spawnedItems.values()) {
			int i = 0;
			while (i < list.size()) {
				if (list.get(i).isDead()) {
					list.remove(i);
				} else {
					i++;
				}
			}
		}
	}
}
