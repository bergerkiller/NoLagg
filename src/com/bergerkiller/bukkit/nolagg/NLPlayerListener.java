package com.bergerkiller.bukkit.nolagg;

import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerInteractEvent;
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
	public void onPlayerInteract(PlayerInteractEvent event) {
		Block b = event.getClickedBlock();
		if (b == null) b = event.getPlayer().getTargetBlock(null, 100);
		if (b != null) {
			PlayerChunkLoader.clear(event.getPlayer(), b.getChunk());
		}
	}
	
	@Override
	public void onPlayerMove(PlayerMoveEvent event) {
		if (!event.isCancelled()) {
			ChunkHandler.handleMove(event.getFrom(), event.getTo(), event.getPlayer());
		}
	}
						
}
