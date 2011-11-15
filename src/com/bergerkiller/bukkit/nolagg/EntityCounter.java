package com.bergerkiller.bukkit.nolagg;

import java.util.List;

import net.minecraft.server.EntityExperienceOrb;
import net.minecraft.server.EntityItem;
import net.minecraft.server.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;

public class EntityCounter {
	
	public static List<WorldServer> getWorlds() {
		return ((CraftServer) Bukkit.getServer()).getHandle().server.worlds;
	}
	public static void fill(WorldServer world, List<Item> items, List<ExperienceOrb> orbs, List<Entity> all) {
		for (Object obj : world.entityList) {
			if (obj instanceof EntityItem) {
				Item item = (Item) ((EntityItem) obj).getBukkitEntity();
				if (ItemHandler.isShowcased(item)) continue;
				if (items != null) items.add(item);
			} else if (obj instanceof EntityExperienceOrb) {
				ExperienceOrb orb = (ExperienceOrb) ((EntityExperienceOrb) obj).getBukkitEntity();
				if (orbs != null) orbs.add(orb);
			}
			if (all != null) all.add(((net.minecraft.server.Entity) obj).getBukkitEntity());
		}
	}

}
