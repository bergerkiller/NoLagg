package com.bergerkiller.bukkit.nolagg.chunks;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.chunks.antiloader.DummyPlayerManager;

public class NLCListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(final PlayerJoinEvent event) {
		ChunkSendQueue.bind(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		ChunkSendQueue.bind(event.getPlayer()).idle(5);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkLoad(ChunkLoadEvent event) {
		DynamicViewDistance.addChunk();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkUnload(ChunkUnloadEvent event) {
		if (event.isCancelled()) {
			return;
		}
		DynamicViewDistance.addChunk();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldLoad(WorldLoadEvent event) {
		if (NoLaggChunks.useDynamicView) {
			DummyPlayerManager.convert(event.getWorld());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldUnload(WorldUnloadEvent event) {
		if (event.isCancelled() || !NoLaggChunks.useDynamicView) {
			return;
		}
		int chunkCount = WorldUtil.getNative(event.getWorld()).chunkProviderServer.chunkList.size();
		for (int i = 0; i < chunkCount; i++) {
			DynamicViewDistance.removeChunk();
		}
	}
}
