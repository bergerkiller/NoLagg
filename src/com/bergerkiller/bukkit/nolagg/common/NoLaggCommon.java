package com.bergerkiller.bukkit.nolagg.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.permissions.NoPermissionException;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;
import com.bergerkiller.bukkit.nolagg.NoLaggComponents;
import com.bergerkiller.bukkit.nolagg.Permission;
import com.bergerkiller.bukkit.nolagg.itembuffer.ItemMap;
import com.bergerkiller.bukkit.nolagg.tnt.TNTHandler;

public class NoLaggCommon extends NoLaggComponent {

	private String[] lastargs = new String[1];
	private Map<String, List<String>> clearShortcuts = new HashMap<String, List<String>>();

	@Override
	public void onDisable(ConfigurationNode config) {
		this.clearShortcuts.clear();
		this.lastargs = new String[1];
	}

	@Override
	public void onEnable(ConfigurationNode config) {
		this.onReload(config);
	}
	
	@Override
	public void onReload(ConfigurationNode config) {
		//clear shortcuts
		this.clearShortcuts.clear();
		this.lastargs = new String[1];
		if (!config.contains("clearShortcuts")) {
			ConfigurationNode node = config.getNode("clearShortcuts");
			node.set("enemies", Arrays.asList("monster"));
			node.set("notneutral", Arrays.asList("monster", "item", "tnt", "egg", "arrow"));
		}
		ConfigurationNode shortc = config.getNode("clearShortcuts");
		shortc.setHeader("");
		shortc.addHeader("Several shortcuts you can use for the /nolagg clear(all) command");
		for (String key : shortc.getKeys()) {
			clearShortcuts.put(key.toLowerCase(), shortc.getList(key, String.class));
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, String[] args) throws NoPermissionException {
		if (args.length > 0) {
			boolean all = args[0].equalsIgnoreCase("clearall");
			if (args[0].equalsIgnoreCase("clear") || all) {
				if (sender instanceof Player) {
					Permission.COMMON_CLEAR.handle(sender);
				} else {
					all = true;
				}
				//fix and partly read args
				boolean tnt = args.length == 1;
				boolean items = tnt;
				boolean animals = false;
				boolean monsters = false;
				boolean remall = false;
				if (args.length == 2) {
					if (args[1].equalsIgnoreCase("last")) {
						args = lastargs;
						sender.sendMessage(ChatColor.GREEN + "The last-used clear command has been invoked:");
					} else {
						List<String> a = this.clearShortcuts.get(args[1].toLowerCase());
						if (a != null) {
							args = a.toArray(new String[0]);
							sender.sendMessage(ChatColor.GREEN + "Using clear shortcut: " + args[1]);
						}
					}
				}
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
				} else {
					worlds = new World[] {((Player) sender).getWorld()};
				}
				if (tnt && NoLaggComponents.TNT.isEnabled()) {
					TNTHandler.clear();
				}
				if (items && NoLaggComponents.ITEMBUFFER.isEnabled()) {
					if (all) {
						ItemMap.clear();
					} else {
						for (World world : worlds) {
							ItemMap.clear(world);
						}
					}
				}
				int remcount = 0;
				for (World world : worlds) {
					for (Entity e : world.getEntities()) {
						boolean remove = false;
						if (e instanceof Player) {
							continue;
						} else if (remall) {
							remove = true;
						} else if (args.length == 0) {
							remove = e instanceof Item || e instanceof TNTPrimed || e instanceof ExperienceOrb;
						} else if (e instanceof TNTPrimed && tnt) {
							remove = true;
						} else if (e instanceof Item && items) {
							remove = true;
						} else {
							String type = EntityUtil.getName(e);
							if (animals && EntityUtil.isAnimal(type)) {
								remove = true;
							} else if (monsters && EntityUtil.isMonster(type)) {
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
				if (all) {
					sender.sendMessage(ChatColor.YELLOW + "All worlds have been cleared: " + remcount + " entities removed!");
				} else {
					sender.sendMessage(ChatColor.YELLOW + "This world has been cleared: " + remcount + " entities removed!");
				}
			} else if (args[0].equalsIgnoreCase("gc")) {
				Permission.COMMON_GC.handle(sender);
				Runtime.getRuntime().gc();
				sender.sendMessage("Memory garbage collected!");
			} else {
				return false;
			}
			return true;
		}
		return false;
	}
}
