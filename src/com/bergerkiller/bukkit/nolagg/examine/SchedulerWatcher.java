package com.bergerkiller.bukkit.nolagg.examine;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.scheduler.CraftScheduler;
import org.bukkit.craftbukkit.scheduler.CraftTask;

import com.bergerkiller.bukkit.common.SafeField;

public class SchedulerWatcher extends TreeMap<CraftTask, Boolean> {

	private static final long serialVersionUID = -3457587669126958810L;
	
	private static SafeField<Runnable> runnable = new SafeField<Runnable>(CraftTask.class, "task");	
	public SchedulerWatcher(TreeMap<CraftTask, Boolean> input) {
		for (Map.Entry<CraftTask, Boolean> entry : input.entrySet()) {
			CraftTask task = entry.getKey();
			Runnable run = runnable.get(task);
			if (!(run instanceof TimedWrapper)) {
				runnable.set(task, PluginLogger.getWrapper(run, null, task.getOwner()));
			}
			super.put(task, entry.getValue());
		}
	}
	
	public Boolean put(CraftTask task, Boolean value) {
		if (value) {
			Runnable run = runnable.get(task);
			if (!(run instanceof TimedWrapper)) {
				runnable.set(task, PluginLogger.getWrapper(run, getStackTrace(), task.getOwner()));
			}
		}
		return super.put(task, value);
	}

	public String getStackTrace() {
		StackTraceElement[] elem = Thread.currentThread().getStackTrace();
		StringBuilder builder = new StringBuilder();
		boolean started = false;
		for (int i = 5; i < elem.length && !elem[i].getClassName().startsWith("net.minecraft.server"); i++) {
			if (!started) {
				if (elem[i].getClassName().startsWith("org.bukkit.craftbukkit")) {
					continue;
				} else {
					started = true;
				}
			} else {
				builder.append('\n');
			}
			builder.append(elem[i].getClassName()).append('.').append(elem[i].getMethodName());
			builder.append("(").append(elem[i].getLineNumber()).append(')');
		}
		return builder.toString();
	}
	
	private static SafeField<TreeMap<CraftTask, Boolean>> queue = new SafeField<TreeMap<CraftTask, Boolean>>(CraftScheduler.class, "schedulerQueue");
	public static void init() {
		if (runnable.isValid() && queue.isValid()) {
			CraftScheduler scheduler = (CraftScheduler) Bukkit.getScheduler();
			TreeMap<CraftTask, Boolean> old = queue.get(scheduler);
			synchronized (old) {
				queue.set(scheduler, new SchedulerWatcher(old));
				old.notify();
			}
		} else {
			NoLaggExamine.plugin.log(Level.SEVERE, "Failed to hook into craft scheduler: scheduled tasks will not be logged when examining!");
		}
	}
	public static void deinit() {
		CraftScheduler scheduler = (CraftScheduler) Bukkit.getScheduler();
		SchedulerWatcher old = null;
		try {
			old = (SchedulerWatcher) queue.get(scheduler);
		} catch (Throwable t) {
			return;
		}
		
		TreeMap<CraftTask, Boolean> repl = new TreeMap<CraftTask, Boolean>();
		for (Map.Entry<CraftTask, Boolean> entry : old.entrySet()) {
			CraftTask task = entry.getKey();
			Runnable run = runnable.get(task);
			if (run instanceof TimedWrapper) {
				run = ((TimedWrapper) run).runnable;
			}
			repl.put(task, entry.getValue());
		}
		queue.set(scheduler, repl);

	}

}
