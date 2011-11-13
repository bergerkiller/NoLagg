package com.bergerkiller.bukkit.nolagg;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.server.Packet29DestroyEntity;

import org.bukkit.Bukkit;
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
					
	private static Logger logger = Logger.getLogger("Minecraft");
	public static void log(Level level, String message) {
		logger.log(level, "[NoLagg] " + message);
	}
	
	public void onEnable() {		
		plugin = this;
		
		//General registering
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_PICKUP_ITEM, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.CHUNK_LOAD, worldListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.CHUNK_UNLOAD, worldListener, Priority.Lowest, this);
		pm.registerEvent(Event.Type.WORLD_LOAD, worldListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.WORLD_UNLOAD, worldListener, Priority.Monitor, this);
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
		AutoSaveChanger.newInterval = config.parse("autoSaveInterval", 400);
		updateInterval = config.parse("updateInterval", 20);
		StackFormer.stackRadius = config.parse("stackRadius", 1.0);
		StackFormer.stackThreshold = config.parse("stackThreshold", 2);
        AsyncSaving.enabled = config.parse("useAsyncChunkSaving", true);
		if (useSpawnLimits) {
			//Spawn restrictions
			ConfigurationSection slimits = config.getConfigurationSection("spawnlimits");
			if (slimits != null) {
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
		}

		//Write out data
		config.save(); 

		TnTHandler.init();
		ItemHandler.init();
		ChunkHandler.init();
		AsyncAutoSave.init();
		AutoSaveChanger.init();
		PerformanceMonitor.init();
		
		
		updateID = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				ItemHandler.update();
				StackFormer.update();
				ChunkHandler.cleanUp();
				SpawnHandler.update();
			}
		}, 0, updateInterval);
				
		AsyncSaving.startSaving();
		
		getCommand("nolagg").setExecutor(this);
		
        //final msg
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
	}
	public void onDisable() {
		getServer().getScheduler().cancelTask(updateID);
		ItemHandler.deinit();
		TnTHandler.deinit();
		ChunkHandler.deinit();
		SpawnHandler.deinit();
		PerformanceMonitor.deinit();
		AsyncSaving.stopSaving();
		AsyncAutoSave.deinit();
		AutoSaveChanger.deinit();
		System.out.println("NoLagg disabled!");
	}
	
	public static void hideEntity(Entity e) {
		Packet29DestroyEntity packet = new Packet29DestroyEntity(e.getEntityId());
		for (Player p : e.getWorld().getPlayers()) {
			((CraftPlayer) p).getHandle().netServerHandler.sendPacket(packet);
		}
	}
	
	public static boolean isOrb(Entity e) {
		return e.getClass().getSimpleName().equals("CraftExperienceOrb");
	}
	
	private String[] lastargs = new String[1];
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] args) {
		if (args.length > 0) {
			boolean all = args[0].equalsIgnoreCase("clearall");
			if (args[0].equalsIgnoreCase("clear") || all) {
				if (sender instanceof Player) {
					if (!((Player) sender).hasPermission("nolagg.clear")) {
						sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to use this command!");
						return true;
					}
				} else {
					all = true;
				}
				//fix and partly read args
				boolean tnt = args.length == 1;
				boolean items = tnt;
				boolean animals = false;
				boolean monsters = false;
				boolean remall = false;
				boolean last = args.length == 2 && args[1].equalsIgnoreCase("last");
				if (last) args = lastargs;
				String[] toremove = new String[args.length - 1];
				for (int i = 1; i < args.length; i++) {
					String name = args[i].toLowerCase();
					if (name.contains("tnt")) {
						tnt = true;
					} else if (name.contains("item")) {
						items = true;
					} else if (name.contains("animal")) {
						animals = true;
					} else if (name.contains("monster")) {
						monsters = true;
					} else if (name.contains("mob")) {
						animals = true;
						monsters = true;
					} else if (name.contains("all")) {
						remall = true;
					}
					toremove[i - 1] = name;
				}
				args = toremove;
				World[] worlds;
				if (all) {
					worlds = Bukkit.getServer().getWorlds().toArray(new World[0]);
					if (items) ItemHandler.clear();
				} else {
					worlds = new World[] {((Player) sender).getWorld()};
					if (items) ItemHandler.clear(worlds[0]);
				}
				if (tnt) TnTHandler.clear();
				int remcount = 0;
				for (World world : worlds) {
					for (Entity e : world.getEntities()) {
						boolean remove = false;
						if (e instanceof Player) {
							continue;
						} else if (remall) {
							remove = true;
					    } else if (args.length == 0) {
							remove = e instanceof Item || e instanceof TNTPrimed || isOrb(e);
						} else if (e instanceof TNTPrimed && tnt) {
							remove = true;
						} else if (e instanceof Item && items) {
							remove = true;
						} else {
							String type = e.getClass().getSimpleName().toLowerCase();
							if (e instanceof Item) {
								type = "item" + ((Item) e).getItemStack().getType().toString().toLowerCase();
							} else if (type.startsWith("craft")) {
								type = type.substring(5);
							} else if (type.contains("tnt")) {
								type = "tnt";
							}
							if (animals && SpawnLimiter.isAnimal(type)) {
								remove = true;
							} else if (monsters && SpawnLimiter.isMonster(type)) {
								remove = true;
							} else {
								for (String arg : args) {
									if (type.contains(arg) || arg.contains(type)) {
										remove = true;
										break;
									}
								}	
							}
						}
						if (remove) {
							e.remove();
							remcount++;
						}
					}
				}
				if (last) {
					sender.sendMessage(ChatColor.GREEN + "The last-used clear command has been invoked:");
				}
				if (all) {
					ItemHandler.init();
					sender.sendMessage(ChatColor.YELLOW + "All worlds have been cleared: " + remcount + " entities removed!");
				} else {
					ItemHandler.init(worlds[0]);
					sender.sendMessage(ChatColor.YELLOW + "This world has been cleared: " + remcount + " entities removed!");
				}
			} else if (args[0].equalsIgnoreCase("monitor")) {
				if (sender instanceof Player) {
					Player p = (Player) sender;
					if (!p.hasPermission("nolagg.monitor")) {
						sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to use this command!");
						return true;
					}
					if (PerformanceMonitor.recipients.remove(p.getName())) {
						p.sendMessage("You are no longer monitoring this server.");
					} else {
						PerformanceMonitor.recipients.add(p.getName());
						p.sendMessage("You are now monitoring this server.");
					}
				} else {
					if (PerformanceMonitor.sendConsole) {
						PerformanceMonitor.sendConsole = false;
						sender.sendMessage("You are no longer monitoring this server.");
					} else {
						PerformanceMonitor.sendConsole = true;
						sender.sendMessage("You are now monitoring this server.");
					}
				}
			} else if (args[0].equalsIgnoreCase("log")) {
				if (sender instanceof Player) {
					if (!((Player) sender).hasPermission("nolagg.log")) {
						sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to use this command!");
						return true;
					}
				}
				if (PerformanceMonitor.sendLog) {
					PerformanceMonitor.sendLog = false;
					sender.sendMessage("Server stats are no longer logged to file.");
				} else {
					PerformanceMonitor.sendLog = true;
					sender.sendMessage("Server stats are now logged to file.");
				}
			} else if (args[0].equalsIgnoreCase("clearlog")) {
				if (sender instanceof Player) {
					if (!((Player) sender).hasPermission("nolagg.clearlog")) {
						sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to use this command!");
						return true;
					}
				}
				if (PerformanceMonitor.clearLog()) {
					sender.sendMessage("Server log cleared");
				} else {
					sender.sendMessage("Failed to clear the server log");
				}
			}
		}
		return true;
	}
	
}
