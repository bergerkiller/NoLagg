package com.bergerkiller.bukkit.nolagg.examine;

import java.util.Arrays;

import org.bukkit.plugin.TimedRegisteredListener;

import com.bergerkiller.bukkit.nolagg.NoLaggUtil;

public class ListenerMeasurement {
	public final TimedRegisteredListener listener;
	public final float[] times;
	public int executionCount;
	public int cancelCount;

	public ListenerMeasurement(TimedRegisteredListener listener, int totalDuration) {
		this.listener = listener;
		this.times = new float[totalDuration];
		this.executionCount = 0;
		this.cancelCount = 0;
		Arrays.fill(this.times, 0.0f);
	}

	public void update(int position) {
		times[position] = (float) (listener.getTotalTime() / 1E6);
		executionCount += listener.getCount();
		listener.reset();
	}

	public void reset() {
		executionCount = 0;
		listener.reset();
	}

	public Class<?> getEventClass() {
		return NoLaggUtil.getEventClass.invoke(listener);
	}

	public boolean wasCalled() {
		return getEventClass() != null && executionCount > 0;
	}
}
