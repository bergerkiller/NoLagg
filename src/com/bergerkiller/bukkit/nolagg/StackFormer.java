package com.bergerkiller.bukkit.nolagg;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class StackFormer {
	public static double stackRadius;
	public static int stackThreshold = 2;
				
	private static Chunk getChunk(World world, int x, int z) {
		if (world.isChunkLoaded(x, z)) {
			return world.getChunkAt(x, z);
		}
		return null;
	}
	private static Chunk[] getNearChunks(Location at) {
		return getNearChunks(at.getWorld(), at.getBlockX(), at.getBlockZ());
	}
	private static Chunk[] getNearChunks(World world, int locx, int locz) {
		int cx = locx >> 4;
		int cz = locz >> 4;
    	Chunk[] chunks = new Chunk[9];
    	chunks[8] = getChunk(world, cx, cz);
    	//Get the 4 mid-corner locations
    	int xleft = cx * 16;
    	int xright = xleft + 16;
    	int ztop = cz * 16;
    	int zbottom = ztop + 16;
    	//Compare
    	boolean left = locx - xleft <= stackRadius;
    	boolean right = xright - locx <= stackRadius;
    	boolean top = locz - ztop <= stackRadius;
    	boolean bottom = zbottom - locz <= stackRadius;
    	if (left) {
    		chunks[0] = getChunk(world, cx - 1, cz);
    		if (top) chunks[4] = getChunk(world, cx - 1, cz - 1);
    		if (bottom) chunks[5] = getChunk(world, cx - 1, cz + 1);
    	}
    	if (right) {
    		chunks[1] = getChunk(world, cx + 1, cz);
    		if (top) chunks[6] = getChunk(world, cx + 1, cz - 1);
    		if (bottom) chunks[7] = getChunk(world, cx + 1, cz + 1);
    	}
    	if (top) chunks[2] = getChunk(world, cx, cz - 1);
    	if (bottom) chunks[3] = getChunk(world, cx, cz + 1);
    	return chunks;
	}
	
	private static boolean checkItem(Entity item) {
		if (!(item instanceof Item)) return false;
		return !ItemHandler.isShowcased((Item) item);
	}
	
    private static boolean addSameEntitiesNear(List<Entity> rval, Entity entity) {
    	if (entity.isDead()) return false;
    	boolean item;
    	if (checkItem(entity)) {
    		item = true;
    	} else if (NoLagg.isOrb(entity)) {
    		item = false;
    	} else {
    		return false;
    	}
    	//get nearby chunks
    	Location m = entity.getLocation();
    	boolean added = false;
    	double rad = stackRadius * stackRadius;
    	//get nearby chunks which could require stacking
    	for (Chunk chunk : getNearChunks(m)) {
    		if (chunk == null) continue;
    		for (Entity e : chunk.getEntities()) {
    			if (e != entity && !e.isDead()) {
    				//compare
    				if (item && e instanceof Item) {
    					Item ie = (Item) e;
    					ItemStack instack = ((Item) entity).getItemStack();
    					ItemStack stack = ie.getItemStack();
    					if (instack.getTypeId() != stack.getTypeId()) {
    						continue;
    					}
    					if (instack.getDurability() != stack.getDurability()) {
    						continue;
    					}
    					//allow distance check
    				} else if (!item && e instanceof ExperienceOrb) {
    					//allow distance check
    				} else {
    					continue;
    				}
    				//distance check
    				if (e.getLocation().distanceSquared(m) <= rad) {
    					rval.add(e);
    					added = true;
    				}
    			}
    		}
    	}    	
    	return added;
    }
	
	public static void update() {
		if (!ItemHandler.formStacks) return;
		List<Entity> near = new ArrayList<Entity>();
		for (World w : Bukkit.getServer().getWorlds()) {
			for (Entity e : w.getEntities()) {
				boolean item;
				if (checkItem(e)) {
					item = true;
				} else if (NoLagg.isOrb(e)) {
					item = false;
				} else {
					continue;
				}
				if (addSameEntitiesNear(near, e)) {
					//continue
					if (near.size() > stackThreshold - 2) {
						if (item) {
							//addition the items
							Item i = (Item) e;
							ItemStack stack = i.getItemStack();
							int maxsize = stack.getType().getMaxStackSize();
							if (stack.getAmount() < maxsize) {
								for (Entity ee : near) {
									if (ee.isDead()) continue;
									Item from = (Item) ee;
									ItemStack stack2 = from.getItemStack();
									if (stack2.getAmount() < maxsize) {
										int newamount = stack.getAmount() + stack2.getAmount();
										if (newamount <= maxsize) {
											stack.setAmount(newamount);
											from.remove();
											ItemHandler.removeSpawnedItem(from);
										} else if (stack2.getAmount() < maxsize) {
											//set to max
											stack.setAmount(maxsize);
											//set prev. item
											stack2.setAmount(newamount - maxsize);
										} else {
											continue;
										}
										i = ItemHandler.respawnItem(i, new Vector());
										stack = i.getItemStack();
									}
								}
							}
						} else {
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
	}
	
}
