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
import java.util.logging.Level;

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
import org.timedbukkit.craftbukkit.scheduler.TimedListenerExecutor;
import org.timedbukkit.craftbukkit.scheduler.TimedWrapper;

import com.bergerkiller.bukkit.common.config.CompressedDataWriter;
import com.bergerkiller.bukkit.common.internal.CommonPlugin;
import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.TimeUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;
import com.bergerkiller.bukkit.nolagg.NoLaggUtil;

public class PluginLogger {
	public Set<String> recipients = new HashSet<String>();
	private Task measuretask;
	private ListenerMeasurement[] events;
	private int duration;
	public int position;
	public Map<String, TaskMeasurement> tasks = new HashMap<String, TaskMeasurement>();

	public TimedWrapper getWrapper(Runnable task, Plugin plugin) {
		return getTask(task, plugin).getWrapper(task);
	}

	public TaskMeasurement getNextTickTask(Runnable task) {
		final String name = "[NextTick] " + task.getClass().getName();
		TaskMeasurement tm = tasks.get(name);
		if (tm == null) {
			Plugin plugin = CommonUtil.getPluginByClass(task.getClass());
			if (plugin == null) {
				plugin = CommonPlugin.getInstance();
			}
			tm = new TaskMeasurement(this, name, plugin);
			tasks.put(name, tm);
		}
		return tm;
	}

	public TaskMeasurement getTask(Runnable task, Plugin plugin) {
		return getTask(task.getClass().getName(), plugin);
	}

	public TaskMeasurement getTask(String name, Plugin plugin) {
		TaskMeasurement tm = tasks.get(name);
		if (tm == null) {
			tm = new TaskMeasurement(this, name, plugin);
			tasks.put(name, tm);
		}
		return tm;
	}

	public TaskMeasurement getServerOperation(String sectionname, String operationname, String desc) {
		sectionname = '#' + sectionname;
		TaskMeasurement tm = tasks.get(operationname);
		if (tm == null) {
			tm = new TaskMeasurement(this, operationname, sectionname);
			tasks.put(operationname, tm);
		}
		if (desc != null) {
			tm.locations.add(desc);
		}
		return tm;
	}

	public boolean isIgnoredTask(Runnable runnable) {
		if (runnable instanceof TimedWrapper) {
			return true;
		}
		final String name = runnable.getClass().getName();
		if (name.startsWith(Common.COMMON_ROOT + ".internal.CommonPlugin$NextTickHandler")) {
			return true;
		}
		return false;
	}

	public double getDurPer() {
		return MathUtil.round((double) position / (double) duration * 100.0, 2);
	}

	public boolean isRunning() {
		return measuretask != null && position < duration;
	}

	public int getDuration() {
		return duration;
	}

	public void stopTask() {
		Task.stop(measuretask);
		measuretask = null;
		position = Integer.MAX_VALUE;

		// Unregister timings listener
		CommonPlugin.getInstance().removeTimingsListener(NLETimingsListener.INSTANCE);

		// Unhook timed listeners from the server
		for (ListenerMeasurement meas : events) {
			Object exec = NoLaggUtil.exefield.get(meas.listener);
			if (exec instanceof TimedListenerExecutor) {
				NoLaggUtil.exefield.set(meas.listener, ((TimedListenerExecutor) exec).getProxyBase());
			}
		}
	}

	public void start(int duration) {
		// Set duration
		this.duration = duration;

		// Register timings listener
		CommonPlugin.getInstance().addTimingsListener(NLETimingsListener.INSTANCE);

		// Initialize event listeners
		List<ListenerMeasurement> rval = new ArrayList<ListenerMeasurement>();
		for (HandlerList handler : HandlerList.getHandlerLists()) {
			RegisteredListener[] listeners = handler.getRegisteredListeners();
			for (int i = 0; i < listeners.length; i++) {
				// Convert to a timed registered listener if needed
				RegisteredListener listener = listeners[i];
				EventExecutor exec = NoLaggUtil.exefield.get(listener);
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
				meas.reset();

				// Hook up an event cancel monitor
				if (exec instanceof TimedListenerExecutor) {
					((TimedListenerExecutor) exec).meas = meas;
				} else {
					NoLaggUtil.exefield.set(listener, new TimedListenerExecutor(this, exec, meas));
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
				if (position++ >= PluginLogger.this.duration - 1) {
					stopTask();
					onFinish();
				}
			}
		}.start(1, 1);
	}

	public void abort() {
		if (!isRunning()) {
			return;
		}
		duration = position + 1;
		Task.stop(measuretask);
		onFinish();
	}

	public void onFinish() {
		measuretask = null;
		if (duration <= 0) {
			return;
		}

		// Prepare output folder and file
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
						Class<?> eventClass = meas.getEventClass();
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
					if (tm.plugin.startsWith("#")) {
						// Server operation
						stream.writeInt(tm.locations.size());
					} else {
						// Task
						stream.writeInt(tm.locations.size() + 1);
						stream.writeUTF("Execution count: " + tm.executionCount);
					}
					for (String loc : tm.locations) {
						stream.writeUTF(loc);
					}
					for (int d = 0; d < duration; d++) {
						stream.writeLong(tm.times[d]);
					}
				}
			}
		}.write();

		// Log to console
		NoLaggExamine.plugin.log(Level.INFO, "The examination log has been generated in " + file.toString());

		// Log to players
		for (String rec : recipients) {
			if (rec != null) {
				Player p = Bukkit.getPlayer(rec);
				if (p != null) {
					p.sendMessage(ChatColor.GREEN + "The examination log has been generated in " + file.toString());
				}
			}
		}
		recipients.clear();
	}
}
