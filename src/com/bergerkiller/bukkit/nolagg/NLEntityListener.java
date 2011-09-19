package com.bergerkiller.bukkit.nolagg;

import java.util.List;

import net.minecraft.server.Explosion;
import net.minecraft.server.MathHelper;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
			
	public void createExplosion(Entity source, Location at, List<Block> affectedBlocks, float yield) {
		net.minecraft.server.World world = ((CraftWorld) at.getWorld()).getHandle();
		world.makeSound(at.getX(), at.getY(), at.getZ(), "random.explode", 4.0F, (1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.2F) * 0.7F);
		
		Explosion ex = new Explosion(world, ((CraftEntity) source).getHandle(), at.getX(), at.getY(), at.getZ(), radius);
		ex.a = fire;
		ex.a();
		at.getWorld().createExplosion(at, 0.1f);
		for (Block b : affectedBlocks) {
			 
			int id = b.getTypeId();
			int x = b.getLocation().getBlockX();
			int y = b.getLocation().getBlockY();
			int z = b.getLocation().getBlockZ();
			
            double d0 = (double) ((float) x + world.random.nextFloat());
            double d1 = (double) ((float) y + world.random.nextFloat());
            double d2 = (double) ((float) z + world.random.nextFloat());
            double d3 = d0 - at.getX();
            double d4 = d1 - at.getY();
            double d5 = d2 - at.getX();
            double d6 = (double) MathHelper.a(d3 * d3 + d4 * d4 + d5 * d5);

            d3 /= d6;
            d4 /= d6;
            d5 /= d6;
            double d7 = 0.5D / (d6 / (double) radius + 0.1D);

            d7 *= (double) (world.random.nextFloat() * world.random.nextFloat() + 0.3F);
            d3 *= d7;
            d4 *= d7;
            d5 *= d7;
            world.a("explode", (d0 + at.getX() * 1.0D) / 2.0D, (d1 + at.getY() * 1.0D) / 2.0D, (d2 + at.getZ() * 1.0D) / 2.0D, d3, d4, d5);
            world.a("smoke", d0, d1, d2, d3, d4, d5);
 
            if (id > 0 && id != Material.FIRE.getId()) {
                net.minecraft.server.Block bb = net.minecraft.server.Block.byId[id];
                if (bb != null) {
        			bb.dropNaturally(world, x, y, z, world.getData(x, y, z), yield);
                }
            }	
            world.setTypeId(x, y, z, 0);
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
			int i = 0;
			boolean hasTNT = false;
			while (i < event.blockList().size()) {
				Block b = event.blockList().get(i);
				if (b.getType() == Material.TNT) {
					TnTHandler.detonate(b);
					hasTNT = true;
					event.blockList().remove(i);
				} else {
					i++;
				}
			}
			if (hasTNT) {
				createExplosion(event.getEntity(), event.getLocation(), event.blockList(), event.getYield());
				event.setCancelled(true);
			}
		}
	}
	
}
