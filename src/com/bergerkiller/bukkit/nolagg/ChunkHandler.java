package com.bergerkiller.bukkit.nolagg;

import java.util.HashSet;
import java.util.WeakHashMap;

import net.minecraft.server.ChunkProviderServer;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkHandler {
	public static int chunkUnloadDelay = 10000;
	
	private static WeakHashMap<Chunk, Long> chunks = new WeakHashMap<Chunk, Long>();
	private static void touch(Chunk chunk, long time) {
		if (!isSpawnChunk(chunk)) {
			chunks.put(chunk, time);
			waitingChunks.remove(chunk);
		}
	}
	
	private static int toChunk(int value) {
		return value >> 4;
	}
	
	/**
	 * Taken over from PerformanceTweaks with minor edit, credit goes to LexManos! :)
	 * @param chunk
	 * @return If this chunk is a spawn chunk
	 */
	private static boolean isSpawnChunk(Chunk chunk){
		if (chunk.getWorld().getKeepSpawnInMemory()) {
			Location spawn = chunk.getWorld().getSpawnLocation();
			int x = chunk.getX() * 16 + 8 - spawn.getBlockX();
			int z = chunk.getZ() * 16 + 8 - spawn.getBlockZ();
			return (x > -128 && x < 128 && z > -128 && z < 128);
		} else {
			return false;
		}
	}
	
	private static boolean canUnload(Chunk c) {
		boolean near = false;
		//any players near?
		int view = Bukkit.getServer().getViewDistance();
		for (Player p : c.getWorld().getPlayers()) {
			int cx = p.getLocation().getBlockX() >> 4;
		    int cz = p.getLocation().getBlockX() >> 4;
		    if (Math.abs(cx - c.getX()) > view) continue;
		    if (Math.abs(cz - c.getZ()) > view) continue;
		    near = true;
		    break;
		}
		if (near) {
			touch(c, System.currentTimeMillis());
			return false;
		} else if (!chunks.containsKey(c)) {
			return true;
		} else {
			long expireTime = chunks.get(c) + chunkUnloadDelay;
			return expireTime < System.currentTimeMillis();
		}
	}
	public static void handleLoad(ChunkLoadEvent event) {
		if (NoLagg.useChunkUnloadDelay) {
			touch(event.getChunk(), System.currentTimeMillis());
		}
	}
	public static void handleUnload(ChunkUnloadEvent event) {
		if (NoLagg.useChunkUnloadDelay) {
			if (isSpawnChunk(event.getChunk())) {
				event.setCancelled(true);
			} else if (!event.isCancelled()) {
				if (canUnload(event.getChunk())) {
					waitingChunks.remove(event.getChunk());
					chunks.remove(event.getChunk());
				} else {
					event.setCancelled(true);
					waitingChunks.add(event.getChunk());
				}
			}
		}
	}
	public static void handleMove(Location from, Location to, Player forPlayer) {
		if (NoLagg.useChunkUnloadDelay) {
			int cx = toChunk(to.getBlockX());
			int cz = toChunk(to.getBlockZ());
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
	
	private static HashSet<Chunk> waitingChunks = new HashSet<Chunk>();
	public static void cleanUp() {
		if (NoLagg.useChunkUnloadDelay) {
			for (Chunk c : waitingChunks.toArray(new Chunk[0])) {
				if (!c.isLoaded()) {
					waitingChunks.remove(c);
				} else if (canUnload(c)) {
					ChunkProviderServer provider = (ChunkProviderServer) ((CraftWorld) c.getWorld()).getHandle().chunkProvider;
					provider.queueUnload(c.getX(), c.getZ());
				}
			}
		}
	}

}
