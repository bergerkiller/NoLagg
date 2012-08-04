package com.bergerkiller.bukkit.nolagg.saving;

import net.minecraft.server.ServerConfigurationManager;
import net.minecraft.server.WorldServer;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class AutoSaveChanger {
	private static Task autoSaveTask;
	
	public static void init() {
		autoSaveTask = new Task(NoLagg.plugin) {
			public void run() {
				ServerConfigurationManager scm = CommonUtil.getServerConfig();
				if (scm != null) scm.savePlayers();
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
