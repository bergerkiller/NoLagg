package org.timedbukkit.craftbukkit.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;

import com.bergerkiller.bukkit.common.proxies.ProxyBase;
import com.bergerkiller.bukkit.nolagg.examine.TaskMeasurement;

/*
 * Please ignore the package leading to the org.bukkit.craftbukkit namespace
 * The author (me) got tired of all the reports with this showing up in stack traces
 * To keep things fair, all rights for this Class go to the Bukkit team
 */
public class TimedWrapper extends ProxyBase<Runnable> implements Runnable {
	private final TaskMeasurement dest;

	public TimedWrapper(final Runnable runnable, final TaskMeasurement dest) {
		super(runnable);
		this.dest = dest;
	}

	@Override
	public void run() {
		try {
			if (dest.logger.isRunning()) {
				long time = System.nanoTime();
				try {
					this.getProxyBase().run();
				} finally {
					try {
						this.dest.setTime(time);
						this.dest.executionCount++;
					} catch (ArrayIndexOutOfBoundsException ex) {
					}
				}
			} else {
				this.getProxyBase().run();
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
