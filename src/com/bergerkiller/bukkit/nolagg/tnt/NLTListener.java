package com.bergerkiller.bukkit.nolagg.tnt;

import java.util.Iterator;

import net.minecraft.server.Entity;
import net.minecraft.server.EntityTNTPrimed;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

public class NLTListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onExplosionPrime(ExplosionPrimeEvent event) {
		if (event.isCancelled())
			return;
		if (event.getEntity() == null)
			return;
		Entity entity = ((CraftEntity) event.getEntity()).getHandle();
		if (entity == null)
			return;
		if (entity instanceof EntityTNTPrimed) {
			event.setCancelled(true);
			// do stuff
			CustomExplosion explosion = new CustomExplosion(entity.world, entity, entity.locX, entity.locY, entity.locZ, event.getRadius(), event.getFire());
			explosion.doAll();
		}
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
