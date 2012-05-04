package com.bergerkiller.bukkit.nolagg.saving;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class NLSListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldLoad(WorldLoadEvent event) {
		AutoSaveChanger.change(event.getWorld());
	}

}
