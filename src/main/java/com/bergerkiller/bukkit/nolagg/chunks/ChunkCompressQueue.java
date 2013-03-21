package com.bergerkiller.bukkit.nolagg.chunks;

import java.util.Iterator;
import java.util.LinkedList;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.protocol.CommonPacket;
import com.bergerkiller.bukkit.common.protocol.PacketFields;
import com.bergerkiller.bukkit.common.reflection.classes.EntityPlayerRef;
import com.bergerkiller.bukkit.common.utils.CommonUtil;

public class ChunkCompressQueue {
	public ChunkCompressQueue(final ChunkSendQueue owner) {
		this.owner = owner;
	}

	private final ChunkSendQueue owner;
	private final LinkedList<Chunk> toCompress = new LinkedList<Chunk>();
	private final LinkedList<ChunkSendCommand> toSend = new LinkedList<ChunkSendCommand>();

	public void sort() {
		synchronized (this.toCompress) {
			this.owner.sort(this.toCompress);
		}
		synchronized (this.toSend) {
			this.owner.sort(this.toSend);
		}
	}

	public int getPendingSize() {
		synchronized (this.toCompress) {
			synchronized (this.toSend) {
				return this.toCompress.size() + this.toSend.size();
			}
		}
	}

	public void clear() {
		synchronized (this.toCompress) {
			this.toCompress.clear();
		}
		synchronized (this.toSend) {
			this.toSend.clear();
		}
	}

	public boolean isAlive() {
		Object ep = Conversion.toEntityHandle.convert(this.owner.player);
		Object playerConnection = EntityPlayerRef.playerConnection.get(ep);
		return playerConnection != null && !EntityPlayerRef.disconnected.get(playerConnection);
	}

	public boolean canSend() {
		synchronized (this.toSend) {
			return !this.toSend.isEmpty();
		}
	}

	public boolean finishedCompressing() {
		synchronized (this.toCompress) {
			return this.toCompress.isEmpty();
		}
	}

	public void enqueue(final Chunk chunk) {
		if (chunk == null) {
			return;
		}
		if (NoLaggChunks.useBufferedLoading) {
			// Tell the threads to start the compression
			synchronized (this.toCompress) {
				this.toCompress.offer(chunk);
			}
		} else {
			// Let the server itself deal with it
			CommonPacket packet = PacketFields.MAP_CHUNK.newInstance(Conversion.toChunkHandle.convert(chunk), true, 0xffff);
			this.enqueue(new ChunkSendCommand(packet, chunk));
		}
	}

	public void enqueue(ChunkSendCommand sendCommand) {
		if (sendCommand == null || !sendCommand.isValid()) {
			return;
		}
		synchronized (this.toSend) {
			this.toSend.offer(sendCommand);
		}
	}

	public boolean remove(int x, int z) {
		synchronized (this.toCompress) {
			Iterator<Chunk> iter = this.toCompress.iterator();
			while (iter.hasNext()) {
				Chunk chunk = iter.next();
				if (chunk.getX() == x && chunk.getZ() == z) {
					iter.remove();
					return true;
				}
			}
		}
		synchronized (this.toSend) {
			Iterator<ChunkSendCommand> iter = this.toSend.iterator();
			while (iter.hasNext()) {
				ChunkSendCommand cmd = iter.next();
				if (cmd.chunk.getX() == x && cmd.chunk.getZ() == z) {
					iter.remove();
					return true;
				}
			}
		}
		return false;
	}

	public boolean isNear(Chunk chunk) {
		return this.isNear(chunk.getX(), chunk.getZ());
	}

	public boolean isNear(int x, int z) {
		synchronized (this.owner) {
			return this.owner.isNear(x, z, CommonUtil.VIEW);
		}
	}

	public Player owner() {
		return this.owner.player;
	}

	public boolean hasChunk() {
		synchronized (this.toCompress) {
			return !this.toCompress.isEmpty();
		}
	}

	public Chunk pollChunk() {
		synchronized (this.toCompress) {
			return this.toCompress.poll();
		}
	}

	public ChunkSendCommand pollSendCommand() {
		synchronized (this.toSend) {
			if (this.toSend.isEmpty()) {
				return null;
			}
			ChunkSendCommand cmd = this.toSend.poll();
			if (this.isNear(cmd.chunk)) {
				// In range of dynamic view?
				return cmd;
			} else {
				this.owner.removeContained(cmd.chunk.getX(), cmd.chunk.getZ());
				return this.pollSendCommand();
			}
		}
	}

	public boolean sendNext() {
		ChunkSendCommand next = this.pollSendCommand();
		if (next == null) {
			return false;
		}
		next.send(this.owner);
		this.owner.removeContained(next.chunk.getX(), next.chunk.getZ());
		return true;
	}
}
