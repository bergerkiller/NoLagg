package com.bergerkiller.bukkit.nolaggchunks;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.getspout.spoutapi.SpoutManager;

public class NoLaggChunks extends JavaPlugin {

	public static NoLaggChunks plugin;
	
	private final NLPlayerListener playerListener = new NLPlayerListener();
	private final NLPacketListener packetListener = new NLPacketListener();
	
	private static Logger logger = Logger.getLogger("Minecraft");
	public static void log(Level level, String message) {
		logger.log(level, "[NoLagg] [Chunks] " + message);
	}
	
	public void onEnable() {
		plugin = this;
		
		//General registering
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_TELEPORT, playerListener, Priority.Monitor, this);
		
		SpoutManager.getPacketManager().addListenerUncompressedChunk(packetListener);
		for (int id : new int[] {50, 51, 52, 53, 130, 23, 24}) {
			SpoutManager.getPacketManager().addListener(id, packetListener);
		}
		
		//Settings
		Configuration config = new Configuration(this);
		config.load();
		PlayerChunkBuffer.sendInterval = config.parse("chunkSendInterval", 4);
		PlayerChunkBuffer.sendRate = config.parse("chunkSendRate", 1);
		PlayerChunkBuffer.viewDistance = config.parse("chunkViewDistance", 10);
		PlayerChunkBuffer.downloadSize = config.parse("chunkDownloadSize", 5);
		config.save();
				
		//load chunks from previous enable (reloads)
		PlayerChunkLoader.loadSentChunks(new File(getDataFolder() + File.separator + "chunks.tmp"));
		
		//init
		PlayerChunkLoader.init();
		
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println("[NoLagg] chunk handler Spout add-on version " + pdfFile.getVersion() + " is enabled!");
	}
	
	public void onDisable() {
		PlayerChunkLoader.saveSentChunks(new File(getDataFolder() + File.separator + "chunks.tmp"));
		PlayerChunkLoader.deinit();
		Compression.deinit();
		//remove the tasks
		System.out.println("[NoLagg] chunk handler disabled!");
	}
	
}
