package com.bergerkiller.bukkit.nolagg.itemstacker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.server.World;
import net.minecraft.server.WorldServer;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class StackFormer extends AsyncTask {
	private static AsyncTask thread;
	private static Task updateTask;
	private static Map<World, WorldStackFormer> globalWorlds = new HashMap<World, WorldStackFormer>();
	private static List<WorldStackFormer> toAdd = new ArrayList<WorldStackFormer>();
	private static boolean updated = false;
	private final List<WorldStackFormer> worlds;

	private StackFormer() {
		synchronized (toAdd) {
			this.worlds = new ArrayList<WorldStackFormer>(toAdd);
			toAdd.clear();
		}
	}

	public static WorldStackFormer get(org.bukkit.World world) {
		return get(WorldUtil.getNative(world));
	}

	public static WorldStackFormer get(World world) {
		WorldStackFormer former = globalWorlds.get(world);
		if (former == null) {
			former = new WorldStackFormer(world.getWorld());
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
					former.runSync();
				}
				updated = true;
			}
		}.start(0, NoLaggItemStacker.interval);

		for (WorldServer world : WorldUtil.getWorlds()) {
			get(world);
		}
		thread = new StackFormer().start(true);
	}

	public static void deinit() {
		Task.stop(updateTask);
		updateTask = null;

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

	@Override
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
						f.runAsync();
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
