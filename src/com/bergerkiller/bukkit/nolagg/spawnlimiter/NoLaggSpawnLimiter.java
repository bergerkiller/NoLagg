package com.bergerkiller.bukkit.nolagg.spawnlimiter;

import java.util.Arrays;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.nolagg.NoLagg;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;

public class NoLaggSpawnLimiter extends NoLaggComponent {
	public static NoLaggSpawnLimiter plugin;
	private Task spawnWaveTask;
	private static final int SPAWN_WAVE_INTERVAL = 20; // Allow a creature spawn wave to occur every second
	private static int spawnWaveCounter = 0;

	@Override
	public void onEnable(ConfigurationNode config) {
		plugin = this;
		this.register(NLSLListener.class);
		this.onReload(config);
	}

	@Override
	public void onReload(ConfigurationNode config) {
		// existing removable
		config.setHeader("forceRemoved", "");
		config.addHeader("forceRemoved", "Entity type or group names that can be removed from loaded chunks");
		config.addHeader("forceRemoved", "If you don't want certain already spawned entities removed while the server runs, ");
		config.addHeader("forceRemoved", "Remove them from this list");

		// default list
		if (!config.contains("forceRemoved")) {
			config.set("forceRemoved", Arrays.asList("monsters", "itemcobblestone", "itemdirt", "itemsand", "itemgravel"));
		}

		ExistingRemovalMap.clear();
		for (String type : config.getList("forceRemoved", String.class)) {
			ExistingRemovalMap.addRemovable(type);
		}

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
		EntitySpawnHandler.initEntities();

		// Spawn wave tick task
		spawnWaveTask = new Task(NoLagg.plugin) {
			public void run() {
				if (spawnWaveCounter++ >= SPAWN_WAVE_INTERVAL) {
					spawnWaveCounter = 0;
				}
			}
		}.start(1, 1);
	}

	@Override
	public void onDisable(ConfigurationNode config) {
		EntitySpawnHandler.GENERALHANDLER.clear();
		EntitySpawnHandler.MOBSPAWNERHANDLER.clear();
		Task.stop(spawnWaveTask);
		spawnWaveTask = null;
	}

	/**
	 * Checks if spawning creatures is currently allowed
	 * 
	 * @return True if spawning is possible, False if not
	 */
	public static boolean isCreatureSpawnAllowed() {
		return spawnWaveCounter == 0;
	}
}
