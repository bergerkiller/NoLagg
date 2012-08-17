package com.bergerkiller.bukkit.nolagg.spawnlimiter;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.ItemUtil;

public class NLSLListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onWorldLoad(WorldLoadEvent event) {
		EntityManager.init(event.getWorld());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldUnload(WorldUnloadEvent event) {
		if (!event.isCancelled()) {
			EntityManager.deinit(event.getWorld());
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onItemSpawn(ItemSpawnEvent event) {
		if (!event.isCancelled()) {
			if (ItemUtil.isIgnored(event.getEntity())) {
				SpawnHandler.ignoreSpawn(event.getEntity());
			}
			if (!EntityManager.addEntity(EntityUtil.getNative(event.getEntity()))) {
				event.setCancelled(true);
			}
		}
	}

	private long prevSpawnWave = System.currentTimeMillis();
	private static long spawnWaitTime = 3000; //how many msecs to wait spawning
	private static long spawnTime = 1000; //how many msecs to allow spawning

//	@EventHandler(priority = EventPriority.HIGHEST)
//	public void onVehicleSpawn(VehicleCreateEvent event) {
//		if (!EntityManager.addEntity(EntityUtil.getNative(event.getVehicle()))) {
//			event.getVehicle().remove();
//		}
//	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (!event.isCancelled()) {
			if (event.getSpawnReason() == SpawnReason.CUSTOM) {
				SpawnHandler.ignoreSpawn(event.getEntity());
			} else if (event.getSpawnReason() == SpawnReason.SPAWNER) {
				SpawnHandler.mobSpawnerSpawned(event.getEntity());
			} else if (event.getSpawnReason() == SpawnReason.NATURAL) {
				long time = System.currentTimeMillis();
				long diff = time - prevSpawnWave - spawnWaitTime;
				//to prevent lots of lag because of spammed spawn events
				if (diff < 0) {
					event.setCancelled(true);
					return;
				} else if (diff > spawnTime) {
					prevSpawnWave = time;
				}
			}
			if (!EntityManager.addEntity(EntityUtil.getNative(event.getEntity()))) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockPhysics(BlockPhysicsEvent event) {
		if (event.isCancelled()) return;
		Material type = event.getBlock().getType();
		if (type == Material.GRAVEL || type == Material.SAND) {
			if (event.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
				EntityLimiter limiter = SpawnHandler.GENERALHANDLER.getEntityLimits(event.getBlock().getWorld(), "falling" + type.toString());
				if (limiter != null) {
					if (limiter.canSpawn()) {
						limiter.spawn();
					} else {
						event.setCancelled(true);
					}
				}
			}
		}
	}
}
