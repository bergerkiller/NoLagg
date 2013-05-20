package com.bergerkiller.bukkit.nolagg.spawnlimiter;

import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import com.bergerkiller.bukkit.common.events.CreaturePreSpawnEvent;
import com.bergerkiller.bukkit.common.events.EntityAddEvent;
import com.bergerkiller.bukkit.common.events.EntityRemoveEvent;
import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;
import com.bergerkiller.bukkit.nolagg.spawnlimiter.limit.EntityLimit;

public class NLSLListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onItemSpawn(ItemSpawnEvent event) {
		if (!event.isCancelled()) {
			if (!EntitySpawnHandler.handlePreSpawn(event.getEntity(), false)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (!event.isCancelled()) {
			if (!EntitySpawnHandler.handlePreSpawn(event.getItemDrop(), false)) {
				event.getPlayer().sendMessage(NoLagg.plugin.getLocale("spawnlimiter.nodrop"));
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onProjectileSpawn(ProjectileLaunchEvent event) {
		if (!event.isCancelled()) {
			if (!EntitySpawnHandler.handlePreSpawn(event.getEntity(), false)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityAdd(EntityAddEvent event) {
		Entity e = event.getEntity();
		if (e.isDead()) {
			return;
		}
		if (!EntitySpawnHandler.addEntity(e)) {
			e.remove();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityRemove(EntityRemoveEvent event) {
		EntitySpawnHandler.removeEntity(event.getEntity());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onChunkLoad(ChunkLoadEvent event) {
		// These entities were mistakingly added
		// If any of them are dead, revoke the dead status and force-add on the limit
		for (Entity e : event.getChunk().getEntities()) {
			if (e.isDead() && !ExistingRemovalMap.isRemovable(EntityUtil.getName(e))) {
				EntityUtil.setDead(e, false);
				EntitySpawnHandler.forceSpawn(e);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onCreaturePreSpawn(CreaturePreSpawnEvent event) {
		World world = event.getSpawnLocation().getWorld();
		String entityName = EntityUtil.getName(event.getEntityType());
		EntityLimit limit = EntitySpawnHandler.GENERALHANDLER.getEntityLimits(world, entityName);
		final int spawnableCount = limit.getSpawnableCount();

		// Work with the spawnable count obtained
		if (spawnableCount == 0) {
			event.setCancelled(true);
		} else if (spawnableCount < event.getMaxSpawnCount()) {
			event.setMaxSpawnCount(spawnableCount);
			if (event.getMinSpawnCount() > event.getMaxSpawnCount()) {
				event.setMinSpawnCount(event.getMaxSpawnCount());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (!event.isCancelled()) {
			if (event.getSpawnReason() == SpawnReason.CUSTOM) {
				EntitySpawnHandler.setIgnored(event.getEntity());
				return;
			}
			if (!EntitySpawnHandler.handlePreSpawn(event.getEntity(), event.getSpawnReason() == SpawnReason.SPAWNER)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockPhysics(BlockPhysicsEvent event) {
		if (event.isCancelled()) {
			return;
		}
		Material type = event.getBlock().getType();
		if (BlockUtil.isType(type, Material.GRAVEL, Material.SAND)) {
			if (event.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
				String name = "falling" + type.toString().toLowerCase(Locale.ENGLISH);
				EntityLimit limiter = EntitySpawnHandler.GENERALHANDLER.getEntityLimits(event.getBlock().getWorld(), name);
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
