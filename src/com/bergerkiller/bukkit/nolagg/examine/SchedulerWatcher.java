package com.bergerkiller.bukkit.nolagg.examine;

import java.util.PriorityQueue;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.scheduler.CraftScheduler;
import org.bukkit.plugin.Plugin;
import org.timedbukkit.craftbukkit.scheduler.TimedWrapper;

import com.bergerkiller.bukkit.common.SafeField;

@SuppressWarnings({"rawtypes", "unchecked"})
public class SchedulerWatcher extends PriorityQueue {
	private static final long serialVersionUID = -3457587669129548810L;
	private static SafeField<PriorityQueue> pending;
	private static SafeField<Runnable> runnable;
	private static SafeField<Plugin> plugin;
	private static boolean isValid;

	static {
		try {
			Class<?> craftTaskClass = Class.forName("org.bukkit.craftbukkit.scheduler.CraftTask");
			pending = new SafeField<PriorityQueue>(CraftScheduler.class, "pending");
			runnable = new SafeField<Runnable>(craftTaskClass, "task");
			plugin = new SafeField<Plugin>(craftTaskClass, "plugin");
			isValid = pending.isValid() && runnable.isValid() && plugin.isValid();
		} catch (Throwable t) {
			t.printStackTrace();
			isValid = false;
		}
	}

	private SchedulerWatcher(PriorityQueue queue) {
		super(queue);
	}

	@Override
	public Object remove() {
		if (!PluginLogger.isRunning()) {
			return super.remove();
		}
		Object o = super.remove();
		Runnable run = runnable.get(o);
		if (!(run instanceof TimedWrapper)) {
			runnable.set(o, PluginLogger.getWrapper(run, plugin.get(o)));
		}
		return o;
	}

	public static void init() {
		if (isValid) {
			CraftScheduler scheduler = (CraftScheduler) Bukkit.getScheduler();
			pending.set(scheduler, new SchedulerWatcher(pending.get(scheduler)));
		} else {
			NoLaggExamine.plugin.log(Level.SEVERE, "Failed to hook into craft scheduler: scheduled tasks will not be logged when examining!");
		}
	}

	public static void deinit() {
		if (isValid) {
			CraftScheduler scheduler = (CraftScheduler) Bukkit.getScheduler();
			try {
				pending.set(scheduler, new PriorityQueue(pending.get(scheduler)));
			} catch (Throwable t) {}
		}
	}
}
