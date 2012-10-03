package com.bergerkiller.bukkit.nolagg;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class NoLaggUtil {

	public static void logFilterMainThread(StackTraceElement[] stackTrace, Level level, String header) {
		logFilterMainThread(Arrays.asList(stackTrace), level, header);
	}

	public static void logFilterMainThread(List<StackTraceElement> stackTrace, Level level, String header) {
		for (StackTraceElement element : stackTrace) {
			String className = element.getClassName().toLowerCase();
			String m = element.getMethodName();
			if (className.equals("net.minecraft.server.minecraftserver") || className.equals("net.minecraft.server.dedicatedserver")) {
				if (m.equals("p") || m.equals("q") || m.equals("ai")) {
					break;
				}
			}
			if (className.equals("org.bukkit.craftbukkit.scheduler.craftscheduler")) {
				if (m.equals("mainThreadHeartbeat")) {
					break;
				}
			}
			Bukkit.getLogger().log(level, header + "    at " + element.toString());
		}
	}

	public static StackTraceElement findExternal(StackTraceElement[] stackTrace) {
		return findExternal(Arrays.asList(stackTrace));
	}

	/**
	 * Gets the first stack trace element that is outside Bukkit/nms scope
	 * 
	 * @param stackTrace
	 *            to look at
	 * @return first element
	 */
	public static StackTraceElement findExternal(List<StackTraceElement> stackTrace) {
		// bottom to top
		for (int j = stackTrace.size() - 1; j >= 0; j--) {
			String className = stackTrace.get(j).getClassName().toLowerCase();
			if (className.startsWith("org.bukkit")) {
				continue;
			}
			if (className.startsWith("net.minecraft.server")) {
				continue;
			}
			return stackTrace.get(j);
		}
		return new StackTraceElement("net.minecraft.server.MinecraftServer", "main", "MinecraftServer.java", 0);
	}

	public static Plugin[] findPlugins(StackTraceElement[] stackTrace) {
		return findPlugins(Arrays.asList(stackTrace));
	}

	public static Plugin[] findPlugins(List<StackTraceElement> stackTrace) {
		Plugin[] plugins = Bukkit.getServer().getPluginManager().getPlugins();
		String[] packages = new String[plugins.length];
		// Generate package paths
		int i;
		for (i = 0; i < plugins.length; i++) {
			packages[i] = plugins[i].getDescription().getMain().toLowerCase();
			int packidx = packages[i].lastIndexOf('.');
			if (packidx == -1) {
				packages[i] = "";
			} else {
				packages[i] = packages[i].substring(0, packidx);
			}
		}
		// Get all the plugins that match this stack trace
		LinkedHashSet<Plugin> found = new LinkedHashSet<Plugin>(3);
		for (StackTraceElement elem : stackTrace) {
			String className = elem.getClassName().toLowerCase();
			for (i = 0; i < plugins.length; i++) {
				if (packages[i].isEmpty()) {
					// No package name - verify
					if (!className.contains(".")) {
						found.add(plugins[i]);
					}
				} else {
					// Package name is there - verify
					if (className.startsWith(packages[i])) {
						found.add(plugins[i]);
					}
				}
			}
		}
		return found.toArray(new Plugin[0]);
	}
}
