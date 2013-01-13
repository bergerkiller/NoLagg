package com.bergerkiller.bukkit.nolagg.spawnlimiter;

import java.util.HashSet;

import org.bukkit.World;
import org.bukkit.entity.Entity;

import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.spawnlimiter.limit.EntityLimit;
import com.bergerkiller.bukkit.nolagg.spawnlimiter.limit.EntitySpawnLimiter;

/**
 * Contains all the Entity limits for both mob-spawner and regularly spawned Entities
 */
public class EntitySpawnHandler {
	public static EntitySpawnLimiter MOBSPAWNERHANDLER = new EntitySpawnLimiter();
	public static EntitySpawnLimiter GENERALHANDLER = new EntitySpawnLimiter();
	private static HashSet<Integer> mobSpawnerEntities = new HashSet<Integer>();
	private static HashSet<Integer> ignoredEntities = new HashSet<Integer>(); //N.B: Excludes ignored items!

	public static boolean isItem(String name) {
		return name.toLowerCase().startsWith("item");
	}

	public static boolean isFalling(String name) {
		return name.toLowerCase().startsWith("falling");
	}

	/**
	 * Marks the Entity as being ignored
	 * 
	 * @param entity to ignore
	 */
	public static void setIgnored(Entity entity) {
		ignoredEntities.add(entity.getEntityId());
	}

	/**
	 * Checks if the given Entity is being ignored
	 * 
	 * @param entity to check
	 * @return True if ignored, False if not
	 */
	public static boolean isIgnored(Entity entity) {
		return EntityUtil.isIgnored(entity) || ignoredEntities.contains(entity.getEntityId());
	}

	/**
	 * Checks if the Entity can spawn<br>
	 * Marks the Entity a mob if it is specified to do so<br>
	 * No actual spawning is performed here.
	 * 
	 * @param entity to check for
	 * @param isMobSpawner state, True if spawned by a mob spawner, False if not
	 * @return True if the entity can Spawn, False if not
	 */
	public static boolean handlePreSpawn(Entity entity, boolean isMobSpawner) {
		if (isIgnored(entity)) {
			return true;
		}
		if (getLimits(entity, isMobSpawner).canSpawn()) {
			if (isMobSpawner) {
				mobSpawnerEntities.add(entity.getEntityId());
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Gets the Entity limits for the Entity specified
	 * 
	 * @param entity to get the limits for
	 * @param mobSpawnerSpawned state, True if by mob spawner, False if not
	 * @return Entity limits
	 */
	public static EntityLimit getLimits(Entity entity, boolean mobSpawnerSpawned) {
		String name = EntityUtil.getName(entity);
		if (mobSpawnerSpawned) {
			return MOBSPAWNERHANDLER.getEntityLimits(entity.getWorld(), name);
		} else {
			return GENERALHANDLER.getEntityLimits(entity.getWorld(), name);
		}
	}

	/**
	 * Fills all handlers with the entities on the server
	 */
	public static void initEntities() {
		for (World world : WorldUtil.getWorlds()) {
			for (Entity entity : WorldUtil.getEntities(world)) {
				if (ExistingRemovalMap.isRemovable(EntityUtil.getName(entity))) {
					// Can remove it - remove if needed
					addEntity(entity);
				} else {
					// Can not remove it - just increment the count
					forceSpawn(entity);
				}
			}
		}
	}

	/**
	 * Force-spawns the Entity, even if the limit does not allow it
	 * 
	 * @param entity to force-spawn
	 */
	public static void forceSpawn(Entity entity) {
		if (!isIgnored(entity)) {
			getLimits(entity, mobSpawnerEntities.contains(entity.getEntityId())).spawn();
		}
	}

	/**
	 * Handles the adding of an Entity
	 * 
	 * @param entity to add
	 * @return True if it was allowed, False if not
	 */
	public static boolean addEntity(Entity entity) {
		if (isIgnored(entity)) {
			return true;
		} else {
			EntityLimit limit = getLimits(entity, mobSpawnerEntities.contains(entity.getEntityId()));
			boolean canSpawn = limit.canSpawn();
			// Spawn anyhow, will be reduced in count when the entity is removed properly
			limit.spawn();
			// Return the spawn state (if true, kills the entity)
			return canSpawn;
		}
	}

	/**
	 * Handles the removal of an Entity
	 * 
	 * @param entity to remove
	 */
	public static void removeEntity(Entity entity) {
		if (!isIgnored(entity)) {
			if (entity.isDead()) {
				getLimits(entity, mobSpawnerEntities.remove(entity.getEntityId())).despawn();
			} else {
				getLimits(entity, mobSpawnerEntities.contains(entity.getEntityId())).despawn();
			}
		}
	}
}
