package com.bergerkiller.bukkit.nolagg;

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
		if (!isSpawnChunk(chunk)) {
			chunks.put(chunk, time);
		}
	}
	
	private static int toChunk(int value) {
		return value >> 4;
	}
	
	private static boolean isSpawnChunk(Chunk chunk){
		if (chunk.getWorld().getKeepSpawnInMemory()) {
			Location spawn = chunk.getWorld().getSpawnLocation();
			int x = chunk.getX() - (spawn.getBlockX() >> 4);
			int z = chunk.getZ() - (spawn.getBlockZ() >> 4);
			return (x > -128 && x < 128 && z > -128 && z < 128);
		} else {
			return false;
		}
	}
	
	private static boolean canUnload(Chunk c) {
		if (chunks.containsKey(c)) {
			boolean denied = false;
			//any players near?
			int view = 15;
			for (Player p : c.getWorld().getPlayers()) {
				int cx = toChunk(p.getLocation().getBlockX());
			    int cz = toChunk(p.getLocation().getBlockZ());
			    if (Math.abs(cx - c.getX()) > view) continue;
			    if (Math.abs(cz - c.getZ()) > view) continue;
			    denied = true;
			    break;
			}
			if (denied) {
				return false;
			} else {
				long expireTime = chunks.get(c) + chunkUnloadDelay;
				return expireTime < System.currentTimeMillis();
			}
		} else {
			return true;
		}
	}
	public static void handleLoad(ChunkLoadEvent event) {
		if (NoLagg.useChunkUnloadDelay && !isSpawnChunk(event.getChunk())) {
			touch(event.getChunk(), System.currentTimeMillis());
		}
	}
	public static void handleUnload(ChunkUnloadEvent event) {
		if (NoLagg.useChunkUnloadDelay) {
			if (isSpawnChunk(event.getChunk())) {
				event.setCancelled(true);
			} else if (!event.isCancelled()) {
				if (canUnload(event.getChunk())) {
					chunks.remove(event.getChunk());
				} else {
					event.setCancelled(true);
					if (!chunks.containsKey(event.getChunk())) {
						touch(event.getChunk(), System.currentTimeMillis());
					}
				}
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
			for (Chunk c : chunks.keySet().toArray(new Chunk[0])) {
				if (!c.isLoaded()) {
					chunks.remove(c);
				} else if (canUnload(c)) {
					chunks.remove(c);
					queueUnload(c);
				} else {
					//can not unload: touch it
					touch(c, time);
				}
			}
		}
	}

	public static void init() {
		long time = System.currentTimeMillis();
		for(org.bukkit.World world : Bukkit.getServer().getWorlds()){
			for(Chunk chunk : world.getLoadedChunks()){
				if (!canUnload(chunk)) {
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
