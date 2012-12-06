package com.bergerkiller.bukkit.nolagg.itembuffer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Item;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class ItemMap {
	private static Map<org.bukkit.Chunk, ChunkItems> items = new WeakHashMap<org.bukkit.Chunk, ChunkItems>();
	private static Task updateTask;

	public static IntVector2 getChunkCoords(Item item) {
		return new IntVector2(MathUtil.locToChunk(EntityUtil.getLocX(item)), MathUtil.locToChunk(EntityUtil.getLocZ(item)));
	}

	private static ChunkItems getItems(World world, IntVector2 chunkCoordinates) {
		if (currentUnload != null && chunkCoordinates.x == currentUnload.x && chunkCoordinates.z == currentUnload.z) {
			return null;
		}
		synchronized (items) {
			return items.get(world.getChunkAt(chunkCoordinates.x, chunkCoordinates.z));
		}
	}

	/**
	 * Clears the item types in the worlds specified
	 * 
	 * @param worlds to clear items in
	 * @param types of items to clear
	 */
	public static void clear(Collection<World> worlds, Set<String> types) {
		synchronized (items) {
			Set<World> worldsClone = new HashSet<World>(worlds);
			if (types.contains("all") || types.contains("items")) {
				// Remove all from worlds
				for (ChunkItems ci : items.values()) {
					if (worldsClone.contains(ci.getWorld())) {
						ci.clear();
					}
				}
			} else {
				// Remove item per type
				for (ChunkItems ci : items.values()) {
					if (worldsClone.contains(ci.getWorld())) {
						ci.clear(types);
					}
				}
			}
		}
	}

	public static void clear(World world) {
		synchronized (items) {
			for (ChunkItems ci : items.values()) {
				if (ci.chunk.getWorld() == world) {
					ci.clear();
				}
			}
		}
	}

	public static void clear() {
		synchronized (items) {
			for (ChunkItems ci : items.values()) {
				ci.clear();
			}
		}
	}

	public static void init() {
		for (World world : WorldUtil.getWorlds()) {
			for (org.bukkit.Chunk chunk : WorldUtil.getChunks(world)) {
				loadChunk(chunk);
			}
		}
		updateTask = new Task(NoLagg.plugin) {
			public void run() {
				synchronized (items) {
					for (ChunkItems citems : items.values()) {
						citems.update();
					}
				}
			}
		}.start(20, 40);
	}

	public static void deinit() {
		for (World world : Bukkit.getWorlds()) {
			for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
				unloadChunk(chunk);
			}
		}
		Task.stop(updateTask);
	}

	public static IntVector2 currentUnload = null;

	public static void unloadChunk(org.bukkit.Chunk chunk) {
		currentUnload = new IntVector2(chunk.getX(), chunk.getZ());
		ChunkItems citems = items.remove(chunk);
		if (citems != null) {
			citems.deinit();
		}
		currentUnload = null;
	}

	public static void loadChunk(org.bukkit.Chunk chunk) {
		synchronized (items) {
			items.put(chunk, new ChunkItems(chunk));
		}
	}

	public static boolean addItem(Item item) {
		if (item == null) {
			return true;
		}
		return addItem(getChunkCoords(item), item);
	}

	public static boolean addItem(IntVector2 coords, Item item) {
		if (item == null) {
			return true;
		}
		ChunkItems citems = getItems(item.getWorld(), coords);
		if (citems == null) {
			return true;
		} else {
			return citems.handleSpawn(item);
		}
	}

	public static void removeItem(Item item) {
		if (item == null) {
			return;
		}
		ChunkItems citems = getItems(item.getWorld(), getChunkCoords(item));
		if (citems != null) {
			citems.spawnedItems.remove(item);
			citems.spawnInChunk();
		}
	}
}
