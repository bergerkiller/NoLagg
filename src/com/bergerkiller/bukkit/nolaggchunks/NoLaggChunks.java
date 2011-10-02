package com.bergerkiller.bukkit.nolaggchunks;

import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.getspout.spoutapi.SpoutManager;

import com.bergerkiller.bukkit.nolagg.NoLagg;

public class NoLaggChunks extends JavaPlugin {

	public static NoLaggChunks plugin;
	
	private final NLPlayerListener playerListener = new NLPlayerListener();
	private final NLPacketListener packetListener = new NLPacketListener();
	
	public void onEnable() {
		plugin = this;
		
		//General registering
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Monitor, this);
		if (NoLagg.chunkSendRate > 0) {
			SpoutManager.getPacketManager().addListenerUncompressedChunk(packetListener);
			//load
			PlayerChunkLoader.init(NoLagg.chunkSendInterval, NoLagg.chunkSendRate);
			
	        PluginDescriptionFile pdfFile = this.getDescription();
	        System.out.println("[NoLagg] chunk handler add-on version " + pdfFile.getVersion() + " is enabled!");
	        System.out.println("[NoLagg] " + NoLagg.chunkSendRate + " chunk packets will be sent every " + NoLagg.chunkSendInterval + " ticks.");
		} else {
			System.out.println("[NoLagg] chunk handler is not used, rate = 0!");
		}
	}
	
	public void onDisable() {
		PlayerChunkLoader.deinit();
		
		System.out.println("[NoLagg] chunk handler disabled!");
	}
	
}
