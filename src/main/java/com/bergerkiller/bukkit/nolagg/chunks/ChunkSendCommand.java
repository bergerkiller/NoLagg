package com.bergerkiller.bukkit.nolagg.chunks;

import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.protocol.CommonPacket;
import com.bergerkiller.bukkit.common.reflection.classes.ChunkRef;
import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

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
		// Send payload
		PacketUtil.sendPacket(queue.player, mapPacket, !NoLaggChunks.useBufferedLoading);

		// Send tile entities
		CommonPacket packet;
		final Object chunkHandle = Conversion.toChunkHandle.convert(chunk);
		for (Object tile : ChunkRef.tileEntities.get(chunkHandle).values()) {
			if ((packet = BlockUtil.getUpdatePacket(tile)) != null) {
				PacketUtil.sendPacket(queue.player, packet);
			}
		}

		// Send Entities
		WorldUtil.getTracker(chunk.getWorld()).spawnEntities(queue.player, chunk);
	}
}
