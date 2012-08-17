package com.bergerkiller.bukkit.nolagg.examine;

import net.timedminecraft.server.TimedChunkProviderServer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

public class NLEListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldInit(WorldInitEvent event) {
		TimedChunkProviderServer.convert(event.getWorld());
	}
	
}
