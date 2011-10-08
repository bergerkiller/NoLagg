package com.bergerkiller.bukkit.nolaggchunks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import org.getspout.spoutapi.SpoutManager;

public class NoLaggChunks extends JavaPlugin {

	public static NoLaggChunks plugin;
	
	private final NLPlayerListener playerListener = new NLPlayerListener();
	private final NLPacketListener packetListener = new NLPacketListener();
	private final int[] watchedPackets = new int[] {50, 52, 53, 130, 201};
	
	public void onEnable() {
		plugin = this;
		
		//General registering
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_TELEPORT, playerListener, Priority.Monitor, this);
		
		SpoutManager.getPacketManager().addListenerUncompressedChunk(packetListener);
		for (int id : watchedPackets) {
			SpoutManager.getPacketManager().addListener(id, packetListener);
		}
		
		//Settings
		Configuration config = this.getConfiguration();
		PlayerChunkLoader.packetSendInterval = PlayerDefault.parseInt(config, "globalChunkSendInterval", 1);
		PlayerChunkLoader.packetSendMaxRate = PlayerDefault.parseInt(config, "globalChunkSendMaxRate", 2);
		
		PlayerChunkBuffer.defaultSendInterval.parse(config, "defaultChunkSendInterval");
		PlayerChunkBuffer.defaultSendRate.parse(config, "defaultChunkSendRate");
		PlayerChunkBuffer.defaultViewDistance.parse(config, "defaultChunkViewDistance");
		PlayerChunkBuffer.defaultDownloadSize.parse(config, "defaultChunkDownloadSize");
		
		if (config.getKeys().contains("players")) {
			List<String> players = config.getStringList("players", new ArrayList<String>());
			for (String player : players) {
				player = "players." + player + ".";
				PlayerChunkBuffer.defaultSendInterval.parse(player, config, player + "chunkSendInterval");
				PlayerChunkBuffer.defaultSendRate.parse(player, config, player + "chunkSendRate");
				PlayerChunkBuffer.defaultViewDistance.parse(player, config, player + "chunkViewDistance");
				PlayerChunkBuffer.defaultDownloadSize.parse(player, config, player + "chunkDownloadSize");
			}
		} else {
			config.setProperty("players.player.chunkSendInterval", PlayerChunkBuffer.defaultSendInterval.get());
			config.setProperty("players.player.chunkSendRate", PlayerChunkBuffer.defaultSendRate.get());
			config.setProperty("players.player.chunkViewDistance", PlayerChunkBuffer.defaultViewDistance.get());
			config.setProperty("players.player.chunkDownloadSize", PlayerChunkBuffer.defaultDownloadSize.get());
		}
		
		config.save();
		
		//load
		PlayerChunkLoader.init();
		PlayerChunkLoader.queueAllChunks();
		
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println("[NoLagg] chunk handler add-on version " + pdfFile.getVersion() + " is enabled!");
	}
	
	public void onDisable() {
		PlayerChunkLoader.deinit();
		
		System.out.println("[NoLagg] chunk handler disabled!");
	}
	
}
