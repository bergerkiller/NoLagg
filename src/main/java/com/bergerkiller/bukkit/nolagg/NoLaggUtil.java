package com.bergerkiller.bukkit.nolagg;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.reflection.FieldAccessor;
import com.bergerkiller.bukkit.common.reflection.MethodAccessor;
import com.bergerkiller.bukkit.common.reflection.SafeDirectMethod;
import com.bergerkiller.bukkit.common.reflection.SafeField;
import com.bergerkiller.bukkit.common.reflection.SafeMethod;
import com.bergerkiller.bukkit.common.utils.CommonUtil;

public class NoLaggUtil {
	public static final FieldAccessor<EventExecutor> exefield = new SafeField<EventExecutor>(RegisteredListener.class, "executor");
	public static final MethodAccessor<Class<?>> getEventClass;

	static {
		Class<?> timedListenerClass = CommonUtil.getClass("org.bukkit.plugin.TimedRegisteredListener");
		if (timedListenerClass == null) {
			NoLagg.plugin.log(Level.WARNING, "Timed event listener class is unavailable - examiner will fail");
			getEventClass = null;
		} else if (SafeMethod.contains(timedListenerClass, "getEventClass")) {
			getEventClass = new SafeMethod<Class<?>>(timedListenerClass, "getEventClass");
		} else {
			final MethodAccessor<Object> getEventMethod = new SafeMethod<Object>(timedListenerClass, "getEvent");
			getEventClass = new SafeDirectMethod<Class<?>>() {
				@Override
				public Class<?> invoke(Object instance, Object... args) {
					try {
						Object event = getEventMethod.invoke(instance);
						return event == null ? null : event.getClass();
					} catch (Throwable t) {
						throw new RuntimeException("Unexpected error while using getEvent", t);
					}
				}
			};
		}
	}

	public static StackTraceElement findExternal(StackTraceElement[] stackTrace) {
		return findExternal(Arrays.asList(stackTrace));
	}

	/**
	 * Gets the first stack trace element that is outside Bukkit/nms scope
	 * 
	 * @param stackTrace to look at
	 * @return first element
	 */
	public static StackTraceElement findExternal(List<StackTraceElement> stackTrace) {
		// bottom to top
		for (int j = stackTrace.size() - 1; j >= 0; j--) {
			String className = stackTrace.get(j).getClassName().toLowerCase();
			if (className.startsWith("org.bukkit")) {
				continue;
			}
			if (className.startsWith(Common.NMS_ROOT)) {
				continue;
			}
			return stackTrace.get(j);
		}
		return new StackTraceElement(Common.NMS_ROOT + ".MinecraftServer", "main", "MinecraftServer.java", 0);
	}

	/**
	 * Checks whether a given player is an NPC, and should not be treated as a player that needs network updates
	 * 
	 * @param player to check
	 * @return True if the player is an NPC, False if not
	 */
	public static boolean isNPCPlayer(Player player) {
		return player.hasMetadata("NPC");
	}
}
