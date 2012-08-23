package com.bergerkiller.bukkit.nolagg.lighting;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.permissions.NoPermissionException;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;
import com.bergerkiller.bukkit.nolagg.Permission;

public class NoLaggLighting extends NoLaggComponent {
	
	public static boolean auto = true;
	public static NoLaggLighting plugin;
	
	@Override
	public void onReload(ConfigurationNode config) {
		config.setHeader("auto", "Whether or not lighting is automatically fixed when a new chunk is generated");
		auto = config.get("auto", auto);
	}
	
	@Override
	public void onEnable(ConfigurationNode config) {
		plugin = this;
		this.onReload(config);
	}

	@Override
	public void onDisable(ConfigurationNode config) {
		LightingFixThread.finish();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, String[] args) throws NoPermissionException {
		if (args.length == 0) return false;
		if (args[0].equalsIgnoreCase("fix")) {
			if (sender instanceof Player) {
				Permission.LIGHTING_FIX.handle(sender);
				Player p = (Player) sender;
				int radius = Bukkit.getServer().getViewDistance();
				if (args.length == 2) {
					try {
						radius = Integer.parseInt(args[1]);
					} catch (Exception ex) {}
				}
				int cx = p.getLocation().getBlockX() >> 4;
				int cz = p.getLocation().getBlockZ() >> 4;
				for (int a = -radius; a <= radius; a++) {
					for (int b = -radius; b <= radius; b++) {
						Chunk chunk = WorldUtil.getChunk(p.getWorld(), cx + a, cz + b);
						if (chunk != null) {
							LightingFixThread.fix(chunk);
						}
					} 
				}
				p.sendMessage(ChatColor.GREEN + "A " + (radius * 2 + 1) + " X " + (radius * 2 + 1) + " chunk area around you is currently being fixed from lighting issues...");
				return true;
			}
		}
		return false;
	}
}
