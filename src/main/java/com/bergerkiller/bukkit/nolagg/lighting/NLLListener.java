package com.bergerkiller.bukkit.nolagg.lighting;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

public class NLLListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	public void onChunkUnload(ChunkUnloadEvent event) {
		if (LightingService.isProcessing(event.getChunk())) {
			event.setCancelled(true);
		}
	}
}
