package com.bergerkiller.bukkit.nolagg.examine;

import java.util.PriorityQueue;

import org.bukkit.Bukkit;
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
		if (CraftTaskRef.TEMPLATE.isType(o)) {
			Runnable run = CraftTaskRef.task.get(o);
			if (run != null) {
				if (!(run instanceof TimedWrapper)) {
					Plugin plugin = CraftTaskRef.plugin.get(o);
					if (plugin != null && plugin.isEnabled()) {
						CraftTaskRef.task.set(o, PluginLogger.getWrapper(run, plugin));
					}
				}
			}
		}
		return o;
	}

	public static void init() {
		CraftSchedulerRef.pending.set(Bukkit.getScheduler(), new SchedulerWatcher(CraftSchedulerRef.pending.get(Bukkit.getScheduler())));
	}

	public static void deinit() {
		CraftSchedulerRef.pending.set(Bukkit.getScheduler(), new PriorityQueue(CraftSchedulerRef.pending.get(Bukkit.getScheduler())));
	}
}
