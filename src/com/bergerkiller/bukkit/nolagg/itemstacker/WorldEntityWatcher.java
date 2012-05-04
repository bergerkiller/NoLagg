package com.bergerkiller.bukkit.nolagg.itemstacker;

import java.util.LinkedHashSet;

import com.bergerkiller.bukkit.common.WorldListener;

import net.minecraft.server.*;

public class WorldEntityWatcher extends WorldListener {

	/*
	 * Initializer functions
	 */
	private WorldEntityWatcher(World world) {
		super(world);
		this.refresh();
	}	
	public static WorldEntityWatcher watch(World world) {
		WorldEntityWatcher rval = new WorldEntityWatcher(world);
		rval.enable();
		return rval;
	}
	
	public void refresh() {
		this.items.clear();
		this.orbs.clear();
		for (Object entity : world.entityList) {
			this.a((Entity) entity);
		}
	}

	public final LinkedHashSet<EntityItem> items = new LinkedHashSet<EntityItem>();
	public final LinkedHashSet<EntityExperienceOrb> orbs = new LinkedHashSet<EntityExperienceOrb>();

	@Override
	public void onEntityAdd(Entity e) {
		if (e instanceof EntityItem) {
			if (!NoLaggItemStacker.isIgnoredItem(e)) {
				this.items.add((EntityItem) e);
			}
		} else if (e instanceof EntityExperienceOrb && NoLaggItemStacker.stackOrbs) {
			this.orbs.add((EntityExperienceOrb) e);
		}
	}

	@Override
	public void onEntityRemove(Entity e) {
		if (e instanceof EntityItem) {
			//don't bother doing an 'ignored item' check as it checks in a map or set anyway
			this.items.remove(e);
		} else if (e instanceof EntityExperienceOrb && NoLaggItemStacker.stackOrbs) {
			this.orbs.remove(e);
		}
	}

}
