package com.bergerkiller.bukkit.nolagg.saving;

import java.util.logging.Level;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;

public class NoLaggSaving extends NoLaggComponent {

	public static NoLaggSaving plugin;
	public static int autoSaveInterval;
	public static int autoSaveBatch;
	public static int writeDataInterval;
	public static boolean writeDataEnabled;

	@Override
	public void onDisable(ConfigurationNode config) {
		AutoSaveChanger.deinit();
		RegionFileFlusher.deinit();
	}

	@Override
	public void onEnable(ConfigurationNode config) {
		plugin = this;
		this.loadConfig(config);
		AutoSaveChanger.init();
	}

	public void loadConfig(ConfigurationNode config) {
		config.setHeader("autoSaveInterval", "The tick interval at which the server saves automatically (20 ticks = 1 second)");
		autoSaveInterval = config.get("autoSaveInterval", 400);
		if (autoSaveInterval < 400) {
			autoSaveInterval = 400;
			config.set("autoSaveInterval", 400);
			NoLaggSaving.plugin.log(Level.WARNING, "Save interval is set too low and has been limited to a 400 tick (20 second) interval");
		}

		config.setHeader("autoSaveBatchSize", "The amount of chunks saved every tick when autosaving");
		config.addHeader("autoSaveBatchSize", "If saving causes severe tick lag, lower it, if it takes too long, increase it");
		autoSaveBatch = config.get("autoSaveBatchSize", 20);

		config.setHeader("writeDataEnabled", "Whether NoLagg will attempt to write all world data to the region files at a set interval");
		config.addHeader("writeDataEnabled", "This is done on another thread, so don't worry about the main thread lagging while this happens");
		writeDataEnabled = config.get("writeDataEnabled", true);

		config.setHeader("writeDataInterval", "The tick interval at which the server actually writes the chunk data to file (20 ticks = 1 second)");
		writeDataInterval = config.get("writeDataInterval", 12000);
		if (writeDataInterval < 600) {
			writeDataInterval = 600;
			config.set("writeDataInterval", 600);
			log(Level.WARNING, "The configuration asked me to use a data write interval lower than 600 ticks (30 seconds)");
			log(Level.WARNING, "Such an interval is far too low to be beneficial, so I changed it to 600 ticks for you");
		}
		if (writeDataEnabled) {
			double time = writeDataInterval / 20;
			String timetext;// = time + " seconds";
			if (time < 60) {
				timetext = time + " seconds";
			} else if (time < 3600) {
				timetext = (time / 60) + " minutes";
			} else {
				timetext = (time / 3600) + " hours";
			}
			log(Level.INFO, "will write world data to all region files every " + writeDataInterval + " ticks (" + timetext + ")");
		}
	}

	@Override
	public void onReload(ConfigurationNode config) {
		this.loadConfig(config);
		AutoSaveChanger.reload();
		RegionFileFlusher.reload();
	}
}
