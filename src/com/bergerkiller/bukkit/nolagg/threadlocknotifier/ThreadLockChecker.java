package com.bergerkiller.bukkit.nolagg.threadlocknotifier;

import java.util.ArrayList;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.nolagg.NoLaggUtil;

public class ThreadLockChecker extends AsyncTask {
	private StackTraceElement[] previous = null;
	private int maxidx = Integer.MAX_VALUE;
	private ArrayList<StackTraceElement> elems = new ArrayList<StackTraceElement>();
	private ArrayList<StackTraceElement> tmpelems = new ArrayList<StackTraceElement>();

	public static boolean pulse = false;
	public static boolean ignored = true;

	@Override
	public void run() {
		if (ignored) {
			sleep(10000);
			return;
		}
		pulse = false;
		try {
			Thread.sleep(10000);
		} catch (InterruptedException ex) {}
		if (pulse) {
			previous = null;
			maxidx = Integer.MAX_VALUE;
		} else if (previous != null) {
			boolean found = false;
			int foundCounter = 0;
			while (true) {
				tmpelems.clear();
				StackTraceElement[] newstack = CommonUtil.MAIN_THREAD.getStackTrace();
				maxidx = Math.min(maxidx, Math.min(newstack.length, previous.length));

				int i = 1;
				for (; i < maxidx; i++) {
					if (newstack[newstack.length - i - 1].equals(previous[previous.length - i - 1])) {
						tmpelems.add(0, newstack[newstack.length - i - 1]);
					} else {
						maxidx = i;
						break;
					}
				}
				if (tmpelems.size() != elems.size()) {
					elems.clear();
					elems.addAll(tmpelems);
					found = true;
				}
				if (found && foundCounter++ > 40) {
					break;
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException ex) {}
			}
			if (!elems.isEmpty()) {
				StackTraceElement head = elems.get(0);
				if (head.getMethodName().equals("sleep") && head.getClassName().equals(Thread.class.getName())) {
					head = elems.get(1);
				}
				Plugin[] plugins = NoLaggUtil.findPlugins(previous);
				Bukkit.getLogger().log(Level.WARNING, "[Server] The main thread is still stuck, current loop line is:");
				Bukkit.getLogger().log(Level.WARNING, "[Server]    at " + head.toString());
				if (plugins.length > 0) {
					Bukkit.getLogger().log(Level.WARNING, "[Server] This appears to be plugin '" + plugins[0].getName() + "'!");
				}
			}
		} else {
			previous = CommonUtil.MAIN_THREAD.getStackTrace();
			maxidx = Integer.MAX_VALUE;
			Plugin[] plugins = NoLaggUtil.findPlugins(previous);
			Bukkit.getLogger().log(Level.WARNING, "[Server] The main thread failed to respond after 10 seconds");
			if (plugins.length > 0) {
				if (plugins.length == 1) {
					Bukkit.getLogger().log(Level.WARNING, "[Server] Probable Plugin cause: '" + plugins[0].getName() + "'");
				} else {
					String[] names = new String[plugins.length];
					for (int i = 0; i < names.length; i++) {
						names[i] = plugins[i].getName();
					}
					Bukkit.getLogger().log(Level.WARNING, "[Server] Probable Plugin causes: '" + StringUtil.combineNames(names) + "'");
				}

			}
			Bukkit.getLogger().log(Level.WARNING, "[Server] What follows is the stack trace of the main thread");
			NoLaggUtil.logFilterMainThread(previous, Level.WARNING, "[Server]");
		}
	}
}
