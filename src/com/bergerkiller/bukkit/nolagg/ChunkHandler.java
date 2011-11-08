package com.bergerkiller.bukkit.nolagg;

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkHandler {
	public static int chunkUnloadDelay;
	private static final boolean debugMode = false;
	
	private static WeakHashMap<Chunk, Long> chunks = new WeakHashMap<Chunk, Long>();
	private static void touch(Chunk chunk, long time) {
		chunks.put(chunk, time);
	}
	
	private static int toChunk(int value) {
		return value >> 4;
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
		int view = Bukkit.getServer().getViewDistance();
		for (Player p : chunk.getWorld().getPlayers()) {
			int x = toChunk(p.getLocation().getBlockX()) - chunk.getX();
		    int z = toChunk(p.getLocation().getBlockZ()) - chunk.getZ();
		    if (x > view) continue;
		    if (x < -view) continue;
		    if (z > view) continue;
		    if (z < -view) continue;
		    return true;
		}
		return false;
	}
	
	private static boolean isExpired(long time, long currenttime) {
		return time + chunkUnloadDelay < currenttime;
	}
	private static boolean isExpired(Chunk c, long currenttime) {
		return isExpired(chunks.get(c), currenttime);
	}
	
	private static long loadcount = 0;
	private static long unloadcount = 0;
		
	public static void handleLoad(ChunkLoadEvent event) {
		if (NoLagg.useChunkUnloadDelay && !isSpawnChunk(event.getChunk())) {
			touch(event.getChunk(), System.currentTimeMillis());
			if (debugMode) loadcount++;
		}
	}
	public static void handleUnload(ChunkUnloadEvent event) {
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
					handleUnload(c);
				}
			} else {
				handleUnload(c);
			}
		}
	}
	
	private static void handleUnload(Chunk chunk) {
		chunks.remove(chunk);
		if (debugMode) unloadcount++;	
	}
	
	public static void unload(Chunk chunk) {
		if (AsyncSaving.enabled) {
			AsyncSaving.scheduleSave(chunk);
			if (chunk.unload(false)) {
				chunks.remove(chunk);
			}
		} else if (chunk.unload()) {
			chunks.remove(chunk);
		}
	}

	public static void cleanUp() {
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
			for (Chunk c : toUnload) {
				unload(c);
			}
			
			if (debugMode) {
				long totalchunks = 0;
				for (World world : Bukkit.getServer().getWorlds()) {
					totalchunks += world.getLoadedChunks().length;
				}
				long buffcount = chunks.size();
				Bukkit.getServer().broadcastMessage("[Total=" + totalchunks + "][Buffered=" + buffcount + "][Loaded=" + loadcount + "][Unloaded=" + unloadcount + "]");
				loadcount = 0;
				unloadcount = 0;
			}
		}
	}

	public static void init() {
		long time = System.currentTimeMillis();
		for(org.bukkit.World world : Bukkit.getServer().getWorlds()){
			for(Chunk chunk : world.getLoadedChunks()){
				if (isSpawnChunk(chunk)) continue;
				if (isPlayerNear(chunk)) {
					touch(chunk, time);
				} else {
					unload(chunk);
				}
			}
		}
	}
	public static void deinit() {
		chunks.clear();
		chunks = null;
	}
	
}
