package com.bergerkiller.bukkit.nolagg.saving;

import java.util.LinkedList;
import java.util.Queue;

import net.minecraft.server.Chunk;
import net.minecraft.server.ServerConfigurationManager;
import net.minecraft.server.WorldServer;

import com.bergerkiller.bukkit.common.Operation;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
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
				ServerConfigurationManager scm = CommonUtil.getServerConfig();
				if (scm != null) {
					scm.savePlayers();
				}
				if (!isSaving()) {
					// Obtain a list of all the chunks to save
					final Queue<Chunk> chunks = new LinkedList<Chunk>();
					new Operation() {
						public void run() {
							this.doChunks();
						}
						@Override
						public void handle(Chunk chunk) {
							if (chunk.a(false)) {
								chunks.offer(chunk);
							}
						}
					};
					final double total = chunks.size();

					activeTask = new Task(NoLagg.plugin) {
						public void run() {
							for (int i = 0; i < NoLaggSaving.autoSaveBatch; i++) {
								Chunk chunk = chunks.poll();
								if (chunk == null) {
									Task.stop(activeTask);
									activeTask = null;
									return;
								} else {
									SAVE_PERCENTAGE = (int) ((100.0 * (double) chunks.size()) / total);
								}
								if (chunk.bukkitChunk != null && chunk.bukkitChunk.isLoaded()) {
									((WorldServer) chunk.world).chunkProviderServer.saveChunk(chunk);
								}
							}
						}
					}.start(1, 1);
				}
				for (WorldServer world : WorldUtil.getWorlds()) {
					world.saveLevel();
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
