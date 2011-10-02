package com.bergerkiller.bukkit.nolagg;

import java.util.HashSet;
import java.util.WeakHashMap;

import net.minecraft.server.NetServerHandler;
import net.minecraft.server.Packet;
import net.minecraft.server.Packet50PreChunk;
import net.minecraft.server.Packet51MapChunk;
import net.minecraft.server.TileEntity;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import com.bergerkiller.bukkit.nolaggchunks.PlayerChunkLoader;

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

	public static boolean send(Location location, Player to) {
		return send(location.getBlockX() >> 4, location.getBlockZ() >> 4, to);
	}
	public static void send(NetServerHandler handler, Packet packet) {
		if (packet != null) handler.sendPacket(packet);
	}
	public static boolean send(int cx, int cz, Player to) {
		try {
			//=============================Getting required objects=======================
			net.minecraft.server.World world = ((CraftWorld) to.getWorld()).getHandle();
            net.minecraft.server.Chunk chunk = world.getChunkAt(cx, cz);
			NetServerHandler handler = ((CraftPlayer) to).getHandle().netServerHandler;
			//=============================================================================
			
			//Send pre-chunk
			send(handler, new Packet50PreChunk(cx * 16, cz * 16, true));
			//Send chunk
			send(handler, new Packet51MapChunk(cx * 16, 0, cz * 16, 16, 128, 16, world));
			//Send entities
			for (Object o : chunk.tileEntities.values()) {
				send(handler, ((TileEntity) o).l());
			}
			return true;
		} catch (Exception ex) {}
		return false;
	}

	public static boolean safeSend(Location location, Player to) {
		return safeSend(location.getBlockX() >> 4, location.getBlockZ() >> 4, to);
	}
	public static boolean safeSend(int cx, int cz, Player to) {
		if (Bukkit.getServer().getPluginManager().isPluginEnabled("NoLaggChunks")) {
			PlayerChunkLoader.clear(to, cx, cz);
			return true;
		} else {
			return send(cx, cz, to);
		}
	}
	
	public static void safeSendAll(Location location) {
		safeSendAll(location.getBlockX() >> 4, location.getBlockZ() >> 4, location.getWorld());
	}
	public static void safeSendAll(int cx, int cz, World world) {
		if (Bukkit.getServer().getPluginManager().isPluginEnabled("NoLaggChunks")) {
			PlayerChunkLoader.clearAll(world.getChunkAt(cx, cz));
		} else {
			for (Player player : world.getPlayers()) {
				send(cx, cz, player);
			}
		}
	}
}
