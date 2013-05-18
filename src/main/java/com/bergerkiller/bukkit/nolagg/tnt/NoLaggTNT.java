package com.bergerkiller.bukkit.nolagg.tnt;

import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.permissions.NoPermissionException;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;
import com.bergerkiller.bukkit.nolagg.Permission;

public class NoLaggTNT extends NoLaggComponent {
	public static NoLaggTNT plugin;
	private TNTHandler tntHandler;

	public TNTHandler getTNTHandler() {
		return tntHandler;
	}

	@Override
	public void onReload(ConfigurationNode config) {
		tntHandler.init(config);

		// Settings that have to do with the explosion
		config.setHeader("explosionRadiusFactor", "The explosion crater size factor");
		CustomExplosion.factor = config.get("explosionRadiusFactor", CustomExplosion.factor);
	}

	@Override
	public void onEnable(ConfigurationNode config) {
		tntHandler = new TNTHandler();
		plugin = this;
		CommonUtil.nextTick(new Runnable() {
			public void run() {
				NoLagg.plugin.register(NLTListener.class);
			}
		});
		// Load settings and start the TNT handler
		this.onReload(config);
	}

	@Override
	public void onDisable(ConfigurationNode config) {
		if (tntHandler != null) {
			tntHandler.deinit();
			tntHandler = null;
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, String[] args) throws NoPermissionException {
		if (args.length == 0)
			return false;
		if (args[0].equalsIgnoreCase("tnt")) {
			if (args.length == 2 && args[1].equalsIgnoreCase("clear")) {
				Permission.TNT_CLEAR.handle(sender);
				tntHandler.clear();
				sender.sendMessage("TNT detonation stopped, pending TNT cleared");
			} else {
				Permission.TNT_INFO.handle(sender);
				sender.sendMessage(tntHandler.getBufferCount() + " TNT blocks are waiting for detonation");
			}
		}
		return false;
	}
}
