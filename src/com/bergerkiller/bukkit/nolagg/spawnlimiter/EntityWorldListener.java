package com.bergerkiller.bukkit.nolagg.spawnlimiter;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.Entity;
import net.minecraft.server.World;

import com.bergerkiller.bukkit.common.WorldListener;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

public class EntityWorldListener extends WorldListener {
	private static Map<World, EntityWorldListener> listeners = new HashMap<World, EntityWorldListener>();

	private EntityWorldListener(World world) {
		super(world);
	}

	public static void init() {
		for (World world : WorldUtil.getWorlds()) {
			addListener(world);
		}
	}

	public static void deinit() {
		for (World world : WorldUtil.getWorlds()) {
			removeListener(world);
		}
	}

	public static void addListener(World world) {
		EntityWorldListener listener = new EntityWorldListener(world);
		listener.enable();
		listeners.put(world, listener);
	}

	public static void removeListener(World world) {
		EntityWorldListener listener = listeners.remove(world);
		if (listener != null) {
			listener.disable();
		}
	}

	@Override
	public void disable() {
		super.disable();
		for (Entity e : WorldUtil.getEntities(this.world)) {
			this.b(e);
		}
	}

	@Override
	public void onEntityAdd(Entity entity) {
		if (!EntitySpawnHandler.addEntity(entity.getBukkitEntity())) {
			entity.dead = true;
		}
	}

	@Override
	public void onEntityRemove(Entity entity) {
		EntitySpawnHandler.removeEntity(entity.getBukkitEntity());
	}
}
