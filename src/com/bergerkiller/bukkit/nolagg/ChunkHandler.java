package com.bergerkiller.bukkit.nolagg;

import java.util.HashSet;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkHandler {
	public static int chunkUnloadDelay = 10000;
	
	private static WeakHashMap<Chunk, Long> chunks = new WeakHashMap<Chunk, Long>();
	private static void touch(Chunk chunk, long time) {
		chunks.put(chunk, time);
		waitingChunks.remove(chunk);
	}
	
	private static int toChunk(int value) {
		return value >> 4;
	}
	
	private static boolean canUnload(Chunk c) {
		if (!chunks.containsKey(c)) return true;
		long expireTime = chunks.get(c) + chunkUnloadDelay;
		return expireTime < System.currentTimeMillis();
	}
	public static void handleLoad(ChunkLoadEvent event) {
		touch(event.getChunk(), System.currentTimeMillis());
	}
	public static void handleUnload(ChunkUnloadEvent event) {
		if (!event.isCancelled()) {
			if (canUnload(event.getChunk())) {
				waitingChunks.remove(event.getChunk());
				chunks.remove(event.getChunk());
			} else {
				event.setCancelled(true);
				waitingChunks.add(event.getChunk());
			}
		}
	}
	public static void handleMove(Location from, Location to, Player forPlayer) {
		int cx = toChunk(to.getBlockX());
		int cz = toChunk(to.getBlockZ());
		PlayerChunkLoader.update(forPlayer, cx, cz, to.getWorld());
		if (from.getWorld() == to.getWorld()) {
			if (toChunk(from.getBlockX()) == cx) {
				if (toChunk(from.getBlockZ()) == cz) {
					return;
				}
			}
			//Handle it
			int radius = Bukkit.getServer().getViewDistance();
			cx -= radius;
			cz -= radius;
			PlayerChunkLoader.clear(forPlayer, cx, cz);
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
	
	private static HashSet<Chunk> waitingChunks = new HashSet<Chunk>();
	public static void cleanUp() {
		if (waitingChunks.size() > 1) {
			for (Chunk c : waitingChunks.toArray(new Chunk[0])) {
				if (canUnload(c)) {
					c.unload();
				}
			}
		} else {
			for (Chunk c : waitingChunks) {
				if (canUnload(c)) {
					c.unload();
				}
			}
		}
	}
}
