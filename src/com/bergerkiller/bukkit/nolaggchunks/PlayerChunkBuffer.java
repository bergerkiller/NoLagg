package com.bergerkiller.bukkit.nolaggchunks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.Packet;
import net.minecraft.server.Packet130UpdateSign;
import net.minecraft.server.Packet23VehicleSpawn;
import net.minecraft.server.Packet24MobSpawn;
import net.minecraft.server.Packet51MapChunk;
import net.minecraft.server.Packet52MultiBlockChange;
import net.minecraft.server.Packet53BlockChange;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public class PlayerChunkBuffer {
	
	public PlayerChunkBuffer(Player player) {
		this.x = player.getLocation().getBlockX() >> 4;
		this.z = player.getLocation().getBlockZ() >> 4;
		this.world = player.getWorld();
		this.player = player;
	}
	
	public static int sendInterval = 4;
	public static int sendRate = 1;
	public static int downloadSize = 5;
	public static int viewDistance = 10;

	private HashMap<Chunk, BufferedChunk> chunks = new HashMap<Chunk, BufferedChunk>();
	private int x, z;
	public World world;
	private int intervalCounter = 0;
	
	private boolean isTerrainDownloaded = false;
	private final static int maxview = 15;
	public Player player;
		
	/*
	 * Auto chunk sending
	 */
	/**
	 * Sends the next batch of chunks to a player
	 * @param player - The player to send to
	 * @param limit - The maximum allowed chunks to send
	 * @return The amount of chunks sent
	 */
	public void sendNext() {
		intervalCounter++;
		if (intervalCounter >= sendInterval) {
			//Global - max rate per tick
			intervalCounter = 0;
			this.send(sendRate);
		}
	}
	
	
	/*
	 * General coding
	 */
	public BufferedChunk get(int cx, int cz) {
		return this.get(new Chunk(cx, cz));
	}
	private BufferedChunk get(Chunk chunk) {
		synchronized (this.chunks) {
			BufferedChunk bc = this.chunks.get(chunk);
			if (bc == null) {
				bc = new BufferedChunk(chunk.x, chunk.z);
				this.chunks.put(chunk, bc);
			}
			return bc;
		}
	}
	public BufferedChunk get(Packet packet) {
		return this.get(Chunk.fromPacket(packet));
	}
	
	public boolean isNear(int cx, int cz, int view) {
		return Math.abs(this.x - cx) <= view && Math.abs(this.z - cz) <= view;
	}
	
	/**
	 * Queue a packet
	 * @param packet
	 * @param chunk
	 * @return If the packet can be sent to the client
	 */
	private boolean queue(Packet packet, Chunk chunk) {
		if (chunk == null) return true;
		if (this.isNear(chunk.x, chunk.z, maxview)) {
			BufferedChunk c = get(chunk);
			if (BufferedChunk.isChunk(packet)) {
				c.queue(packet);
				return false;
			} else {
				if (c.isQueueingChunk()) {
					//We are queuing a chunk, add it to the queue
					c.queue(packet);
					return false;
				} else if (c.hasFullChunkSent()) {
					//Chunk is present, just send it right away
					return true;
				} else {
					//Chunk not yet received...yet we got a minor chunk update
					c.queue(packet);
					return false;
				}
			}
		} else {
			//Out of reach, handled
			return false;
		}
	}
	public boolean queue(Packet packet) {
		return this.queue(packet, Chunk.fromPacket(packet));
	}
	
	public void queueAllChunks() {
		this.queueAllChunks(true);
	}
	public void queueAllChunks(boolean forced) {
		for (int a = -viewDistance; a <= viewDistance; a++) {
			for (int b = -viewDistance; b <= viewDistance; b++) {
				this.queueChunk(this.x + a, this.z + b, forced);
			}
		}
	}
	private void queueChunk(BufferedChunk c, boolean forced) {
		if (forced || !c.isQueueingChunk()) {
			c.queueChunk(this.world);
		}
	}
	public void queueChunk(int cx, int cz, boolean forced) {
		this.queueChunk(this.get(cx, cz), forced);
	}
	public void queueChunk(int cx, int cz) {
		this.queueChunk(cx, cz, true);
	}
	
	public void update() {
		this.update(this.player.getLocation());
	}
	public void update(Location newLocation) {
		synchronized (this.chunks) {
			if (newLocation.getWorld() == this.world) {
				int cx = newLocation.getBlockX() >> 4;
				int cz = newLocation.getBlockZ() >> 4;
				if (this.x != cx || this.z != cz) {
					this.x = cx;
					this.z = cz;
					//Remove chunks that will be re-sent by the game later on anyway
					BufferedChunk[] gen = this.chunks.values().toArray(new BufferedChunk[0]);
					this.chunks.clear();
					for (BufferedChunk bc : gen) {
						if (this.isNear(bc.x, bc.z, maxview)) {
							this.chunks.put(new Chunk(bc.x, bc.z), bc);
						}
					}
				}
			} else {
				this.world = newLocation.getWorld();
				this.x = newLocation.getBlockX() >> 4;
				this.z = newLocation.getBlockZ() >> 4;
				this.chunks.clear();
				this.isTerrainDownloaded = false;
			}
		}
	}
	
	private void markSent(Chunk chunk) {
		BufferedChunk b = new BufferedChunk(chunk.x, chunk.z);
		b.setFullChunkSent();
		synchronized (this.chunks) {
			this.chunks.put(chunk, b);
		}
	}
	
	
	/*
	 * Send coding
	 */
	private boolean send(int cx, int cz) {
		if (this.isNear(cx, cz, viewDistance)) {
			BufferedChunk c = get(cx, cz);
			if (c != null && !c.isEmpty() && c.isQueueingChunk()) {
				c.send(this.player);
				return true;
			}
		}
		return false;
	}
	private int sendLayer(int sendcount, int sendlimit, int layer, BlockFace direction) {
		//get modifiers from direction
		MoveMod[] mods = MoveMod.get(direction);
		//Get the chunk to start at
		int startx = this.x + direction.getModX() * layer;
		int startz = this.z + direction.getModZ() * layer;
		//Send starter chunk
		if (this.send(startx, startz)) {
			if (--sendcount == 0) return 0;
		}
		//Peel
		int x1 = startx;
		int z1 = startz;
		int x2 = startx;
		int z2 = startz;
		sendlimit++;
		while (true) {
			if (--sendlimit == 0) return sendcount;
			//offset the chunks
			x1 += mods[0].direction.getModX();
			z1 += mods[0].direction.getModZ();
			x2 += mods[1].direction.getModX();
			z2 += mods[1].direction.getModZ();
			//mod update
			mods[0].next(this.x, this.z, x1, z1, layer);
			mods[1].next(this.x, this.z, x2, z2, layer);
			//got till the end?
			if (x1 == x2 && z1 == z2) {
				if (this.send(x1, z1)) {
					--sendcount;
				}
				return sendcount;
			} else {
				if (this.send(x1, z1)) {
					if (--sendcount == 0) return 0;
				}
				if (this.send(x2, z2)) {
					if (--sendcount == 0) return 0;
				}
			}
		}
	}
	
	public void send(int sendcount) {
		this.send(sendcount, getDirection(this.player));
	}
	private int send(int sendcount, BlockFace direction) {
		if (sendcount > 0) {
			//main chunk
			if (this.send(this.x, this.z)) {
				if (--sendcount == 0) return 0;
			}
			
			final int threshold1 = 2; //to this layer full layers chunks are sent, after half
			final int threshold2 = 4; //at this layer less than half are sent
			
			//full layers
			for (int layer = 1; layer < threshold1; layer++) {
				sendcount = this.sendLayer(sendcount, layer * 4, layer, direction);
				if (sendcount == 0) return 0;
			}
			
			//half layers
			for (int layer = threshold1; layer <= threshold2; layer++) {
				sendcount = this.sendLayer(sendcount, (int) (layer * 2), layer, direction);
				if (sendcount == 0) return 0;
			}
			
			//less than half layers
			for (int layer = threshold2; layer <= viewDistance; layer++) {
				sendcount = this.sendLayer(sendcount, (int) (layer * 1.5), layer, direction);
				if (sendcount == 0) return 0;
			}
			
			//fill the square
			for (int a = -viewDistance; a <= viewDistance; a++) {
				for (int b = -viewDistance; b <= viewDistance; b++) {
					if (this.send(this.x + a, this.z + b)) {
						if (--sendcount == 0) return 0;
					}
				}
			}
		}
		return sendcount;
	}
		
	public static BlockFace getDirection(Player p) {
		int yaw = (int) p.getLocation().getYaw() - 90;
        while (yaw <= -180) yaw += 360;
        while (yaw > 180) yaw -= 360;
		switch ((int) yaw) {
		case 0 : return BlockFace.NORTH;
		case 45 : return BlockFace.NORTH_EAST;
		case 90 : return BlockFace.EAST;
		case 135 : return BlockFace.SOUTH_EAST;
		case 180 : return BlockFace.SOUTH;
		case -135 : return BlockFace.SOUTH_WEST;
		case -90 : return BlockFace.WEST;
		case -45 : return BlockFace.NORTH_WEST;
		}
		//Let's apply angle differences
		if (yaw >= -22.5 && yaw < 22.5) {
			return BlockFace.NORTH;
		} else if (yaw >= 22.5 && yaw < 67.5) {
			return BlockFace.NORTH_EAST;
		} else if (yaw >= 67.5 && yaw < 112.5) {
			return BlockFace.EAST;
		} else if (yaw >= 112.5 && yaw < 157.5) {
			return BlockFace.SOUTH_EAST;
		} else if (yaw >= -67.5 && yaw < -22.5) {
			return BlockFace.NORTH_WEST;
		} else if (yaw >= -112.5 && yaw < -67.5) {
			return BlockFace.WEST;
		} else if (yaw >= -157.5 && yaw < -112.5) {
			return BlockFace.SOUTH_WEST;
		} else {
			return BlockFace.SOUTH;
		}
	}	
	
	private static class MoveMod {
		private MoveMod(BlockFace direction, boolean right) {
			this.direction = direction;
			this.right = right;
		}
		
		public BlockFace direction;
		public boolean right;
			
		public void next(int midx, int midz, int curx, int curz, int limit) {
			if (Math.abs(midx - curx) == limit && Math.abs(midz - curz) == limit) {
				if (this.right) {
					if (direction == BlockFace.NORTH) {
						direction = BlockFace.EAST;
					} else if (direction == BlockFace.EAST) {
						direction = BlockFace.SOUTH;
					} else if (direction == BlockFace.SOUTH) {
						direction = BlockFace.WEST;
					} else if (direction == BlockFace.WEST) {
						direction = BlockFace.NORTH;
					}
				} else {
					if (direction == BlockFace.NORTH) {
						direction = BlockFace.WEST;
					} else if (direction == BlockFace.WEST) {
						direction = BlockFace.SOUTH;
					} else if (direction == BlockFace.SOUTH) {
						direction = BlockFace.EAST;
					} else if (direction == BlockFace.EAST) {
						direction = BlockFace.NORTH;
					}
				}
			}
		}
		
		public static MoveMod[] get(BlockFace direction) {
			MoveMod[] mods = new MoveMod[2];
			if (direction == BlockFace.NORTH) {
				mods[0] = new MoveMod(BlockFace.WEST, false);
				mods[1] = new MoveMod(BlockFace.EAST, true);
			} else if (direction == BlockFace.SOUTH) {
				mods[0] = new MoveMod(BlockFace.WEST, true);
				mods[1] = new MoveMod(BlockFace.EAST, false);
			} else if (direction == BlockFace.EAST) {
				mods[0] = new MoveMod(BlockFace.NORTH, false);
				mods[1] = new MoveMod(BlockFace.SOUTH, true);
			} else if (direction == BlockFace.WEST) {
				mods[0] = new MoveMod(BlockFace.NORTH, true);
				mods[1] = new MoveMod(BlockFace.SOUTH, false);
			} else if (direction == BlockFace.NORTH_EAST) {
				mods[0] = new MoveMod(BlockFace.WEST, false);
				mods[1] = new MoveMod(BlockFace.SOUTH, true);
			} else if (direction == BlockFace.SOUTH_EAST) {
				mods[0] = new MoveMod(BlockFace.WEST, true);
				mods[1] = new MoveMod(BlockFace.NORTH, false);
			} else if (direction == BlockFace.SOUTH_WEST) {
				mods[0] = new MoveMod(BlockFace.NORTH, true);
				mods[1] = new MoveMod(BlockFace.EAST, false);
			} else if (direction == BlockFace.NORTH_WEST) {
				mods[0] = new MoveMod(BlockFace.SOUTH, false);
				mods[1] = new MoveMod(BlockFace.EAST, true);
			}
			return mods;
		}	
	}
	private static class Chunk {
		public int x, z;
		
		public Chunk(int x, int z) {
			this.x = x;
			this.z = z;
		}
	    @Override
	    public int hashCode() {
	        int hash = 3;
	        hash = 41 * hash + (this.x ^ (this.x >> 16));
	        hash = 41 * hash + (this.z ^ (this.z >> 16));
	        return hash;
	    }	    
	    @Override
	    public boolean equals(Object object) {
	    	if (object == this) return true;
	    	if (object instanceof Chunk) {
	    		Chunk c = (Chunk) object;
	    		return c.x == this.x && c.z == this.z;
	    	}
	    	return false;
	    }
	    
	    public static Chunk fromPacket(Packet packet) {
			if (packet instanceof Packet51MapChunk) {
				Packet51MapChunk p = (Packet51MapChunk) packet;
				return new Chunk(p.a >> 4, p.c >> 4);
			} else if (packet instanceof Packet52MultiBlockChange) {
				Packet52MultiBlockChange p = (Packet52MultiBlockChange) packet;
				return new Chunk(p.a, p.b);
			} else if (packet instanceof Packet53BlockChange) {
				Packet53BlockChange p = (Packet53BlockChange) packet;
				return new Chunk(p.a >> 4, p.c >> 4);
			} else if (packet instanceof Packet130UpdateSign) {
				Packet130UpdateSign p = (Packet130UpdateSign) packet;
				return new Chunk(p.x >> 4, p.z >> 4);
			} else if (packet instanceof Packet23VehicleSpawn) {
				Packet23VehicleSpawn p = (Packet23VehicleSpawn) packet;
				return new Chunk(p.b / 512, p.d / 512);
			} else if (packet instanceof Packet24MobSpawn) {
				Packet24MobSpawn p = (Packet24MobSpawn) packet;
				return new Chunk(p.c / 512, p.e / 512);
			}
			return null;
	    }
	}

	//World downloading
	public boolean isDownloaded() {
		return this.isTerrainDownloaded;
	}
	public boolean handleChunkDownload(Packet packet) {
		Chunk c = Chunk.fromPacket(packet);
		if (c == null) return true;
		if (this.isNear(c.x, c.z, downloadSize)) {
			this.markSent(c);
			return true;
		} else {
			//buffer
			this.queue(packet, c);
			return false;
		}
	}
	public void finalizeTerrainDownload() {
		if (this.isTerrainDownloaded) return;
		for (int a = -downloadSize; a <= downloadSize; a++) {
			if (a <= -2 && a >= 2) {
				for (int b = -downloadSize; b <= downloadSize; b++) {
					if (b <= -2 && b >= 2) {
						Chunk c = new Chunk(this.x + a, this.z + b);
						this.markSent(c);
						BufferedChunk.sendChunk(this.player, c.x, c.z, true);
					}
				}
			}
		}
		this.isTerrainDownloaded = true;
	}

	public void saveSentChunks(DataOutputStream stream) throws IOException {
		ArrayList<Chunk> sent = new ArrayList<Chunk>();
		for (Map.Entry<Chunk, BufferedChunk> chunk : this.chunks.entrySet()) {
			if (chunk.getValue().hasFullChunkSent()) {
				sent.add(chunk.getKey());
			}
		}
		stream.writeInt(sent.size());
		for (Chunk c : sent) {
			stream.writeInt(c.x);
			stream.writeInt(c.z);
		}
	}
	public void loadSentChunks(DataInputStream stream) throws IOException {
		int sent = stream.readInt();
		for (int i = 0; i < sent; i++) {
			this.markSent(new Chunk(stream.readInt(), stream.readInt()));
		}
	}
}

