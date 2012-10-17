package com.bergerkiller.bukkit.nolagg.examine;

import java.util.PriorityQueue;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.scheduler.CraftScheduler;
import org.bukkit.plugin.Plugin;
import org.timedbukkit.craftbukkit.scheduler.TimedWrapper;

import com.bergerkiller.bukkit.common.reflection.classes.CraftSchedulerRef;
import com.bergerkiller.bukkit.common.reflection.classes.CraftTaskRef;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class SchedulerWatcher extends PriorityQueue {
	private static final long serialVersionUID = -3457587669129548810L;

	private SchedulerWatcher(PriorityQueue queue) {
		super(queue);
	}

	@Override
	public Object remove() {
		if (!PluginLogger.isRunning()) {
			return super.remove();
		}
		Object o = super.remove();
		if (o == null) {
			return null;
		}
		Runnable run = CraftTaskRef.task.get(o);
		if (run != null) {
			if (!(run instanceof TimedWrapper)) {
				Plugin plugin = CraftTaskRef.plugin.get(o);
				if (plugin != null && plugin.isEnabled()) {
					CraftTaskRef.task.set(o, PluginLogger.getWrapper(run, plugin));
				}
			}
		}
		return o;
	}

	public static void init() {
		CraftScheduler scheduler = (CraftScheduler) Bukkit.getScheduler();
		CraftSchedulerRef.pending.set(scheduler, new SchedulerWatcher(CraftSchedulerRef.pending.get(scheduler)));
	}

	public static void deinit() {
		CraftScheduler scheduler = (CraftScheduler) Bukkit.getScheduler();
		CraftSchedulerRef.pending.set(scheduler, new PriorityQueue(CraftSchedulerRef.pending.get(scheduler)));
	}
}
