package com.bergerkiller.bukkit.nolagg;

import java.util.HashSet;
import java.util.WeakHashMap;

import net.minecraft.server.Packet51MapChunk;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.*;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class PlayerChunkLoader {
	private static WeakHashMap<Player, PlayerChunkLoader> loaders = new WeakHashMap<Player, PlayerChunkLoader>();
	private static PlayerChunkLoader get(Player player) {
		PlayerChunkLoader loader = loaders.get(player);
		if (loader == null) {
			loader = new PlayerChunkLoader(player);
			loaders.put(player, loader);
		}
		return loader;
	}
	
	private static int taskid = -1;
	public static void init(int rate) {
		if (rate <= 0) return;
		taskid = NoLagg.plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(NoLagg.plugin, new Runnable() {
			public void run() {
				for (World w : Bukkit.getServer().getWorlds()) {
					for (org.bukkit.Chunk c : w.getLoadedChunks()) {
						((CraftChunk) c).getHandle();
					}
				}
				PlayerChunkLoader.sendAll(1);
			}
		}, 0, rate);
	}
	public static void deinit() {
		if (taskid != -1) {
			NoLagg.plugin.getServer().getScheduler().cancelTask(taskid);
		}
	}
	
	
	
	public static void clearAll(org.bukkit.Chunk chunk) {
		for (PlayerChunkLoader loader : loaders.values()) {
			loader.clear(chunk);
		}
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
	        int hash = 7;
	        hash = 63 * hash + (this.x ^ (this.x >> 16));
	        hash = 63 * hash + (this.z ^ (this.z >> 16));
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
	
	public boolean send(int cx, int cz, Player to) {
		if (to != null && !isSent(cx, cz)) {
			Packet51MapChunk packet = new Packet51MapChunk(cx * 16, 0, cz * 16, 16, 128, 16, ((CraftWorld) world).getHandle());
			((CraftPlayer) to).getHandle().netServerHandler.sendPacket(packet);
			sentChunks.add(new Chunk(cx, cz));
			return true;
		} else {
			return false;
		}
	}
	
	public void clear() {
		sentChunks.clear();
	}
	public void clear(org.bukkit.Chunk chunk) {
		sentChunks.remove(new Chunk(chunk.getX(), chunk.getZ()));
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
			Chunk[] sent = sentChunks.toArray(new Chunk[0]);
			sentChunks.clear();
			for (Chunk c : sent) {
				if (c.isNear(cx, cz)) {
					sentChunks.add(c);
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
			//8 surrounding chunks
			for (int a = -1; a < 2; a++) {
				for (int b = -1; b < 2; b++) {
					if (a != 0 || b != 0) {
						if (this.send(cx + a, cz + b, to)) {
							if (--sendcount == 0) return;
						}
					}
				}
			}
			//get modifiers from direction
			int mod1x = 0;
			int mod1z = 0;
			int mod2x = 0;
			int mod2z = 0;
			if (direction == BlockFace.NORTH || direction == BlockFace.SOUTH) {
				mod1z = -1;
				mod2z = 1;
			} else if (direction == BlockFace.EAST || direction == BlockFace.WEST) {
				mod1x = -1;
				mod2x = 1;
			} else if (direction == BlockFace.NORTH_EAST) {
				mod1z = 1;
				mod2x = 1;
			} else if (direction == BlockFace.SOUTH_EAST) {
				mod1x = -1;
				mod2z = 1;
			} else if (direction == BlockFace.SOUTH_WEST) {
				mod1x = -1;
				mod2z = -1;
			} else if (direction == BlockFace.NORTH_WEST) {
				mod1x = 1;
				mod2z = -1;
			}
			
			//ray tracing using direction
			for (int i = 1; i < view; i++) {
				int startx = cx + direction.getModX() * i;
				int startz = cz + direction.getModZ() * i;
				for (int j = 0; j < i; j++) {
					//i is the forwards index
					//j is the sideways index
					int x1 = startx + mod1x * j;
					int z1 = startz + mod1z * j;
					if (this.send(x1, z1, to)) {
						if (--sendcount == 0) return;
					}
					int x2 = startx + mod2x * j;
					int z2 = startz + mod2z * j;
					if (this.send(x2, z2, to)) {
						if (--sendcount == 0) return;
					}
				}
			}
			
		}
	}
	
}
