package com.bergerkiller.bukkit.nolagg.saving;

import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.NativeUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class AutoSaveChanger {
	private static Task autoSaveTask;
	private static Task activeTask;

	public static boolean isSaving() {
		return activeTask != null;
	}

	public static int SAVE_PERCENTAGE = 100;
	
	public static void init() {
		autoSaveTask = new Task(NoLagg.plugin) {
			public void run() {
				CommonUtil.getServerConfig().savePlayers();
				if (!isSaving()) {
					// Obtain a list of all the chunks to save
					final Queue<org.bukkit.Chunk> chunks = new LinkedList<org.bukkit.Chunk>();
					for (World world : WorldUtil.getWorlds()) {
						for (org.bukkit.Chunk chunk : WorldUtil.getChunks(world)) {
							if (NativeUtil.getNative(chunk).a(false)) {
								chunks.offer(chunk);
							}
						}
					}
					final double total = chunks.size();

					activeTask = new Task(NoLagg.plugin) {
						public void run() {
							for (int i = 0; i < NoLaggSaving.autoSaveBatch; i++) {
								org.bukkit.Chunk chunk = chunks.poll();
								if (chunk == null) {
									Task.stop(activeTask);
									activeTask = null;
									return;
								} else {
									SAVE_PERCENTAGE = (int) ((100.0 * (double) chunks.size()) / total);
								}
								if (chunk.isLoaded()) {
									WorldUtil.saveChunk(chunk);
								}
							}
						}
					}.start(1, 1);
				}
			}
		}.start(NoLaggSaving.autoSaveInterval, NoLaggSaving.autoSaveInterval);
	}

	public static void deinit() {
		Task.stop(autoSaveTask);
	}

	public static void reload() {
		Task.stop(autoSaveTask);
		init();
	}
}
