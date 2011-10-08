package com.bergerkiller.bukkit.nolaggchunks;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.zip.Deflater;

import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.minecraft.server.Chunk;
import net.minecraft.server.NetServerHandler;
import net.minecraft.server.Packet;
import net.minecraft.server.Packet51MapChunk;
import net.minecraft.server.TileEntity;
import net.minecraft.server.World;

public class BufferedChunk {

	public BufferedChunk(int cx, int cz) {
		this.x = cx;
		this.z = cz;
	}
		
	private boolean hasEntireChunk = false;
	private ArrayList<Packet> toSend = new ArrayList<Packet>();
	public int x, z;
	private int size = 0;
	private boolean isChunkSent = false;
	
	public static boolean isChunk(Packet packet) {
		if (packet instanceof Packet51MapChunk) {
			Packet51MapChunk p = (Packet51MapChunk) packet;
			return p.d == 16 && p.e == 128 && p.f == 16;
		}
		return false;
	}

	public boolean isQueueingChunk() {
		return this.hasEntireChunk;
	}
	public boolean hasFullChunkSent() {
		return this.isChunkSent;
	}
	public void setFullChunkSent() {
		this.isChunkSent = true;
	}
	public boolean isEmpty() {
		return size == 0;
	}
	
	private synchronized void syncoperation(Player player, Packet packet) {
		if (player == null && packet != null) {
			if (isChunk(packet)) {
				toSend.clear();
				hasEntireChunk = true;
			}
			toSend.add(packet);
		} else if (player != null && packet == null) {
			NLPacketListener.ignorePackets = true;
			for (Packet p : toSend) {
				send(player, p);
			}
			NLPacketListener.ignorePackets = false;
			hasEntireChunk = false;
			toSend.clear();
			this.isChunkSent = true;
		}
		this.size = toSend.size();
	}
	
	public void queue(Packet packet) {
        this.syncoperation(null, packet);
	}
	public void queueChunk(org.bukkit.World world) {
		World w = ((CraftWorld) world).getHandle();
		this.queue(new Packet51MapChunk(this.x * 16, 0, this.z * 16, 16, 128, 16, w)); 
	}
	
	public void send(Player to) {
		this.syncoperation(to, null);
	}
	
	
	private static Field timestamp;
	public static void send(Player to, Packet packet) {
		if (packet == null || to == null) return;
		try {
			if (timestamp == null) {
				timestamp = Packet.class.getField("timestamp");
				timestamp.setAccessible(true);
			}

			//Set the time
			timestamp.set(packet, System.currentTimeMillis());
				
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
			System.out.println("[NoLaggChunks] Failed to send '" + packet.getClass().getSigners() + "' to '" + to.getName() + "'!");
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
