package com.bergerkiller.bukkit.nolagg.itemstacker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.server.WorldServer;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.WorldProperty;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class StackFormer extends AsyncTask {
	private static StackFormer thread;
	private static Task alterRefreshTask;
	private static Task updateTask;

	public static WorldProperty<Double> stackRadius = new WorldProperty<Double>(2.0);

	private static Map<WorldServer, WorldStackFormer> globalWorlds = new HashMap<WorldServer, WorldStackFormer>();
	private static List<WorldStackFormer> toAdd = new ArrayList<WorldStackFormer>();
	private static boolean updated = false;

	public static WorldStackFormer get(WorldServer world) {
		WorldStackFormer former = globalWorlds.get(world);
		if (former == null) {
			former = new WorldStackFormer(world);
			former.stackRadiusSquared = Math.pow(stackRadius.get(world.getWorld()), 2.0);
			globalWorlds.put(world, former);
			synchronized (former) {
				toAdd.add(former);
			}
		}
		return former;
	}

	public static void remove(WorldServer world) {
		WorldStackFormer f = globalWorlds.remove(world);
		if (f != null) {
			f.disable();
		}
	}

	public static void init() {
		updateTask = new Task(NoLagg.plugin) {
			public void run() {
				for (WorldStackFormer former : globalWorlds.values()) {
					former.update();
				}
				updated = true;
			}
		}.start(20, 20);

		for (WorldServer world : WorldUtil.getWorlds()) {
			get(world);
		}
		thread = new StackFormer();
		thread.start(true);
	}

	public static void deinit() {
		Task.stop(updateTask);
		updateTask = null;
		Task.stop(alterRefreshTask);
		alterRefreshTask = null;

		AsyncTask.stop(thread);
		thread = null;
		synchronized (toAdd) {
			toAdd.clear();
		}
		for (WorldStackFormer f : globalWorlds.values()) {
			f.disable();
		}
		globalWorlds.clear();
	}

	private final List<WorldStackFormer> worlds;

	private StackFormer() {
		synchronized (toAdd) {
			this.worlds = new ArrayList<WorldStackFormer>(toAdd);
			toAdd.clear();
		}
	}

	public void run() {
		try {
			if (updated) {
				updated = false;
				synchronized (toAdd) {
					if (!toAdd.isEmpty()) {
						this.worlds.addAll(toAdd);
						toAdd.clear();
					}
				}
				WorldStackFormer f;
				Iterator<WorldStackFormer> iter = this.worlds.iterator();
				while (iter.hasNext()) {
					f = iter.next();
					if (f.isDisabled()) {
						iter.remove();
					} else {
						f.run();
					}
				}
				sleep(300);
			} else {
				sleep(500);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
