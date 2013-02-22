package com.bergerkiller.bukkit.nolagg.lighting;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.permissions.NoPermissionException;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;
import com.bergerkiller.bukkit.nolagg.Permission;

public class NoLaggLighting extends NoLaggComponent {
	public static NoLaggLighting plugin;

	@Override
	public void onReload(ConfigurationNode config) {
	}

	@Override
	public void onEnable(ConfigurationNode config) {
		plugin = this;
		this.onReload(config);
	}

	@Override
	public void onDisable(ConfigurationNode config) {
		LightingService.abort();
	}

	@Override
	public boolean onCommand(CommandSender sender, String[] args) throws NoPermissionException {
		if (args.length == 0)
			return false;
		if (args[0].equalsIgnoreCase("fixworld")) {
			Permission.LIGHTING_FIX.handle(sender);
			final World world;
			if (args.length >= 2) {
				world = Bukkit.getWorld(args[1]);
				if (world == null) {
					sender.sendMessage(ChatColor.RED + "World '" + args[1] + "' was not found!");
					return true;
				}
			} else if (sender instanceof Player) {
				world = ((Player) sender).getWorld();
			} else {
				sender.sendMessage("As a console you have to specify the world to fix!");
				return true;
			}
			// Fix all the chunks in this world
			sender.sendMessage(ChatColor.YELLOW + "The world is now being fixed, this may take very long!");
			sender.sendMessage(ChatColor.YELLOW + "To view the fixing status, use /lag stat");
			LightingService.addRecipient(sender);
			// Get an iterator for all the chunks to fix
			LightingService.scheduleWorld(world);
			return true;
		}
		if (args[0].equalsIgnoreCase("resend")) {
			Permission.LIGHTING_RESEND.handle(sender);
			if (sender instanceof Player) {
				Player p = (Player) sender;
				int radius = Bukkit.getServer().getViewDistance();
				if (args.length == 2) {
					try {
						radius = Integer.parseInt(args[1]);
					} catch (Exception ex) {
					}
				}
				int cx = p.getLocation().getBlockX() >> 4;
				int cz = p.getLocation().getBlockZ() >> 4;
				for (int a = -radius; a <= radius; a++) {
					for (int b = -radius; b <= radius; b++) {
						for (Player player : WorldUtil.getPlayers(p.getWorld())) {
							if (EntityUtil.isNearChunk(player, cx + a, cz + b, CommonUtil.VIEW)) {
								EntityUtil.queueChunkSend(player, cx + a, cz + b);
							}
						}
					}
				}
				LightingService.addRecipient(p);
				p.sendMessage(ChatColor.GREEN + "A " + (radius * 2 + 1) + " X " + (radius * 2 + 1) + " chunk area around you is being resent...");
				return true;
			}
		}
		if (args[0].equalsIgnoreCase("fix")) {
			if (sender instanceof Player) {
				Permission.LIGHTING_FIX.handle(sender);
				Player p = (Player) sender;
				int radius = Bukkit.getServer().getViewDistance();
				if (args.length == 2) {
					radius = ParseUtil.parseInt(args[1], radius);
				}
				Location l = p.getLocation();
				LightingService.scheduleArea(p.getWorld(), l.getBlockX() >> 4, l.getBlockZ() >> 4, radius);
				p.sendMessage(ChatColor.GREEN + "A " + (radius * 2 + 1) + " X " + (radius * 2 + 1) + " chunk area around you is currently being fixed from lighting issues...");
				return true;
			}
		}
		return false;
	}
}
