package com.bergerkiller.bukkit.nolaggchunks;

import java.util.HashSet;

import net.minecraft.server.Packet;

import org.bukkit.entity.Player;
import org.getspout.spoutapi.packet.listener.PacketListener;
import org.getspout.spoutapi.packet.standard.MCPacket;

public class NLPacketListener implements PacketListener {
	
	public static boolean ignorePackets = false;
	public static HashSet<Packet> ignoreList = new HashSet<Packet>();
	public static void ignore(Packet p) {
		synchronized (ignoreList) {
			ignoreList.add(p);
		}
	}
	
	@SuppressWarnings("deprecation")
	public boolean checkPacket(Player player, MCPacket mcpacket) {
		if (ignorePackets) return true;
		Packet packet = (Packet) mcpacket.getPacket();
		int id = mcpacket.getId();
		PlayerChunkBuffer buffer = PlayerChunkLoader.getBuffer(player);
		if (buffer == null) return true;
		if (!buffer.isDownloaded()) {
			if (id == 50) {
				buffer.finalizeTerrainDownload();
			} else {
				return buffer.handleChunkDownload(packet);
			}
		} else if (id == 51 || id == 52 || id == 53 || id == 130 || id == 23 || id == 24) {
			synchronized (ignoreList) {
				if (ignoreList.remove(packet)) {
					return true;
				}
			}
			return buffer.queue(packet);
		}
		return true;
	}


}
