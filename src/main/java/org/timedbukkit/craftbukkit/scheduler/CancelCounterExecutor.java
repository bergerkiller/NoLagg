package org.timedbukkit.craftbukkit.scheduler;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import com.bergerkiller.bukkit.common.proxies.ProxyBase;

/**
 * Keeps track of how often a specific event was cancelled while executing
 */
public class CancelCounterExecutor extends ProxyBase<EventExecutor> implements EventExecutor {
	public int cancelCount = 0;
	public Plugin owner;

	public CancelCounterExecutor(EventExecutor base, Plugin owner) {
		super(base);
		this.owner = owner;
	}

	@Override
	public void execute(Listener listener, Event event) throws EventException {
		if (event instanceof Cancellable) {
			Cancellable canc = (Cancellable) event;
			// If not cancelled and then cancelled afterwards, increment counter
			if (!canc.isCancelled()) {
				this.base.execute(listener, event);
				if (canc.isCancelled()) {
					this.cancelCount++;
				}
				return;
			}
		}
		this.base.execute(listener, event);
	}
}
