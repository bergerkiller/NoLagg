package com.bergerkiller.bukkit.nolagg.itemstacker;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import com.bergerkiller.bukkit.common.events.EntityAddEvent;
import com.bergerkiller.bukkit.common.events.EntityRemoveEvent;

public class NLISListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldLoad(WorldLoadEvent event) {
		StackFormer.get(event.getWorld());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldUnload(WorldUnloadEvent event) {
		if (!event.isCancelled()) {
			StackFormer.remove(event.getWorld());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityAdd(EntityAddEvent event) {
		if (event.getEntity().isDead()) {
			return;
		}
		StackFormer.get(event.getEntity().getWorld()).addEntity(event.getEntity()); 
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityRemove(EntityRemoveEvent event) {
		StackFormer.get(event.getEntity().getWorld()).removeEntity(event.getEntity()); 
	}
}
