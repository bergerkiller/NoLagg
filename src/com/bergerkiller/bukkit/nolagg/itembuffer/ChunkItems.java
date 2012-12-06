package com.bergerkiller.bukkit.nolagg.itembuffer;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.entity.Item;

import net.minecraft.server.ChunkCoordIntPair;
import net.minecraft.server.EntityItem;
import net.minecraft.server.WorldServer;

import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.NativeUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

public class ChunkItems {
	public final Set<Item> spawnedItems = new HashSet<Item>();
	public final Queue<Item> hiddenItems = new LinkedList<Item>();
	public final org.bukkit.Chunk chunk;

	public ChunkItems(final org.bukkit.Chunk chunk) {
		this.chunk = chunk;
		for (org.bukkit.entity.Entity entity : WorldUtil.getEntities(chunk)) {
			if (entity instanceof Item) {
				if (entity.isDead() || EntityUtil.isIgnored(entity)) {
					continue;
				}
				if (this.spawnedItems.size() < NoLaggItemBuffer.maxItemsPerChunk) {
					this.spawnedItems.add((Item) entity);
				} else {
					this.hiddenItems.add((Item) entity);
					entity.remove();
				}
			}
		}
	}

	public World getWorld() {
		return chunk.getWorld();
	}

	private void restoreItem(Item bitem) {
		ChunkCoordIntPair coord = ItemMap.getChunkCoords(bitem);
		WorldServer world = NativeUtil.getNative(getWorld());
		world.getChunkAt(coord.x, coord.z);
		EntityItem item = NativeUtil.getNative(bitem);
		item.dead = false;
		world.addEntity(item);
	}

	public synchronized void deinit() {
		if (!this.hiddenItems.isEmpty()) {
			for (Item item : this.hiddenItems) {
				restoreItem(item);
			}
			this.hiddenItems.clear();
		}
		this.spawnedItems.clear();
	}

	public synchronized void clear(Set<String> types) {
		Iterator<Item> iter = this.hiddenItems.iterator();
		while (iter.hasNext()) {
			if (types.contains(EntityUtil.getName(iter.next()))) {
				iter.remove();
			}
		}
	}

	public synchronized void clear() {
		this.hiddenItems.clear();
	}

	public synchronized void spawnInChunk() {
		while (!hiddenItems.isEmpty() && spawnedItems.size() < NoLaggItemBuffer.maxItemsPerChunk) {
			restoreItem(hiddenItems.poll());
		}
	}

	public synchronized void update() {
		if (this.hiddenItems.isEmpty())
			return;
		if (!this.spawnedItems.isEmpty()) {
			refreshSpawnedItems();
		}
		this.spawnInChunk();
	}

	public synchronized boolean handleSpawn(Item item) {
		if (this.spawnedItems.contains(item)) {
			return true;
		}
		boolean allowed = false;
		if (this.spawnedItems.size() < NoLaggItemBuffer.maxItemsPerChunk) {
			allowed = true;
		} else {
			allowed = refreshSpawnedItems();
		}
		if (allowed) {
			this.spawnedItems.add(item);
		} else {
			this.hiddenItems.offer(item);
		}
		return allowed;
	}

	/**
	 * Refreshes all the spawned item information
	 * 
	 * @return True if a change occurred, False if not
	 */
	private boolean refreshSpawnedItems() {
		Iterator<Item> iter = this.spawnedItems.iterator();
		Item e;
		ChunkCoordIntPair pair;
		try {
			while (iter.hasNext()) {
				e = iter.next();
				if (e.isDead()) {
					iter.remove();
					return true; // Changed
				} else {
					pair = ItemMap.getChunkCoords(e);
					if (pair.x != this.chunk.getX() || pair.z != this.chunk.getZ()) {
						// respawn in correct chunk
						iter.remove();
						ItemMap.addItem(pair, e);
						return true; // Changed
					}
				}
			}
		} catch (ConcurrentModificationException ex) {
			return refreshSpawnedItems(); // Retry
		} catch (StackOverflowError ex) {
		}
		return false;
	}
}
