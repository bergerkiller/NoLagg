package com.bergerkiller.bukkit.nolagg.chunks;

import java.util.Collection;

import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.NativeUtil;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

import net.minecraft.server.v1_4_6.Chunk;
import net.minecraft.server.v1_4_6.ChunkCoordIntPair;
import net.minecraft.server.v1_4_6.Packet;
import net.minecraft.server.v1_4_6.Packet51MapChunk;
import net.minecraft.server.v1_4_6.TileEntity;

public class ChunkSendCommand {
	public ChunkSendCommand(final Packet51MapChunk mapPacket, final org.bukkit.Chunk chunk) {
		this.mapPacket = mapPacket;
		this.chunk = chunk;
	}

	private final Packet51MapChunk mapPacket;
	public final org.bukkit.Chunk chunk;

	public boolean isValid() {
		return this.chunk != null && this.mapPacket != null;
	}

	public void send(final ChunkSendQueue queue) {
		send(queue, this.mapPacket, this.chunk);
	}

	@SuppressWarnings("unchecked")
	public static void send(final ChunkSendQueue queue, final Packet51MapChunk mapPacket, final org.bukkit.Chunk chunk) {
		if (mapPacket == null) {
			return;
		}
		final Chunk nativeChunk = NativeUtil.getNative(chunk);
		queue.sentChunks.add(new ChunkCoordIntPair(chunk.getX(), chunk.getZ()));
		PacketUtil.sendPacket(queue.player, mapPacket, !NoLaggChunks.useBufferedLoading);
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
