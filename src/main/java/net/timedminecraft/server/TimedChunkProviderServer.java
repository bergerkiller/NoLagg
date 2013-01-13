package net.timedminecraft.server;

import java.util.Random;

import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_4_6.chunkio.ChunkIOExecutor;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.generator.BlockPopulator;

import com.bergerkiller.bukkit.common.reflection.classes.ChunkProviderServerRef;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.NativeUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.examine.PluginLogger;
import com.bergerkiller.bukkit.nolagg.examine.TaskMeasurement;

import net.minecraft.server.v1_4_6.BlockSand;
import net.minecraft.server.v1_4_6.Chunk;
import net.minecraft.server.v1_4_6.ChunkProviderServer;
import net.minecraft.server.v1_4_6.ChunkRegionLoader;
import net.minecraft.server.v1_4_6.CrashReport;
import net.minecraft.server.v1_4_6.CrashReportSystemDetails;
import net.minecraft.server.v1_4_6.IChunkLoader;
import net.minecraft.server.v1_4_6.IChunkProvider;
import net.minecraft.server.v1_4_6.ReportedException;
import net.minecraft.server.v1_4_6.WorldServer;

/*
 * Please ignore the package leading to the net.minecraft.server namespace
 * The author (me) got tired of all the reports with this showing up in stack traces
 * To keep things fair, all rights for this Class go to the Mojang team
 */
public class TimedChunkProviderServer extends ChunkProviderServer {
	private static void transfer(ChunkProviderServer to, org.bukkit.World bworld) {
		WorldServer world = NativeUtil.getNative(bworld);
		try {
			ChunkProviderServerRef.TEMPLATE.transfer(world.chunkProviderServer, to);
			if (world.chunkProviderServer instanceof TimedChunkProviderServer) {
				((TimedChunkProviderServer) world.chunkProviderServer).enabled = false;
			}
			world.chunkProvider = world.chunkProviderServer = to;
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static void convert(org.bukkit.World world) {
		transfer(new TimedChunkProviderServer(world), world);
	}

	public static void restore(org.bukkit.World world) {
		transfer(new ChunkProviderServer(NativeUtil.getNative(world), null, null), world);
	}

	private boolean enabled = true;
	private TaskMeasurement loadmeas, genmeas, unloadmeas;
	private long prevtime;

	private TimedChunkProviderServer(org.bukkit.World world) {
		super(NativeUtil.getNative(world), null, null);
		loadmeas = PluginLogger.getServerOperation("Chunk provider", "Chunk load", "Loads chunks from file");
		genmeas = PluginLogger.getServerOperation("Chunk provider", "Chunk generate", "Generates the basic terrain");
		unloadmeas = PluginLogger.getServerOperation("Chunk provider", "Chunk unload", "Unloads chunks and saves them to file");
	}

	public boolean isEnabled() {
		return this.enabled && PluginLogger.isRunning();
	}

	@Override
	public Chunk loadChunk(int x, int z) {
		if (isEnabled()) {
			prevtime = System.nanoTime();
			Chunk c = super.loadChunk(x, z);
			this.loadmeas.setTime(prevtime);
			return c;
		} else {
			return super.loadChunk(x, z);
		}
	}

	@Override
	public Chunk getChunkAt(int i, int j, Runnable runnable) {
		if (isEnabled()) {
			WorldUtil.setChunkUnloading(this.world.getWorld(), i, j, false);
			Chunk chunk = NativeUtil.getNative(WorldUtil.getChunk(this.world.getWorld(), i, j));
	        boolean newChunk = false;
	        ChunkRegionLoader loader = null;
	        IChunkLoader l = ChunkProviderServerRef.chunkLoader.get(this);

	        if (l instanceof ChunkRegionLoader) {
	            loader = (ChunkRegionLoader) l;
	        }

	        // If the chunk exists but isn't loaded do it async
	        if (chunk == null && runnable != null && loader != null && loader.chunkExists(this.world, i, j)) {
	            ChunkIOExecutor.queueChunkLoad(this.world, loader, this, i, j, runnable);
	            return null;
	        }
	        // CraftBukkit end

	        if (chunk == null) {
	            chunk = this.loadChunk(i, j);
	            if (chunk == null) {
	                if (this.chunkProvider == null) {
	                    chunk = this.emptyChunk;
	                } else {
	                    try {
							prevtime = System.nanoTime();
							chunk = this.chunkProvider.getOrCreateChunk(i, j);
							this.genmeas.setTime(prevtime);
	                    } catch (Throwable throwable) {
	                        CrashReport crashreport = CrashReport.a(throwable, "Exception generating new chunk");
	                        CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Chunk to be generated");

	                        crashreportsystemdetails.a("Location", String.format("%d,%d", new Object[] { Integer.valueOf(i), Integer.valueOf(j)}));
	                        crashreportsystemdetails.a("Position hash", Long.valueOf(MathUtil.longHashToLong(i, j)));
	                        crashreportsystemdetails.a("Generator", this.chunkProvider.getName());
	                        throw new ReportedException(crashreport);
	                    }
	                }
	                newChunk = true; // CraftBukkit
	            }

	            WorldUtil.setChunk(this.world.getWorld(), i, j, NativeUtil.getChunk(chunk));
	            if (chunk != null) {
	                chunk.addEntities();
	            }

	            // CraftBukkit start
	            Server server = this.world.getServer();
	            if (server != null) {
	                /*
	                 * If it's a new world, the first few chunks are generated inside
	                 * the World constructor. We can't reliably alter that, so we have
	                 * no way of creating a CraftWorld/CraftServer at that point.
	                 */
	                server.getPluginManager().callEvent(new org.bukkit.event.world.ChunkLoadEvent(chunk.bukkitChunk, newChunk));
	            }
	            // CraftBukkit end

	            chunk.a(this, this, i, j);
	        }

	        // CraftBukkit start - If we didn't need to load the chunk run the callback now
	        if (runnable != null) {
	            runnable.run();
	        }
	        // CraftBukkit end

			return chunk;
		} else {
			return super.getChunkAt(i, j, runnable);
		}
	}

	@Override
	public void getChunkAt(IChunkProvider ichunkprovider, int i, int j) {
		if (isEnabled()) {
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
							// get associated task
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

	@Override
	public boolean unloadChunks() {
		if (isEnabled()) {
			prevtime = System.nanoTime();
			boolean rval = super.unloadChunks();
			unloadmeas.setTime(prevtime);
			return rval;
		} else {
			return super.unloadChunks();
		}
	}
}
