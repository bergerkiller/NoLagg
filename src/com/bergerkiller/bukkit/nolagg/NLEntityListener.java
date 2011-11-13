package com.bergerkiller.bukkit.nolagg;

import org.bukkit.entity.Item;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.ItemSpawnEvent;

public class NLEntityListener extends EntityListener {
	
	@Override
	public void onItemSpawn(ItemSpawnEvent event) {
		SpawnHandler.handleSpawn(event);
		if (!event.isCancelled()) {
			if (!ItemHandler.handleItemSpawn((Item) event.getEntity())) {
				event.setCancelled(true);
			}
		}
	}
	
	@Override
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (event.getSpawnReason() == SpawnReason.CUSTOM) {
			SpawnHandler.ignoreSpawn(event.getEntity());
		} else {
			SpawnHandler.handleSpawn(event);
		}
	}
				
	@Override
	public void onEntityExplode(EntityExplodeEvent event) {
		if (!event.isCancelled()) {
			if (TnTHandler.createExplosion(event)) {
				//I know, we monitor, but the end result is the same anyway!
				event.setCancelled(true);
			}
		}
	}
	
	@Override
	public void onEntityDeath(EntityDeathEvent event) {
		SpawnHandler.removeSpawn(event.getEntity());
	}
	
}
