package com.bergerkiller.bukkit.nolagg.chunks;

import java.util.logging.Level;

import org.bukkit.craftbukkit.v1_4_6.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.lishid.orebfuscator.internal.v1_4_6.Packet51;
import com.lishid.orebfuscator.obfuscation.Calculations;

import net.minecraft.server.v1_4_6.Packet51MapChunk;

public class ObfSender
{
	public static void sendChunkPacket(Packet51MapChunk mapchunk, Player toPlayer)
	{
		try {
			Packet51 pack = new Packet51();
			pack.setPacket(mapchunk);
			Calculations.Obfuscate(pack, (CraftPlayer) toPlayer, false);
		} catch (Throwable t) {
			NoLaggChunks.plugin.log(Level.SEVERE, "An error occured in Orebfuscator: support for this plugin had to be removed!");
			t.printStackTrace();
			NoLaggChunks.isOreObfEnabled = false;
		}
	}
}
