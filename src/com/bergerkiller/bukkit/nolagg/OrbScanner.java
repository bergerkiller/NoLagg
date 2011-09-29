package com.bergerkiller.bukkit.nolagg;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;

public class OrbScanner {
	private static int id;
	public static int interval;
	
	public static void init() {
		if (interval > 0) {
			Runnable r = new Runnable() {
				public void run() {
					combine();
				}
			};
			id = NoLagg.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(NoLagg.plugin, r, interval, interval);
			r.run();
		}
	}
	public static void deinit() {
		NoLagg.plugin.getServer().getScheduler().cancelTask(id);
	}
	
	public static void combine() {
		for (World w : Bukkit.getServer().getWorlds()) {
			combine(w);
		}
	}
	
	public static void combine(World world) {
		List<Entity> ee = world.getEntities();
		for (Entity e1 : ee) {
			if (valid(e1)) {
				ExperienceOrb o1 = (ExperienceOrb) e1;
				for (Entity e2 : ee) {
					if (e2 != e1 && valid(e2)) {
						ExperienceOrb o2 = (ExperienceOrb) e2;
						//compare o1 and o2
						if (o1.getLocation().distanceSquared(o2.getLocation()) < 4) {
							o1.setExperience(o1.getExperience() + o2.getExperience());
							o2.remove();
						}
					}		
				}
			}
		}
	}
	
	private static boolean valid(Entity e) {
		return !e.isDead() && NoLagg.isOrb(e);
	}

}
