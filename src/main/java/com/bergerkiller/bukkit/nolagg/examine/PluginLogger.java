package com.bergerkiller.bukkit.nolagg.examine;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.TimedRegisteredListener;
import org.timedbukkit.craftbukkit.scheduler.TimedWrapper;

import com.bergerkiller.bukkit.common.config.CompressedDataWriter;
import com.bergerkiller.bukkit.common.internal.CommonPlugin;
import com.bergerkiller.bukkit.common.internal.NextTickListener;
import com.bergerkiller.bukkit.common.reflection.SafeField;
import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class PluginLogger {
	private static SafeField<EventExecutor> exefield = new SafeField<EventExecutor>(RegisteredListener.class, "executor");
	public static Set<String> recipients = new HashSet<String>();
	private static Task measuretask;
	private static TimedRegisteredListener[] listeners;
	private static float[][] eventtimes;
	public static int duration = 500;
	public static int position;
	public static Map<String, TaskMeasurement> tasks = new HashMap<String, TaskMeasurement>();

	public static TimedWrapper getWrapper(Runnable task, Plugin plugin) {
		return getTask(task, plugin).getWrapper(task);
	}

	public static TaskMeasurement getNextTickTask(Runnable task) {
		final String name = "[NextTick] " + task.getClass().getName();
		TaskMeasurement tm = tasks.get(name);
		if (tm == null) {
			Plugin plugin = CommonUtil.getPluginByClass(task.getClass());
			if (plugin == null) {
				plugin = CommonPlugin.getInstance();
			}
			tm = new TaskMeasurement(name, plugin);
			tasks.put(name, tm);
		}
		return tm;
	}

	public static TaskMeasurement getTask(Runnable task, Plugin plugin) {
		return getTask(task.getClass().getName(), plugin);
	}

	public static TaskMeasurement getTask(String name, Plugin plugin) {
		TaskMeasurement tm = tasks.get(name);
		if (tm == null) {
			tm = new TaskMeasurement(name, plugin);
			tasks.put(name, tm);
		}
		return tm;
	}

	public static TaskMeasurement getServerOperation(String sectionname, String operationname, String desc) {
		sectionname = '#' + sectionname;
		TaskMeasurement tm = tasks.get(operationname);
		if (tm == null) {
			tm = new TaskMeasurement(operationname, sectionname);
			tasks.put(operationname, tm);
		}
		if (desc != null) {
			tm.locations.add(desc);
		}
		return tm;
	}

	public static boolean isIgnoredTask(Runnable runnable) {
		if (runnable instanceof TimedWrapper) {
			return true;
		}
		final String name = runnable.getClass().getName();
		if (name.startsWith(Common.COMMON_ROOT + ".internal.CommonPlugin$NextTickHandler")) {
			return true;
		}
		return false;
	}

	public static double getDurPer() {
		return MathUtil.round((double) position / (double) duration * 100.0, 2);
	}

	public static boolean isRunning() {
		return measuretask != null && PluginLogger.position < PluginLogger.duration;
	}

	public static void stopTask() {
		Task.stop(measuretask);
		measuretask = null;
		position = Integer.MAX_VALUE;
		CommonPlugin.getInstance().removeNextTickListener(nextTickListener);
	}

	public static void start() {
		// Register next-tick listener
		CommonPlugin.getInstance().addNextTickListener(nextTickListener);

		// Initialize event listeners
		List<TimedRegisteredListener> rval = new ArrayList<TimedRegisteredListener>();
		RegisteredListener[] a;
		for (HandlerList handler : HandlerList.getHandlerLists()) {
			a = handler.getRegisteredListeners();
			for (int i = 0; i < a.length; i++) {
				if (a[i] instanceof TimedRegisteredListener) {
					rval.add((TimedRegisteredListener) a[i]);
				} else {
					// convert
					EventExecutor exec = exefield.get(a[i]);
					if (exec == null)
						continue;
					Listener list = a[i].getListener();
					Plugin plug = a[i].getPlugin();
					EventPriority prio = a[i].getPriority();
					boolean ignoreCancelled = a[i].isIgnoringCancelled();
					TimedRegisteredListener listener = new TimedRegisteredListener(list, exec, prio, plug, ignoreCancelled);
					a[i] = listener;
					rval.add(listener);
				}
			}
		}
		listeners = rval.toArray(new TimedRegisteredListener[0]);
		eventtimes = new float[listeners.length][duration];
		for (int i = 0; i < eventtimes.length; i++) {
			listeners[i].reset();
			Arrays.fill(eventtimes[i], 0f);
		}
		position = 0;

		for (TaskMeasurement tm : tasks.values()) {
			tm.reset();
		}

		// start measurement task
		Task.stop(measuretask);
		measuretask = new Task(NoLagg.plugin) {
			public void run() {
				for (int i = 0; i < eventtimes.length; i++) {
					eventtimes[i][position] = (float) (listeners[i].getTotalTime() / 1E6);
					listeners[i].reset();
				}
				if (position++ >= duration - 1) {
					stopTask();
					onFinish();
				}
			}
		}.start(1, 1);
	}

	public static String now(String dateformat) {
		return new SimpleDateFormat(dateformat).format(Calendar.getInstance().getTime()).trim();
	}

	public static void onFinish() {
		measuretask = null;
		StringBuilder filename = new StringBuilder();
		filename.append("plugins").append(File.separator);
		filename.append(now("yyyy_MM_dd-H_mm_ss"));
		filename.append(".exam");
		File file = new File(filename.toString());
		file.getAbsoluteFile().getParentFile().mkdirs();

		// Start a new compressed (deflater, ZIP) data writer
		new CompressedDataWriter(file) {
			@Override
			public void write(DataOutputStream stream) throws IOException {
				stream.writeInt(listeners.length);
				stream.writeInt(duration);
				for (int i = 0; i < listeners.length; i++) {
					Event event = listeners[i].getEvent();
					if (event == null) {
						stream.writeBoolean(false);
					} else {
						stream.writeBoolean(true);
						stream.writeUTF(listeners[i].getPlugin().getDescription().getName());
						stream.writeUTF(event.getClass().getSimpleName());
						stream.writeInt(listeners[i].getPriority().getSlot());
						stream.writeUTF(listeners[i].getListener().getClass().toString());
						for (int d = 0; d < duration; d++) {
							stream.writeLong((long) (eventtimes[i][d] * 1E6));
						}
					}
				}
				stream.writeInt(tasks.size());
				for (Map.Entry<String, TaskMeasurement> task : tasks.entrySet()) {
					stream.writeUTF(task.getKey());
					TaskMeasurement tm = task.getValue();
					stream.writeUTF(tm.plugin);
					stream.writeInt(tm.locations.size());
					for (String loc : tm.locations) {
						stream.writeUTF(loc);
					}
					for (long value : tm.times) {
						stream.writeLong(value);
					}
				}
			}
		}.write();
		for (String rec : recipients) {
			if (rec == null) {
				System.out.println("The examination log has been generated in " + file.toString());
			} else {
				Player p = Bukkit.getPlayer(rec);
				if (p != null) {
					p.sendMessage(ChatColor.GREEN + "The examination log has been generated in " + file.toString());
				}
			}
		}
		recipients.clear();
	}

	private static final BKCNextTickListener nextTickListener = new BKCNextTickListener();

	private static class BKCNextTickListener implements NextTickListener {
		@Override
		public void onNextTicked(Runnable runnable, long executionTime) {
			getNextTickTask(runnable).addDelta(executionTime);
		}
	}
}
