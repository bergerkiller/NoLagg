package com.bergerkiller.bukkit.nolagg.monitor;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.permissions.NoPermissionException;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;
import com.bergerkiller.bukkit.nolagg.Permission;

public class NoLaggMonitor extends NoLaggComponent {

	public static NoLaggMonitor plugin;
	
	@Override
	public void onReload(ConfigurationNode config) {
		PerformanceMonitor.deinit();
		
		//interval and logging init
		config.setHeader("monitorInterval", "The interval at which new performance snapshots are generated");
		config.setHeader("startLoggingOnStartup", "Whether or not to start logging server performance on startup");
		PerformanceMonitor.monitorInterval = config.get("monitorInterval", 40);
		PerformanceMonitor.sendLog = config.get("startLoggingOnStartup", false);
		
		//broadcast if lagging
		ConfigurationNode blag = config.getNode("lagNotifier");
		blag.setHeader("\nThe server notifies players (with a permission) when the tick rate drops below the threshold");
		blag.setHeader("enabled", "Enable or disable this feature");
		blag.setHeader("message", "The message to send to these players");
		blag.setHeader("threshold", "The tick rate at which it starts broadcasting");
		blag.setHeader("interval", "The interval in miliseconds to send this message (1000 ms = 1 second)");
		PerformanceMonitor.broadcastLagging = blag.get("enabled", false);
		PerformanceMonitor.broadcastInterval = blag.get("interval", 10000);
		PerformanceMonitor.broadcastThreshold = blag.get("threshold", 15.0);
		PerformanceMonitor.broadcastMessage = blag.get("message", "&cThe server can't keep up!");
		
		PerformanceMonitor.init();
	}

	@Override
	public void onEnable(ConfigurationNode config) {
		plugin = this;
		this.register(NLMListener.class);
		this.onReload(config);
	}

	@Override
	public void onDisable(ConfigurationNode config) {
		PerformanceMonitor.deinit();
	}

	@Override
	public boolean onCommand(CommandSender sender, String[] args) throws NoPermissionException {
		if (args.length != 0) {
			if (args[0].equalsIgnoreCase("monitor") || args[0].equalsIgnoreCase("stat") || args[0].equalsIgnoreCase("stats")) {
				boolean once = !args[0].equalsIgnoreCase("monitor");
				if (sender instanceof Player) {
					Permission.MONITOR_USE.handle(sender);
					Player p = (Player) sender;
					if (PerformanceMonitor.recipients.remove(p.getName())) {
						for (int i = 0; i < 10; i++) {
							p.sendMessage(" ");
						}
						p.sendMessage(ChatColor.YELLOW + "You are no longer monitoring this server.");
					} else {
						PerformanceMonitor.recipients.add(p.getName());
						if (once) {
							p.sendMessage(ChatColor.GREEN + "You will receive a snapshot shortly...");
							PerformanceMonitor.removalReq.add(p.getName());
						} else {
							p.sendMessage(ChatColor.GREEN + "You are now monitoring this server.");
							p.sendMessage(ChatColor.GREEN + "To stop monitoring, perform this command again.");
							PerformanceMonitor.removalReq.remove(p.getName());
						}
					}
				} else {
					if (PerformanceMonitor.sendConsole) {
						PerformanceMonitor.sendConsole = false;
						sender.sendMessage("You are no longer monitoring this server.");
					} else {
						PerformanceMonitor.sendConsole = true;
						if (once) {
							sender.sendMessage("You will receive a snapshot shortly...");
							PerformanceMonitor.removalCon = true;
						} else {
							sender.sendMessage("You are now monitoring this server.");
							sender.sendMessage("To stop monitoring, perform this command again.");
							PerformanceMonitor.removalCon = false;
						}
					}
				}
			} else if (args[0].equalsIgnoreCase("log")) {
				Permission.MONITOR_LOG.handle(sender);
				if (PerformanceMonitor.sendLog) {
					PerformanceMonitor.sendLog = false;
					sender.sendMessage("Server stats are no longer logged to file.");
				} else {
					PerformanceMonitor.sendLog = true;
					sender.sendMessage("Server stats are now logged to file.");
					sender.sendMessage("To stop logging, perform this command again.");
				}
			} else if (args[0].equalsIgnoreCase("clearlog")) {
				Permission.MONITOR_CLEARLOG.handle(sender);
				if (PerformanceMonitor.clearLog()) {
					sender.sendMessage("Server log cleared");
				} else {
					sender.sendMessage("Failed to clear the server log");
				}
			} else if (args[0].equalsIgnoreCase("mem") || args[0].equalsIgnoreCase("memory")) {
				if (sender instanceof Player) {
					Permission.MONITOR_SHOWMEMORY.handle(sender);
					sender.sendMessage(ChatColor.GREEN + "[Static memory] " + ChatColor.YELLOW + "[Dynamic memory] " + ChatColor.RED + "[Unused memory]");
				}
				sender.sendMessage(PerformanceMonitor.getMemory(sender instanceof Player));
			} else if (args[0].equalsIgnoreCase("lagmem") || args[0].equalsIgnoreCase("memlag")) {
				if (sender instanceof Player) {
					Permission.MONITOR_SHOWTICKRATE.handle(sender);
					Permission.MONITOR_SHOWMEMORY.handle(sender);
					sender.sendMessage(ChatColor.GREEN + "[Static memory] " + ChatColor.YELLOW + "[Dynamic memory] " + ChatColor.RED + "[Unused memory]");
				}
				sender.sendMessage(PerformanceMonitor.getMemory(sender instanceof Player));
				sender.sendMessage(PerformanceMonitor.getTPS(sender instanceof Player));
			} else {
				return false;
			}
		} else {
			Permission.MONITOR_SHOWTICKRATE.handle(sender);
			sender.sendMessage(PerformanceMonitor.getTPS(sender instanceof Player));
		}
		return true;
	}
}
