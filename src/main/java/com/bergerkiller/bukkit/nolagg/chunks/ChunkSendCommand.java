package com.bergerkiller.bukkit.nolagg.chunks;

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
		if (mapPacket == null) {
			return;
		}
		PacketUtil.sendPacket(queue.player, mapPacket, !NoLaggChunks.useBufferedLoading);
		PacketUtil.sendChunk(queue.player, chunk, false);
	}
}
