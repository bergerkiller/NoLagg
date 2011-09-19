package com.bergerkiller.bukkit.nolagg;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.server.Explosion;
import net.minecraft.server.MathHelper;
import net.minecraft.server.Packet60Explosion;
import net.minecraft.server.ServerConfigurationManager;
import net.minecraft.server.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.ExplosionPrimeEvent;
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
			
	@SuppressWarnings("rawtypes")
	public void createExplosion(net.minecraft.server.Entity source, Location at, List<Block> affectedBlocks, float yield) {
		try {
			WorldServer world = ((CraftWorld) at.getWorld()).getHandle();
			
			for (Block b : affectedBlocks) {
				 
				int id = b.getTypeId();
				 
	            if (id == Material.TNT.getId()) {
					TnTHandler.detonate(b);
	            } else {
	    			int x = b.getLocation().getBlockX();
	    			int y = b.getLocation().getBlockY();
	    			int z = b.getLocation().getBlockZ();
	            	if (id > 0 && id != Material.FIRE.getId()) {
	            		net.minecraft.server.Block bb = net.minecraft.server.Block.byId[id];
	            		if (bb != null) {
	            			bb.dropNaturally(world, x, y, z, world.getData(x, y, z), yield);
	            		}
	            	}
	                world.setTypeId(x, y, z, 0);
	            }
			}
			Packet60Explosion packet = new Packet60Explosion(at.getX(), at.getY(), at.getZ(), yield, new HashSet());
			ServerConfigurationManager manager = world.server.serverConfigurationManager;
			manager.sendPacketNearby(at.getX(), at.getY(), at.getZ(), 64.0D, world.dimension, packet);
		} catch (Throwable t) {
			System.out.println("[NoLagg] Warning: explosion did not go as planned!");
			t.printStackTrace();
		}
	}
	
	@Override
	public void onExplosionPrime(ExplosionPrimeEvent event) {
		radius = event.getRadius();
		fire = event.getFire();
	}
	
	private static float radius;
	private static boolean fire;
	
	@Override
	public void onEntityExplode(EntityExplodeEvent event) {
		if (!event.isCancelled()) {
			for (Block b : event.blockList()) {
				if (b.getType() == Material.TNT) {
					net.minecraft.server.Entity entity = ((CraftEntity) event.getEntity()).getHandle();
					if (entity != null) {
						createExplosion(entity, event.getLocation(), event.blockList(), event.getYield());
						event.setCancelled(true);
						break;
					}
				}
			}
		}
	}
	
}
