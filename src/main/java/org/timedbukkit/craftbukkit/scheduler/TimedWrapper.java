package org.timedbukkit.craftbukkit.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;

import com.bergerkiller.bukkit.nolagg.examine.PluginLogger;
import com.bergerkiller.bukkit.nolagg.examine.TaskMeasurement;

/*
 * Please ignore the package leading to the org.bukkit.craftbukkit namespace
 * The author (me) got tired of all the reports with this showing up in stack traces
 * To keep things fair, all rights for this Class go to the Bukkit team
 */
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
				try {
					this.runnable.run();
				} finally {
					try {
						this.dest.setTime(time);
					} catch (ArrayIndexOutOfBoundsException ex) {
					}
				}
			} else {
				this.runnable.run();
			}
		} catch (Throwable t) {
			List<StackTraceElement> stack = new ArrayList<StackTraceElement>();
			for (StackTraceElement elem : t.getStackTrace()) {
				if (elem.getClassName().equals("org.timedbukkit.craftbukkit.scheduler.TimedWrapper")) {
					break;
				}
				stack.add(elem);
			}
			t.setStackTrace(stack.toArray(new StackTraceElement[0]));
			Bukkit.getLogger().log(Level.WARNING, "Task of '" + this.dest.plugin + "' generated an exception", t);
		}
	}
}
