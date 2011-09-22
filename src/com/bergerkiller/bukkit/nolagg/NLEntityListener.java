package com.bergerkiller.bukkit.nolagg;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
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
			    } catch (Throwable ex) {
			    }
			}
		}
	}
				
	@Override
	public void onEntityExplode(EntityExplodeEvent event) {
		if (!event.isCancelled()) {
			for (Block b : event.blockList()) {
				if (b.getType() == Material.TNT) {
					net.minecraft.server.Entity entity = ((CraftEntity) event.getEntity()).getHandle();
					if (entity != null) {
						TnTHandler.createExplosion(event.getLocation(), event.blockList(), event.getYield());
						event.setCancelled(true);
						break;
					}
				}
			}
		}
	}
	
}
