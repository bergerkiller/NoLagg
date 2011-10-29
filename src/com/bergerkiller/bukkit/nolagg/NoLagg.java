package com.bergerkiller.bukkit.nolagg;

import java.util.Set;

import net.minecraft.server.Packet29DestroyEntity;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NoLagg extends JavaPlugin {
	public static NoLagg plugin;

	private final NLPlayerListener playerListener = new NLPlayerListener();
	private final NLEntityListener entityListener = new NLEntityListener();
	private final NLWorldListener worldListener = new NLWorldListener();
	private int updateID = -1;
	private int updateInterval;
	
	public static boolean bufferItems;
	public static boolean bufferTNT;
	public static boolean useSpawnLimits;
	public static boolean useChunkUnloadDelay;
	public static boolean isShowcaseEnabled = false;
				
	public void onEnable() {		
		plugin = this;
		
		//General registering
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_PICKUP_ITEM, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.CHUNK_LOAD, worldListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.CHUNK_UNLOAD, worldListener, Priority.Lowest, this);
		pm.registerEvent(Event.Type.WORLD_LOAD, worldListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.ITEM_SPAWN, entityListener, Priority.Lowest, this);
		pm.registerEvent(Event.Type.CREATURE_SPAWN, entityListener, Priority.Lowest, this);
		
		//Make sure our events fires last, we don't want plugins reading the event after changes!
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				getServer().getPluginManager().registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Priority.Monitor, NoLagg.plugin);
				getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DEATH, entityListener, Priority.Monitor, NoLagg.plugin);
				if (getServer().getPluginManager().isPluginEnabled("Showcase")) {
					isShowcaseEnabled = true;
				}
			}
		}, 1);
				
		//General settings
		Configuration config = new Configuration(this);
		config.load();
		bufferItems = config.parse("bufferItems", true);
		bufferTNT = config.parse("bufferTNT", true);
		useSpawnLimits = config.parse("useSpawnLimits", true);
		useChunkUnloadDelay = config.parse("useChunkUnloadDelay", true);
		TnTHandler.interval = config.parse("tntDetonationInterval", 1);
		TnTHandler.rate = config.parse("tntDetonationRate", 10);
		TnTHandler.explosionRate = config.parse("explosionRate", 5);
		ItemHandler.maxItemsPerChunk = config.parse("maxItemsPerChunk", 40);
		ItemHandler.formStacks = config.parse("formItemStacks", true);
		ChunkHandler.chunkUnloadDelay = config.parse("chunkUnloadDelay", 10000);
		AutoSaveChanger.newInterval = config.parse("autoSaveInterval", 0);
		updateInterval = config.parse("updateInterval", 20);
		StackFormer.stackRadius = config.parse("stackRadius", 1.0);
		if (useSpawnLimits) {
			//Spawn restrictions
			ConfigurationSection slimits = config.getConfigurationSection("spawnlimits");
			ConfigurationSection tmp = slimits.getConfigurationSection("default");
			Set<String> tmplist = null;
			if (tmp != null) {
				tmplist = tmp.getKeys(false);
				if (tmplist != null && tmplist.size() > 0) {
					for (String deflimit : tmplist) {
						String key = "spawnlimits.default." + deflimit;
						SpawnHandler.setDefaultLimit(deflimit, config.getInt(key, -1));
					}
				}
			}
			tmp = slimits.getConfigurationSection("global");
			if (tmp != null) {
				tmplist = tmp.getKeys(false);
				if (tmplist != null && tmplist.size() > 0) {
					for (String glimit : tmplist) {
						String key = "spawnlimits.global." + glimit;
						SpawnHandler.setDefaultLimit(glimit, config.getInt(key, -1));
					}
				}
			}
			tmp = slimits.getConfigurationSection("worlds");
			if (tmp != null) {
				tmplist = tmp.getKeys(false);
				if (tmplist != null && tmplist.size() > 0) {
					for (String world : tmplist) {
						for (String deflimit : config.getKeys("spawnlimits.worlds." + world)) {
							String key = "spawnlimits.worlds." + world + "." + deflimit;
							SpawnHandler.setWorldLimit(world, deflimit, config.getInt(key, -1));
						}
					}
				}
			}
		}
		
		//init it to write out correctly
		AutoSaveChanger.init();
				
		//Write out data
		config.save(); 

		TnTHandler.init();
		ItemHandler.loadAll();
		StackFormer.init();
		ChunkHandler.init();
		
		updateID = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				ItemHandler.update();
				StackFormer.update();
				ChunkHandler.cleanUp();
				SpawnHandler.update();
			}
		}, 0, updateInterval);
		
		getCommand("nolagg").setExecutor(this);
		
        //final msg
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
	}
	public void onDisable() {
		getServer().getScheduler().cancelTask(updateID);
		ItemHandler.unloadAll();
		AutoSaveChanger.deinit();
		TnTHandler.deinit();
		System.out.println("NoLagg disabled!");
	}
	
	public static void hideEntity(Entity e) {
		Packet29DestroyEntity packet = new Packet29DestroyEntity(e.getEntityId());
		for (Player p : e.getWorld().getPlayers()) {
			((CraftPlayer) p).getHandle().netServerHandler.sendPacket(packet);
		}
	}
	private void clear(World w) {
		for (Entity e : w.getEntities()) {
			if (e instanceof Item) {
				e.remove();
			} else if (e instanceof TNTPrimed) {
				e.remove();
			} else if (isOrb(e)) {
				e.remove();
			}
		}
	}
	
	public static boolean isOrb(Entity e) {
		return e.getClass().getSimpleName().equals("CraftExperienceOrb");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] args) {
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("clear")) {
				if (sender instanceof Player) {
					if (!((Player) sender).hasPermission("nolagg.clear")) {
						sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to use this command!");
						return true;
					}
				}
				TnTHandler.clear();
				if (!(sender instanceof Player) || (args.length == 2 && args[1].equalsIgnoreCase("all"))) {
					ItemHandler.clear();
					for (World w : getServer().getWorlds()) {
						clear(w);
					}
					sender.sendMessage("All items, TnT and experience orbs on this server are cleared!");
				} else {
					World w = ((Player) sender).getWorld();
					ItemHandler.clear(w);
					clear(w);
					sender.sendMessage("All items, TnT and experience orbs on this world are cleared!");
				}
				ItemHandler.loadAll();
				return true;
			}
		}
		return false;
	}
	
}
