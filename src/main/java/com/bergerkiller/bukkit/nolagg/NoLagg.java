package com.bergerkiller.bukkit.nolagg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.PluginBase;
import com.bergerkiller.bukkit.common.config.FileConfiguration;
import com.bergerkiller.bukkit.common.metrics.Graph;
import com.bergerkiller.bukkit.common.permissions.NoPermissionException;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.nolagg.chunks.NoLaggChunks;
import com.bergerkiller.bukkit.nolagg.monitor.PerformanceMonitor;

public class NoLagg extends PluginBase {
	public static NoLagg plugin;
	private List<NoLaggComponent> components = new ArrayList<NoLaggComponent>();

	public void register(NoLaggComponent component) {
		this.components.add(component);
	}

	@Override
	public int getMinimumLibVersion() {
		return Common.VERSION;
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
			// NoLagg enabled components
			getMetrics().addGraph(new Graph("Enabled Components") {
				@Override
				public void onUpdate() {
					clearPlotters();
					for (NoLaggComponent comp : NoLagg.plugin.components) {
						if(comp.isEnabled()) {
							addPlotter(comp.getName());
						}
					}
				}
			});

			// Dependencies
			getMetrics().addGraph(new Graph("Dependencies") {
				@Override
				public void onUpdate() {
					clearPlotters();
					if (NoLaggChunks.isOreObfEnabled) {
						addPlotter("Orebfuscator");
					}
					if (CommonUtil.getPlugin("Vault") != null) {
						addPlotter("Vault");
					}
				}
			});

			// Performance
			getMetrics().addGraph(new Graph("Performance") {
				@Override
				public void onUpdate() {
					clearPlotters();
					if (NoLaggComponents.MONITOR.isEnabled()) {
						addPlotter("TPS (Ticks per second", (int) PerformanceMonitor.tps);
						addPlotter("Memory (MB)", (int) (PerformanceMonitor.usedmem / (1024 * 1024)));
					}
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
				if (args[0].equalsIgnoreCase("ver") || args[0].equalsIgnoreCase("version")) {
					sender.sendMessage(ChatColor.GREEN + this.getName() + " version " + this.getVersion());
					return true;
				} else if (args[0].equalsIgnoreCase("reload")) {
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
