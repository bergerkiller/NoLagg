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
			if (AutoSaveChanger.newInterval < 400) {
				throw new Exception("Interval is too low for async auto saving to work. Set the auto save interval in the configuration to a value higher than 400 ticks (20 seconds) to use async auto saving.");
			}
			worldsave = World.class.getDeclaredMethod("w");
			worldsave.setAccessible(true);
			taskID = NoLagg.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(NoLagg.plugin, new Runnable() {
				public void run() {
					try {
						if (AsyncSaving.enabled) {
							if (AsyncSaving.getSize() > 1000) {
								NoLagg.log(Level.WARNING, "Skipping auto-save for worlds: Saving queue overloaded.");
							} else {
								for (org.bukkit.World world : Bukkit.getServer().getWorlds()) {
									World w = ((CraftWorld) world).getHandle();
									if (w.chunkProvider.canSave()) {
										AutoSaveChanger.change(world);
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
			NoLagg.log(Level.WARNING, "Failed to initialize async auto saver!");
			t.printStackTrace();
			NoLagg.log(Level.INFO, "Native sync auto-save feature is used instead.");
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
