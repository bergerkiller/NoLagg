package com.bergerkiller.bukkit.nolagg.threadlocknotifier;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.nolagg.NoLagg;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;

public class NoLaggThreadLockNotifier extends NoLaggComponent {
	private AsyncTask checkerThread;
	private Task pulseTask;

	@Override
	public void onReload(ConfigurationNode config) {
	}

	@Override
	public void onEnable(ConfigurationNode config) {
		this.register(TLNListener.class);
		ThreadLockChecker.ignored = true; // wait for all plugins to enable
		this.checkerThread = new ThreadLockChecker().start(true);
		this.pulseTask = new Task(NoLagg.plugin) {
			public void run() {
				ThreadLockChecker.pulse = true;
				ThreadLockChecker.ignored = false;
			}
		}.start(1, 1);
	}

	@Override
	public void onDisable(ConfigurationNode config) {
		AsyncTask.stop(this.checkerThread);
		Task.stop(this.pulseTask);
		this.checkerThread = null;
		this.pulseTask = null;
	}
}
