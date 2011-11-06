package com.bergerkiller.bukkit.nolagg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.narrowtux.showcase.Showcase;

public class ItemHandler {
	public static int maxItemsPerChunk;
	public static boolean formStacks;
	public static boolean ignoreSpawn = false;
	private static HashMap<Chunk, ArrayList<Item>> spawnedItems = new HashMap<Chunk, ArrayList<Item>>();
	private static HashMap<Chunk, ArrayList<Item>> hiddenItems = new HashMap<Chunk, ArrayList<Item>>();
	public static ArrayList<Item> getSpawnedItems(Chunk c) {
		if (spawnedItems == null) return new ArrayList<Item>();
		ArrayList<Item> items = spawnedItems.get(c);
		if (items == null) {
			items = new ArrayList<Item>();
			spawnedItems.put(c, items);
		}
		return items;	
	}
	public static ArrayList<Item> getHiddenItems(Chunk c) {
		if (hiddenItems == null) return new ArrayList<Item>();
		ArrayList<Item> items = hiddenItems.get(c);
		if (items == null) {
			items = new ArrayList<Item>();
			hiddenItems.put(c, items);
		}
		return items;
	}
	
	private static void spawnInChunk(Chunk c, ArrayList<Item> hiddenitems) {
		for (int i = getSpawnedItems(c).size(); i <= maxItemsPerChunk && hiddenitems.size() > 0; i++) {
			respawnItem(hiddenitems.remove(0));
		}
	}
	public static void spawnInChunk(Chunk c) {
		spawnInChunk(c, getHiddenItems(c));
	}
	public static void spawnInAllChunks() {
		if (hiddenItems == null) return;
		for (Map.Entry<Chunk, ArrayList<Item>> entry : hiddenItems.entrySet()) {
			spawnInChunk(entry.getKey(), entry.getValue());
		}
	}
	
	public static boolean isShowcased(Item item) {
		if (NoLagg.isShowcaseEnabled) {
			try {
		        return Showcase.instance.getItemByDrop(item) != null;
			} catch (Throwable t) {
				System.out.println("[NoLagg] Showcase item verification failed, contact the authors!");
				t.printStackTrace();
				NoLagg.isShowcaseEnabled = false;
			}
		}
		return false;
	}
	public static void loadChunk(Chunk c) {
		unloadChunk(c);
		for (Entity e : c.getEntities().clone()) {
			if (e instanceof Item) {
				if (!handleItemSpawn((Item) e)) {
					e.remove();
				}
			}
		}
	}
	public static void unloadChunk(Chunk c) {
		if (hiddenItems == null) return;
		ignoreSpawn = true;
		ArrayList<Item> items = hiddenItems.remove(c);
		if (items != null) {
			for (Item item : items) {
				respawnItem(item);
			}
		}
		spawnedItems.remove(c);
		ignoreSpawn = false;
	}
	public static void unloadAll() {
		if (hiddenItems == null) return;
		for (Chunk c : hiddenItems.keySet().toArray(new Chunk[0])) {
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
	
	public static boolean handleItemSpawn(Item item) {
		if (ignoreSpawn || !NoLagg.bufferItems) return true;
		if (maxItemsPerChunk == 0) return false;
		if (maxItemsPerChunk < 0) return true;
		if (spawnedItems == null || hiddenItems == null) return true;
		if (isShowcased(item)) return true;
		Chunk c = item.getLocation().getBlock().getChunk();
		int currentcount = ItemHandler.getSpawnedItems(c).size();
		if (currentcount > ItemHandler.maxItemsPerChunk) {
			if (isShowcased(item)) return true;
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
	
	public static void deinit() {
		unloadAll();
		spawnedItems.clear();
		hiddenItems.clear();
		spawnedItems = null;
		hiddenItems = null;
	}
	
	public static void setItem(Item item, ItemStack data) {
		boolean prev = ignoreSpawn;
		ignoreSpawn = true;
		item.remove();
		item.getWorld().dropItem(item.getLocation(), data);
		ignoreSpawn = prev;
	}
	
	private static Chunk getChunk(Entity e) {
		return e.getWorld().getChunkAt(e.getLocation());
	}
	
	public static Item respawnItem(Item item) {
		return respawnItem(item, item.getVelocity(), true);
	}
	public static Item respawnItem(Item item, Vector velocity) {
		return respawnItem(item, velocity, false);
	}
	public static Item respawnItem(Item item, Vector velocity, boolean naturally) {
		if (item.isDead()) return item;
		ignoreSpawn = true;
		item.remove();
		ArrayList<Item> items = getSpawnedItems(getChunk(item));
		items.remove(item);
		//Get required data
		World w = item.getWorld();
		Location l = item.getLocation();
		ItemStack data = item.getItemStack();
		int pickupdelay = -1;
		try {
			pickupdelay = item.getPickupDelay();
		} catch (Throwable t) {}
		//Respawn and set properties
		if (naturally) {
			item = w.dropItemNaturally(l, data);
		} else {
			item = w.dropItem(l, data);
		}
		item.setVelocity(velocity);
		if (pickupdelay != -1) {
			item.setPickupDelay(pickupdelay);
		}
		items.add(item);
		ignoreSpawn = false;
		return item;
	}
	
	public static void removeSpawnedItem(Item item) {
		if (maxItemsPerChunk <= 0) return;
		if (spawnedItems == null) return;
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
	public static void addSpawnedItem(Item item) {
		if (maxItemsPerChunk <= 0) return;
		Chunk c = item.getLocation().getBlock().getChunk();
		getSpawnedItems(c).add(item);
	}

	public static void clear() {
		if (spawnedItems == null || hiddenItems == null) return;
		spawnedItems.clear();
		hiddenItems.clear();
	}
	
	public static void clear(World world) {
		if (spawnedItems == null || hiddenItems == null) return;
		if (spawnedItems.size() + hiddenItems.size() > 0) {
			HashSet<Chunk> toremove = new HashSet<Chunk>();
			for (Chunk c : spawnedItems.keySet()) {
				if (c.getWorld() == world) {
					toremove.add(c);
				}
			}
			for (Chunk c : hiddenItems.keySet()) {
				if (c.getWorld() == world) {
					toremove.add(c);
				}
			}
			for (Chunk c : toremove) {
				spawnedItems.remove(c);
				hiddenItems.remove(c);
			}
		}
	}
	
	public static void update() {
		if (maxItemsPerChunk < 0) return;
		if (!NoLagg.bufferItems) return;
		if (spawnedItems == null) return;
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
