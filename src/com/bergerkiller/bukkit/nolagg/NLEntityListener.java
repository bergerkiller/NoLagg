package com.bergerkiller.bukkit.nolagg;

import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;

public class NLEntityListener extends EntityListener {
	private static NoLagg plugin;
	public NLEntityListener(NoLagg instance) {
		plugin = instance;
	}
	
	@Override
	public void onItemSpawn(ItemSpawnEvent event) {
		SpawnHandler.handleSpawn(event);
		if (!event.isCancelled()) {
			if (!ItemHandler.handleItemSpawn((Item) event.getEntity())) {
				event.setCancelled(true);
			} else {
				Item item = (Item) event.getEntity();
				StackFormer.add(item);
			}
		}
	}
	
	@Override
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		SpawnHandler.handleSpawn(event);
	}
		
	@Override
	public void onEntityDamage(EntityDamageEvent event) {
		if (!event.isCancelled() && event.getEntity() instanceof Player) {
			Player p = (Player) event.getEntity();
			if (event.getDamage() >= p.getHealth()) {
			    try {
			    	if (p.getExperience() > 0) {
						ExperienceOrb orb = p.getWorld().spawn(p.getLocation(), ExperienceOrb.class);
						orb.setExperience(p.getExperience());
						p.setExperience(0);
			    	}
			    } catch (Exception ex) {
			    }
			}
		}
	}
	
	@Override
	public void onExplosionPrime(ExplosionPrimeEvent event) {
		SpawnHandler.handleSpawn(event);
	}
		
	public static int maxTNTIgnites = 40;
	public static int TNTIgnites = 0;
	
	public static void delayedSubtract() {
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				TNTIgnites -= 1;
				if (TNTIgnites < 0) TNTIgnites = 0;
			}
		}, 20L);
	}

	@Override
	public void onEntityCombust(EntityCombustEvent event) {
		if (!event.isCancelled() && event.getEntity() instanceof Item) {
			ItemHandler.removeSpawnedItem((Item) event.getEntity());
		}
	}
	
	@Override
	public void onEntityExplode(EntityExplodeEvent event) {
		if (!event.isCancelled()) {
			if (event.getEntity() instanceof TNTPrimed) {
				if (TNTIgnites < maxTNTIgnites) {
					TNTIgnites += 1;
					delayedSubtract();
				} else {
					Entity entity = event.getLocation().getWorld().spawn(event.getLocation(), org.bukkit.entity.TNTPrimed.class);
					SpawnHandler.handleSpawn(entity);
					event.setCancelled(true);
				}
			}
		}
	}
}
