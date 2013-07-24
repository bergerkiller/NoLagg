package com.bergerkiller.bukkit.nolagg.saving;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class RegionFileFlusher {
	private static Task flushTask;
	private static AsyncTask savingTask;

	public static void reload() {
		Task.stop(flushTask);
		if (NoLaggSaving.writeDataEnabled) {
			flushTask = new Task(NoLagg.plugin) {
				public void run() {
					// Still saving, don't save again
					if (savingTask != null) {
						return;
					}
					// Start a new task to save all worlds
					final List<World> worlds = Bukkit.getWorlds();
					savingTask = new AsyncTask("NoLagg saving data writer") {
						public void run() {
							for (World world : worlds) {
								WorldUtil.saveToDisk(world);
							}
							// All done!
							savingTask = null;
						}
					}.start(false);
				}
			}.start(NoLaggSaving.writeDataInterval, NoLaggSaving.writeDataInterval);
		}
	}

	public static void deinit() {
		Task.stop(flushTask);
		flushTask = null;
	}
}
