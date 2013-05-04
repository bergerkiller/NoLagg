package com.bergerkiller.bukkit.nolagg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.bergerkiller.bukkit.common.PluginBase;
import com.bergerkiller.bukkit.common.config.FileConfiguration;
import com.bergerkiller.bukkit.common.metrics.Graph;
import com.bergerkiller.bukkit.common.metrics.SoftDependenciesGraph;
import com.bergerkiller.bukkit.common.permissions.NoPermissionException;

public class NoLagg extends PluginBase {
	public static NoLagg plugin;
	private List<NoLaggComponent> components = new ArrayList<NoLaggComponent>();

	public void register(NoLaggComponent component) {
		this.components.add(component);
	}

	@Override
	public int getMinimumLibVersion() {
		String result = "";
		Pattern pattern = Pattern.compile("\\d+");
		Matcher matcher = pattern.matcher("${project.bkcversion}");
		
		while(matcher.find()) {
			result += matcher.group();
		}
		
		return result != "" ? Integer.valueOf(result) : 0;
	}

	@Override
	public void permissions() {
		this.loadPermissions(Permission.class);
	}

	@Override
	public void localization() {
		this.loadLocale("spawnlimiter.nodrop", ChatColor.YELLOW+"[NoLagg] "+ChatColor.RED+"Can not drop this item: Item spawn limit reached!");
	}

	@Override
	public void enable() {
		plugin = this;

		FileConfiguration config = new FileConfiguration(this);
		config.load();

		// Top header
		config.setHeader("This is the configuration of NoLagg, in here you can enable or disable features as you please");
		config.addHeader("For more information, you can visit the following websites:");
		config.addHeader("http://dev.bukkit.org/server-mods/nolagg/");
		config.addHeader("http://forums.bukkit.org/threads/nolagg.36986/");

		// Load components
		NoLaggComponents.loadAll(config);
		for (NoLaggComponent comp : this.components) {
			comp.enable(config);
		}

		config.save();

		// Initialize Metrics
		if (hasMetrics()) {
			// NoLagg soft dependencies
			getMetrics().addGraph(new SoftDependenciesGraph());

			// NoLagg enabled components
			getMetrics().addGraph(new Graph("Enabled Components") {
				@Override
				public void onUpdate(Plugin plugin) {
					for (NoLaggComponent comp : NoLagg.plugin.components) {
						togglePlotter(comp.getName(), comp.isEnabled());
					}
				}
			});

			// Total server memory
			getMetrics().addGraph(new Graph("Total server memory") {
				@Override
				public void onUpdate(Plugin plugin) {
					clearPlotters();
					// Get server total memory in MB (>> 20 = / (1024 * 1024))
					final long mem = Runtime.getRuntime().totalMemory() >> 20;
					final String key;
					if (mem <= 512) {
						key = "0-512 MB";
					} else if (mem <= 1024) {
						key = "512-1024 MB";
					} else if (mem <= 2048) {
						key = "1024-2048 MB";
					} else if (mem <= 4096) {
						key = "2048-4096 MB";
					} else if (mem <= 8192) {
						key = "4096-8192 MB";
					} else if (mem <= 16384) {
						key = "8-16 GB";
					} else {
						key = "16+ GB";
					}
					togglePlotter(key, true);
				}
			});
		}
	}

	protected List<NoLaggComponent> getComponents() {
		return this.components;
	}

	@Override
	public void updateDependency(Plugin plugin, String pluginName, boolean enabled) {
		for (NoLaggComponent comp : this.components) {
			comp.updateDependency(plugin, pluginName, enabled);
		}
	}

	@Override
	public void disable() {
		FileConfiguration config = new FileConfiguration(this);
		config.load();
		for (NoLaggComponent comp : this.components) {
			comp.disable(config);
		}
		config.save();
	}

	@Override
	public boolean command(CommandSender sender, String command, String[] args) {
		try {
			if (args.length != 0) {
				if (args[0].equalsIgnoreCase("reload")) {
					Permission.RELOAD.handle(sender);

					// check if all components are still enabled
					FileConfiguration config = new FileConfiguration(this);
					config.load();
					Iterator<NoLaggComponent> iter = this.components.iterator();
					while (iter.hasNext()) {
						NoLaggComponent comp = iter.next();
						if (!config.get(comp.getName().toLowerCase() + ".enabled", true)) {
							comp.disable(config);
							iter.remove();
						}
					}

					// enable new components or reload loaded components
					NoLaggComponents.loadAll(config);
					for (NoLaggComponent comp : this.components) {
						if (comp.isEnabled()) {
							comp.reload(config);
						} else {
							comp.enable(config);
						}
					}
					config.save();
					sender.sendMessage("Configuration of all NoLagg components reloaded!");
					return true;
				}
			}
			for (NoLaggComponent plugin : this.components) {
				if (plugin.onCommand(sender, args))
					return true;
			}
			if (sender instanceof Player) {
				sender.sendMessage(ChatColor.RED + "Unknown sub-command!");
			} else {
				sender.sendMessage("Unknown sub-command!");
			}
		} catch (NoPermissionException ex) {
			if (sender instanceof Player) {
				sender.sendMessage(ChatColor.RED + "You don't have permission to use this!");
			} else {
				sender.sendMessage("This command is only for players!");
			}
		}
		return true;
	}
}
