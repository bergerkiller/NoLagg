package com.bergerkiller.bukkit.nolagg;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;

import com.bergerkiller.bukkit.nolaggchunks.BufferedChunk;
import com.bergerkiller.bukkit.nolaggchunks.PlayerChunkBuffer;
import com.bergerkiller.bukkit.nolaggchunks.PlayerChunkLoader;

import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkProviderServer;
import net.minecraft.server.EmptyChunk;
import net.minecraft.server.Entity;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.Packet;

public class AsyncSaving extends Thread {
	
	public static boolean enabled = true;
	
	public static int getSize() {
		synchronized (toSave) {
			synchronized (toLightSave) {
					return toSave.size() + toLightSave.size();
			}
		}
	}
	private static AsyncSaving thread;
	public static void startSaving() {
		if (enabled) {
			thread = new AsyncSaving();
			thread.start();
		}
	}
	public static void stopSaving() {
		enabled = false;
		synchronized (toSave) {
			if (getSize() == 0) return;
			NoLagg.log(Level.INFO, "Saving chunks left by async saving queue (" + toSave.size() + ")...");
			while (next());
			toSave.clear();
			toSave = null;
			toLightSave.clear();
			toLightSave = null;
			NoLagg.log(Level.INFO, "Done.");
		}
	}
	
	public static void fixChunk(org.bukkit.Chunk c) {
		fixChunk(ChunkHandler.getNative(c));
	}
	public static void fixChunk(Chunk c) {
		if (toSave == null) return;
		synchronized (toSave) {
			for (Chunk cc : toSave) {
				if (cc.x == c.x && cc.z == c.z && cc.world == c.world) {
					//time to fix this chunk up!
				    //ignore entities, as they were cleared previously
					if (ChunkHandler.transferData(cc, c)) {
						toSave.remove(cc);
					}
					return;
				}
			}
		}
	}
	
	private static Queue<Chunk> toSave = new LinkedList<Chunk>();	
	private static Queue<Chunk> toLightSave = new LinkedList<Chunk>(); //to fix lighting and then save
	
	public static void scheduleLightingFix(org.bukkit.Chunk chunk) {
		scheduleLightingFix(ChunkHandler.getNative(chunk));
	}
	public static void scheduleLightingFix(Chunk chunk) {
		if (!enabled) return;
		if (NoLagg.isAddonEnabled) {
			try {
				for (PlayerChunkBuffer pcb : PlayerChunkLoader.getBuffersNear(chunk)) {
					pcb.get(chunk.x, chunk.z).setLocked(true);
				}
			} catch (Throwable t) {
				NoLagg.log(Level.SEVERE, "An error occured while using NoLaggChunks (update needed?):");
				t.printStackTrace();
				NoLagg.isAddonEnabled = false;
			}
		}
		scheduleSave(chunk, true);
	}
	
	public static void scheduleSave(Chunk chunk) {
		scheduleSave(chunk, false);
	}
	public static void scheduleSave(Chunk chunk, boolean fixlighting) {
		if (!enabled || toSave == null || toLightSave == null) return;
		if (chunk instanceof EmptyChunk) return;
		chunk.q = false;
		if (fixlighting) {
			synchronized (toLightSave) {
				toLightSave.add(chunk);
			}
		} else {
			synchronized (toSave) {
				toSave.add(chunk);
			}
		}
	}
	
	private static Chunk poll() {
		if (toSave == null) return null;
		synchronized (toSave) {
			return toSave.poll();
		}
	}
	private static Chunk pollLight() {
		if (toLightSave == null) return null;
		synchronized (toLightSave) {
			return toLightSave.poll();
		}
	}
	
	private static boolean next() {
		try {
			boolean light = true;
			Chunk c = pollLight();
			if (c == null) {
				c = poll();
				light = false;
			}
			if (c == null) return false;
			if (light) {
				c.initLighting();

				//prepare the packets to send
				Packet[] toSend = ChunkHandler.getChunkPackets(c);
				
				//send data to clients
				//if the add-on is enabled: unlock the chunk and send
				//else just send it right away
				boolean send = true;
				if (NoLagg.isAddonEnabled) {
					try {
						for (PlayerChunkBuffer pcb : PlayerChunkLoader.getBuffersNear(c)) {
							BufferedChunk bc = pcb.get(c.x, c.z);
							bc.setLocked(false);
							bc.clear();
							for (Packet p : toSend) bc.queue(p);
						}
						send = false;
					} catch (Throwable t) {
						NoLagg.log(Level.SEVERE, "An error occured while using NoLaggChunks (update needed?):");
						t.printStackTrace();
						NoLagg.isAddonEnabled = false;
					}
				}
				if (send) {
					//send it using native coding
					for (Object player : c.world.players) {
						if (player instanceof EntityPlayer) {
							EntityPlayer ep = (EntityPlayer) player;
							int d = (int) ep.locX >> 4 - c.x;
							if (d > 15) continue;
							if (d < -15) continue;
							d = (int) ep.locZ >> 4 - c.z;
							if (d > 15) continue;
							if (d < -15) continue;
							for (Packet p : toSend) {
								ep.netServerHandler.sendPacket(p);
							}
						}
					}
				}
			}
			save(c);
			return true;
		} catch (Exception ex) {
			NoLagg.log(Level.SEVERE, "An error occured while saving a chunk async:");
			ex.printStackTrace();
		}
		return false;
	}
	
	public void run() {
		while (enabled && !this.isInterrupted()) {
			try {
				if (next()) {
					Thread.sleep(5);
				} else {
					Thread.sleep(500);
				}
			} catch (InterruptedException ex) {}
		}
		if (enabled) {
			NoLagg.log(Level.SEVERE, "Async saving disabled, some features are disabled which can cause complications!");
			enabled = false;
		}
		thread = null;
	}

	@SuppressWarnings("rawtypes")
	private static void save(Chunk c) {
		//clear entities no longer in this chunk
		for (List l : c.entitySlices) {
			int i = 0;
			while (i < l.size()) {
				Entity e = (Entity) l.get(i);
				if (((int) e.locX) >> 4 == c.x) {
					if (((int) e.locZ) >> 4 == c.z) {
						i++;
						continue;
					}
				}
				l.remove(i);
			}
		}
		//save
		ChunkProviderServer cps = (ChunkProviderServer) c.world.chunkProvider;
		cps.saveChunk(c);
		cps.saveChunkNOP(c);
	}
}
