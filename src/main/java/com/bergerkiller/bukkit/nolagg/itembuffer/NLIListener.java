package com.bergerkiller.bukkit.nolagg.itembuffer;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class NLIListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onItemSpawn(ItemSpawnEvent event) {
		if (event.getEntityType() == EntityType.DROPPED_ITEM) {
			if(NoLaggItemBuffer.shouldIgnore(event.getEntity()))
				return;
			
			if (!ItemMap.addItem(event.getEntity())) {
				event.setCancelled(true);
			}
		}
	}

	public void onItemDespawn(Item item) {
		if (item.getType() == EntityType.DROPPED_ITEM) {
			if(NoLaggItemBuffer.shouldIgnore(item)) {
				NoLaggItemBuffer.remove(item);
			} else {
				ItemMap.removeItem(item);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onItemDespawn(ItemDespawnEvent event) {
		if (!event.isCancelled()) {
			onItemDespawn(event.getEntity());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onItemPickup(PlayerPickupItemEvent event) {
		if (!event.isCancelled()) {
			onItemDespawn(event.getItem());
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onChunkLoad(ChunkLoadEvent event) {
		ItemMap.loadChunk(event.getChunk());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkUnload(ChunkUnloadEvent event) {
		if (!event.isCancelled()) {
			ItemMap.unloadChunk(event.getChunk());
		}
	}

}
