package com.bergerkiller.bukkit.nolagg.examine;

import net.minecraft.server.WorldServer;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.permissions.NoPermissionException;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;
import com.bergerkiller.bukkit.nolagg.Permission;

public class NoLaggExamine extends NoLaggComponent {

	public static NoLaggExamine plugin;
	
	@Override
	public void onEnable(ConfigurationNode config) {
		plugin = this;
		for (WorldServer world : WorldUtil.getWorlds()) {
			TimedChunkProviderServer.convert(world);
		}
		this.register(NLEListener.class);
		SchedulerWatcher.init();
	}

	@Override
	public void onDisable(ConfigurationNode config) {
		SchedulerWatcher.deinit();
		PluginLogger.stopTask();
		for (WorldServer world : WorldUtil.getWorlds()) {
			TimedChunkProviderServer.restore(world);
		}
	}

	@Override
	public void onReload(ConfigurationNode config) {
	}
	
	@Override
	public boolean onCommand(CommandSender sender, String[] args) throws NoPermissionException {
		if (args.length != 0) {
			if (args[0].equalsIgnoreCase("examine")) {
				int duration = 500;
				try {
					duration = Integer.parseInt(args[1]);
				} catch (Exception ex) {}
				if (sender instanceof Player) {
					Permission.EXAMINE_RUN.handle(sender);
					PluginLogger.recipients.add(sender.getName());
				} else {
					PluginLogger.recipients.add(null);
				}
				if (PluginLogger.isRunning()) {
					CommonUtil.sendMessage(sender, ChatColor.RED + "The server is already being examined: " + PluginLogger.getDurPer() + "% completed");
					CommonUtil.sendMessage(sender, ChatColor.GREEN + "You will be notified when the report has been generated");
				} else {
					PluginLogger.duration = duration;
					PluginLogger.start();
					CommonUtil.sendMessage(sender, ChatColor.GREEN + "The server will be examined for " + duration + " ticks (" + (duration / 20) + " seconds)");
					CommonUtil.sendMessage(sender, ChatColor.GREEN + "You will be notified when the report has been generated");
				}
				return true;
			}
		}
		return false;
	}
}
