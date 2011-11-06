package com.bergerkiller.bukkit.nolagg;

import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class NLPlayerListener extends PlayerListener {

	@Override
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		if (!event.isCancelled()) {
			ItemHandler.removeSpawnedItem(event.getItem());
		}
	}
								
}
