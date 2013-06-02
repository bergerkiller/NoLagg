package com.bergerkiller.bukkit.nolagg.examine;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.plugin.Plugin;
import org.timedbukkit.craftbukkit.scheduler.TimedWrapper;

public class TaskMeasurement {
	public final String name;
	public final Set<String> locations = new HashSet<String>();
	public long[] times;
	public final String plugin;
	public int executionCount;
	public PluginLogger logger;

	public TaskMeasurement(PluginLogger logger, String name, Plugin plugin) {
		this(logger, name, plugin.getName());
	}

	public TaskMeasurement(PluginLogger logger, String name, String plugin) {
		this.logger = logger;
		this.name = name;
		this.reset();
		this.plugin = plugin;
		this.executionCount = 0;
	}

	public void addDelta(long deltaTime) {
		this.times[logger.position] += deltaTime;
	}

	public void subtractTime(long time) {
		this.times[logger.position] -= System.nanoTime() - time;
	}

	public void setTime(long time) {
		this.times[logger.position] += System.nanoTime() - time;
	}

	public void reset() {
		this.times = new long[logger.getDuration()];
	}

	public TimedWrapper getWrapper(Runnable runnable) {
		return new TimedWrapper(runnable, this);
	}

}
