package com.bergerkiller.bukkit.nolagg;

import java.util.logging.Level;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.config.FileConfiguration;

public enum NoLaggComponents {
	CHUNKS("Chunks", "Manages chunk loading and sending to clients using various new settings, also fixes chunk unload problems"), 
	EXAMINE("Examine", "Can examine server tick rate performance"), 
	MONITOR("Monitor", "Can monitor and log server and player performance statistics"), 
	ITEMBUFFER("ItemBuffer", "Buffers items in chunks to prevent lag-outs because of lots of items"), 
	ITEMSTACKER("ItemStacker", "Stacks nearby items to counter item-drop spammers and reduce item count on the server"), 
	LIGHTING("Lighting", "Attempts to fix block and sky lighting bugs in worlds"), 
	SPAWNLIMITER("SpawnLimiter", "Keeps entity counts below multiple configured thresholds", false), 
	TNT("TNT", "Replaces explosion creation with a faster version and buffers TNT ignites to prevent TNT server crashes"), 
	SAVING("Saving", "Alters the way worlds are saved to reduce disk usage and to force proper saves"), 
	COMMON("Common", "Common features such as the clear and garbage collect commands"), 
	THREADLOCKNOTIFIER("ThreadLockNotifier", "Notifies the current stack trace of the main thread when the server freezes"),
	THREADCHECK("ThreadCheck", "Notifies when a main-thread only event is called from another thread to detect instabilities it may cause"),
	PATCHES("Patches", "Patches certain classes and functions from CraftBukkt");

	private boolean enabled;
	private final boolean enabledByDefault;
	private String name, mainclass, description;

	private NoLaggComponents(String name, String description) {
		this(name, description, true);
	}

	private NoLaggComponents(String name, String description, boolean enabledByDefault) {
		this(name, "com.bergerkiller.bukkit.nolagg." + name.toLowerCase() + ".NoLagg" + name, description, enabledByDefault);
	}

	private NoLaggComponents(String name, String mainclass, String description, boolean enabledByDefault) {
		this.enabled = false;
		this.name = name;
		this.mainclass = mainclass;
		this.description = description;
		this.enabledByDefault = enabledByDefault;
	}

	protected static void loadAll(FileConfiguration config) {
		for (NoLaggComponents comp : values()) {
			comp.load(config);
		}
	}

	protected void load(FileConfiguration config) {
		for (NoLaggComponent comp : NoLagg.plugin.getComponents()) {
			if (comp.getName().equals(this.name)) {
				return; // already loaded
			}
		}
		ConfigurationNode node = config.getNode(this.name.toLowerCase());
		node.setHeader("\n" + description);
		node.setHeader("enabled", "Whether " + name + " should be loaded on startup");
		if (node.get("enabled", this.enabledByDefault)) {
			try {
				Class<?> clazz = Class.forName(this.mainclass);
				NoLaggComponent comp = (NoLaggComponent) clazz.newInstance();
				comp.comp = this;
				NoLagg.plugin.getComponents().add(comp);
			} catch (Throwable t) {
				NoLagg.plugin.log(Level.SEVERE, "Failed to load component '" + this.name + "':");
				NoLagg.plugin.handle(t);
			}
		}
	}

	public String getName() {
		return this.name;
	}

	protected void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return this.enabled;
	}
}
