package com.bergerkiller.bukkit.nolagg.itembuffer;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;

public class NoLaggItemBuffer extends NoLaggComponent {

	public static NoLaggItemBuffer plugin;

	public static int maxItemsPerChunk = 80;

	@Override
	public void onEnable(ConfigurationNode config) {
		plugin = this;
		this.register(NLIListener.class);
		this.onReload(config);
		ItemMap.init();
	}

	@Override
	public void onReload(ConfigurationNode config) {
		config.setHeader("maxItemsPerChunk", "The maximum amount of items allowed per chunk");
		maxItemsPerChunk = config.get("maxItemsPerChunk", maxItemsPerChunk);
	}

	@Override
	public void onDisable(ConfigurationNode config) {
		ItemMap.deinit();
	}
}
