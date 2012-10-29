package com.bergerkiller.bukkit.nolagg.lighting;

import java.io.File;
import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.craftbukkit.util.LongHash;
import org.bukkit.craftbukkit.util.LongHashSet;

import net.minecraft.server.Block;
import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkCoordIntPair;
import net.minecraft.server.ChunkSection;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.EnumSkyBlock;
import net.minecraft.server.RegionFile;
import net.minecraft.server.WorldServer;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.Operation;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.reflection.classes.RegionFileCacheRef;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class LightingFixThread extends AsyncTask {
	private static AsyncTask task;
	private static LinkedHashSet<Chunk> toFix = new LinkedHashSet<Chunk>();
	private static List<FixOperation> next = new ArrayList<FixOperation>();
	private static LinkedList<PendingChunk> pendingChunks = new LinkedList<PendingChunk>();
	private static int pending = 0;
	private static Task chunkLoadTask;

	public static int getPendingSize() {
		return pending;
	}

	private static void addForFixing(org.bukkit.Chunk chunk) {
		addForFixing(WorldUtil.getNative(chunk));
	}

	private static void addForFixing(Chunk chunk) {
		synchronized (toFix) {
			toFix.add(chunk);
			if (task == null) {
				task = new LightingFixThread().start(true);
			}
		}
		// get rid of current sending requests for this chunk
		ChunkCoordIntPair p = new ChunkCoordIntPair(chunk.x, chunk.z);
		for (EntityPlayer ep : CommonUtil.getOnlinePlayers()) {
			if (ep != null && ep.world == chunk.world && ep.chunkCoordIntPairQueue != null) {
				if (EntityUtil.isNearChunk(ep, chunk.x, chunk.z, CommonUtil.view)) {
					ep.chunkCoordIntPairQueue.remove(p);
				}
			}
		}
	}

	public static File getRegionFolder(World world) {
		StringBuilder path = new StringBuilder();
		// Main root path
		path.append(Bukkit.getWorldContainer()).append(File.separator).append(world.getName());
		// Special dim folder for nether and the_end
		if (world.getEnvironment() == Environment.NETHER) {
			path.append(File.separator).append("DIM-1");
		} else if (world.getEnvironment() == Environment.THE_END) {
			path.append(File.separator).append("DIM1");
		}
		// Final region folder appending
		path.append(File.separator).append("region");
		// Convert path to file
		return new File(path.toString());
	}

	public static void fix(World world) {
		LongHashSet chunks = new LongHashSet();
		// Add the initial chunks that are already loaded
		for (long value : WorldUtil.getNative(world).chunkProviderServer.chunks.keySet()) {
			chunks.add(value);
		}
		// Get the region folder to look in
		File regionFolder = getRegionFolder(world);
		if (regionFolder.exists()) {
			// Loop through all region files of the world
			int dx, dz;
			int rx, rz;
			for (String regionFileName : regionFolder.list()) {
				// Validate file
				File file = new File(regionFolder, regionFileName);
				if (!file.isFile() || !file.exists()) {
					continue;
				}
				String[] parts = regionFileName.split("\\.");
				if (parts.length != 4 || !parts[0].equals("r") || !parts[3].equals("mca")) {
					continue;
				}
				// Obtain the chunk offset of this region file
				try {
					rx = Integer.parseInt(parts[1]) << 5;
					rz = Integer.parseInt(parts[2]) << 5;
				} catch (Exception ex) {
					continue;
				}
				// Is it contained in the cache?
				Reference<RegionFile> ref = RegionFileCacheRef.FILES.get(file);
				RegionFile reg = null;
				if (ref != null) {
					reg = ref.get();
				}
				if (reg == null) {
					// Manually load this region file and close it (we don't use it to load chunks)
					reg = new RegionFile(file);
					reg.c();
				}
				// Obtain all generated chunks in this region file
				for (dx = 0; dx < 32; dx++) {
					for (dz = 0; dz < 32; dz++) {
						if (reg.c(dx, dz)) {
							// Region file exists - add it
							chunks.add(rx + dx, rz + dz);
						}
					}
				}
			}
		}
		for (long key : chunks.toArray()) {
			fix(world, LongHash.msw(key), LongHash.lsw(key), true);
		}
	}

	public static void fix(org.bukkit.Chunk chunk) {
		pending++;
		addForFixing(chunk);
	}

	public static void fix(World world, int x, int z, boolean allowLoad) {
		pending++;
		org.bukkit.Chunk chunk = WorldUtil.getChunk(world, x, z);
		if (chunk != null) {
			// Already loaded, no need to queue it
			addForFixing(chunk);
			return;
		}
		// Load this chunk in a tick task
		pendingChunks.add(new PendingChunk(world, x, z));
		if (chunkLoadTask == null && allowLoad) {
			chunkLoadTask = new Task(NoLagg.plugin) {
				public void run() {
					// If nothing left to do, abort this task
					if (pendingChunks.isEmpty()) {
						stop();
						chunkLoadTask = null;
						return;
					}
					// Ignore this run if fixing is still being performed
					synchronized (toFix) {
						if (toFix.size() > 500) {
							return;
						}
					}
					// Load a maximum of 20 chunks
					final int loadRate = 20;
					for (int i = 0; i < loadRate && !pendingChunks.isEmpty(); i++) {
						PendingChunk pending = pendingChunks.poll();
						addForFixing(pending.world.getChunkAt(pending.x, pending.z));
					}
				}
			}.start(1, 1);
		}
	}

	public static boolean isFixing(org.bukkit.Chunk chunk) {
		return isFixing(WorldUtil.getNative(chunk));
	}

	public static boolean isFixing(Chunk chunk) {
		synchronized (toFix) {
			return toFix.contains(chunk);
		}
	}

	public static void finish() {
		Task.stop(chunkLoadTask);
		chunkLoadTask = null;
		pending -= pendingChunks.size();
		pendingChunks.clear();
		if (getPendingSize() > 10) {
			NoLagg.plugin.log(Level.INFO, "Finishing " + getPendingSize() + " remaining lighting fix operations...");
		}
		executeAll();
		AsyncTask.stop(task);
		task = null;
	}

	@Override
	public void run() {
		if (!executeAll()) {
			this.stop();
			task = null;
		}
	}

	private static boolean executeAll() {
		int maxChunksPerRun = 500;
		synchronized (next) {
			synchronized (toFix) {
				if (toFix.isEmpty())
					return false;
				for (Chunk c : toFix) {
					next.add(new FixOperation(c));
					if (maxChunksPerRun-- <= 0) {
						break;
					}
				}
			}

			for (FixOperation fix : next) {
				fix.prepare();
			}
			int procctr = 0;

			// Sky light
			boolean haderror = true;
			boolean first = true;
			while (haderror) {
				haderror = false;
				for (FixOperation fix : next) {
					haderror |= fix.smooth(EnumSkyBlock.SKY);
					if (first && (procctr++ % 2) == 0) {
						pending--;
					}
				}
				first = false;
			}

			// Block light
			haderror = true;
			first = true;
			while (haderror) {
				haderror = false;
				for (FixOperation fix : next) {
					haderror |= fix.smooth(EnumSkyBlock.BLOCK);
					if (first && (procctr++ % 2) == 0) {
						pending--;
					}
				}
				first = false;
			}

			// remove
			synchronized (toFix) {
				for (FixOperation fix : next) {
					toFix.remove(fix.chunk);
				}
			}

			// finish (next tick operations)
			for (FixOperation fix : next) {
				fix.finish();
			}
			next.clear();
			return true;
		}
	}

	private static class PendingChunk extends ChunkCoordIntPair {
		public PendingChunk(World world, int x, int z) {
			super(x, z);
			this.world = world;
		}
		public final World world;
	}

	private static class FixOperation {
		private final Chunk chunk;
		private final WorldServer world;
		public final ChunkSection[] sections;

		public FixOperation(final Chunk chunk) {
			this.chunk = chunk;
			this.world = (WorldServer) chunk.world;
			this.sections = this.chunk.i();
		}

		private Chunk getChunk(final int x, final int z) {
			return WorldUtil.getChunk(this.world, x, z);
		}

		private int getLightLevel(EnumSkyBlock mode, int x, final int y, int z) {
			if (y <= 0 || y >= this.chunk.world.getHeight())
				return 0;
			if (x >= 0 && z >= 0 && x < 16 && z < 16) {
				return this.chunk.getBrightness(mode, x, y, z);
			}
			Chunk chunk = this.getChunk(this.chunk.x + (x >> 4), this.chunk.z + (z >> 4));
			if (chunk == null)
				return 0;
			x -= (chunk.x - this.chunk.x) << 4;
			z -= (chunk.z - this.chunk.z) << 4;
			return chunk.getBrightness(mode, x, y, z);
		}

		/**
		 * Performs light smoothing to fix dark glitches
		 * 
		 * @param mode of lighting
		 * @return True if errors were fixed, False if not
		 */
		public boolean smooth(EnumSkyBlock mode) {
			int x, y, z, typeid, light, factor;
			int loops = 0;
			boolean haserror = true;
			boolean haderror = false;
			int lasterrx, lasterry, lasterrz;
			lasterrx = lasterry = lasterrz = 0;
			while (haserror) {
				if (loops > 100) {
					lasterrx += this.chunk.x << 4;
					lasterrz += this.chunk.z << 4;
					StringBuilder msg = new StringBuilder();
					msg.append("Failed to fix all " + mode.toString().toLowerCase() + " lighting at [");
					msg.append(lasterrx).append('/').append(lasterry);
					msg.append('/').append(lasterrz).append(']');
					NoLaggLighting.plugin.log(Level.WARNING, msg.toString());
					break;
				}
				haserror = false;
				loops++;
				int inity;
				for (x = 0; x < 16; x++) {
					for (z = 0; z < 16; z++) {
						if (mode == EnumSkyBlock.SKY) {
							inity = this.chunk.b(x, z);
							if (inity >= this.world.getHeight()) {
								inity = this.world.getHeight() - 1;
							}
						} else {
							inity = this.world.getHeight() - 1;
						}
						for (y = inity; y > 0; --y) {
							if (this.chunk.i()[y >> 4] == null) {
								continue;
							}
							typeid = this.chunk.getTypeId(x, y, z);
							if (!MaterialUtil.ISSOLID.get(typeid)) {
								factor = Math.max(1, MaterialUtil.OPACITY.get(typeid));
								light = this.chunk.getBrightness(mode, x, y, z);
								// actual editing here
								int newlight = light + factor;
								// obtain lighting from all sides
								newlight = Math.max(newlight, getLightLevel(mode, x - 1, y, z));
								newlight = Math.max(newlight, getLightLevel(mode, x + 1, y, z));
								newlight = Math.max(newlight, getLightLevel(mode, x, y, z - 1));
								newlight = Math.max(newlight, getLightLevel(mode, x, y, z + 1));
								newlight = Math.max(newlight, getLightLevel(mode, x, y - 1, z));
								newlight = Math.max(newlight, getLightLevel(mode, x, y + 1, z));
								newlight -= factor;
								// pick the highest value
								if (newlight > light) {
									ChunkSection chunksection = this.chunk.i()[y >> 4];
									if (chunksection != null) {
										if (mode == EnumSkyBlock.SKY) {
											chunksection.c(x, y & 0xf, z, newlight);
										} else {
											chunksection.d(x, y & 0xf, z, newlight);
										}
										lasterrx = x;
										lasterry = y;
										lasterrz = z;
										haserror = true;
										haderror = true;
									}
								}
							}
						}
					}
				}
			}
			return haderror;
		}

		public void prepare() {
			int x, y, z;
			int slicesLight = this.chunk.h();
			int maxheight = this.world.getHeight() - 1;
			ChunkSection sec;
			// initial calculation of sky light
			for (x = 0; x < 16; x++) {
				for (z = 0; z < 16; z++) {
					int ll = 15;
					int kk = slicesLight + 15;

					for (y = maxheight; y >= 0; y--) {
						sec = this.sections[y >> 4];
						if (sec == null)
							continue;

						if (ll <= 0 || --kk <= 0 || (ll -= this.chunk.b(x, y, z)) <= 0) {
							ll = 0;
						}
						sec.c(x, y & 0xf, z, ll);
						sec.d(x, y & 0xf, z, Block.lightEmission[sec.a(x, y & 0xf, z)]);
					}
				}
			}
		}

		public void finish() {
			// send chunk (update)
			CommonUtil.nextTick(new Operation(false) {
				boolean found = false;

				public void run() {
					this.doPlayers(world);
					if (!found) {
						// unload chunk if there were no players nearby
						world.chunkProviderServer.queueUnload(chunk.x, chunk.z);
					}
				}

				@SuppressWarnings("unchecked")
				public void handle(EntityPlayer ep) {
					if (Math.abs(chunk.x - MathUtil.locToChunk(ep.locX)) > CommonUtil.view)
						return;
					if (Math.abs(chunk.z - MathUtil.locToChunk(ep.locZ)) > CommonUtil.view)
						return;
					ep.chunkCoordIntPairQueue.add(0, new ChunkCoordIntPair(chunk.x, chunk.z));
					found = true;
				}
			});
		}
	}
}
