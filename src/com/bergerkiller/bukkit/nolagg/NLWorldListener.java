package com.bergerkiller.bukkit.nolagg;

import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class NLWorldListener extends WorldListener {

	@Override
	public void onChunkLoad(ChunkLoadEvent event) {
		ChunkHandler.handleLoad(event);
		ItemHandler.loadChunk(event.getChunk());
	}
	
	@Override
	public void onChunkUnload(ChunkUnloadEvent event) {
		ChunkHandler.handleUnload(event);
		if (!event.isCancelled()) {
			ItemHandler.unloadChunk(event.getChunk());
		}
	}
		
	@Override
	public void onWorldLoad(WorldLoadEvent event) {
		AutoSaveChanger.change(event.getWorld());
		ItemHandler.init(event.getWorld());
	}
	
	@Override
	public void onWorldUnload(WorldUnloadEvent event) {
		ChunkHandler.handleUnload(event);
		if (!event.isCancelled()) ItemHandler.unloadWorld(event.getWorld());
	}
	
	@Override
	public void onChunkPopulate(ChunkPopulateEvent event) {
		AsyncSaving.scheduleLightingFix(event.getChunk());
	}

	
}
