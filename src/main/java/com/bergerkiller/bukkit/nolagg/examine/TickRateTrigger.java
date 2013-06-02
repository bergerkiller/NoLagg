package com.bergerkiller.bukkit.nolagg.examine;

import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.common.Task;

/**
 * Task that starts examining the server once the tick rate is suffering
 */
public class TickRateTrigger extends Task {
	private long currentTickTime;
	private long lastTickTime;
	private long lastExamineTime;
	private int lagTickCounter;
	private final PluginLogger logger;
	private final long minExamineInterval;
	private final int measurementDuration;
	private final int minTickTime;
	private final int minLagDuration;

	public TickRateTrigger(PluginLogger logger, JavaPlugin plugin, double minTPS, int minLagDuration, int measurementDuration, int measurementInterval) {
		super(plugin);
		this.logger = logger;
		this.minTickTime = (int) (1000 / minTPS);
		this.measurementDuration = measurementDuration;
		this.minLagDuration = minLagDuration;
		this.minExamineInterval = 1000L * (long) measurementInterval;
		this.lastTickTime = System.currentTimeMillis();
		this.lagTickCounter = 0;
		this.lastExamineTime = System.currentTimeMillis() - this.minExamineInterval;
	}

	@Override
	public void run() {
		this.currentTickTime = System.currentTimeMillis();
		if ((this.currentTickTime - this.lastTickTime) > this.minTickTime) {
			this.lagTickCounter++;
			if (this.lagTickCounter >= this.minLagDuration && !logger.isRunning()) {
				long diff = this.currentTickTime - this.lastExamineTime;
				if (diff > this.minExamineInterval) {
					this.lastExamineTime = this.currentTickTime;
					NoLaggExamine.plugin.log(Level.WARNING, "Tick Rate Trigger detected tick rate issues and is now creating an examine report");
					logger.start(measurementDuration);
				}
			}
		} else {
			this.lagTickCounter = 0;
		}
		this.lastTickTime = this.currentTickTime;
	}
}
