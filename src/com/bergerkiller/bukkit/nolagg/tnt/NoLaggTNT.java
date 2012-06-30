package com.bergerkiller.bukkit.nolagg.tnt;

import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.permissions.NoPermissionException;
import com.bergerkiller.bukkit.nolagg.NoLagg;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;
import com.bergerkiller.bukkit.nolagg.Permission;

public class NoLaggTNT extends NoLaggComponent {
	
	public static NoLaggTNT plugin;
	
	@Override
	public void onReload(ConfigurationNode config) {
		config.setHeader("detonationInterval", "The interval (in ticks) at which TNT is detonated by explosions");
		config.setHeader("detonationRate", "How many TNT is detonated by explosions per interval");
		config.setHeader("explosionRadiusFactor", "The explosion crater size factor");
		config.setHeader("explosionRate", "The amount of explosion packets to send to the clients per tick");
		config.setHeader("changeBlocks", "If TNT explosions can change non-TNT blocks");

		TNTHandler.interval = config.get("detonationInterval", 1);
		TNTHandler.rate = config.get("detonationRate", 10);
		CustomExplosion.factor = config.get("explosionRadiusFactor", 1.0);
		TNTHandler.explosionRate = config.get("explosionRate", 5);
		TNTHandler.changeBlocks = config.get("changeBlocks", true);
	}

	@Override
	public void onEnable(ConfigurationNode config) {
		plugin = this;
		new Task(NoLagg.plugin) {
			public void run() {
				NoLagg.plugin.register(NLTListener.class);
			}
		}.start(1);
		this.onReload(config);
		TNTHandler.init();
	}

	@Override
	public void onDisable(ConfigurationNode config) {
		TNTHandler.deinit();
	}

	@Override
	public boolean onCommand(CommandSender sender, String[] args) throws NoPermissionException {
		if (args.length == 0) return false;
		if (args[0].equalsIgnoreCase("tnt")) {
			if (args.length == 2 && args[1].equalsIgnoreCase("clear")) {
				Permission.TNT_CLEAR.handle(sender);
				TNTHandler.clear();
				sender.sendMessage("TNT detonation stopped, pending TNT cleared");
			} else {
				Permission.TNT_INFO.handle(sender);
				sender.sendMessage(TNTHandler.getBufferCount() + " TNT blocks are waiting for detonation");
			}
		}
		return false;
	}
}
