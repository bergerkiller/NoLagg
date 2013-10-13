package com.bergerkiller.bukkit.nolagg;

import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.common.Task;

/**
 * Task that starts examining the server once the tick rate is suffering
 */
public abstract class TickRateTrigger extends Task {
	private long currentTickTime;
	private long lastTickTime;
	private long lastDetectTime;
	private long minDetectInterval;
	private int lagTickCounter;
	private final int minTickTime;
	private final int minLagDuration;

	public TickRateTrigger(JavaPlugin plugin, double minTPS, int minLagDuration, int minDetectInterval) {
		super(plugin);
		this.minDetectInterval = 1000L * (long) minDetectInterval;
		this.minTickTime = (int) (1000 / minTPS);
		this.minLagDuration = minLagDuration;
		this.lastTickTime = System.currentTimeMillis();
		this.lagTickCounter = 0;
		this.lastDetectTime = System.currentTimeMillis() - this.minDetectInterval;
	}

	public abstract void onLagDetected();

	@Override
	public void run() {
		this.currentTickTime = System.currentTimeMillis();
		if ((this.currentTickTime - this.lastTickTime) > this.minTickTime) {
			this.lagTickCounter++;
			if (this.lagTickCounter >= this.minLagDuration && (this.currentTickTime - this.lastDetectTime) > this.minDetectInterval) {
				this.lastDetectTime = this.currentTickTime;
				this.onLagDetected();
			}
		} else {
			this.lagTickCounter = 0;
		}
		this.lastTickTime = this.currentTickTime;
	}
}
