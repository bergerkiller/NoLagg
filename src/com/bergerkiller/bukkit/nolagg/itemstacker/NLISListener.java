package com.bergerkiller.bukkit.nolagg.itemstacker;

import net.minecraft.server.Entity;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import com.bergerkiller.bukkit.common.events.EntityAddEvent;
import com.bergerkiller.bukkit.common.events.EntityRemoveEvent;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
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

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityAdd(EntityAddEvent event) {
		Entity e = EntityUtil.getNative(event.getEntity());
		if (e.dead) {
			return;
		}
		StackFormer.get(e.world).addEntity(e); 
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityRemove(EntityRemoveEvent event) {
		Entity e = EntityUtil.getNative(event.getEntity());
		StackFormer.get(e.world).removeEntity(e); 
	}
}
