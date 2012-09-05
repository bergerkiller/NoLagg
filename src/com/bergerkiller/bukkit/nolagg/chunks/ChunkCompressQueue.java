package com.bergerkiller.bukkit.nolagg.chunks;

import java.util.Iterator;
import java.util.LinkedList;

import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.utils.CommonUtil;

import net.minecraft.server.Chunk;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.Packet51MapChunk;

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
		if (this.owner.ep.netServerHandler == null)
			return false;
		if (this.owner.ep.netServerHandler.disconnected)
			return false;
		return true;
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
		if (chunk == null)
			return;
		if (NoLaggChunks.useBufferedLoading) {
			synchronized (this.toCompress) {
				this.toCompress.offer(chunk);
			}
		} else {
			Packet51MapChunk packet = ChunkCompressionThread.createPacket(chunk, null);
			packet.lowPriority = true;
			this.enqueue(new ChunkSendCommand(packet, chunk));
		}
	}

	public void enqueue(ChunkSendCommand sendCommand) {
		if (sendCommand == null)
			return;
		if (!sendCommand.isValid())
			return;
		synchronized (this.toSend) {
			this.toSend.offer(sendCommand);
		}
	}

	public boolean remove(int x, int z) {
		synchronized (this.toCompress) {
			Iterator<Chunk> iter = this.toCompress.iterator();
			while (iter.hasNext()) {
				Chunk chunk = iter.next();
				if (chunk.x == x && chunk.z == z) {
					iter.remove();
					return true;
				}
			}
		}
		synchronized (this.toSend) {
			Iterator<ChunkSendCommand> iter = this.toSend.iterator();
			while (iter.hasNext()) {
				ChunkSendCommand cmd = iter.next();
				if (cmd.chunk.x == x && cmd.chunk.z == z) {
					iter.remove();
					return true;
				}
			}
		}
		return false;
	}

	public boolean isNear(Chunk chunk) {
		return this.isNear(chunk.x, chunk.z);
	}

	public boolean isNear(int x, int z) {
		synchronized (this.owner) {
			return this.owner.isNear(x, z, CommonUtil.view);
		}
	}

	public EntityPlayer nativeOwner() {
		return this.owner.ep;
	}

	public Player owner() {
		return (Player) this.owner.ep.getBukkitEntity();
	}

	public boolean hasChunk() {
		synchronized (this.toCompress) {
			return !this.toCompress.isEmpty();
		}
	}

	public Chunk pollChunk() {
		Chunk chunk;
		synchronized (this.toCompress) {
			if (this.toCompress.isEmpty())
				return null;
			chunk = this.toCompress.poll();
		}
		return chunk;
	}

	public ChunkSendCommand pollSendCommand() {
		synchronized (this.toSend) {
			if (this.toSend.isEmpty())
				return null;
			ChunkSendCommand cmd = this.toSend.poll();
			if (this.isNear(cmd.chunk)) {
				// In range of dynamic view?
				return cmd;
			} else {
				this.owner.removeContained(cmd.chunk.x, cmd.chunk.z);
				return this.pollSendCommand();
			}
		}
	}

	public boolean sendNext() {
		ChunkSendCommand next = this.pollSendCommand();
		if (next == null)
			return false;
		next.send(this.owner);
		this.owner.removeContained(next.chunk.x, next.chunk.z);
		return true;
	}
}
