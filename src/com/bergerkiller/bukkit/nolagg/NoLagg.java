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
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

public class NoLagg extends JavaPlugin {
	public static NoLagg plugin;

	private final NLPlayerListener playerListener = new NLPlayerListener();
	private final NLEntityListener entityListener = new NLEntityListener(this);
	private final NLWorldListener worldListener = new NLWorldListener();
	private int updateID = -1;
	private int updateInterval = 20;
	
	public void onEnable() {	
		plugin = this;
		
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.ITEM_SPAWN, entityListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.PLAYER_PICKUP_ITEM, playerListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.CHUNK_LOAD, worldListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.CHUNK_UNLOAD, worldListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.WORLD_LOAD, worldListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Priority.Lowest, this);
		pm.registerEvent(Event.Type.ENTITY_COMBUST, entityListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.EXPLOSION_PRIME, entityListener, Priority.Lowest, this);
		pm.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Priority.Monitor, this);
				
		//General settings
		Configuration config = getConfiguration();
		NLEntityListener.maxTNTIgnites = config.getInt("maxTnTIgnites", 40);
		ItemHandler.maxItemsPerChunk = config.getInt("maxItemsPerChunk", 40);
		ItemHandler.formStacks = config.getBoolean("formItemStacks", true);
		ChunkHandler.chunkUnloadDelay = config.getInt("chunkUnloadDelay", 10000);
		AutoSaveChanger.newInterval = config.getInt("autoSaveInterval", 0);
		OrbScanner.interval = config.getInt("orbScannerInterval", 200);
		updateInterval = config.getInt("updateInterval", updateInterval);
		
		//Spawn restrictions
		List<String> tmplist = config.getKeys("spawnlimits.default");
		if (tmplist != null && tmplist.size() > 0) {
			for (String deflimit : tmplist) {
				String key = "spawnlimits.default." + deflimit;
				SpawnHandler.setLimit(null, deflimit, config.getInt(key, -1));
			}
		}
		tmplist = config.getKeys("spawnlimits.worlds");
		if (tmplist != null && tmplist.size() > 0) {
			for (String world : tmplist) {
				for (String deflimit : config.getKeys("spawnlimits.worlds." + world)) {
					String key = "spawnlimits.worlds." + world + "." + deflimit;
					SpawnHandler.setLimit(world, deflimit, config.getInt(key, -1));
				}
			}
		}
		
		//init it to write out correctly
		AutoSaveChanger.init();
				
		//Write out data
		config.setProperty("maxTnTIgnites", NLEntityListener.maxTNTIgnites);
		config.setProperty("maxItemsPerChunk", ItemHandler.maxItemsPerChunk);
		config.setProperty("formItemStacks", ItemHandler.formStacks);
		config.setProperty("chunkUnloadDelay", ChunkHandler.chunkUnloadDelay);
		config.setProperty("autoSaveInterval", AutoSaveChanger.newInterval);
		config.setProperty("orbScannerInterval", OrbScanner.interval);
		config.setProperty("updateInterval", updateInterval);
		config.save(); 

		ItemHandler.loadAll();
		SpawnHandler.init();
		OrbScanner.init();
		
		updateID = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				ItemHandler.update();
				StackFormer.update();
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
		SpawnHandler.deinit();
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
		if (args.length == 1) {
			if (args[0].equalsIgnoreCase("clear")) {
				if (sender instanceof Player) {
					if (!((Player) sender).hasPermission("nolagg.clear")) {
						sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to use this command!");
						return true;
					}
				}
				if (!(sender instanceof Player) || (args.length == 2 && args[1].equalsIgnoreCase("all"))) {
					for (World w : getServer().getWorlds()) {
						clear(w);
					}
				} else {
					clear(((Player) sender).getWorld());
				}
				sender.sendMessage("All spawned items on this server are cleared!");
				ItemHandler.loadAll();
				return true;
			}
		}
		return false;
	}
	
}