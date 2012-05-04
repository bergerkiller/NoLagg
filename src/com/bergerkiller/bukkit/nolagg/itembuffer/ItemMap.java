package com.bergerkiller.bukkit.nolagg.itembuffer;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.Operation;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkCoordIntPair;
import net.minecraft.server.EntityItem;

public class ItemMap {
	private static Map<Chunk, ChunkItems> items = new HashMap<Chunk, ChunkItems>();
	private static Task updateTask;
	
	public static ChunkCoordIntPair getChunkCoords(EntityItem item) {
		return new ChunkCoordIntPair(MathUtil.locToChunk(item.locX), MathUtil.locToChunk(item.locZ));
	}
	
	public static void clear(World world) {
		synchronized (items) {
			for (ChunkItems ci : items.values()) {
				if (ci.chunk.world.getWorld() == world) {
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
		new Operation() {
			public void run() {
				this.doChunks();
			}
			public void handle(Chunk chunk) {
				loadChunk(chunk);
			}
		};
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
		new Operation() {
			public void run() {
				this.doChunks();
			}
			public void handle(Chunk chunk) {
				unloadChunk(chunk);
			}
		};
		Task.stop(updateTask);
	}
	
	public static int currentUnloadX = Integer.MAX_VALUE;
	public static int currentUnloadZ = Integer.MAX_VALUE;
	
	public static void unloadChunk(org.bukkit.Chunk chunk) {
		unloadChunk(WorldUtil.getNative(chunk));
	}
	public static void unloadChunk(Chunk chunk) {
		currentUnloadX = chunk.x;
		currentUnloadZ = chunk.z;
		ChunkItems citems = items.remove(chunk);
		if (citems != null) {
			citems.deinit();
			items.remove(chunk);
		}
		currentUnloadX = Integer.MAX_VALUE;
		currentUnloadZ = Integer.MAX_VALUE;
	}
	
	public static void loadChunk(org.bukkit.Chunk chunk) {
		loadChunk(WorldUtil.getNative(chunk));
	}
	public static void loadChunk(Chunk chunk) {
		synchronized (items) {
			items.put(chunk, new ChunkItems(chunk));
		}
	}
	
	public static boolean addItem(EntityItem item) {
		if (item == null) return true;
		return addItem(getChunkCoords(item), item);
	}
	public static boolean addItem(ChunkCoordIntPair coords, EntityItem item) {
		if (item == null) return true;
		if (coords.x == currentUnloadX && coords.z == currentUnloadZ) {
			return true;
		}
		return addItem(item.world.getChunkAt(coords.x, coords.z), item);
	}
	private static boolean addItem(Chunk chunk, EntityItem item) {
		if (item == null) return true;
		ChunkItems citems;
		synchronized (items) {
			citems = items.get(chunk);
		}
		if (citems == null) {
			return true;
		} else {
			return citems.handleSpawn(item);
		}
	}
	
	public static void removeItem(EntityItem item) {
		if (item == null) return;
		removeItem(getChunkCoords(item), item);
	}
	public static void removeItem(ChunkCoordIntPair coords, EntityItem item) {
		if (item == null) return;
		if (coords.x == currentUnloadX && coords.z == currentUnloadZ) {
			return;
		}
		removeItem(item.world.getChunkAt(coords.x, coords.z), item);
	}
	private static void removeItem(Chunk chunk, EntityItem item) {
		ChunkItems citems;
		synchronized (items) {
			citems = items.get(chunk);
		}
		if (citems != null) {
			citems.spawnedItems.remove(item);
			citems.spawnInChunk();
		}
	}

}
