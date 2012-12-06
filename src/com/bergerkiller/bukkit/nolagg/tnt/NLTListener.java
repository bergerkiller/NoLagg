package com.bergerkiller.bukkit.nolagg.tnt;

import java.util.Iterator;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

public class NLTListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onExplosionPrime(ExplosionPrimeEvent event) {
		if (event.isCancelled() || !(event.getEntity() instanceof TNTPrimed)) {
			return;
		}
		// do stuff
		CustomExplosion explosion = new CustomExplosion(event.getEntity(), event.getEntity().getLocation(), event.getRadius(), event.getFire());
		explosion.doAll();
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityExplode(EntityExplodeEvent event) {
		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Block next = iter.next();
			if (TNTHandler.isScheduledForDetonation(next)) {
				iter.remove();
			} else if (!TNTHandler.changeBlocks && next.getTypeId() != Material.TNT.getId()) {
				iter.remove();
			}
		}
	}
}
