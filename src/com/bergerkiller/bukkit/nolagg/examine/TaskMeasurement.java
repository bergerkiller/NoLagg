package com.bergerkiller.bukkit.nolagg.examine;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.plugin.Plugin;
import org.timedbukkit.craftbukkit.scheduler.TimedWrapper;

public class TaskMeasurement {

	public TaskMeasurement(final String name, Plugin plugin) {
		this(name, plugin.getName());
	}

	public TaskMeasurement(final String name, final String plugin) {
		this.name = name;
		this.reset();
		this.plugin = plugin;
	}

	public final String name;
	public final Set<String> locations = new HashSet<String>();
	public long[] times;
	public final String plugin;

	public void setTime(long time) {
		this.times[PluginLogger.position] += System.nanoTime() - time;
	}

	public void reset() {
		this.times = new long[PluginLogger.duration];
	}

	public TimedWrapper getWrapper(Runnable runnable) {
		return new TimedWrapper(runnable, this);
	}

}
