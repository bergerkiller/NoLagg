package com.bergerkiller.bukkit.nolagg;

import java.util.List;

import net.minecraft.server.Packet29DestroyEntity;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
import org.bukkit.util.config.Configuration;

public class NoLagg extends JavaPlugin {
	public static NoLagg plugin;

	private final NLPlayerListener playerListener = new NLPlayerListener();
	private final NLEntityListener entityListener = new NLEntityListener();
	private final NLWorldListener worldListener = new NLWorldListener();
	private int updateID = -1;
	private int updateInterval = 20;
	
	public static int chunkSendInterval = 5;
	public static int chunkSendRate = 1;
			
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
		pm.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.CREATURE_SPAWN, entityListener, Priority.Lowest, this);
						
		int explrate = 40;
		double sendinter = 5;
		
		//General settings
		Configuration config = getConfiguration();
		TnTHandler.interval = config.getInt("tntDetonationInterval", TnTHandler.interval);
		TnTHandler.rate = config.getInt("tntDetonationRate", TnTHandler.rate);
		ItemHandler.maxItemsPerChunk = config.getInt("maxItemsPerChunk", 40);
		ItemHandler.formStacks = config.getBoolean("formItemStacks", true);
		ChunkHandler.chunkUnloadDelay = config.getInt("chunkUnloadDelay", 10000);
		AutoSaveChanger.newInterval = config.getInt("autoSaveInterval", 0);
		OrbScanner.interval = config.getInt("orbScannerInterval", 200);
		updateInterval = config.getInt("updateInterval", updateInterval);
		explrate = config.getInt("explosionRate", 40);
		sendinter = config.getDouble("chunkSendInterval", sendinter);
		
		//Convert interval
		if (sendinter >= 1) {
			chunkSendInterval = (int) sendinter;
			chunkSendRate = 1;
		} else if (sendinter < 0.1) {
			chunkSendInterval = 0;
			chunkSendRate = 0;
		} else {
			chunkSendInterval = 1;
			chunkSendRate = (int) (1 / sendinter);
		}
		
		//Spawn restrictions
		List<String> tmplist = config.getKeys("spawnlimits.default");
		if (tmplist != null && tmplist.size() > 0) {
			for (String deflimit : tmplist) {
				String key = "spawnlimits.default." + deflimit;
				SpawnHandler.setDefaultLimit(deflimit, config.getInt(key, -1));
			}
		}
		tmplist = config.getKeys("spawnlimits.global");
		if (tmplist != null && tmplist.size() > 0) {
			for (String glimit : tmplist) {
				String key = "spawnlimits.global." + glimit;
				SpawnHandler.setDefaultLimit(glimit, config.getInt(key, -1));
			}
		}
		tmplist = config.getKeys("spawnlimits.worlds");
		if (tmplist != null && tmplist.size() > 0) {
			for (String world : tmplist) {
				for (String deflimit : config.getKeys("spawnlimits.worlds." + world)) {
					String key = "spawnlimits.worlds." + world + "." + deflimit;
					SpawnHandler.setWorldLimit(world, deflimit, config.getInt(key, -1));
				}
			}
		}
		
		//init it to write out correctly
		AutoSaveChanger.init();
				
		//Write out data
		config.setProperty("tntDetonationInterval", TnTHandler.interval);
		config.setProperty("tntDetonationRate", TnTHandler.rate);
		config.setProperty("maxItemsPerChunk", ItemHandler.maxItemsPerChunk);
		config.setProperty("formItemStacks", ItemHandler.formStacks);
		config.setProperty("chunkUnloadDelay", ChunkHandler.chunkUnloadDelay);
		config.setProperty("autoSaveInterval", AutoSaveChanger.newInterval);
		config.setProperty("orbScannerInterval", OrbScanner.interval);
		config.setProperty("updateInterval", updateInterval);
		config.setProperty("explosionRate", explrate);
		config.setProperty("chunkSendInterval", sendinter);
		config.save(); 

		TnTHandler.setExplosionRate(explrate);
		ItemHandler.loadAll();
		OrbScanner.init();
		
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
		OrbScanner.deinit();
		ItemHandler.unloadAll();
		AutoSaveChanger.deinit();
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
