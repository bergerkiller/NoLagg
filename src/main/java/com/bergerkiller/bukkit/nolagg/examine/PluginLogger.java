package com.bergerkiller.bukkit.nolagg.examine;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.TimedRegisteredListener;
import org.timedbukkit.craftbukkit.scheduler.CancellableEventExecutor;
import org.timedbukkit.craftbukkit.scheduler.TimedWrapper;

import com.bergerkiller.bukkit.common.config.CompressedDataWriter;
import com.bergerkiller.bukkit.common.internal.CommonPlugin;
import com.bergerkiller.bukkit.common.reflection.FieldAccessor;
import com.bergerkiller.bukkit.common.reflection.SafeField;
import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.TimeUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class PluginLogger {
	public static FieldAccessor<EventExecutor> exefield = new SafeField<EventExecutor>(RegisteredListener.class, "executor");
	public static Set<String> recipients = new HashSet<String>();
	private static Task measuretask;
	private static ListenerMeasurement[] events;
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
		// Unregister timings listener
		CommonPlugin.getInstance().removeTimingsListener(NLETimingsListener.INSTANCE);
	}

	public static void start() {
		// Register timings listener
		CommonPlugin.getInstance().addTimingsListener(NLETimingsListener.INSTANCE);

		// Initialize event listeners
		List<ListenerMeasurement> rval = new ArrayList<ListenerMeasurement>();
		for (HandlerList handler : HandlerList.getHandlerLists()) {
			RegisteredListener[] listeners = handler.getRegisteredListeners();
			for (int i = 0; i < listeners.length; i++) {
				// Convert to a timed registered listener if needed
				RegisteredListener listener = listeners[i];
				EventExecutor exec = exefield.get(listener);
				if (exec == null) {
					continue;
				}
				if (!(listener instanceof TimedRegisteredListener)) {
					Listener list = listener.getListener();
					Plugin plug = listener.getPlugin();
					EventPriority prio = listener.getPriority();
					boolean ignoreCancelled = listener.isIgnoringCancelled();
					listeners[i] = listener = new TimedRegisteredListener(list, exec, prio, plug, ignoreCancelled);
				}

				// Set up a new measurement and reset to clear initial data
				ListenerMeasurement meas = new ListenerMeasurement((TimedRegisteredListener) listener, duration);
				meas.listener.reset();

				// Hook up an event cancel monitor
				if (exec instanceof CancellableEventExecutor) {
					((CancellableEventExecutor) exec).meas = meas;
				} else {
					exefield.set(listener, new CancellableEventExecutor(exec, meas));
				}

				// Done
				rval.add(meas);
			}
		}
		events = rval.toArray(new ListenerMeasurement[0]);
		position = 0;

		for (TaskMeasurement tm : tasks.values()) {
			tm.reset();
		}

		// start measurement task
		Task.stop(measuretask);
		measuretask = new Task(NoLagg.plugin) {
			public void run() {
				for (int i = 0; i < events.length; i++) {
					events[i].update(position);
				}
				if (position++ >= duration - 1) {
					stopTask();
					onFinish();
				}
			}
		}.start(1, 1);
	}

	public static void onFinish() {
		measuretask = null;
		NoLaggExamine.exportFolder.mkdirs();
		final File file = new File(NoLaggExamine.exportFolder, TimeUtil.now("yyyy_MM_dd-H_mm_ss") + ".exam");

		// Start a new compressed (deflater, ZIP) data writer
		new CompressedDataWriter(file) {
			@Override
			public void write(DataOutputStream stream) throws IOException {
				stream.writeInt(events.length);
				stream.writeInt(duration);
				StringBuilder descBuilder = new StringBuilder(200);
				for (ListenerMeasurement meas : events) {
					final boolean wasCalled = meas.wasCalled();
					stream.writeBoolean(wasCalled);
					if (wasCalled) {
						Class<?> eventClass = meas.listener.getEvent().getClass();
						// Plugin that fired the event
						stream.writeUTF(meas.listener.getPlugin().getDescription().getName());
						// Name of the event fired
						stream.writeUTF(eventClass.getSimpleName());
						// Priority of the event
						stream.writeInt(meas.listener.getPriority().getSlot());
						// Description
						descBuilder.setLength(0);
						descBuilder.append(meas.listener.getListener().getClass().toString());
						descBuilder.append("\nExecution count: ").append(meas.executionCount);
						if (Cancellable.class.isAssignableFrom(eventClass)) {
							descBuilder.append("\nCancelled: ").append(meas.cancelCount);
						}
						stream.writeUTF(descBuilder.toString());
						// The duration values
						for (int d = 0; d < duration; d++) {
							stream.writeLong((long) (meas.times[d] * 1E6));
						}
					}
				}
				stream.writeInt(tasks.size());
				for (Map.Entry<String, TaskMeasurement> task : tasks.entrySet()) {
					stream.writeUTF(task.getKey());
					TaskMeasurement tm = task.getValue();
					stream.writeUTF(tm.plugin);
					// Descriptions
					stream.writeInt(tm.locations.size() + 1);
					stream.writeUTF("Execution count: " + tm.executionCount);
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
}
