package com.bergerkiller.bukkit.nolaggchunks;

import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;

public class NLPlayerListener extends PlayerListener {
		
	@Override
	public void onPlayerMove(PlayerMoveEvent event) {
		if (!event.isCancelled()) {
			PlayerChunkLoader.update(event.getPlayer(), event.getTo().getBlockX() >> 4, event.getTo().getBlockZ() >> 4, event.getTo().getWorld());
		}
	}
	
}
