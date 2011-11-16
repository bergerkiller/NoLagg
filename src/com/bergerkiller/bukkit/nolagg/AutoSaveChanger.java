package com.bergerkiller.bukkit.nolagg;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.World;

public class AutoSaveChanger {
	private static Field saveInterval;
	private static int defaultInterval;
	public static int newInterval;
		
	private static boolean validate(String fieldname) {
		try {
			saveInterval = net.minecraft.server.World.class.getDeclaredField(fieldname);
			if (saveInterval.getType() == int.class) {
				saveInterval.setAccessible(true);
				for (World world : Bukkit.getServer().getWorlds()) {
					defaultInterval = saveInterval.getInt(ChunkHandler.getNative(world));
					if (newInterval <= 0) {
						newInterval = defaultInterval;
					}
					return true;
				}
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
			System.out.println("[NoLagg] Auto-save field bound to '" + saveInterval.getName() + "'!");
			changeAll();
		} else {
			System.out.println("[NoLagg] Failed to bind to the field to enable Auto save changer!");
		}
	}
	public static void deinit() {
		if (saveInterval != null) {
			setAll(defaultInterval);
		}
		saveInterval = null;
	}
	
	private static void setAll(int value) {
		for (World world : Bukkit.getServer().getWorlds()) {
			set(world, value);
		}
	}
	
 	private static void set(World world, int value) {
		if (saveInterval != null) {
			if (AsyncAutoSave.enabled) {
				value = Integer.MAX_VALUE;
			}
			try {
				saveInterval.setInt(ChunkHandler.getNative(world), value);
			} catch (Exception e) {
				System.out.println("[NoLagg] Failed to set save interval on world: " + world.getName());
				e.printStackTrace();
			}
		}
		
	}
		
	public static void changeAll() {
		setAll(newInterval);
	}
	public static void change(World world) {
		set(world, newInterval);
	}

}
