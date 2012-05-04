package com.bergerkiller.bukkit.nolagg.saving;

import java.lang.reflect.Field;
import java.util.logging.Level;

import net.minecraft.server.ServerConfigurationManager;
import net.minecraft.server.WorldServer;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class AutoSaveChanger {
	private static Field saveInterval;
	private static final int defaultInterval = 40;
	private static Task autoSaveTask;
	private static int internalInterval = defaultInterval;
	
	private static boolean validate(String fieldname) {
		try {
			saveInterval = net.minecraft.server.World.class.getDeclaredField(fieldname);
			if (saveInterval.getType() == int.class) {
				saveInterval.setAccessible(true);
				return true;
			}
		} catch (NoSuchFieldException ex) {
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		saveInterval = null;
		return false;
	}
	
	public static void init() {
		if (validate("u") || validate("p")) {
			if (NoLaggSaving.autoSaveInterval > 400) {
				autoSaveTask = new Task(NoLagg.plugin) {
					public void run() {
						ServerConfigurationManager scm = CommonUtil.getServerConfig();
						if (scm != null) scm.savePlayers();
						for (WorldServer world : WorldUtil.getWorlds()) {
							world.saveLevel();
						}
					}
				}.start(NoLaggSaving.autoSaveInterval, NoLaggSaving.autoSaveInterval);
				internalInterval = Integer.MAX_VALUE;
			} else if (NoLaggSaving.autoSaveInterval <= 10) {
				internalInterval = defaultInterval;
			} else {
				internalInterval = NoLaggSaving.autoSaveInterval;
			}
			changeAll();
		} else {
			NoLaggSaving.plugin.log(Level.SEVERE, "Failed to bind to the field to enable Auto-save interval changer!");
		}
	}
	public static void deinit() {
		internalInterval = defaultInterval;
		if (saveInterval != null) {
			changeAll();
		}
		saveInterval = null;
		Task.stop(autoSaveTask);
	}

	public static void reload() {
		Task.stop(autoSaveTask);
		init();
	}

	public static void change(WorldServer world) {
		if (saveInterval != null) {
			try {
				saveInterval.setInt(world, internalInterval);
			} catch (Exception e) {
				NoLaggSaving.plugin.log(Level.SEVERE, "Failed to set save interval on world: " + world.getWorld().getName());
				e.printStackTrace();
			}
		}
	}
	
	public static void change(World world) {
		change(WorldUtil.getNative(world));
	}
	
    public static void changeAll() {
    	for (WorldServer world : WorldUtil.getWorlds()) {
    		change(world);
    	}
	}
}
