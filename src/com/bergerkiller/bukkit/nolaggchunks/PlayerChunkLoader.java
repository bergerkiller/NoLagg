package com.bergerkiller.bukkit.nolaggchunks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class PlayerChunkLoader {
	private static HashMap<String, PlayerChunkBuffer> buffers = new HashMap<String, PlayerChunkBuffer>();
	
	public static PlayerChunkBuffer[] getBuffersNear(net.minecraft.server.Chunk chunk) {
		return getBuffersNear(chunk.world.getWorld(), chunk.x, chunk.z);
	}
	public static PlayerChunkBuffer[] getBuffersNear(Chunk chunk) {
		return getBuffersNear(chunk.getWorld(), chunk.getX(), chunk.getZ());
	}
	public static PlayerChunkBuffer[] getBuffersNear(World world, int cx, int cz) {
		synchronized (buffers) {
			ArrayList<PlayerChunkBuffer> rval = new ArrayList<PlayerChunkBuffer>(buffers.size());
			for (PlayerChunkBuffer b : buffers.values()) {
				if (b.world != world) continue;
				int dx = b.x - cx;
				if (dx > PlayerChunkBuffer.maxview) continue;
				if (dx < -PlayerChunkBuffer.maxview) continue;
				int dz = b.z - cz;
				if (dz > PlayerChunkBuffer.maxview) continue;
				if (dz < -PlayerChunkBuffer.maxview) continue;
				rval.add(b);
			}
			return rval.toArray(new PlayerChunkBuffer[0]);
		}
	}
	public static PlayerChunkBuffer getBuffer(Player player) {
		if (buffers == null) return null;
		synchronized (buffers) {
			PlayerChunkBuffer loader = buffers.get(player.getName());
			if (loader == null) {
				loader = new PlayerChunkBuffer(player);
				buffers.put(player.getName(), loader);
			}
			return loader;
		}
	}
	public static void remove(Player player) {
		if (buffers == null) return;
		synchronized (buffers) {
			buffers.remove(player.getName());
		}
	}
	public static void update(Player player) {
		getBuffer(player).update();
	}
	
	public static void saveSentChunks(DataOutputStream stream) throws IOException {
		synchronized (buffers) {
			stream.writeInt(buffers.size());
			for (PlayerChunkBuffer buffer : buffers.values()) {
				stream.writeUTF(buffer.player.getName());
				buffer.saveSentChunks(stream);
			}
		}
	}
	public static void loadSentChunks(DataInputStream stream) throws IOException {
		int storedcount = stream.readInt();
		for (int i = 0; i < storedcount; i++) {
			Player player = Bukkit.getServer().getPlayer(stream.readUTF());
			if (player == null) {
				int chunkcount = stream.readInt();
				for (int j = 0; j < chunkcount; j++) {
					stream.readInt();
					stream.readInt();
				}
			} else {
				getBuffer(player).loadSentChunks(stream);
			}
		}
	}

	public static void loadSentChunks(File file) {
		if (!file.exists()) return;
		if (Bukkit.getServer().getOnlinePlayers().length > 0) {
			DataInputStream stream = null;
			try {
				stream = new DataInputStream(new FileInputStream(file));
				loadSentChunks(stream);
			} catch (IOException ex) {
				NoLaggChunks.log(Level.WARNING, "Failed to load player chunk lists!");
				ex.printStackTrace();
			}
			try {
				 if (stream != null) stream.close();
			} catch (IOException ex) {}
		}
		file.delete();
	}
	public static void saveSentChunks(File file) {
		if (file.exists() && !file.delete()) {
			NoLaggChunks.log(Level.WARNING, "Failed to save player chunk lists: NO access.");
			return;
		}
		if (Bukkit.getServer().getOnlinePlayers().length > 0) {
			DataOutputStream stream = null;
			try {
				stream = new DataOutputStream(new FileOutputStream(file));
				saveSentChunks(stream);
			} catch (IOException ex) {
				NoLaggChunks.log(Level.WARNING, "Failed to save player chunk lists!");
				ex.printStackTrace();
			}
			try {
				 if (stream != null) stream.close();
			} catch (IOException ex) {}
		}
	}
	
	/*
	 * Task init and deinit
	 */
	private static Thread mainthread;
	private static boolean enabled = false;
	public static void init() {
		if (buffers == null) return;
		synchronized (buffers) {
			if (buffers.size() > 0) {
				NoLaggChunks.log(Level.INFO, "Queueing player chunks...");
				for (PlayerChunkBuffer buff : buffers.values()) {
					buff.queueAllChunks(false);
				}
				NoLaggChunks.log(Level.INFO, "Player chunks queued and will be sent shortly.");
			}
		}
		enabled = true;
		mainthread = new Thread() {
			public void run() {
				long time;
				while (enabled && !this.isInterrupted()) {
					try {
						time = System.currentTimeMillis();
						//======================================
						synchronized (buffers) {
							for (PlayerChunkBuffer buffer : buffers.values()) {
								buffer.sendNext();
							}
						}
						//======================================
						//use remaining time for compression - sync to a duration of 50ms
						//use a buffer of 10 ms
						while (System.currentTimeMillis() - time <= 40 && Compression.execute());
						//sleep for the remaining time
						time = 50 - (System.currentTimeMillis() - time);
						if (time < 10) time = 10;
						Thread.sleep(time);
					} catch (InterruptedException ex) {
					} catch (Exception ex) {
						if (enabled) {
							NoLaggChunks.log(Level.SEVERE, "Failed to perform routine chunk processing:");
							ex.printStackTrace();
						}
					}
				}
			}
		};
		mainthread.start();
	}
	public static void deinit() {
		enabled = false;
		mainthread = null;
		if (buffers != null) {
			buffers.clear();
			buffers = null;
		}
	}
	
}
