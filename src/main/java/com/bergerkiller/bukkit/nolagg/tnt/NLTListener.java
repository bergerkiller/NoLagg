package com.bergerkiller.bukkit.nolagg.tnt;

import java.util.Iterator;

import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

public class NLTListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onExplosionPrime(ExplosionPrimeEvent event) {
		if (!(event.getEntity() instanceof TNTPrimed)) {
			return;
		}
		// do stuff
		CustomExplosion explosion = new CustomExplosion(event.getEntity(), event.getEntity().getLocation(), event.getRadius(), event.getFire());
		explosion.doAll();

		// Cancel the explosion (we handle it with better performance, so monitor is allowed. Same result!)
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		TNTHandler handler = NoLaggTNT.plugin.getTNTHandler();
		if (handler == null) {
			return;
		}
		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			if (!handler.isExplosionAllowed(iter.next())) {
				iter.remove();
			}
		}
	}
}
