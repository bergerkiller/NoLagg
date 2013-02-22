package com.bergerkiller.bukkit.nolagg;

import org.bukkit.permissions.PermissionDefault;

import com.bergerkiller.bukkit.common.permissions.PermissionEnum;

public class Permission extends PermissionEnum {
	public static final Permission RELOAD = new Permission("nolagg.reload", PermissionDefault.OP, "Allows a player to reload NoLagg");
	public static final Permission COMMON_CLEAR = new Permission("nolagg.common.clear", PermissionDefault.OP, "Allows a player to clear all entities on the server");
	public static final Permission COMMON_GC = new Permission("nolagg.common.gc", PermissionDefault.OP, "Allows a player to garbage collect the server memory");
	public static final Permission CHUNKS_SENDING = new Permission("nolagg.chunks.sending", PermissionDefault.OP, "The player can view sending information about himself");
	public static final Permission EXAMINE_RUN = new Permission("nolagg.examine.run", PermissionDefault.OP, "Allows a player to examine the server");
	public static final Permission LIGHTING_FIX = new Permission("nolagg.lighting.fix", PermissionDefault.OP, "Allows a player to fix lighting issues in chunks around him");
	public static final Permission LIGHTING_RESEND = new Permission("nolagg.lighting.resend", PermissionDefault.OP, "Allows a player to resend all the chunks around to all players");
	public static final Permission MONITOR_USE = new Permission("nolagg.monitor.use", PermissionDefault.OP, "Allows a player to monitor server stats");
	public static final Permission MONITOR_LOG = new Permission("nolagg.monitor.log", PermissionDefault.OP, "Allows a player to toggle server stats being written to a log file");
	public static final Permission MONITOR_CLEARLOG = new Permission("nolagg.monitor.clearlog", PermissionDefault.OP, "Allows a player to clear the NoLagg log file");
	public static final Permission MONITOR_SHOWTICKRATE = new Permission("nolagg.monitor.showtickrate", PermissionDefault.TRUE, "Allows a player to see the tick rate of the server");
	public static final Permission MONITOR_SHOWMEMORY = new Permission("nolagg.monitor.showmemory", PermissionDefault.OP, "Allows a player to see the current memory usage of the server");
	public static final Permission MONITOR_NOTIFYLAGGING = new Permission("nolagg.monitor.notifylagging", PermissionDefault.OP, "Lets the player receive a message if the tick rate dropped signficiantly");
	public static final Permission TNT_INFO = new Permission("nolagg.tnt.info", PermissionDefault.OP, "The player can show TNT information");
	public static final Permission TNT_CLEAR = new Permission("nolagg.tnt.clear", PermissionDefault.OP, "The player can stop TNT detonation");

	private Permission(final String path, final PermissionDefault def, final String desc) {
		super(path, def, desc);
	}
}