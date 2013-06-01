package com.bergerkiller.bukkit.nolagg.examine;

import java.io.File;
import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.permissions.NoPermissionException;
import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;
import com.bergerkiller.bukkit.nolagg.Permission;

public class NoLaggExamine extends NoLaggComponent {
	public static NoLaggExamine plugin;
	public static int maxExamineTime;
	public static File exportFolder;

	@Override
	public void onEnable(ConfigurationNode config) {
		plugin = this;
		this.onReload(config);
		SchedulerWatcher.init();
	}

	@Override
	public void onDisable(ConfigurationNode config) {
		SchedulerWatcher.deinit();
		PluginLogger.abort();
	}

	@Override
	public void onReload(ConfigurationNode config) {
		config.setHeader("maxExamineTime", "\nThe maximum time in ticks a generated examine report can be");
		config.addHeader("maxExamineTime", "It can be increased, but the generated file might be too large for the viewer to handle");
		maxExamineTime = config.get("maxExamineTime", 72000);
		config.setHeader("exportFolder", "\nThe folder in which the .exam files are saved");
		exportFolder = new File(config.get("exportFolder", "plugins"));
	}

	@Override
	public boolean onCommand(CommandSender sender, String[] args) throws NoPermissionException {
		if (args.length != 0) {
			if (args[0].equalsIgnoreCase("examine")) {
				int duration = MathUtil.clamp(500, maxExamineTime);
				if (args.length >= 2) {
					if (LogicUtil.contains(args[1].toLowerCase(Locale.ENGLISH), "stop", "cancel", "abort")) {
						// Abort any examine operation going on right now
						if (!PluginLogger.isRunning()) {
							sender.sendMessage(ChatColor.YELLOW + "The server is not being examined right now.");
						} else {
							PluginLogger.abort();
							sender.sendMessage(ChatColor.YELLOW + "Examining aborted: examined data of " + PluginLogger.duration + " ticks is saved");
						}
						return true;
					}
					duration = ParseUtil.parseInt(args[1], duration);
				}
				if (sender instanceof Player) {
					Permission.EXAMINE_RUN.handle(sender);
					PluginLogger.recipients.add(sender.getName());
				} else {
					PluginLogger.recipients.add(null);
				}
				if (PluginLogger.isRunning()) {
					sender.sendMessage(ChatColor.RED + "The server is already being examined: " + PluginLogger.getDurPer() + "% completed");
					sender.sendMessage(ChatColor.GREEN + "You will be notified when the report has been generated");
				} else if (duration > maxExamineTime) {
					sender.sendMessage(ChatColor.RED + "Examine duration of " + duration + " exceeded the maximum possible: " + maxExamineTime + " ticks");
				} else {
					PluginLogger.duration = duration;
					PluginLogger.start();
					sender.sendMessage(ChatColor.GREEN + "The server will be examined for " + duration + " ticks (" + (duration / 20) + " seconds)");
					sender.sendMessage(ChatColor.GREEN + "You will be notified when the report has been generated");
					sender.sendMessage(ChatColor.YELLOW + "To abort examining the server, use /lag examine abort");
				}
				return true;
			}
		}
		return false;
	}
}
