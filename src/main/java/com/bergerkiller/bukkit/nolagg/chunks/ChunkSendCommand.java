package com.bergerkiller.bukkit.nolagg.chunks;

import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.protocol.CommonPacket;
import com.bergerkiller.bukkit.common.reflection.classes.ChunkRef;
import com.bergerkiller.bukkit.common.reflection.classes.EntityTrackerRef;
import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

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

	public static void send(final ChunkSendQueue queue, final CommonPacket mapPacket, final org.bukkit.Chunk chunk) {
		if (mapPacket == null) {
			return;
		}

		final Object chunkHandle = Conversion.toChunkHandle.convert(chunk);
		queue.sentChunks.add(new IntVector2(chunk.getX(), chunk.getZ()));
		PacketUtil.sendCommonPacket(queue.player, mapPacket, !NoLaggChunks.useBufferedLoading);
		ChunkRef.seenByPlayer.set(chunkHandle, true);

		// Tile entities
		CommonPacket packet;
		for (Object tile : ChunkRef.tileEntities.get(chunkHandle).values()) {
			if ((packet = BlockUtil.getUpdatePacket(tile)) != null) {
				PacketUtil.sendCommonPacket(queue.player, packet);
			}
		}
		// Spawn messages
		CommonUtil.nextTick(new Runnable() {
			public void run() {
				EntityTrackerRef.updatePlayer(WorldUtil.getTracker(queue.player.getWorld()), queue.player, chunk);
			}
		});
	}
}
