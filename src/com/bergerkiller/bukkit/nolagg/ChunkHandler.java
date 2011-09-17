package com.bergerkiller.bukkit.nolagg;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkHandler {
	public static int chunkUnloadDelay = 10000;
	
	private static HashMap<Chunk, Long> chunks = new HashMap<Chunk, Long>();
	private static void touch(Chunk chunk, long time) {
		chunks.put(chunk, time);
	}
	
	private static int toChunk(int value) {
		return value >> 4;
	}
	
	public static void handleLoad(ChunkLoadEvent event) {
		touch(event.getChunk(), System.currentTimeMillis());
	}
	public static void handleUnload(ChunkUnloadEvent event) {
		if (!event.isCancelled() && chunks.containsKey(event.getChunk())) {
			long time = chunks.get(event.getChunk());
			if (time + chunkUnloadDelay < System.currentTimeMillis()) {
				event.setCancelled(true);
			} else {
				chunks.remove(event.getChunk());
			}
		}
	}
	public static void handleMove(Location from, Location to) {
		if (from.getWorld() == to.getWorld()) {
			int cx = toChunk(to.getBlockX());
			int cz = toChunk(to.getBlockZ());
			if (toChunk(from.getBlockX()) == cx) {
				if (toChunk(from.getBlockZ()) == cz) {
					return;
				}
			}
			//Handle it
			int radius = Bukkit.getServer().getViewDistance();
			cx -= radius;
			cz -= radius;
			radius *= 2;
			World w = to.getWorld();
			long time = System.currentTimeMillis();
			for (int a = 0; a < radius; a++) {
				for (int b = 0; b < radius; b++) {
					int chunkX = cx + a;
					int chunkZ = cz + b;
					if (w.isChunkLoaded(chunkX, chunkZ)) {
					    touch(w.getChunkAt(chunkX, chunkZ), time);
					}
				}
			}
		}
	}
}
