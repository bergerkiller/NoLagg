package com.bergerkiller.bukkit.nolagg.lagcommands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.nolagg.TickRateTrigger;

public class TriggeredCommand extends TickRateTrigger {
	private final String command;
	private final String[] args;
	private Command commandInstance;

	public TriggeredCommand(JavaPlugin plugin, double minTPS, int minLagDuration, int minDetectInterval, String command) {
		super(plugin, minTPS, minLagDuration, minDetectInterval);
		String[] args = command.split(" ");
		if (args.length == 0) {
			this.command = "";
			this.args = StringUtil.EMPTY_ARRAY;
		} else {
			this.command = args[0];
			this.args = StringUtil.remove(args, 0);
		}
	}

	@Override
	public void onLagDetected() {
		if (this.commandInstance == null) {
			this.commandInstance = CommonUtil.getCommandMap().getCommand(this.command);
			if (this.commandInstance == null) {
				return;
			}
		}
		this.commandInstance.execute(Bukkit.getServer().getConsoleSender(), this.command, this.args);
	}
}
