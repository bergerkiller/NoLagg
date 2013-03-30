package com.bergerkiller.bukkit.nolagg.chunks;

import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.protocol.CommonPacket;
import com.bergerkiller.bukkit.common.utils.PacketUtil;

public class ChunkSendCommand {
	private final CommonPacket mapPacket;
	public final org.bukkit.Chunk chunk;

	public ChunkSendCommand(final CommonPacket mapPacket, final org.bukkit.Chunk chunk) {
		this.mapPacket = mapPacket;
		this.chunk = chunk;
	}

	public boolean isValid() {
		return this.chunk != null && this.mapPacket != null;
	}

	public void send(final ChunkSendQueue queue) {
		send(queue, this.mapPacket, this.chunk);
	}

	public static void send(final ChunkSendQueue queue, final CommonPacket mapPacket, final org.bukkit.Chunk chunk) {
		if (mapPacket == null) {
			return;
		}
		queue.sentChunks.add(new IntVector2(chunk.getX(), chunk.getZ()));
		PacketUtil.sendPacket(queue.player, mapPacket, !NoLaggChunks.useBufferedLoading);
		PacketUtil.sendChunk(queue.player, chunk, false);
	}
}
