package com.bergerkiller.bukkit.nolagg.threadlocknotifier;

import java.util.ArrayList;
import java.util.logging.Level;

import org.bukkit.Bukkit;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.utils.CommonUtil;

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
			sleep(5000);
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
					break;
				} else {
					try {
						Thread.sleep(200);
					} catch (InterruptedException ex) {}
				}
			}
			elems.clear();
			elems.addAll(tmpelems);
			if (!elems.isEmpty()) {
				Bukkit.getLogger().log(Level.WARNING, "[NoLagg TLN] The main thread is still stuck, updated stack trace is:");
				for (StackTraceElement elem : elems) {
					Bukkit.getLogger().log(Level.INFO, "    at " + elem);
				}
			}
		} else {
			boolean isNoLaggBug = false;
			for (StackTraceElement elem : previous) {
				if (elem.getClassName().startsWith("com.bergerkiller.bukkit.nolagg")) {
					isNoLaggBug = true;
					break;
				}
			}
			previous = CommonUtil.MAIN_THREAD.getStackTrace();
			maxidx = Integer.MAX_VALUE;
			Bukkit.getLogger().log(Level.WARNING, "[NoLagg TLN] The main thread failed to respond after 10 seconds");
			if (isNoLaggBug) {
				Bukkit.getLogger().log(Level.WARNING, "[NoLagg TLN] This appears to be caused by NoLagg, report it!");
			} else {
				Bukkit.getLogger().log(Level.WARNING, "[NoLagg TLN] This is not caused by NoLagg, it is only being reported!");
				Bukkit.getLogger().log(Level.WARNING, "[NoLagg TLN] Please read the stack trace to find the problematic plugin.");
			}
			Bukkit.getLogger().log(Level.WARNING, "[NoLagg TLN] What follows is the stack trace of the main thread");
			Bukkit.getLogger().log(Level.WARNING, "[NoLagg TLN] This stack trace will be further refined as long as the thread is stuck");
			for (StackTraceElement elem : previous) {
				Bukkit.getLogger().log(Level.INFO, "    at " + elem);
			}
		}
	}
}
