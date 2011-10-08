package com.bergerkiller.bukkit.nolaggchunks;

import java.util.HashMap;

import net.minecraft.server.Packet;
import net.minecraft.server.Packet201PlayerInfo;

import org.bukkit.entity.Player;
import org.getspout.spoutapi.packet.listener.PacketListener;
import org.getspout.spoutapi.packet.standard.MCPacket;
import org.getspout.spoutapi.packet.standard.MCPacket51MapChunk;

public class NLPacketListener implements PacketListener {
	private static HashMap<String, Integer> playerPing = new HashMap<String, Integer>();
	public static int getPing(String playername) {
		if (playerPing.containsKey(playername)) {
			return playerPing.get(playername);
		} else {
			return 0;
		}
	}
	
	public static boolean ignorePackets = false;
					
	@SuppressWarnings("deprecation")
	public boolean checkPacket(Player player, MCPacket mcpacket) {
		if (ignorePackets) return true;
		Packet packet = (Packet) mcpacket.getPacket();
		int id = mcpacket.getId();
		if (id == 50) {
			PlayerChunkBuffer buffer = PlayerChunkLoader.getBuffer(player);
			if (!buffer.isDownloaded()) {
				ignorePackets = true;
				buffer.finalizeTerrainDownload();
				ignorePackets = false;
			}
		} else if (mcpacket instanceof MCPacket51MapChunk) {
			PlayerChunkBuffer buffer = PlayerChunkLoader.getBuffer(player);
			if (buffer.isDownloaded()) {
				return !buffer.queue(packet);
			} else {
				return buffer.handleChunkDownload(packet);
			}
		} else if (id == 201) {
			Packet201PlayerInfo p = (Packet201PlayerInfo) packet;
			playerPing.put(p.a, p.c);
		} else if (id == 52 || id == 53 || id == 130) {
			return !PlayerChunkLoader.getBuffer(player).queue(packet);
		}

		return true;
	}
	
}
