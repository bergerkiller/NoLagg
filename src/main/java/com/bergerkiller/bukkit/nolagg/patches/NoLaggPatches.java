package com.bergerkiller.bukkit.nolagg.patches;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;

public class NoLaggPatches extends NoLaggComponent {
	public static boolean headRotationPatch;
	public static NoLaggPatches plugin;
	
	@Override
	public void onReload(ConfigurationNode config) {
		config.setHeader("headRotationOnSpawn", "Should we automaticly fix the player head rotation when it is being spawned?");
		headRotationPatch = config.get("headRotationOnSpawn", true);
		
		HeadRotation.deinit();
		if(headRotationPatch)
			HeadRotation.init();
	}

	@Override
	public void onDisable(ConfigurationNode config) {
		HeadRotation.deinit();
	}

	@Override
	public void onEnable(ConfigurationNode config) {
		plugin = this;
		this.onReload(config);
	}
}
