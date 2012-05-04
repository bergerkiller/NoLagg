package com.bergerkiller.bukkit.nolagg.lighting;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class NLLListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkPopulate(ChunkPopulateEvent event) {
		if (NoLaggLighting.auto) {
			LightingFixThread.fix(event.getChunk());
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onChunkUnload(ChunkUnloadEvent event) {
		if (LightingFixThread.isFixing(event.getChunk())) {
			event.setCancelled(true);
		}
	}
		
}
