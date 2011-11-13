package com.bergerkiller.bukkit.nolagg;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;

import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkProviderServer;
import net.minecraft.server.Entity;

public class AsyncSaving extends Thread {
	
	public static boolean enabled = true;
	
	public static int getSize() {
		synchronized (toSave) {
			return toSave.size();
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
			if (toSave.size() == 0) return;
			NoLagg.log(Level.INFO, "Saving chunks left by async saving queue (" + toSave.size() + ")...");
			for (Chunk c : toSave) {
				save(c);
			}
			toSave.clear();
			toSave = null;
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
	public static void scheduleSave(Chunk chunk) {
		if (!enabled || toSave == null) return;
		synchronized (toSave) {
           toSave.add(chunk);
		}
	}
	private static Chunk poll() {
		if (toSave == null) return null;
		synchronized (toSave) {
			return toSave.poll();
		}
	}
	
	public void run() {
		while (enabled && !this.isInterrupted()) {
			try {
				Chunk c = poll();
				if (c == null) {
					//Nothing to do but to wait this out...
					Thread.sleep(500);
					continue;
				} else {
					save(c);
				}
			} catch (InterruptedException ex) {
				NoLagg.log(Level.SEVERE, "Async chunk saving was interrupted!");
			} catch (Exception ex) {
				NoLagg.log(Level.SEVERE, "An error occured while saving a chunk async:");
				ex.printStackTrace();
			}
		}
		if (enabled) {
			NoLagg.log(Level.WARNING, "Async saving disabled!");
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
