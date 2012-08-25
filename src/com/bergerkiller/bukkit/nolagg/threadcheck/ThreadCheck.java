package com.bergerkiller.bukkit.nolagg.threadcheck;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.scheduler.CraftScheduler;
import org.bukkit.craftbukkit.util.ServerShutdownThread;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.timedbukkit.craftbukkit.scheduler.TimedWrapper;

import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.nolagg.NoLaggComponents;
import com.google.common.collect.Lists;

public class ThreadCheck {

	@SuppressWarnings("unused")
	private static ThreadCheck checker;
	public static void init(JavaPlugin plugin) {
		try {
			checker = new ThreadCheck(plugin);
		} catch (Throwable t) {
			System.out.println("Failed to initialize thread checker:");
			t.printStackTrace();
		}
	}
	public static void deinit() {
		checker = null;
	}
		
	private static Logger logger = Logger.getLogger("minecraft");
	
	private ThreadCheck(JavaPlugin p) {
		p.getServer().getPluginManager().registerEvents(this.listener, p);
	}
			
	private final Listener listener = new Listener();

	private static HashSet<String> thrownErrors = new HashSet<String>();
	
	private static String getListenerName(String classname) {
		if (classname.equals("PlayerListener")) return "org.bukkit.event.player.PlayerListener";
		if (classname.equals("EntityListener")) return "org.bukkit.event.entity.EntityListener";
		if (classname.equals("VehicleListener")) return "org.bukkit.event.vehicle.VehicleListener";
		if (classname.equals("WorldListener")) return "org.bukkit.event.world.WorldListener";
		if (classname.equals("WeatherListener")) return "org.bukkit.event.weather.WeatherListener";
		if (classname.equals("BlockListener")) return "org.bukkit.event.block.BlockListener";
		return "org.bukkit.event." + classname;
	}
	
    //java.lang.Thread.run(Unknown Source)
    //org.bukkit.craftbukkit.scheduler.CraftWorker.run(CraftWorker.java:34)
	@SuppressWarnings("rawtypes")
	private static boolean classCheck(StackTraceElement element, Class classtocheck) {
		return element.getClassName().equals(classtocheck.getName());
	}
	public static boolean check(String event) {
		final Thread t = Thread.currentThread();
		if (t != CommonUtil.MAIN_THREAD) {
			List<StackTraceElement> stack = Lists.newArrayList(t.getStackTrace());
			stack.remove(0); //remove check function from stacktrace
			stack.remove(0); //remove check function from stacktrace
			if (t.getClass().equals(ServerShutdownThread.class)) return true;
			if (event != null && event.equals("")) {
				return false; //no messages for custom events
			}

			//remove timed wrapper from stack trace
			if (NoLaggComponents.EXAMINE.isEnabled()) {
				Iterator<StackTraceElement> iter = stack.iterator();
				while (iter.hasNext()) {
					if (classCheck(iter.next(), TimedWrapper.class)) {
						iter.remove();
					}
				}
			}

			String classname;
			//general thread?
			if (classCheck(stack.get(stack.size() - 1), Thread.class)) {
				stack.remove(stack.size() - 1);
				//bukkit task?
				if (classCheck(stack.get(stack.size() - 1), CraftScheduler.class)) {
					classname = stack.get(stack.size() - 2).getClassName();
				} else {
					classname = stack.get(stack.size() - 1).getClassName();
				}
			} else {
				classname = t.getClass().getName();
			}

			if (event != null) {
				//replace call
				final StackTraceElement el = stack.get(0);
				final String mname = el.getMethodName();
				String cname = el.getClassName();
				cname = cname.substring(cname.lastIndexOf('.') + 1);
				stack.set(0, new StackTraceElement(getListenerName(cname), mname, cname, 0));
			}

			//now convert the stack trace to an array
			StackTraceElement[] elements = new StackTraceElement[stack.size()]; //stack.toArray(new StackTraceElement[0]);
			StringBuilder sb = new StringBuilder(elements.length * 80); //assume 80 characters per line
			for (int i = 0; i < elements.length; i++) {
				if (i != 0) sb.append('\n');
				sb.append((elements[i] = stack.get(i)).toString());
			}
			if (thrownErrors.add(sb.toString())) {
				String msg;
				if (event == null) {
					msg = "Could not properly handle a code section:";
				} else {
					msg = "Could not properly handle event " + event + ":";
				}
				IllegalAccessError err = new IllegalAccessError("Synchronized code got accessed from another thread: " + classname);
				err.setStackTrace(elements);
				logger.log(Level.WARNING, msg, err);
				logger.log(Level.INFO, "This error is logged only once: it could have occurred multiple times by now.");
				//get the plugin that caused this
				try {
					classname = classname.substring(0, classname.lastIndexOf('.')).toLowerCase();
					for (Plugin p : Bukkit.getServer().getPluginManager().getPlugins()) {
						if (p.getDescription().getMain().toLowerCase().startsWith(classname)) {
							msg = "Please contact one of the authors of plugin '" + p.getDescription().getName() + "': ";
							List<String> authors = p.getDescription().getAuthors();
							for (int i = 0; i < authors.size(); i++) {
								if (i != 0) msg += ", ";
								msg += authors.get(i);
							}
							logger.log(Level.INFO, msg);
						}
					}
				} catch (Throwable tt) {}
			}
			return false;
		}
		return true;
	}
}
