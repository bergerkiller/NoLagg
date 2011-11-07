package com.bergerkiller.bukkit.nolagg;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.server.ChunkProviderServer;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkHandler {
	public static int chunkUnloadDelay;
	
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
	
	private static boolean canUnload(Chunk c) {
		if (chunks.containsKey(c)) {
			if (isPlayerNear(c)) {
				return false;
			} else {
				return isExpired(chunks.get(c), System.currentTimeMillis());
			}
		} else {
			return false;
		}
	}
	public static void handleLoad(ChunkLoadEvent event) {
		if (NoLagg.useChunkUnloadDelay && !isSpawnChunk(event.getChunk())) {
			touch(event.getChunk(), System.currentTimeMillis());
		}
	}
	public static void handleUnload(ChunkUnloadEvent event) {
		if (NoLagg.useChunkUnloadDelay && !event.isCancelled()) {
			if (isSpawnChunk(event.getChunk())) {
				event.setCancelled(true);
			} else if (canUnload(event.getChunk())) {
				chunks.remove(event.getChunk());
			} else {
				event.setCancelled(true);
				touch(event.getChunk(), System.currentTimeMillis());
			}
		}
	}
	
	private static void queueUnload(Chunk chunk) {
		ChunkProviderServer provider = (ChunkProviderServer) ((CraftWorld) chunk.getWorld()).getHandle().chunkProvider;
		provider.queueUnload(chunk.getX(), chunk.getZ());
	}

	public static void cleanUp() {
		if (NoLagg.useChunkUnloadDelay) {			
			//Unload invisible chunks
			long time = System.currentTimeMillis();
			for (Map.Entry<Chunk, Long> entry : chunks.entrySet()) {
				Chunk c = entry.getKey();
				if (isPlayerNear(c)) {
					entry.setValue(time);
				} else if (isExpired(entry.getValue(), time)) {
					queueUnload(c);
				}
			}
		}
	}

	public static void init() {
		long time = System.currentTimeMillis();
		for(org.bukkit.World world : Bukkit.getServer().getWorlds()){
			for(Chunk chunk : world.getLoadedChunks()){
				if (isPlayerNear(chunk) && !isSpawnChunk(chunk)) {
					touch(chunk, time);
				}
			}
		}
	}
	public static void deinit() {
		chunks.clear();
		chunks = null;
	}
	
}
