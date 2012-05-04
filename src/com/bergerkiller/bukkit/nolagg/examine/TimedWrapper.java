package com.bergerkiller.bukkit.nolagg.examine;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;

public class TimedWrapper implements Runnable {
	
	public final Runnable runnable;
	private final TaskMeasurement dest;
	public TimedWrapper(final Runnable runnable, final TaskMeasurement dest) {
		this.runnable = runnable;
		this.dest = dest;
	}
	
	public void run() {
		try {
			if (PluginLogger.isRunning()) {
				long time = System.nanoTime();
				this.runnable.run();
				try {
					this.dest.setTime(time);
				} catch (ArrayIndexOutOfBoundsException ex) {}
			} else {
				this.runnable.run();
			}
		} catch (Throwable t) {
			List<StackTraceElement> stack = new ArrayList<StackTraceElement>();
			boolean add = true;
			for (StackTraceElement elem : t.getStackTrace()) {
				if (elem.getClassName().equals("com.bergerkiller.bukkit.nolagg.examine.TimedWrapper")) {
					add = false;
				} else if (add) {
					stack.add(elem);
				}
			}
			t.setStackTrace(stack.toArray(new StackTraceElement[0]));
			Bukkit.getLogger().log(Level.WARNING, "Task of '" + this.dest.plugin + "' generated an exception", t);
		}
	}
	
}
