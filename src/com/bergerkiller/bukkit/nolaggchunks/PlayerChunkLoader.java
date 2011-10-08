package com.bergerkiller.bukkit.nolaggchunks;

import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerChunkLoader {
	private static WeakHashMap<Player, PlayerChunkBuffer> buffers = new WeakHashMap<Player, PlayerChunkBuffer>();
	
	public static synchronized PlayerChunkBuffer getBuffer(Player player) {
		PlayerChunkBuffer loader = buffers.get(player);
		if (loader == null) {
			loader = new PlayerChunkBuffer(player);
			buffers.put(player, loader);
		}
		return loader;
	}
	
	public static void update(Player player) {
		getBuffer(player).update();
	}
	public static void remove(Player player) {
		buffers.remove(player);
	}
	public static void queueAllChunks() {
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			getBuffer(p).queueAllChunks();
		}
	}
	
	private static int[] getIndices(int[] keys, boolean desc) {
		int[] rval = new int[keys.length];
		for (int i = 0; i < rval.length; i++) {
			rval[i] = -1;
		}
		for (int i = 0; i < rval.length; i++) {
			int tosetindex = -1;
			int limit = Integer.MIN_VALUE;
			for (int ii = 0; ii < rval.length; ii++) {
				if (rval[ii] == -1) {
					if (keys[ii] >= limit) {
						tosetindex = ii;
						limit = keys[ii];
					}
				}
			}
			rval[tosetindex] = i;
		}
		if (desc) {
			return rval;
		} else {
			int[] rev = new int[rval.length];
			for (int i = 0; i < rev.length; i++) {
				rev[i] = rval[rval.length - i - 1];
			}
			return rev;
		}
	}
	private static <T> T[] sort(T[] toSort, int[] by, boolean desc) {
		int[] indices = getIndices(by, desc);
		T[] rval = toSort.clone();
		for (int i = 0; i < rval.length; i++) {
			rval[i] = toSort[indices[i]];
		}
		return rval;
	}
	public static PlayerChunkBuffer[] getSortedBuffers() {
		PlayerChunkBuffer[] rval = buffers.values().toArray(new PlayerChunkBuffer[0]);
		int[] prio = new int[rval.length];
		for (int i = 0; i < prio.length; i++) {
			prio[i] = rval[i].getPriority();
		}
		return sort(rval, prio, true);
	}
	
		
	public static int packetSendMaxRate = 2;
	public static int packetSendInterval = 1;
	
	/*
	 * Task init and deinit
	 */
	private static int taskid = -1;
	public static void init() {
		taskid = NoLaggChunks.plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(NoLaggChunks.plugin, new Runnable() {
			public void run() {
				int limit = packetSendMaxRate;
				for (PlayerChunkBuffer buffer : getSortedBuffers()) {
					limit -= buffer.sendNext(limit);
					if (limit == 0) break;
				}
			}
		}, 0, packetSendInterval);
	}
	public static void deinit() {
		if (taskid != -1) {
			NoLaggChunks.plugin.getServer().getScheduler().cancelTask(taskid);
		}
	}
	
}
