package com.bergerkiller.bukkit.nolagg.examine;

import java.util.PriorityQueue;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.timedbukkit.craftbukkit.scheduler.TimedWrapper;

import com.bergerkiller.bukkit.common.reflection.classes.CraftSchedulerRef;
import com.bergerkiller.bukkit.common.reflection.classes.CraftTaskRef;
import com.bergerkiller.bukkit.nolagg.NoLagg;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class SchedulerWatcher extends PriorityQueue {
	private static final long serialVersionUID = -3457587669129548810L;
	private final PluginLogger logger;

	private SchedulerWatcher(PriorityQueue queue, PluginLogger logger) {
		super(queue);
		this.logger = logger;
	}

	@Override
	public Object remove() {
		if (!logger.isRunning()) {
			return super.remove();
		}
		Object o = super.remove();
		if (o == null) {
			return null;
		}
		try {
			if (CraftTaskRef.TEMPLATE.isType(o)) {
				Runnable run = CraftTaskRef.task.get(o);
				if (run != null && !logger.isIgnoredTask(run)) {
					Plugin plugin = CraftTaskRef.plugin.get(o);
					if (plugin != null && plugin.isEnabled()) {
						CraftTaskRef.task.set(o, logger.getWrapper(run, plugin));
					}
				}
			}
		} catch (Throwable t) {
			NoLagg.plugin.handle(t);
		}
		return o;
	}

	public static void init(PluginLogger logger) {
		CraftSchedulerRef.pending.set(Bukkit.getScheduler(), new SchedulerWatcher(CraftSchedulerRef.pending.get(Bukkit.getScheduler()), logger));
	}

	public static void deinit() {
		// Obtain a blank new Queue with the elements of the original, and filter out timed wrappers
		PriorityQueue<Object> queue = new PriorityQueue<Object>(CraftSchedulerRef.pending.get(Bukkit.getScheduler()));
		for (Object element : queue) {
			// Remove the timed wrapper if needed
			if (CraftTaskRef.TEMPLATE.isType(element)) {
				Runnable run = CraftTaskRef.task.get(element);
				if (run instanceof TimedWrapper) {
					CraftTaskRef.task.set(element, ((TimedWrapper) run).getProxyBase());
				}
			}
		}
		// Set in the server
		CraftSchedulerRef.pending.set(Bukkit.getScheduler(), queue);
	}
}
