package com.bergerkiller.bukkit.nolagg;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;

import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkProviderServer;

public class AsyncSaving extends Thread {
	
	public static boolean enabled = true;
	
	private static AsyncSaving thread;
	private static boolean saving = false;
	public static void startSaving() {
		thread = new AsyncSaving();
		saving = true;
		thread.start();
	}
	public static void stopSaving() {
		NoLagg.log(Level.INFO, "Async saving disabled!");
		saving = false;
	}
	
	public static void fixChunk(org.bukkit.Chunk c) {
		fixChunk(ChunkHandler.getNative(c));
	}
	public static void fixChunk(Chunk c) {
		synchronized (toSave) {
			for (Chunk cc : toSave) {
				if (cc.x == c.x && cc.z == c.z && cc.world == c.world) {
					//time to fix this chunk up!
				    //ignore entities, as they were cleared previously
					if (ChunkHandler.transferData(cc, c)) {
						toSave.remove(cc);
						NoLagg.log(Level.WARNING, "Chunk [" + c.x + "/" + c.z + "/" + c.world.getWorld().getName() + "] was restored from the chunk saving queue!");
					}
					return;
				}
			}
		}
	}
	
	private static Queue<Chunk> toSave = new LinkedList<Chunk>();	
	public static void scheduleSave(Chunk chunk) {
		synchronized (toSave) {
           toSave.add(chunk);
		}
	}
	private static Chunk poll() {
		synchronized (toSave) {
			return toSave.poll();
		}
	}
	
	public void run() {
		while (saving && !this.isInterrupted()) {
			try {
				Chunk c = poll();
				if (c == null) {
					//Nothing to do but to wait this out...
					Thread.sleep(500);
					continue;
				} else {
					ChunkProviderServer cps = (ChunkProviderServer) c.world.chunkProvider;
					//save async
					cps.saveChunk(c);
					cps.saveChunkNOP(c);
				}
			} catch (InterruptedException ex) {
				NoLagg.log(Level.SEVERE, "Async chunk saving was interrupted!");
			} catch (Exception ex) {
				NoLagg.log(Level.SEVERE, "An error occured while saving a chunk async:");
				ex.printStackTrace();
			}
		}
		if (saving) {
			NoLagg.log(Level.INFO, "Async saving disabled!");
			saving = false;
		}
		thread = null;
	}

}
