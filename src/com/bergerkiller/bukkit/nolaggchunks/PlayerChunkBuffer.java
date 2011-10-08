package com.bergerkiller.bukkit.nolaggchunks;

import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.Packet;
import net.minecraft.server.Packet130UpdateSign;
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
		this.sendInterval = defaultSendInterval.get(player);
		this.sendRate = defaultSendRate.get(player);
		this.downloadSize = defaultDownloadSize.get(player);
		this.view = defaultViewDistance.get(player);
	}
	
	public static PlayerDefault<Integer> defaultSendInterval = new PlayerDefault<Integer>(4);
	public static PlayerDefault<Integer> defaultSendRate = new PlayerDefault<Integer>(1);
	public static PlayerDefault<Integer> defaultDownloadSize = new PlayerDefault<Integer>(5);
	public static PlayerDefault<Integer> defaultViewDistance = new PlayerDefault<Integer>(12);

	private ConcurrentHashMap<Chunk, BufferedChunk> chunks = new ConcurrentHashMap<Chunk, BufferedChunk>();
	private int x, z;
	public World world;
	public int view = 5;
	public int downloadSize = 6;
	private int intervalCounter = 0;
	
	public int sendInterval;
	public int sendRate;
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
	public int sendNext(int limit) {
		intervalCounter++;
		if (limit > 0 && intervalCounter >= sendInterval) {
			//Global - max rate per tick
			int rate = this.sendRate;
			if (limit < rate) rate = limit;
			intervalCounter = 0;
			return this.send(rate);
		}
		return 0;
	}
	
	/*
	 * General coding
	 */
	private BufferedChunk get(int cx, int cz) {
		return this.get(new Chunk(cx, cz));
	}
	private synchronized BufferedChunk get(Chunk chunk) {
		BufferedChunk bc = this.chunks.get(chunk);
		if (bc == null) {
			bc = new BufferedChunk(chunk.x, chunk.z);
			this.chunks.put(chunk, bc);
		}
		return bc;
	}
	
	public boolean isNear(int cx, int cz, int view) {
		return Math.abs(this.x - cx) <= view && Math.abs(this.z - cz) <= view;
	}
	
	/**
	 * Queue a packet
	 * @param packet
	 * @param chunk
	 * @return If the packet is handled by this buffer and should not be sent
	 */
	private boolean queue(Packet packet, Chunk chunk) {
		if (this.isNear(chunk.x, chunk.z, maxview)) {
			BufferedChunk c = get(chunk);
			if (BufferedChunk.isChunk(packet)) {
				c.queue(packet);
				return true;
			} else {
				if (c.isQueueingChunk()) {
					//We are queuing a chunk, add it to the queue
					c.queue(packet);
					return true;
				} else if (c.hasFullChunkSent()) {
					//Chunk is present, just send it right away
					return false;
				} else {
					//Chunk not yet received...yet we got a minor chunk update
					//Time to load this chunk
					c.queueChunk(this.world);
					c.queue(packet);
					return true;
				}
			}
		} else {
			//Out of reach, handled
			return true;
		}
	}
	public boolean queue(Packet packet) {
		return this.queue(packet, Chunk.fromPacket(packet));
	}
	
	public void queueAllChunks() {
		for (int a = -view; a <= view; a++) {
			for (int b = -view; b <= view; b++) {
				BufferedChunk c = this.get(this.x + a, this.z + b);
				if (!c.hasFullChunkSent()) {
					c.queueChunk(this.world);
				}
			}
		}
	}
	
	public void update() {
		this.update(this.player.getLocation());
	}
	public synchronized void update(Location newLocation) {
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
			this.chunks.clear();
			this.isTerrainDownloaded = false;
		}
	}
	
	private void markSent(Chunk chunk) {
		BufferedChunk b = new BufferedChunk(chunk.x, chunk.z);
		b.setFullChunkSent();
		this.chunks.put(chunk, b);
	}
	
	/*
	 * Send coding
	 */
	public boolean send(int cx, int cz) {
		if (this.isNear(cx, cz, this.view)) {
			BufferedChunk c = get(cx, cz);
			if (c != null && !c.isEmpty()) {
				c.send(this.player);
				return true;
			}
		}
		return false;
	}
	public int sendLayer(int sendcount, int sendlimit, int layer, BlockFace direction) {
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
	public int send(int sendcount) {
		return send(getDirection(this.player), sendcount);
	}
	public int send(BlockFace direction, int sendcount) {
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
			for (int layer = threshold2; layer <= view; layer++) {
				sendcount = this.sendLayer(sendcount, (int) (layer * 1.5), layer, direction);
				if (sendcount == 0) return 0;
			}
			
			//fill the square
			for (int a = -this.view; a <= this.view; a++) {
				for (int b = -this.view; b <= this.view; b++) {
					if (this.send(this.x + a, this.z + b)) {
						if (--sendcount == 0) return 0;
					}
				}
			}
					
//			//remove non-sent chunks
//			ArrayList<Chunk> toRemove = new ArrayList<Chunk>();
//			for (Map.Entry<Chunk, BufferedChunk> entry : this.chunks.entrySet()) {
//				if (!entry.getValue().isNear(this.x, this.z, this.view)) {
//					toRemove.add(entry.getKey());
//				}
//			}
//			for (Chunk c : toRemove) {
//				this.chunks.remove(c);
//			}
		}
		return sendcount;
	}
		
	/*
	 * The highest value has highest priority!
	 * Priority value decreases with the amount of ahead chunks
	 */
	public int getPriority() {
		int priority = 0;
		for (BufferedChunk bc : this.chunks.values()) {
			if (bc.isQueueingChunk()) {
				priority += this.view - Math.max(Math.abs(bc.x - this.x), Math.abs(bc.z - this.z));
			}
		}
		return priority;
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
		if (c != null && Math.abs(this.x - c.x) <= this.downloadSize && Math.abs(this.z - c.z) <= this.downloadSize) {
			this.markSent(c);
			return true;
		} else {
			//buffer
			this.queue(packet, c);
			return false;
		}
	}
	public void finalizeTerrainDownload() {
		for (int a = -this.downloadSize; a <= this.downloadSize; a++) {
			if (a > -2 && a < 2) {
				for (int b = -this.downloadSize; b <= this.downloadSize; b++) {
					if (b > -2 && b < 2) {
						Chunk c = new Chunk(this.x + a, this.z + b);
						BufferedChunk.sendChunk(this.player, c.x, c.z, true);
						this.markSent(c);
					}
				}
			}
		}
		this.isTerrainDownloaded = true;
	}
}
