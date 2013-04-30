package com.bergerkiller.bukkit.nolagg;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.Common;

public class NoLaggUtil {

	public static StackTraceElement findExternal(StackTraceElement[] stackTrace) {
		return findExternal(Arrays.asList(stackTrace));
	}

	/**
	 * Gets the first stack trace element that is outside Bukkit/nms scope
	 * 
	 * @param stackTrace to look at
	 * @return first element
	 */
	public static StackTraceElement findExternal(List<StackTraceElement> stackTrace) {
		// bottom to top
		for (int j = stackTrace.size() - 1; j >= 0; j--) {
			String className = stackTrace.get(j).getClassName().toLowerCase();
			if (className.startsWith("org.bukkit")) {
				continue;
			}
			if (className.startsWith(Common.NMS_ROOT)) {
				continue;
			}
			return stackTrace.get(j);
		}
		return new StackTraceElement(Common.NMS_ROOT + ".MinecraftServer", "main", "MinecraftServer.java", 0);
	}

	/**
	 * Checks whether a given player is an NPC, and should not be treated as a player that needs network updates
	 * 
	 * @param player to check
	 * @return True if the player is an NPC, False if not
	 */
	public static boolean isNPCPlayer(Player player) {
		return player.hasMetadata("NPC");
	}
}
