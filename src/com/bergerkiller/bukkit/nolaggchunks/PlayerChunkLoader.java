package com.bergerkiller.bukkit.nolaggchunks;

import java.util.HashSet;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.nolagg.ChunkHandler;

public class PlayerChunkLoader {
	private static WeakHashMap<Player, PlayerChunkLoader> loaders = new WeakHashMap<Player, PlayerChunkLoader>();
	private static synchronized PlayerChunkLoader get(Player player) {
		PlayerChunkLoader loader = loaders.get(player);
		if (loader == null) {
			loader = new PlayerChunkLoader(player);
			loaders.put(player, loader);
		}
		return loader;
	}
	
	private static int taskid = -1;
	private static int rate = 1;
	public static void init(int sendinterval, int sendrate) {
		rate = sendrate;
		if (rate <= 0) return;
		taskid = NoLaggChunks.plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(NoLaggChunks.plugin, new Runnable() {
			public void run() {
				PlayerChunkLoader.sendAll(rate);
			}
		}, 0, sendinterval);
	}
	public static void deinit() {
		if (taskid != -1) {
			NoLaggChunks.plugin.getServer().getScheduler().cancelTask(taskid);
		}
	}
	
	
	public static void clearAll(org.bukkit.Chunk chunk) {
		for (PlayerChunkLoader loader : loaders.values()) {
			loader.clear(chunk);
		}
	}
	public static void clear(Player player, int x, int z) {
		get(player).clear(player.getWorld(), x, z);
	}
	public static void clear(Player player, org.bukkit.Chunk chunk) {
		get(player).clear(chunk);
	}
	
