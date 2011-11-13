package com.bergerkiller.bukkit.nolaggchunks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerChunkLoader {
	private static HashMap<String, PlayerChunkBuffer> buffers = new HashMap<String, PlayerChunkBuffer>();
	
	
	public static PlayerChunkBuffer getBuffer(Player player) {
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
	private static int taskid = -1;
	public static void init() {
		synchronized (buffers) {
			if (buffers.size() > 0) {
				NoLaggChunks.log(Level.INFO, "Queueing player chunks...");
				for (PlayerChunkBuffer buff : buffers.values()) {
					buff.queueAllChunks(false);
				}
				NoLaggChunks.log(Level.INFO, "Player chunks queued and will be sent shortly.");
			}
		}
		taskid = NoLaggChunks.plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(NoLaggChunks.plugin, new Runnable() {
			public void run() {
				synchronized (buffers) {
					for (PlayerChunkBuffer buffer : buffers.values()) {
						buffer.sendNext();
					}
				}
			}
		}, 0, 1);
	}
	public static void deinit() {
		if (taskid != -1) {
			NoLaggChunks.plugin.getServer().getScheduler().cancelTask(taskid);
		}
	}
	
}
