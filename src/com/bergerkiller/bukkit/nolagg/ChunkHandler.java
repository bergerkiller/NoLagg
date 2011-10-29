package com.bergerkiller.bukkit.nolagg;

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
				touch(c, System.currentTimeMillis());
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
				int view = Bukkit.getServer().getViewDistance();
				World w = to.getWorld();
				long time = System.currentTimeMillis();
				for (int a = -view; a <= view; a++) {
					for (int b = -view; b <= view; b++) {
						int chunkX = cx + a;
						int chunkZ = cz + b;
						touch(w.getChunkAt(chunkX, chunkZ), time);
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
			for (Chunk c : chunks.keySet().toArray(new Chunk[0])) {
				if (!c.isLoaded()) {
					chunks.remove(c);
				} else if (canUnload(c)) {
					queueUnload(c);
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
	
}
