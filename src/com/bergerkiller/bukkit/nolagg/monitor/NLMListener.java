package com.bergerkiller.bukkit.nolagg.monitor;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class NLMListener implements Listener {

	public static int loadedChunks = 0;
	public static int unloadedChunks = 0;
	public static int generatedChunks = 0;
	
	public static void reset() {
		loadedChunks = 0;
		unloadedChunks = 0;
		generatedChunks = 0;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkLoad(ChunkLoadEvent event) {
		if (event.isNewChunk()) {
			generatedChunks++;
		} else {
			loadedChunks++;
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkUnload(ChunkUnloadEvent event) {
		if (!event.isCancelled()) {
			unloadedChunks++;
		}
	}
	
}
