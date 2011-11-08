package com.bergerkiller.bukkit.nolagg;

import java.util.LinkedList;
import java.util.Queue;

import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkProviderServer;

import org.bukkit.craftbukkit.CraftChunk;

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
		saving = false;
	}
		
	private static Queue<Chunk> toUnload = new LinkedList<Chunk>();	
	public static void scheduleSave(org.bukkit.Chunk chunk) {
		synchronized (toUnload) {
			Chunk c = ((CraftChunk) chunk).getHandle();
			toUnload.add(c);
		}
	}	
	private static Chunk poll() {
		synchronized (toUnload) {
			return toUnload.poll();
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
				}
			} catch (InterruptedException ex) {
				System.out.println("[NoLagg] Async saving was interrupted!");
			} catch (Exception ex) {
				System.out.println("[NoLagg] An error occured while saving chunks Async:");
				ex.printStackTrace();
			}
		}
		System.out.println("[NoLagg] Async saving disabled!");
		enabled = false;
		thread = null;
	}

}
