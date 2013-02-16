package com.bergerkiller.bukkit.nolagg.chunks;

import java.util.Collection;

import com.bergerkiller.bukkit.common.protocol.CommonPacket;
import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.NativeUtil;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

import net.minecraft.server.v1_4_R1.Chunk;
import net.minecraft.server.v1_4_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_4_R1.Packet;
import net.minecraft.server.v1_4_R1.TileEntity;

public class ChunkSendCommand {
	public ChunkSendCommand(final CommonPacket mapPacket, final org.bukkit.Chunk chunk) {
		this.mapPacket = mapPacket;
		this.chunk = chunk;
	}

	private final CommonPacket mapPacket;
	public final org.bukkit.Chunk chunk;

	public boolean isValid() {
		return this.chunk != null && this.mapPacket != null;
	}

	public void send(final ChunkSendQueue queue) {
		send(queue, this.mapPacket, this.chunk);
	}

	@SuppressWarnings("unchecked")
	public static void send(final ChunkSendQueue queue, final CommonPacket mapPacket, final org.bukkit.Chunk chunk) {
		if (mapPacket == null) {
			return;
		}
		final Chunk nativeChunk = NativeUtil.getNative(chunk);
		queue.sentChunks.add(new ChunkCoordIntPair(chunk.getX(), chunk.getZ()));
		PacketUtil.sendCommonPacket(queue.player, mapPacket, !NoLaggChunks.useBufferedLoading);
		nativeChunk.seenByPlayer = true;
		// Tile entities
		Packet p;
		for (TileEntity tile : (Collection<TileEntity>) nativeChunk.tileEntities.values()) {
			if ((p = BlockUtil.getUpdatePacket(tile)) != null) {
				PacketUtil.sendPacket(queue.player, p);
			}
		}
		// Spawn messages
		CommonUtil.nextTick(new Runnable() {
			public void run() {
				WorldUtil.getTracker(queue.player.getWorld()).a(NativeUtil.getNative(queue.player), nativeChunk);
			}
		});
	}
}
