package com.bergerkiller.bukkit.nolagg;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

import com.bergerkiller.bukkit.common.permissions.IPermissionDefault;
import com.bergerkiller.bukkit.common.permissions.NoPermissionException;

public enum Permission implements IPermissionDefault {
	
	COMMON_CLEAR("nolagg.common.clear", PermissionDefault.OP, "Allows a player to clear all entities on the server"),
	COMMON_GC("nolagg.common.gc", PermissionDefault.OP, "Allows a player to garbage collect the server memory"),
	CHUNKS_SENDING("nolagg.chunks.sending", PermissionDefault.OP, "The player can view sending information about himself"),
	EXAMINE_RUN("nolagg.examine.run", PermissionDefault.OP, "Allows a player to examine the server"),
	LIGHTING_FIX("nolagg.lighting.fix", PermissionDefault.OP, "Allows a player to fix lighting issues in chunks around him"),
	MONITOR_USE("nolagg.monitor.use", PermissionDefault.OP, "Allows a player to monitor server stats"),
	MONITOR_LOG("nolagg.monitor.log", PermissionDefault.OP, "Allows a player to toggle server stats being written to a log file"),
	MONITOR_CLEARLOG("nolagg.monitor.clearlog", PermissionDefault.OP, "Allows a player to clear the NoLagg log file"),
	MONITOR_SHOWTICKRATE("nolagg.monitor.showtickrate", PermissionDefault.TRUE, "Allows a player to see the tick rate of the server"),
	MONITOR_SHOWMEMORY("nolagg.monitor.showmemory", PermissionDefault.OP, "Allows a player to see the current memory usage of the server"),
	MONITOR_NOTIFYLAGGING("nolagg.monitor.notifylagging", PermissionDefault.OP, "Lets the player receive a message if the tick rate dropped signficiantly"),
	TNT_INFO("nolagg.tnt.info", PermissionDefault.OP, "The player can show TNT information"),
	TNT_CLEAR("nolagg.tnt.clear", PermissionDefault.OP, "The player can stop TNT detonation");
	
	private Permission(final String path, final PermissionDefault def, final String desc) {
		this.path = path;
		this.def = def;
		this.desc = desc;
	}
	
	public boolean has(Player player) {
		return player.hasPermission(this.path);
	}
	
	public void handle(CommandSender sender) throws NoPermissionException {
		if (sender instanceof Player) {
			if (!has((Player) sender)) throw new NoPermissionException();
		}
	}
	
	private final String path;
	private final PermissionDefault def;
	private final String desc;

	@Override
	public String getName() {
		return this.path;
	}

	@Override
	public PermissionDefault getDefault() {
		return this.def;
	}

	@Override
	public String getDescription() {
		return this.desc;
	}

}
