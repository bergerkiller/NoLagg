package com.bergerkiller.bukkit.nolagg;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
	
	public void onEnable() {	
		plugin = this;
		
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.ITEM_SPAWN, entityListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.PLAYER_PICKUP_ITEM, playerListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.CHUNK_LOAD, worldListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.CHUNK_UNLOAD, worldListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.ENTITY_COMBUST, entityListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.EXPLOSION_PRIME, entityListener, Priority.Highest, this);
				
		Configuration config = getConfiguration();
		NLEntityListener.maxTNTIgnites = config.getInt("maxTnTIgnites", 40);
		ItemHandler.maxItemsPerChunk = config.getInt("maxItemsPerChunk", 40);
		ItemHandler.formStacks = config.getBoolean("formItemStacks", true);
		config.setProperty("maxTnTIgnites", NLEntityListener.maxTNTIgnites);
		config.setProperty("maxItemsPerChunk", ItemHandler.maxItemsPerChunk);
		config.setProperty("formItemStacks", ItemHandler.formStacks);
		config.save(); 
		
		ItemHandler.loadAll();
		
		getCommand("nolagg").setExecutor(this);
		
        //final msg
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
	}
	public void onDisable() {
		ItemHandler.unloadAll();
		System.out.println("NoLagg disabled!");
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
				ItemHandler.unloadAll();
				for (World w : getServer().getWorlds()) {
					for (Entity e : w.getEntities().toArray(new Entity[0])) {
						if (e instanceof Item) e.remove();
					}
				}
				sender.sendMessage("All spawned items on this server are cleared!");
				ItemHandler.loadAll();
				return true;
			}
		}
		return false;
	}
	
}
