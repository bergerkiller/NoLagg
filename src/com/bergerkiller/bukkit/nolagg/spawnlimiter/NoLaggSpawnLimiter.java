package com.bergerkiller.bukkit.nolagg.spawnlimiter;

import java.util.logging.Level;

import com.bergerkiller.bukkit.common.WorldListener;
import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;

public class NoLaggSpawnLimiter extends NoLaggComponent {
	public static NoLaggSpawnLimiter plugin;

	@Override
	public void onEnable(ConfigurationNode config) {
		if (WorldListener.isValid()) {
			plugin = this;
			this.register(NLSLListener.class);
			this.onReload(config);
		} else {
			log(Level.SEVERE, "Failed to initialize spawn limiter: could not bind to world entity listener");
		}
	}

	@Override
	public void onReload(ConfigurationNode config) {
		// Deinit entities
		EntityWorldListener.deinit();
		// default spawn limits
		if (!config.contains("spawnlimits")) {
			ConfigurationNode limits = config.getNode("spawnlimits");
			ConfigurationNode node = limits.getNode("default");
			node.set("mob", 800);
			node.set("cow", 60);
			node.set("item", 1500);
			node.set("creepers", 30);
			node.set("monsters", 400);
			node = limits.getNode("worlds");
			node.set("world1.monsters", 300);
			node.set("world2.chickens", 30);
			node = limits.getNode("global");
			node.set("mobs", 3000);
			limits = config.getNode("mobSpawnerLimits");
			limits.set("default.mob", 300);
			limits.set("default.zombie", 30);
			limits.set("worlds.world3.cavespider", 0);
			limits.set("worlds.creativeworld.mob", 0);
			limits.set("global.mob", 1000);
			// others
		}

		config.setHeader("spawnlimits", "");
		config.addHeader("spawnlimits", "The general spawn limits (natural spawning)");
		config.addHeader("spawnlimits", "For more information, see http://dev.bukkit.org/server-mods/nolagg/pages/spawn-limits-nolagg/");
		config.setHeader("spawnlimits.default", "The default spawn limits per world, overridden by world limits");
		config.setHeader("spawnlimits.worlds", "The world-specific spawn limits");
		config.setHeader("spawnlimits.global", "The global spawn limits");

		config.setHeader("mobSpawnerLimits", "");
		config.addHeader("mobSpawnerLimits", "The spawn limits for mob spawners");
		config.addHeader("mobSpawnerLimits", "For more information, see http://dev.bukkit.org/server-mods/nolagg/pages/spawn-limits-nolagg/");
		config.setHeader("mobSpawnerLimits.default", "The default spawn limits per world, overridden by world limits");
		config.setHeader("mobSpawnerLimits.worlds", "The world-specific spawn limits");
		config.setHeader("mobSpawnerLimits.global", "The global spawn limits");

		// Spawn restrictions
		EntitySpawnHandler.GENERALHANDLER.clear().load(config.getNode("spawnlimits"));
		// Mob spawn restrictions
		EntitySpawnHandler.MOBSPAWNERHANDLER.clear().load(config.getNode("mobSpawnerLimits"));
		// Init entities
		EntityWorldListener.init();
	}

	@Override
	public void onDisable(ConfigurationNode config) {
		EntityWorldListener.deinit();
		EntitySpawnHandler.GENERALHANDLER.clear();
		EntitySpawnHandler.MOBSPAWNERHANDLER.clear();
	}
}
