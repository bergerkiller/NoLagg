package com.bergerkiller.bukkit.nolagg;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;

import org.bukkit.event.world.ChunkEvent;

import com.bergerkiller.bukkit.nolagg.ChunkOperation.Type;

import net.minecraft.server.Chunk;
import net.minecraft.server.World;

public class ChunkScheduler extends Thread {
	
	private static class ChunkPosition {
		public World world;
		public int x, z;
		public ChunkPosition(Chunk chunk) {
			this.x = chunk.x;
			this.z = chunk.z;
			this.world = chunk.world;
		}
	    @Override
	    public int hashCode() {
	        int hash = 3;
	        hash = 53 * hash + ((this.world != null) ? this.world.hashCode() : 0);
	        hash = 53 * hash + (this.x ^ (this.x >> 16));
	        hash = 53 * hash + (this.z ^ (this.z >> 16));
	        return hash;
	    }
	    @Override
	    public boolean equals(Object obj) {
	    	if (obj == this) return true;
	    	if (obj instanceof ChunkPosition) {
	    		ChunkPosition pos = (ChunkPosition) obj;
	    		return pos.x == this.x && pos.z == this.z && pos.world == this.world;
	    	}
	    	return false;
	    }
	}
	
	private static HashMap<ChunkPosition, ChunkOperation> operations = new HashMap<ChunkPosition, ChunkOperation>();
	private static LinkedList<ChunkOperation> input = new LinkedList<ChunkOperation>();
	private static Queue<ChunkOperation> active = new LinkedList<ChunkOperation>();
	private static int inputsize = 0;
	private static int activesize = 0;
	private static boolean enabled = false;
	private static ChunkScheduler thread;
	
	public static void init() {
		enabled = true;
		thread = new ChunkScheduler();
		thread.start();
	}
	public static void deinit() {
		enabled = false;
		transfer();
		synchronized (active) {
			if (active.size() > 0) {
				NoLagg.log(Level.INFO, "Performing remaining chunk operations (" + active.size() + ")...");
				try {
					for (ChunkOperation cmd : active) {
						cmd.execute(true);
					}
					NoLagg.log(Level.INFO, "Operations performed.");
				} catch (Exception ex) {
					NoLagg.log(Level.SEVERE, "Operation failed:");
					ex.printStackTrace();
				}
			}
		}
		input.clear();
		input = null;
		active.clear();
		active = null;
		operations.clear();
		operations = null;
		thread = null;
	}
	
	public static void schedule(ChunkEvent event, Type mode) {
		schedule(event.getChunk(), mode);
	}
	public static void schedule(org.bukkit.Chunk chunk, Type mode) {
		schedule(ChunkHandler.getNative(chunk), mode);
	}
	public static void schedule(Chunk chunk, Type mode) {
		schedule(new ChunkOperation(chunk, mode));
	}
	public static void schedule(ChunkOperation command) {
		synchronized(input) {
			input.add(command);
			inputsize++;
			synchronized(operations) {
				operations.put(new ChunkPosition(command.c), command);
			}
			command.preexecute();
		}
	}	
	public static void fixChunk(Chunk chunk) {
		ChunkOperation op = operations.get(new ChunkPosition(chunk));
		if (op != null && op.c != chunk) ChunkHandler.transferData(op.c, chunk);
	}
	public static int size() {
		return inputsize + activesize;
	}
	
	@SuppressWarnings("unchecked")
	private static boolean transfer() {
		if (inputsize == 0) return false;
		if (activesize > 0) return false;
		synchronized(active) {
			synchronized(input) {
				active.addAll(input);
				input.clear();
				inputsize = 0;
			}
			Collections.sort((LinkedList<ChunkOperation>) active);
			activesize = active.size();
		}
		return true;
	}
	
	public void run() {
		ChunkOperation cmd;
		while (!this.isInterrupted() && enabled) {
			try {
				synchronized(active) {
					cmd = active.poll();
					if (cmd != null) {
						operations.remove(new ChunkPosition(cmd.c));
						--activesize;
					}
					if (cmd != null) cmd.execute(false);
				}
				if (cmd != null) {
					cmd = null;
					Thread.sleep(5);
				} else {
					Thread.sleep(200);
					while (!transfer()) {
						Thread.sleep(500);
					}
				}
			} catch (InterruptedException ex) {
			} catch (Exception ex) {
				if (enabled) {
					//log
					ex.printStackTrace();
				}
			}
		}
	}
	
}
