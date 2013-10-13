package com.bergerkiller.bukkit.nolagg.examine;

import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.nolagg.TickRateTrigger;

public class TriggeredExamine extends TickRateTrigger {
	private final int measurementDuration;
	private final PluginLogger logger;

	public TriggeredExamine(PluginLogger logger, JavaPlugin plugin, double minTPS, int minLagDuration, int measurementDuration, int measurementInterval) {
		super(plugin, minTPS, minLagDuration, measurementInterval);
		this.logger = logger;
		this.measurementDuration = measurementDuration;
	}

	@Override
	public void onLagDetected() {
		if (!logger.isRunning()) {
			NoLaggExamine.plugin.log(Level.WARNING, "Tick Rate Trigger detected tick rate issues and is now creating an examine report");
			logger.start(measurementDuration);
		}
	}
}
