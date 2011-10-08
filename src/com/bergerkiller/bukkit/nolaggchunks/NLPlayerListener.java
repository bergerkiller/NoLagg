package com.bergerkiller.bukkit.nolaggchunks;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class NLPlayerListener extends PlayerListener {
		
	@Override
	public void onPlayerMove(PlayerMoveEvent event) {
		if (!event.isCancelled()) {
			PlayerChunkLoader.update(event.getPlayer());
		}
	}
	
	@Override
	public void onPlayerQuit(PlayerQuitEvent event) {
		PlayerChunkLoader.remove(event.getPlayer());
	}
	
	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		//PlayerChunkLoader.getBuffer(event.getPlayer()).
	}
	
	@Override
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (!event.isCancelled()) {
			PlayerChunkBuffer buffer = PlayerChunkLoader.getBuffer(event.getPlayer());
			if (buffer.world != event.getTo().getWorld()) {
				buffer.update(event.getTo());
			}
		}
	}
	
}
