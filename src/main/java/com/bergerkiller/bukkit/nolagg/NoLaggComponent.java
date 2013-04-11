package com.bergerkiller.bukkit.nolagg;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.config.FileConfiguration;
import com.bergerkiller.bukkit.common.permissions.NoPermissionException;

public abstract class NoLaggComponent {
	protected NoLaggComponents comp;

	public void log(Level level, String text) {
		Bukkit.getLogger().log(level, "[NoLagg " + this.getName() + "] " + text);
	}

	public void updateDependency(Plugin plugin, String pluginName, boolean enabled) {
	}

	public final void enable() {
		FileConfiguration config = new FileConfiguration(NoLagg.plugin);
		config.load();
		this.enable(config);
		config.save();
	}

	public final void disable() {
		FileConfiguration config = new FileConfiguration(NoLagg.plugin);
		config.load();
		this.disable(config);
		config.save();
	}

	public final void reload() {
		FileConfiguration config = new FileConfiguration(NoLagg.plugin);
		config.load();
		this.reload(config);
		config.save();
	}

	protected final void reload(FileConfiguration config) {
		if (this.isEnabled()) {
			try {
				this.onReload(config.getNode(this.getName().toLowerCase()));
			} catch (Throwable t) {
				log(Level.SEVERE, "Failed to reload NoLagg component '" + this.getName() + "':");
				NoLagg.plugin.handle(t);
			}
		}
	}

	protected final void enable(FileConfiguration config) {
		if (!this.isEnabled()) {
			try {
				this.onEnable(config.getNode(this.getName().toLowerCase()));
				this.comp.setEnabled(true);
			} catch (Throwable t) {
				log(Level.SEVERE, "Failed to enable NoLagg component '" + this.getName() + "':");
				NoLagg.plugin.handle(t);
			}
		}
	}

	protected final void disable(FileConfiguration config) {
		if (this.isEnabled()) {
			this.comp.setEnabled(false);
			try {
				this.onDisable(config.getNode(this.getName().toLowerCase()));
			} catch (Throwable t) {
				log(Level.SEVERE, "Failed to disable NoLagg component '" + this.getName() + "':");
				NoLagg.plugin.handle(t);
			}
		}
	}

	public void register(Class<? extends Listener> listener) {
		NoLagg.plugin.register(listener);
	}

	public void register(Listener listener) {
		NoLagg.plugin.register(listener);
	}

	public String getName() {
		return this.comp.getName();
	}

	public boolean isEnabled() {
		return this.comp.isEnabled();
	}

	public abstract void onReload(ConfigurationNode config);

	public abstract void onDisable(ConfigurationNode config);

	public abstract void onEnable(ConfigurationNode config);

	public boolean onCommand(CommandSender sender, String[] args) throws NoPermissionException {
		return false;
	}
}
