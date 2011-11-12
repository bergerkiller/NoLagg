package com.bergerkiller.bukkit.nolagg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class ChunkHandler {
	public static int chunkUnloadDelay;
	
	private static long loadedChunks;
	private static long unloadedChunks;
	
	public static long getLoadCount() {
		return loadedChunks;
	}
	public static long getUnloadCount() {
		return unloadedChunks;
	}
	public static int getBufferCount() {
		return chunks.size();
	}
	public static int getTotalCount() {
		int rval = 0;
		for (World w : Bukkit.getServer().getWorlds()) {
			net.minecraft.server.WorldServer ww = ((CraftWorld) w).getHandle();
			rval += ww.chunkProviderServer.chunkList.size();
		}
		return rval;
	}
	public static void reset() {
		loadedChunks = 0;
		unloadedChunks = 0;
	}
	
	private static HashMap<Chunk, Long> chunks = new HashMap<Chunk, Long>();
	private static void touch(Chunk chunk, long time) {
		chunks.put(chunk, time);
	}
		
	private static boolean isSpawnChunk(Chunk chunk){
		if (chunk.getWorld().getKeepSpawnInMemory()) {
			Location spawn = chunk.getWorld().getSpawnLocation();
			int x = chunk.getX() - (spawn.getBlockX() >> 4);
			int z = chunk.getZ() - (spawn.getBlockZ() >> 4);
			return (x > -8 && x < 8 && z > -8 && z < 8);
		} else {
			return false;
		}
	}
	
	private static boolean isPlayerNear(Chunk chunk) {
		return ((CraftWorld) chunk.getWorld()).isChunkInUse(chunk.getX(), chunk.getZ());
	}
	
	private static boolean isExpired(long time, long currenttime) {
		return time + chunkUnloadDelay < currenttime;
	}
	private static boolean isExpired(Chunk c, long currenttime) {
		return isExpired(chunks.get(c), currenttime);
	}
			
	public static void handleLoad(ChunkLoadEvent event) {
		loadedChunks++;
		if (chunks == null) return;
		if (NoLagg.useChunkUnloadDelay && !isSpawnChunk(event.getChunk())) {
			//restore from saving if needed
			if (AsyncSaving.enabled) AsyncSaving.fixChunk(event.getChunk());
			touch(event.getChunk(), System.currentTimeMillis());
		}
	}
	public static void handleUnload(ChunkUnloadEvent event) {
		if (chunks == null) return;
		if (NoLagg.useChunkUnloadDelay && !event.isCancelled()) {
			Chunk c = event.getChunk();
			if (isSpawnChunk(c)) {
				event.setCancelled(true);
			} else if (isPlayerNear(c)) {
				event.setCancelled(true);
				touch(c, System.currentTimeMillis());
			} else if (chunks.containsKey(c)) {
				if (!isExpired(c, System.currentTimeMillis())) {
					event.setCancelled(true);
				} else {
					chunks.remove(c);
				}
			}
		}
		if (!event.isCancelled()) unloadedChunks++;
	}
	public static void handleUnload(WorldUnloadEvent event) {
		if (!event.isCancelled() && NoLagg.useChunkUnloadDelay) {
			for (Chunk c : event.getWorld().getLoadedChunks()) {
				chunks.remove(c);
			}
		}
	}
		
	public static net.minecraft.server.Chunk getNative(Chunk chunk) {
		return ((CraftChunk) chunk).getHandle();
	}
	
	public static void unload(Chunk chunk) {
		try {
			if (AsyncSaving.enabled) {
				net.minecraft.server.Chunk c = getNative(chunk);
				if (chunk.unload(false)) {
					c.removeEntities();
					AsyncSaving.scheduleSave(c);
					chunks.remove(chunk);
				}
			} else if (chunk.unload()) {
				chunks.remove(chunk);
			}
		} catch (Exception ex) {
			chunks.remove(chunk);
		}
	}
	
	/**
	 * This clones the chunk, breaking it loose from the original
	 * Do NOT use this method in async methods!
	 * Use the cloned version only for async saving, as it is partly dereferenced
	 * @param chunk
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static net.minecraft.server.Chunk cloneChunk(net.minecraft.server.Chunk chunk) {
		try {
			net.minecraft.server.Chunk newc = new net.minecraft.server.Chunk(chunk.world, chunk.x, chunk.z);
			newc.bukkitChunk = null;
			if (!transferData(chunk, newc)) return null;
			for (int i = 0; i < newc.entitySlices.length; i++) {
				newc.entitySlices[i].addAll(chunk.entitySlices[i]);
			}
			newc.tileEntities.putAll(chunk.tileEntities);
			return newc;
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}
	
	public static boolean transferData(net.minecraft.server.Chunk from, net.minecraft.server.Chunk to) {
		try {
			to.b = from.b;
			to.g = from.g;
			to.h = from.h;
			to.i = from.i;
			to.heightMap = from.heightMap;
			to.done = from.done;
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}
	}

	public static void cleanUp() {
		if (chunks == null) return;
		if (NoLagg.useChunkUnloadDelay) {			
			//Unload invisible chunks
			long time = System.currentTimeMillis();
			
			ArrayList<Chunk> toUnload = new ArrayList<Chunk>();
			for (Map.Entry<Chunk, Long> entry : chunks.entrySet()) {
				Chunk c = entry.getKey();
				if (isPlayerNear(c)) {
					entry.setValue(time);
				} else if (isExpired(entry.getValue(), time)) {
					//queueUnload(c);
					toUnload.add(c);
				}
			}
			unloadedChunks += toUnload.size();
			for (Chunk c : toUnload) {
				unload(c);
			}
		}
	}

	public static void init() {
		if (!NoLagg.useChunkUnloadDelay) return;
		long time = System.currentTimeMillis();
		for(org.bukkit.World world : Bukkit.getServer().getWorlds()){
			for(Chunk chunk : world.getLoadedChunks()){
				if (isSpawnChunk(chunk)) continue;
				touch(chunk, time);
			}
		}
	}
	public static void deinit() {
		if (chunks != null) {
			chunks.clear();
			chunks = null;
		}
	}
	
}
