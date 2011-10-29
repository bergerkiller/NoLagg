package com.bergerkiller.bukkit.nolagg;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class StackFormer {
	private static ArrayList<ExperienceOrb> watchedOrbs = new ArrayList<ExperienceOrb>();
	private static ArrayList<Item> watchedItems = new ArrayList<Item>();
	private static boolean ignorenext = false;
	public static double stackRadius;
			
	public static void init() {
		for (World world : Bukkit.getServer().getWorlds()) {
			init(world);
		}
	}
	public static void init(World world) {
		for (Entity entity : world.getEntities()) {
			add(entity);
		}
	}
	
	public static void clear() {
		watchedOrbs.clear();
		watchedItems.clear();
	}
	public static void clear(World world) {
		int i = 0;
		while (i < watchedOrbs.size()) {
			ExperienceOrb o = watchedOrbs.get(i);
			if (o.getWorld() == world) {
				watchedOrbs.remove(i);
			} else {
				i++;
			}
		}
		i = 0;
		while (i < watchedItems.size()) {
			Item item = watchedItems.get(i);
			if (item.getWorld() == world) {
				watchedItems.remove(i);
			} else {
				i++;
			}
		}
	}
	
	public static void add(Entity entity) {
		if (entity instanceof Item) {
			add((Item) entity);
		} else if (NoLagg.isOrb(entity)) {
			add((ExperienceOrb) entity);
		}
	}
	public static void add(Item item) {
		if (!ignorenext && ItemHandler.formStacks && !ItemHandler.isShowcased(item)) {
			watchedItems.add(item);
		}
	}
	public static void add(ExperienceOrb orb) {
		if (ItemHandler.formStacks) {
			watchedOrbs.add(orb);
		}
	}
	
	private static void updateOrbs() {
		watchedOrbs.clear();
		for (World world : Bukkit.getServer().getWorlds()) {
			for (Entity entity : world.getEntities()) {
				if (NoLagg.isOrb(entity)) {
					add((ExperienceOrb) entity);
				}
			}
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
	
	private static int orbUpdateCounter = 0;
	public static void unloadChunk(Chunk chunk) {
		if (!ItemHandler.formStacks) return;
		if (orbUpdateCounter++ == 5) {
			orbUpdateCounter = 0;
			updateOrbs();
		}
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
		//Orbs
		while (i < watchedOrbs.size()) {
			ExperienceOrb orb = watchedOrbs.get(i);
			if (orb.isDead()) {
				watchedOrbs.remove(i);
			} else {
				for (Entity e : orb.getNearbyEntities(stackRadius, stackRadius, stackRadius)) {
					if (e instanceof ExperienceOrb && e != orb && !e.isDead()) {
						ExperienceOrb orb2 = (ExperienceOrb) e;
						orb.setExperience(orb.getExperience() + orb2.getExperience());
						orb2.remove();
					}
				}
				i++;
			}
		}
		//Items
		i = 0;
		while (i < watchedItems.size()) {
			Item item = watchedItems.get(i);
			if (item.isDead() || ItemHandler.isShowcased(item)) {
				watchedItems.remove(i);
				ItemHandler.removeSpawnedItem(item);
			} else {
				//check for nearby items
				ItemStack stack = item.getItemStack();
				int maxsize = stack.getType().getMaxStackSize();
				for (Entity e : item.getNearbyEntities(stackRadius, stackRadius, stackRadius)) {
					if (e instanceof Item && e != item && !e.isDead()) {
						Item ii = (Item) e;
						ItemStack stack2 = ii.getItemStack();
						if (stack2.getType() == stack.getType()) {
							if (stack.getDurability() == stack2.getDurability()) {
								if (!ItemHandler.isShowcased(ii)) {
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
