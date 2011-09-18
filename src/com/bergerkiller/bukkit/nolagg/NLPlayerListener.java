package com.bergerkiller.bukkit.nolagg;

import org.bukkit.Material;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class NLPlayerListener extends PlayerListener {

	@Override
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		if (!event.isCancelled()) {
			ItemHandler.removeSpawnedItem(event.getItem());
		}
	}
	
	@Override
	public void onPlayerMove(PlayerMoveEvent event) {
		if (!event.isCancelled()) {
			ChunkHandler.handleMove(event.getFrom(), event.getTo());
		}
	}
		
}
