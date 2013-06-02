package com.bergerkiller.bukkit.nolagg.examine;

import java.io.File;
import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.permissions.NoPermissionException;
import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;
import com.bergerkiller.bukkit.nolagg.Permission;

public class NoLaggExamine extends NoLaggComponent {
	public static NoLaggExamine plugin;
	public static int maxExamineTime;
	public static File exportFolder;
	public static final PluginLogger logger = new PluginLogger();
	private TickRateTrigger tickRateTrigger;

	@Override
	public void onEnable(ConfigurationNode config) {
		plugin = this;
		this.onReload(config);
		SchedulerWatcher.init(logger);
	}

	@Override
	public void onDisable(ConfigurationNode config) {
		Task.stop(tickRateTrigger);
		tickRateTrigger = null;

		SchedulerWatcher.deinit();
		logger.abort();
	}

	@Override
	public void onReload(ConfigurationNode config) {
		config.setHeader("maxExamineTime", "\nThe maximum time in ticks a generated examine report can be");
		config.addHeader("maxExamineTime", "It can be increased, but the generated file might be too large for the viewer to handle");
		maxExamineTime = config.get("maxExamineTime", 72000);
		config.setHeader("exportFolder", "\nThe folder in which the .exam files are saved");
		exportFolder = new File(config.get("exportFolder", "plugins"));

		ConfigurationNode triggerConfig = config.getNode("tickRateTrigger");

		// Set trigger headers
		triggerConfig.setHeader("\nSets options for automatic examining triggered by a change in tick rate");
		triggerConfig.setHeader("enabled", "Whether Tick Rate Triggered examining is enabled");
		triggerConfig.setHeader("minTPS", "The ticks-per-second threshold below which measurement is started");
		triggerConfig.setHeader("minLagDuration", "The amount of ticks (not seconds!) the TPS must be below minTPS before measurement is started");
		triggerConfig.setHeader("measurementDuration", "The tick duration of a single Examine report that is created by the trigger");
		triggerConfig.setHeader("measurementInterval", "The minimum interval in seconds between two triggered examine reports");
		triggerConfig.addHeader("measurementInterval", "This is used to avoid generating too many reports during persistent tick rate issues");

		// Load trigger options
		boolean enabled = triggerConfig.get("enabled", false);
		double minTPS = triggerConfig.get("minTPS", 10.0);
		int minLagDuration = triggerConfig.get("minLagDuration", 10);
		int measurementDuration = triggerConfig.get("measurementDuration", 500);
		int measurementInterval = triggerConfig.get("measurementInterval", 15 * 60);
		if (enabled) {
			tickRateTrigger = new TickRateTrigger(logger, NoLagg.plugin, minTPS, minLagDuration, measurementDuration, measurementInterval);
			tickRateTrigger.start(1, 1);
		} else {
			Task.stop(tickRateTrigger);
			tickRateTrigger = null;
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, String[] args) throws NoPermissionException {
		if (args.length != 0) {
			if (args[0].equalsIgnoreCase("examine")) {
				int duration = MathUtil.clamp(500, maxExamineTime);
				if (args.length >= 2) {
					if (LogicUtil.contains(args[1].toLowerCase(Locale.ENGLISH), "stop", "cancel", "abort")) {
						// Abort any examine operation going on right now
						if (!logger.isRunning()) {
							sender.sendMessage(ChatColor.YELLOW + "The server is not being examined right now.");
						} else {
							logger.abort();
							sender.sendMessage(ChatColor.YELLOW + "Examining aborted: examined data of " + logger.getDuration() + " ticks is saved");
						}
						return true;
					}
					duration = ParseUtil.parseInt(args[1], duration);
				}
				if (sender instanceof Player) {
					Permission.EXAMINE_RUN.handle(sender);
					logger.recipients.add(sender.getName());
				} else {
					logger.recipients.add(null);
				}
				if (logger.isRunning()) {
					sender.sendMessage(ChatColor.RED + "The server is already being examined: " + logger.getDurPer() + "% completed");
					sender.sendMessage(ChatColor.GREEN + "You will be notified when the report has been generated");
				} else if (duration > maxExamineTime) {
					sender.sendMessage(ChatColor.RED + "Examine duration of " + duration + " exceeded the maximum possible: " + maxExamineTime + " ticks");
				} else {
					logger.start(duration);
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
