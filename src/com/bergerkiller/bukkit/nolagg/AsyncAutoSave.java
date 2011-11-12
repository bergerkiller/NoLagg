package com.bergerkiller.bukkit.nolagg;

import java.lang.reflect.Method;
import java.util.logging.Level;

import net.minecraft.server.Chunk;
import net.minecraft.server.World;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftWorld;

public class AsyncAutoSave {

	public static boolean enabled = false;
	private static int taskID = -1;
	private static Method worldsave;

	public static void init() {
		if (!AsyncSaving.enabled) return;
		try {
			worldsave = World.class.getDeclaredMethod("w");
			worldsave.setAccessible(true);
			taskID = NoLagg.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(NoLagg.plugin, new Runnable() {
				public void run() {
					try {
						if (AsyncSaving.enabled) {
							for (org.bukkit.World world : Bukkit.getServer().getWorlds()) {
								World w = ((CraftWorld) world).getHandle();
								if (w.chunkProvider.canSave()) {
									worldsave.invoke(w);
									for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
										Chunk c = ChunkHandler.cloneChunk(ChunkHandler.getNative(chunk));
										if (c == null) {
											throw new Exception("Chunk cloning failed.");
										} else {
											AsyncSaving.scheduleSave(c);
										}
									}
								}
							}
						} else {
							throw new Exception("Async chunk save thread terminated.");
						}
					} catch (Throwable t) {
						NoLagg.log(Level.SEVERE, "Async auto save terminated, built-in autosave is now active:");
						t.printStackTrace();
						deinit();
					}
				}
			}, AutoSaveChanger.newInterval, AutoSaveChanger.newInterval);
			enabled = true;
		} catch (Throwable t) {
			NoLagg.log(Level.SEVERE, "Failed to initialize async auto saver!");
			t.printStackTrace();
			enabled = false;
		}
	}
	public static void deinit() {
		if (enabled) {
			NoLagg.plugin.getServer().getScheduler().cancelTask(taskID);
			enabled = false;
			AutoSaveChanger.changeAll();
		}
	}

}
