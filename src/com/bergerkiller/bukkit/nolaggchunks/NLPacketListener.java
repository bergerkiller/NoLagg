package com.bergerkiller.bukkit.nolaggchunks;

import org.bukkit.entity.Player;
import org.getspout.spoutapi.packet.listener.PacketListener;
import org.getspout.spoutapi.packet.standard.MCPacket;
import org.getspout.spoutapi.packet.standard.MCPacket51MapChunk;

public class NLPacketListener implements PacketListener {

	public static boolean spoutAllowChunk = false;

	public boolean checkPacket(Player player, MCPacket packet) {
		if (!spoutAllowChunk && packet instanceof MCPacket51MapChunk) {
			MCPacket51MapChunk m = (MCPacket51MapChunk) packet;
			if (m.getSizeX() == 16 && m.getSizeY() == 128 && m.getSizeZ() == 16) {
				//whole packet being sent: retrieve
				PlayerChunkLoader.clear(player, m.getX() >> 4, m.getZ() >> 4);
				return false;
			}
		}
		return true;
	}
	
}
