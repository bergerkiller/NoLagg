package com.bergerkiller.bukkit.nolagg.itemstacker;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import com.bergerkiller.bukkit.common.utils.WorldUtil;

public class NLISListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldLoad(WorldLoadEvent event) {
		StackFormer.get(WorldUtil.getNative(event.getWorld()));
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldUnload(WorldUnloadEvent event) {
		if (!event.isCancelled()) {
			StackFormer.remove(WorldUtil.getNative(event.getWorld()));
		}
	}
}
