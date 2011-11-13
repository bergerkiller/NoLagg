package com.bergerkiller.bukkit.nolagg;

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;

import net.minecraft.server.EntityPlayer;

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
	
	private static WeakHashMap<Chunk, Long> chunks = new WeakHashMap<Chunk, Long>();
	private static void touch(Chunk chunk, long time) {
		chunks.put(chunk, time);
	}
		
	private static boolean isSpawnChunk(Chunk chunk){
		if (chunk.getWorld().getKeepSpawnInMemory()) {
			Location spawn = chunk.getWorld().getSpawnLocation();
			int x = chunk.getX() - (spawn.getBlockX() >> 4);
			int z = chunk.getZ() - (spawn.getBlockZ() >> 4);
			return x >= -12 && x <= 12 && z >= -12 && z <= 12;
		} else {
			return false;
		}
	}
	
	public static boolean isPlayerNear(Chunk chunk) {
		return isPlayerNear(chunk.getWorld(), chunk.getX(), chunk.getZ());
	}
	public static boolean isPlayerNear(World world, int cx, int cz) {
		return isPlayerNear(getNative(world), cx, cz);
	}
	public static boolean isPlayerNear(net.minecraft.server.World world, int cx, int cz) {
		cx = cx << 4;
		cz = cz << 4;
		int max = 32 + (Bukkit.getServer().getViewDistance() << 4);
		for (Object o : world.entityList) {
			if (o instanceof EntityPlayer) {
				EntityPlayer p = (EntityPlayer) o;
				int x = ((int) p.locX) - cx;
			    if (x > max) continue;
			    if (x < -max) continue;
			    int z = ((int) p.locZ) - cz;
			    if (z > max) continue;
			    if (z < -max) continue;
			    return true;
			}
		}
		return false;
	}
	
	public static boolean isPlayerNear2(net.minecraft.server.World world, int cx, int cz) {
		int max = Bukkit.getServer().getViewDistance() + 1;
		for (Object o : world.entityList) {
			if (o instanceof EntityPlayer) {
				EntityPlayer p = (EntityPlayer) o;
				int x = (((int) p.locX) >> 4) - cx;
			    if (x > max) continue;
			    if (x < -max) continue;
			    int z = (((int) p.locZ) >> 4) - cz;
			    if (z > max) continue;
			    if (z < -max) continue;
			    return true;
			}
		}
		return false;
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
	public static net.minecraft.server.World getNative(World world) {
		return ((CraftWorld) world).getHandle();
	}
	
	public static String toString(Chunk chunk) {
		return "[" + chunk.getX() + "/" + chunk.getZ() + "/" + chunk.getWorld().getName() + "]";
	}
	
	public static void unload(Chunk chunk) {
		if (!chunk.isLoaded()) {
			chunks.remove(chunk);
			return;
		}
		try {
			try {
				if (AsyncSaving.enabled) {
					net.minecraft.server.Chunk c = getNative(chunk);
					if (chunk.unload(false)) {
						c.removeEntities();
						AsyncSaving.scheduleSave(c);
					}
					return;
				}
			} catch (Exception ex) {}
			chunk.unload();
		} catch (Exception ex) {
			NoLagg.log(Level.WARNING, "Failed to unload chunk " + toString(chunk) + ":");
			ex.printStackTrace();
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
