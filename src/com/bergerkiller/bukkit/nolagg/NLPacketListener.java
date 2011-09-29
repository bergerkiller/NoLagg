package com.bergerkiller.bukkit.nolagg;

import org.bukkit.entity.Player;
import org.getspout.spoutapi.packet.listener.PacketListener;
import org.getspout.spoutapi.packet.standard.MCPacket;
import org.getspout.spoutapi.packet.standard.MCPacket51MapChunk;

public class NLPacketListener implements PacketListener {

	public static boolean allow = false;
	
	@Override
	public boolean checkPacket(Player player, MCPacket packet) {
		if (!allow && packet instanceof MCPacket51MapChunk) {
			MCPacket51MapChunk m = (MCPacket51MapChunk) packet;
			if (m.getSizeX() == 16 && m.getSizeY() == 128 && m.getSizeZ() == 16) {
				int cx = m.getX() >> 4;
				int cz = m.getZ() >> 4;
				PlayerChunkLoader.clear(player, player.getWorld().getChunkAt(cx, cz));
				return false;
			}
		}
		return true;
	}

}
