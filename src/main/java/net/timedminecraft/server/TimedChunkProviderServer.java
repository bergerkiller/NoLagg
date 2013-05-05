package net.timedminecraft.server;

import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Server;
import org.bukkit.generator.BlockPopulator;

import com.bergerkiller.bukkit.common.bases.ChunkProviderServerBase;
import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.reflection.classes.ChunkProviderServerRef;
import com.bergerkiller.bukkit.common.reflection.classes.ChunkRef;
import com.bergerkiller.bukkit.common.reflection.classes.ChunkRegionLoaderRef;
import com.bergerkiller.bukkit.common.reflection.classes.WorldServerRef;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.examine.PluginLogger;
import com.bergerkiller.bukkit.nolagg.examine.TaskMeasurement;

/*
 * Please ignore the package leading to the net.minecraft.server namespace
 * The author (me) got tired of all the reports with this showing up in stack traces
 * To keep things fair, all rights for this Class go to the Mojang team
 */
public class TimedChunkProviderServer extends ChunkProviderServerBase {

	public static void convert(org.bukkit.World world) {
		WorldServerRef.chunkProviderServer.set(Conversion.toWorldHandle.convert(world), new TimedChunkProviderServer(world));
	}

	public static void restore(org.bukkit.World world) {
		Object worldHandle = Conversion.toWorldHandle.convert(world);
		Object chunkProviderServer = WorldServerRef.chunkProviderServer.get(worldHandle);
		if (chunkProviderServer instanceof TimedChunkProviderServer) {
			TimedChunkProviderServer timed = (TimedChunkProviderServer) chunkProviderServer;
			timed.enabled = false;
			WorldServerRef.chunkProviderServer.set(worldHandle, timed.revert());
		}
	}

	private boolean enabled = true;
	private TaskMeasurement loadmeas, genmeas, unloadmeas;
	private long prevtime;

	private TimedChunkProviderServer(org.bukkit.World world) {
		super(world);
		loadmeas = PluginLogger.getServerOperation("Chunk provider", "Chunk load", "Loads chunks from file");
		genmeas = PluginLogger.getServerOperation("Chunk provider", "Chunk generate", "Generates the basic terrain");
		unloadmeas = PluginLogger.getServerOperation("Chunk provider", "Chunk unload", "Unloads chunks and saves them to file");
	}

	public boolean isEnabled() {
		return this.enabled && PluginLogger.isRunning();
	}

	@Override
	public Chunk loadBukkitChunk(int x, int z) {
		if (isEnabled()) {
			prevtime = System.nanoTime();
			final Chunk c = super.loadBukkitChunk(x, z);
			this.loadmeas.setTime(prevtime);
			return c;
		} else {
			return super.loadBukkitChunk(x, z);
		}
	}

	@Override
	public Chunk getBukkitChunkAt(int x, int z, Runnable taskWhenFinished) {
		if (isEnabled()) {
			WorldUtil.setChunkUnloading(world, x, z, false);
			Chunk chunk = WorldUtil.getChunk(world, x, z);
			boolean newChunk = false;

			if (chunk == null) {
				// If the chunk exists but isn't loaded do it async
				if (taskWhenFinished != null) {
					final Object chunkRegionLoader = CommonUtil.tryCast(ChunkProviderServerRef.chunkLoader.get(this), ChunkRegionLoaderRef.TEMPLATE.getType());
					if (chunkRegionLoader != null && ChunkRegionLoaderRef.chunkExists(chunkRegionLoader, world, x, z)) {
						ChunkRegionLoaderRef.queueChunkLoad(chunkRegionLoader, world, this, x, z, taskWhenFinished);
						return null;
					}
				}
				// CraftBukkit end

				chunk = this.loadBukkitChunk(x, z);
				if (chunk == null) {
					try {
						prevtime = System.nanoTime();
						chunk = this.generateChunk(x, z);
						this.genmeas.setTime(prevtime);
					} catch (Throwable throwable) {
						handleGeneratorError(throwable, x, z);
					}
					newChunk = true; // CraftBukkit
				}
				// Empty chunks should not be dealt with
				if (chunk != null) {
					final Object chunkHandle = Conversion.toChunkHandle.convert(chunk);
					WorldUtil.setChunk(world, x, z, chunk);
					if (chunk != null) {
						ChunkRef.addEntities(chunkHandle);
					}

					// CraftBukkit start
					Server server = WorldUtil.getServer(world);
					if (server != null) {
						/*
						 * If it's a new world, the first few chunks are generated
						 * inside the World constructor. We can't reliably alter
						 * that, so we have no way of creating a
						 * CraftWorld/CraftServer at that point.
						 */
						server.getPluginManager().callEvent(new org.bukkit.event.world.ChunkLoadEvent(chunk, newChunk));
					}
					// CraftBukkit end
					ChunkRef.loadNeighbours(chunkHandle, this, this, x, z);
				}
			}

			// CraftBukkit start - If we didn't need to load the chunk run the
			// callback now
			if (taskWhenFinished != null) {
				taskWhenFinished.run();
			}
			// CraftBukkit end

			return chunk;
		} else {
			return super.getBukkitChunkAt(x, z, taskWhenFinished);
		}
	}

	@Override
	public void onPopulate(Chunk chunk, BlockPopulator populator, Random random) {
		if (isEnabled()) {
			// get associated task
			final String classname = populator.getClass().getSimpleName();
			final String loc = populator.getClass().getName();
			TaskMeasurement tm = PluginLogger.getServerOperation("Chunk populators", classname, loc);
			prevtime = System.nanoTime();
			super.onPopulate(chunk, populator, random);
			tm.setTime(prevtime);
		} else {
			super.onPopulate(chunk, populator, random);
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
