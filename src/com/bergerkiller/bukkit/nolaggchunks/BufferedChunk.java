package com.bergerkiller.bukkit.nolaggchunks;

import java.util.ArrayList;
import java.util.zip.Deflater;

import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.minecraft.server.Chunk;
import net.minecraft.server.NetServerHandler;
import net.minecraft.server.Packet;
import net.minecraft.server.Packet51MapChunk;
import net.minecraft.server.Packet52MultiBlockChange;
import net.minecraft.server.Packet53BlockChange;
import net.minecraft.server.TileEntity;
import net.minecraft.server.World;

public class BufferedChunk {

	public BufferedChunk(int cx, int cz) {
		this.x = cx;
		this.z = cz;
	}
		
	private boolean hasEntireChunk = false;
	private boolean locked = false;
	private ArrayList<Packet> toSend = new ArrayList<Packet>();
	public int x, z;
	private boolean isSent = false;
	private long chunkTime = Long.MIN_VALUE;
	
	public static boolean isChunk(Packet packet) {
		if (packet instanceof Packet51MapChunk) {
			Packet51MapChunk p = (Packet51MapChunk) packet;
			return p.d == 16 && p.e == 128 && p.f == 16;
		}
		return false;
	}
	public static boolean isBlockChange(Packet packet) {
		if (packet instanceof Packet51MapChunk) return true;
		if (packet instanceof Packet52MultiBlockChange) return true;
		if (packet instanceof Packet53BlockChange) return true;
		return false;
	}

	public boolean isLocked() {
		return this.locked;
	}
	public void setLocked(boolean locked) {
		this.locked = locked;
	}
	public boolean isQueueingChunk() {
		return this.hasEntireChunk;
	}
	public boolean hasSent() {
		return this.isSent;
	}
	public void markSent() {
		this.isSent = true;
	}
	public boolean isEmpty() {
		synchronized (this.toSend) {
			return this.toSend.size() == 0;
		}
	}
	
	public void clear() {
		synchronized (this.toSend) {
			this.toSend.clear();
		}
	}
	
	public void queue(Packet packet) {
		if (packet == null) return;
		synchronized (this.toSend) {
			if (isBlockChange(packet)) {
				if (packet.timestamp <= chunkTime) return;
				if (isChunk(packet)) {
					chunkTime = packet.timestamp;
					this.hasEntireChunk = true;
					this.isSent = false;
					//remove all packets before this chunk
					int i = 0;
					while (i < toSend.size()) {
						Packet p = toSend.get(i);
						if (p.timestamp < chunkTime && isBlockChange(p)) {
							toSend.remove(i);
						} else {
							i++;
						}
					}
				}
			}
			toSend.add(packet);
			
		}
	}
	public void queueChunk(org.bukkit.World world) {
		if (world == null) return;
		World w = ((CraftWorld) world).getHandle();
		if (w == null) return;
		Chunk c = w.getChunkAt(this.x, this.z);
		if (c == null) return;
		this.queue(new Packet51MapChunk(this.x * 16, 0, this.z * 16, 16, 128, 16, c.world));
		for (Object o : c.tileEntities.values()) {
			if (o instanceof TileEntity) {
				this.queue(((TileEntity) o).l());
			}
		}
	}
		
	public void send(Player to) {
		synchronized (this.toSend) {
			NLPacketListener.ignorePackets = true;
			for (Packet p : toSend) {
				NLPacketListener.ignore(p);
				try {
					send(to, p);
				} catch (Exception ex) {
					System.out.println("[NoLaggChunks] Failed to send '" + p.getClass().getSimpleName() + "' to '" + to.getName() + "'!");
					ex.printStackTrace();
				}
			}
			NLPacketListener.ignorePackets = false;
			hasEntireChunk = false;
			toSend.clear();
			this.isSent = true;
		}
	}
	
	public static void send(Player to, Packet packet) {
		if (packet == null || to == null) return;
		try {				
			if (packet instanceof Packet51MapChunk) {
				Packet51MapChunk p = (Packet51MapChunk) packet;
				if (!p.k && p.g == null) {
					//To prevent npe's: compress
			        int dataSize = p.rawData.length;
			        Deflater deflater = new Deflater();
			        byte[] deflateBuffer = new byte[dataSize + 100];
			        
			        deflater.reset();
			        deflater.setLevel(dataSize < 20480 ? 1 : 6);
			        deflater.setInput(p.rawData);
			        deflater.finish();
			        int size = deflater.deflate(deflateBuffer);
			        if (size == 0) {
			            size = deflater.deflate(deflateBuffer);
			        }

			        // copy compressed data to packet
			        p.g = new byte[size];
			        p.h = size;
			        System.arraycopy(deflateBuffer, 0, p.g, 0, size);
				}
			}
			
			//Send
			NetServerHandler handler = ((CraftPlayer) to).getHandle().netServerHandler;
			handler.sendPacket(packet);
		} catch (Exception ex) {
			System.out.println("[NoLaggChunks] Failed to send '" + packet.getClass().getSimpleName() + "' to '" + to.getName() + "'!");
			ex.printStackTrace();
		}
	}
	
	public static void sendChunk(Player to, int cx, int cz, boolean instant) {
		try {
			World world = ((CraftPlayer) to).getHandle().world;
			Packet51MapChunk packet = new Packet51MapChunk(cx * 16, 0, cz * 16, 16, 128, 16, world);
			packet.k = !instant;
			send(to, packet);
	      
			Chunk chunk = world.getChunkAt(cx, cz);
			for (Object object : chunk.tileEntities.values()) {
				TileEntity entity = (TileEntity) object;
				send(to, entity.l());
			}
		} catch (Exception ex) {
			System.out.println("[NoLaggChunks] Failed to send chunk [" + cx + "|" + cz + "] to player '" + to.getName() + "'!");
			ex.printStackTrace();
		}
	}

}
