package org.timedbukkit.craftbukkit.scheduler;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import com.bergerkiller.bukkit.common.proxies.ProxyBase;
import com.bergerkiller.bukkit.nolagg.examine.ListenerMeasurement;
import com.bergerkiller.bukkit.nolagg.examine.PluginLogger;

public class CancellableEventExecutor extends ProxyBase<EventExecutor> implements EventExecutor {
	public ListenerMeasurement meas;

	public CancellableEventExecutor(EventExecutor base, ListenerMeasurement meas) {
		super(base);
		this.meas = meas;
	}

	@Override
	public void execute(Listener listener, Event event) throws EventException {
		if (!PluginLogger.isRunning() || !(event instanceof Cancellable)) {
			// Disable this listening executor
			PluginLogger.exefield.set(meas.listener, base);
			// Execute like normal
			base.execute(listener, event);
			return;
		}
		final Cancellable canc = (Cancellable) event;
		final boolean wasCancelled = canc.isCancelled();
		base.execute(listener, event);
		if (!wasCancelled && canc.isCancelled()) {
			// Event was cancelled by this listener, keep track of this!
			this.meas.cancelCount++;
		}
	}
}