	public static void sendAll(int sendcount) {
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			send(player, sendcount);
		}
	}
	
	public static void send(Player player, int sendcount) {
		PlayerChunkLoader loader = get(player);
		loader.send(sendcount, player);
	}
	public static void update(Player player, int newcx, int newcz, World newworld) {
		PlayerChunkLoader loader = get(player);
		loader.updateChunk(newcx, newcz, newworld);
	}
	
	private class Chunk {
		public int x, z;
		
		public Chunk(int x, int z) {
			this.x = x;
			this.z = z;
		}
		public boolean isNear(int x, int z) {
			return Math.abs(this.x - x) <= view && Math.abs(this.z - z) <= view;
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
	}
	
	private static int view = Bukkit.getServer().getViewDistance();
	private int cx, cz;
	private World world;
	private HashSet<Chunk> sentChunks = new HashSet<Chunk>();
	
	private PlayerChunkLoader(Player player) {
		this.world = player.getWorld();
		this.cx = player.getLocation().getBlockX() >> 4;
		this.cz = player.getLocation().getBlockZ() >> 4;
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
	
	public boolean isSent(int cx, int cz) {
		return sentChunks.contains(new Chunk(cx, cz));
	}
	
	public synchronized void clear() {
		sentChunks.clear();
	}
	public synchronized void clear(org.bukkit.Chunk chunk) {
		sentChunks.remove(new Chunk(chunk.getX(), chunk.getZ()));
	}
	public void clear(World world, int x, int z) {
		this.clear(world.getChunkAt(x, z));
	}
	
	public void updateChunk(int cx, int cz, World world) {
		if (this.world != world) {
			this.world = world;
			sentChunks.clear();
			this.cx = cx;
			this.cz = cz;
		} else if (cx != this.cx || cz != this.cz) {
			this.cx = cx;
			this.cz = cz;
			//remove chunks no longer visible
			synchronized (this) {
				Chunk[] sent = sentChunks.toArray(new Chunk[0]);
				sentChunks.clear();
				for (Chunk c : sent) {
					if (c.isNear(cx, cz)) {
						sentChunks.add(c);
					}
				}
			}
		}
	}
	
	private static class MoveMod {
		private MoveMod(BlockFace direction, boolean right) {
			this.x = direction.getModX();
			this.z = direction.getModZ();
			this.right = right;
		}
		
		public int x;
		public int z;
		public boolean right;
			
		public void next(int midx, int midz, int curx, int curz, int limit) {
			if (Math.abs(midx - curx) == limit && Math.abs(midz - curz) == limit) {
				if (this.right) {
					if (x == 1 && z == 0) {
					    z = 1;
					    x = 0;
					} else if (x == 0 && z == 1) {
						x = -1;
						z = 0;
					} else if (x == -1 && z == 0) {
					    z = -1;
					    x = 0;
					} else if (x == 0 && z == -1) {
						x = 1;
						z = 0;
					}
				} else {
					if (x == 1 && z == 0) {
					    z = -1;
					    x = 0;
					} else if (x == 0 && z == -1) {
						x = -1;
						z = 0;
					} else if (x == -1 && z == 0) {
					    z = 1;
					    x = 0;
					} else if (x == 0 && z == 1) {
						x = 1;
						z = 0;
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
	
	private int failedCount = 0;
	public synchronized boolean send(int cx, int cz, Player to) {
		if (to != null && !isSent(cx, cz)) {
			NLPacketListener.spoutAllowChunk = true;
			boolean succ = ChunkHandler.send(cx, cz, to);
			NLPacketListener.spoutAllowChunk = false;
			if (succ) {
				sentChunks.add(new Chunk(cx, cz));
				return true;
			} else {
				failedCount++;
				if (failedCount == 20) {
				    System.out.println("[NoLagg] failed to send 20 chunks to player " + to.getName() + ", the chunk add-on may have to be disabled!");
				    failedCount = 0;
				}
			}
		}
		return false;
	}
	
	public int sendLayer(int sendcount, int sendlimit, int layer, BlockFace direction, Player to) {
		//get modifiers from direction
		MoveMod[] mods = MoveMod.get(direction);
		//Get the chunk to start at
		int startx = cx + direction.getModX() * layer;
		int startz = cz + direction.getModZ() * layer;
		//Send starter chunk
		if (this.send(startx, startz, to)) {
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
			x1 += mods[0].x;
			z1 += mods[0].z;
			x2 += mods[1].x;
			z2 += mods[1].z;
			//mod update
			mods[0].next(cx, cz, x1, z1, layer);
			mods[1].next(cx, cz, x2, z2, layer);
			//got till the end?
			if (x1 == x2 && z1 == z2) {
				if (this.send(x1, z1, to)) {
					--sendcount;
				}
				return sendcount;
			} else {
				if (this.send(x1, z1, to)) {
					if (--sendcount == 0) return 0;
				}
				if (this.send(x2, z2, to)) {
					if (--sendcount == 0) return 0;
				}
			}
		}
	}
	
	public void send(int sendcount, Player to) {
		send(getDirection(to), sendcount, to);
	}
	public void send(BlockFace direction, int sendcount, Player to) {
		if (sendcount > 0) {
			//main chunk
			if (this.send(cx, cz, to)) {
				if (--sendcount == 0) return;
			}
			
			final int threshold1 = 3; //to this layer full layers chunks are sent, after half
			final int threshold2 = 5; //at this layer less than half are sent
			
			//full layers
			for (int layer = 1; layer < threshold1; layer++) {
				sendcount = sendLayer(sendcount, layer * 4, layer, direction, to);
				if (sendcount == 0) return;
			}
			
			//half layers
			for (int layer = threshold1; layer <= threshold2; layer++) {
				sendcount = sendLayer(sendcount, (int) (layer * 2), layer, direction, to);
				if (sendcount == 0) return;
			}
			
			//less than half layers
			for (int layer = threshold2; layer <= view; layer++) {
				sendcount = sendLayer(sendcount, (int) (layer * 1.5), layer, direction, to);
				if (sendcount == 0) return;
			}
						
			//remainder
			for (int a = -view; a <= view; a++) {
				for (int b = -view; b <= view; b++) {
					if (this.send(cx + a, cz + b, to)) {
						if (--sendcount == 0) return;
					}
				}
			}
		}
	}
	
}
