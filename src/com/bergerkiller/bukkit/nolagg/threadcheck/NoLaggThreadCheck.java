package com.bergerkiller.bukkit.nolagg.threadcheck;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;

public class NoLaggThreadCheck extends NoLaggComponent {
	@Override
	public void onReload(ConfigurationNode config) {
	}

	@Override
	public void onEnable(ConfigurationNode config) {
		ThreadCheck.init();
	}

	@Override
	public void onDisable(ConfigurationNode config) {
		ThreadCheck.deinit();
	}
}
