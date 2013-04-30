package com.bergerkiller.bukkit.nolagg.threadlocknotifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

import org.bukkit.plugin.Plugin;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.ModuleLogger;
import com.bergerkiller.bukkit.common.StackTraceFilter;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.StringUtil;

public class ThreadLockChecker extends AsyncTask {
	private StackTraceElement[] previous = null;
	private int maxidx = Integer.MAX_VALUE;
	private ArrayList<StackTraceElement> elems = new ArrayList<StackTraceElement>();
	private ArrayList<StackTraceElement> tmpelems = new ArrayList<StackTraceElement>();

	public static boolean pulse = false;
	public static boolean ignored = true;
	public static final ModuleLogger SERVER_LOGGER = new ModuleLogger("Server");

	@Override
	public void run() {
		if (ignored) {
			sleep(10000);
			return;
		}
		pulse = false;
		try {
			Thread.sleep(10000);
		} catch (InterruptedException ex) {
		}
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
				} catch (InterruptedException ex) {
				}
			}
			if (!elems.isEmpty()) {
				StackTraceElement head = elems.get(0);
				if (head.getMethodName().equals("sleep") && head.getClassName().equals(Thread.class.getName())) {
					head = elems.get(1);
				}
				Plugin[] plugins = CommonUtil.findPlugins(previous);
				SERVER_LOGGER.log(Level.WARNING, "The main thread is still stuck, current loop line is:");
				SERVER_LOGGER.log(Level.WARNING, "    at " + head.toString());
				if (plugins.length > 0) {
					SERVER_LOGGER.log(Level.WARNING, "This appears to be plugin '" + plugins[0].getName() + "'!");
				}
			}
		} else {
			previous = CommonUtil.MAIN_THREAD.getStackTrace();
			maxidx = Integer.MAX_VALUE;
			Plugin[] plugins = CommonUtil.findPlugins(previous);
			SERVER_LOGGER.log(Level.WARNING, "The main thread failed to respond after 10 seconds");
			if (plugins.length > 0) {
				if (plugins.length == 1) {
					SERVER_LOGGER.log(Level.WARNING, "Probable Plugin cause: '" + plugins[0].getName() + "'");
				} else {
					String[] names = new String[plugins.length];
					for (int i = 0; i < names.length; i++) {
						names[i] = plugins[i].getName();
					}
					SERVER_LOGGER.log(Level.WARNING, "Probable Plugin causes: '" + StringUtil.combineNames(names) + "'");
				}

			}
			SERVER_LOGGER.log(Level.WARNING, "What follows is the stack trace of the main thread");
			// Print filtered stack trace
			ArrayList<StackTraceElement> stackTrace = new ArrayList<StackTraceElement>(Arrays.asList(previous));
			StackTraceFilter.SERVER.filter(stackTrace);
			for (StackTraceElement element : stackTrace) {
				SERVER_LOGGER.log(Level.WARNING, "    at " + element.toString());
			}
		}
	}
}
