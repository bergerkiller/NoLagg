package com.bergerkiller.bukkit.nolagg.itembuffer;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Item;

import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.chunks.NoLaggChunks;

public class ChunkItems {
	public final Set<Item> spawnedItems = new HashSet<Item>();
	public final Queue<Item> hiddenItems = new LinkedList<Item>();
	public final Chunk chunk;

	public ChunkItems(final Chunk chunk) {
		this.chunk = chunk;
		for (org.bukkit.entity.Entity entity : WorldUtil.getEntities(chunk)) {
			if (entity instanceof Item && !entity.isDead() && !EntityUtil.isIgnored(entity)) {
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

	public synchronized void deinit() {
		if (!this.hiddenItems.isEmpty()) {
			for (Item item : this.hiddenItems) {
				EntityUtil.addEntity(item);
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

	public synchronized boolean canSpawnItem() {
		return spawnedItems.size() < NoLaggItemBuffer.maxItemsPerChunk;
	}
	
	public synchronized void spawnInChunk() {
		while (!hiddenItems.isEmpty() && canSpawnItem()) {
			EntityUtil.addEntity(hiddenItems.poll());
		}
	}

	public synchronized void update() {
		if (this.hiddenItems.isEmpty()) {
			return;
		}
		if (!this.spawnedItems.isEmpty()) {
			refreshSpawnedItems();
		}
		this.spawnInChunk();
	}

	public synchronized boolean handleSpawn(Item item) {
		if (this.spawnedItems.contains(item)) {
			return true;
		}
		final boolean allowed;
		if (canSpawnItem()) {
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
	private synchronized boolean refreshSpawnedItems() {
		Iterator<Item> iter = this.spawnedItems.iterator();
		try {
			while (iter.hasNext()) {
				final Item e = iter.next();
				if (e.isDead()) {
					iter.remove();
					return true; // Changed
				} else {
					final IntVector2 pair = ItemMap.getChunkCoords(e);
					if (pair.x != this.chunk.getX() || pair.z != this.chunk.getZ()) {
						// respawn in correct chunk
						iter.remove();
						ItemMap.addItem(pair, e);
						return true; // Changed
					}
				}
			}
		} catch (ConcurrentModificationException ex) {
			NoLaggChunks.plugin.log(Level.WARNING, "Spawned items changed while updating: other threads spawning items?!");
		}
		return false;
	}
}
