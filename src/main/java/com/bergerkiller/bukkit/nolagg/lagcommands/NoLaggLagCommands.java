package com.bergerkiller.bukkit.nolagg.lagcommands;

import java.util.ArrayList;
import java.util.List;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;

public class NoLaggLagCommands extends NoLaggComponent {
	private List<TriggeredCommand> commandTasks = new ArrayList<TriggeredCommand>();

	@Override
	public void onReload(ConfigurationNode config) {
		abortTasks();

		// Header
		config.setHeader("commands", "\nA list of commands that are executed when a certain tick rate lag is detected");
		config.addHeader("commands", "The node name for each section is unused, use whatever you need to find it back");

		// Defaults
		final boolean needsGen = !config.contains("commands");
		final ConfigurationNode commands = config.getNode("commands");
		if (needsGen) {
			// Set up a sample Command structure
			ConfigurationNode command = commands.getNode("dummy");
			command.setHeader("command", "The command to execute (example: 'say hello' for /say hello)");
			command.set("command", "");
			command.setHeader("tpsThreshold", "The minimum ticks per second value below which lag is considered");
			command.set("tpsThreshold", 14.0);
			command.setHeader("triggerLagTicks", "How many ticks the TPS has to be below the threshold for lag to be detected");
			command.set("triggerLagTicks", 20);
			command.setHeader("minExecuteInterval", "The minimum command execution interval in seconds");
			command.addHeader("minExecuteInterval", "This is used to prevent command execution spams");
			command.set("minExecuteInterval", 60);
		}
		// Read the command tasks to execute
		for (ConfigurationNode command : commands.getNodes()) {
			String cmd = command.get("command", String.class);
			if (LogicUtil.nullOrEmpty(cmd)) {
				continue;
			}
			double minTPS = command.get("tpsThreshold", 14.0);
			int minLagDuration = command.get("triggerLagTicks", 20);
			int minExecuteInterval = command.get("minExecuteInterval", 60);
			TriggeredCommand tcmd = new TriggeredCommand(NoLagg.plugin, minTPS, minLagDuration, minExecuteInterval, cmd);
			tcmd.start(1, 1);
			commandTasks.add(tcmd);
		}
	}

	@Override
	public void onDisable(ConfigurationNode config) {
		abortTasks();
	}

	@Override
	public void onEnable(ConfigurationNode config) {
		this.onReload(config);
	}

	private void abortTasks() {
		for (TriggeredCommand command : commandTasks) {
			command.stop();
		}
		commandTasks.clear();
	}
}
