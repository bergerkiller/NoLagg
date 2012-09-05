package com.bergerkiller.bukkit.nolagg.spawnlimiter;

import net.minecraft.server.Entity;
import net.minecraft.server.World;

import com.bergerkiller.bukkit.common.WorldListener;

public class EntityWorldWatcher extends WorldListener {

	public EntityWorldWatcher(World world) {
		super(world);
	}

	@Override
	public void onEntityAdd(Entity entity) {
		if (!EntityManager.addEntity(entity)) {
			entity.dead = true;
		}
	}

	@Override
	public void onEntityRemove(Entity entity) {
		EntityManager.removeEntity(entity);
	}
}
