package com.bergerkiller.bukkit.nolagg.examine;

import java.util.Random;
import java.util.logging.Level;

import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.generator.BlockPopulator;

import com.bergerkiller.bukkit.common.SafeField;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

import net.minecraft.server.BlockSand;
import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkProviderServer;
import net.minecraft.server.IChunkLoader;
import net.minecraft.server.IChunkProvider;
import net.minecraft.server.WorldServer;

public class TimedChunkProviderServer extends ChunkProviderServer {

	public static SafeField<IChunkLoader> field;
		
	public static boolean initFields() {
		if (field == null) {
			field = new SafeField<IChunkLoader>(ChunkProviderServer.class, "e");
			if (field.isValid()) {
				return true;
			} else {
				NoLaggExamine.plugin.log(Level.WARNING, "Failed to hook into chunk loader; chunk load times will not be examine!");
				return false;
			}
		} else {
			return field.isValid();
		}
	}
	
	public static void transfer(ChunkProviderServer to, WorldServer world) {
		to.chunkList = world.chunkProviderServer.chunkList;
		to.chunkProvider = world.chunkProviderServer.chunkProvider;
		to.chunks = world.chunkProviderServer.chunks;
		if (world.chunkProviderServer instanceof TimedChunkProviderServer) {
			((TimedChunkProviderServer) world.chunkProviderServer).enabled = false;
		}
		world.chunkProvider = world.chunkProviderServer = to;
	}
	
	public static void convert(org.bukkit.World world) {
		convert(WorldUtil.getNative(world));
	}

	public static void convert(WorldServer world) {
		if (initFields()) {
			try {
				IChunkLoader old = field.get(world.chunkProviderServer);
				transfer(new TimedChunkProviderServer(world, old, world.chunkProviderServer.chunkProvider), world);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	public static void restore(org.bukkit.World world) {
		restore(WorldUtil.getNative(world));
	}
	
	public static void restore(WorldServer world) {
		if (initFields()) {
			try {
				IChunkLoader old = field.get(world.chunkProviderServer);
				transfer(new ChunkProviderServer(world, old, world.chunkProviderServer.chunkProvider), world);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	private boolean enabled = true;
	private TaskMeasurement loadmeas, genmeas;
	private long prevtime;
	
	private TimedChunkProviderServer(WorldServer worldserver, IChunkLoader ichunkloader, IChunkProvider ichunkprovider) {
		super(worldserver, ichunkloader, ichunkprovider);
		loadmeas = PluginLogger.getServerOperation("Chunk creation", "Chunk load", "Loads chunks from file");
		genmeas = PluginLogger.getServerOperation("Chunk creation", "Chunk generate", "Generates the basic terrain");
	}
	
	@Override
	public Chunk loadChunk(int x, int z) {
		if (enabled && PluginLogger.isRunning()) {
			prevtime = System.nanoTime();
			Chunk c = super.loadChunk(x, z);
			this.loadmeas.setTime(prevtime);
			return c;
		} else {
			return super.loadChunk(x, z);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
    public Chunk getChunkAt(int i, int j) {
		if (enabled && PluginLogger.isRunning()) {
	        // CraftBukkit start
	        this.unloadQueue.remove(i, j);
	        Chunk chunk = (Chunk) this.chunks.get(i, j);
	        boolean newChunk = false;
	        // CraftBukkit end

	        if (chunk == null) {
	            chunk = this.loadChunk(i, j);
	            if (chunk == null) {
	                if (this.chunkProvider == null) {
	                    chunk = this.emptyChunk;
	                } else {
	                	prevtime = System.nanoTime();
	                    chunk = this.chunkProvider.getOrCreateChunk(i, j);
	                    this.genmeas.setTime(prevtime);
	                }
	                newChunk = true; // CraftBukkit
	            }

	            this.chunks.put(i, j, chunk); // CraftBukkit
	            this.chunkList.add(chunk);
	            if (chunk != null) {
	                chunk.addEntities();
	            }

	            // CraftBukkit start
	            org.bukkit.Server server = this.world.getServer();
	            if (server != null) {
	                /*
	                 * If it's a new world, the first few chunks are generated inside
	                 * the World constructor. We can't reliably alter that, so we have
	                 * no way of creating a CraftWorld/CraftServer at that point.
	                 */
	                server.getPluginManager().callEvent(new ChunkLoadEvent(chunk.bukkitChunk, newChunk));
	            }
	            // CraftBukkit end

	            chunk.a(this, this, i, j);
	        }

	        return chunk;
		} else {
			return super.getChunkAt(i, j);
		}
    }
	
	@Override
    public void getChunkAt(IChunkProvider ichunkprovider, int i, int j) {
		if (enabled && PluginLogger.isRunning()) {
	        Chunk chunk = this.getOrCreateChunk(i, j);

	        if (!chunk.done) {
	            chunk.done = true;
	            if (this.chunkProvider != null) {
	                this.chunkProvider.getChunkAt(ichunkprovider, i, j);

	                // CraftBukkit start
	                BlockSand.instaFall = true;
	                Random random = new Random();
	                random.setSeed(world.getSeed());
	                long xRand = random.nextLong() / 2L * 2L + 1L;
	                long zRand = random.nextLong() / 2L * 2L + 1L;
	                random.setSeed((long) i * xRand + (long) j * zRand ^ world.getSeed());

	                org.bukkit.World world = this.world.getWorld();
	                if (world != null) {
	                    for (BlockPopulator populator : world.getPopulators()) {
	                    	//get associated task
	                    	String classname = populator.getClass().getSimpleName();
	                    	String loc = populator.getClass().getName();
	                    	TaskMeasurement tm = PluginLogger.getServerOperation("Chunk populators", classname, loc);
	                    	prevtime = System.nanoTime();
	                        populator.populate(world, random, chunk.bukkitChunk);
	                        tm.setTime(prevtime);
	                    }
	                }
	                BlockSand.instaFall = false;
	                this.world.getServer().getPluginManager().callEvent(new ChunkPopulateEvent(chunk.bukkitChunk));
	                // CraftBukkit end

	                chunk.e();
	            }
	        }
		} else {
			super.getChunkAt(ichunkprovider, i, j);
		}
    }

}
