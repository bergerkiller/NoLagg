package com.bergerkiller.bukkit.nolagg.lighting;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.craftbukkit.v1_4_5.util.LongHash;
import org.bukkit.craftbukkit.v1_4_5.util.LongHashSet;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_4_5.ChunkSection;
import net.minecraft.server.v1_4_5.RegionFile;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.reflection.classes.ChunkRef;
import com.bergerkiller.bukkit.common.reflection.classes.ChunkSectionRef;
import com.bergerkiller.bukkit.common.reflection.classes.RegionFileCacheRef;
import com.bergerkiller.bukkit.common.utils.ChunkUtil;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.common.utils.NativeUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class LightingFixThread extends AsyncTask {
	private static AsyncTask task;
	private static LinkedHashSet<org.bukkit.Chunk> toFix = new LinkedHashSet<org.bukkit.Chunk>();
	private static List<FixOperation> next = new ArrayList<FixOperation>();
	private static LinkedList<PendingChunk> pendingChunks = new LinkedList<PendingChunk>();
	private static int pending = 0;
	private static Task chunkLoadTask;

	public static int getPendingSize() {
		return pending;
	}

	private static void addForFixing(org.bukkit.Chunk chunk) {
		synchronized (toFix) {
			toFix.add(chunk);
			if (task == null) {
				task = new LightingFixThread().start(true);
			}
		}
		// get rid of current sending requests for this chunk
		for (Player player : CommonUtil.getOnlinePlayers()) {
			if (EntityUtil.isNearChunk(player, chunk.getX(), chunk.getZ(), CommonUtil.VIEW)) {
				EntityUtil.cancelChunkSend(player, chunk);
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
		for (org.bukkit.Chunk chunk : WorldUtil.getChunks(world)) {
			chunks.add(LongHash.toLong(chunk.getX(), chunk.getZ()));
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
				RegionFile reg = RegionFileCacheRef.FILES.get(file);
				if (reg == null) {
					// Manually load this region file and close it (we don't use it to load chunks)
					reg = new RegionFile(file);
					try {
						reg.c();
					} catch (IOException e) {
						e.printStackTrace();
					}
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
				for (org.bukkit.Chunk c : toFix) {
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
			int maxTries = 2;
			while (haderror && maxTries-- > 0) {
				haderror = false;
				for (FixOperation fix : next) {
					haderror |= fix.smooth(true);
					if (first && (procctr++ % 2) == 0) {
						pending--;
					}
				}
				first = false;
			}

			// Block light
			haderror = true;
			first = true;
			maxTries = 2;
			while (haderror && maxTries-- > 0) {
				haderror = false;
				for (FixOperation fix : next) {
					haderror |= fix.smooth(false);
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

	private static class PendingChunk extends IntVector2 {
		public final World world;

		public PendingChunk(World world, int x, int z) {
			super(x, z);
			this.world = world;
		}
	}

	private static class FixOperation {
		private final org.bukkit.Chunk chunk;
		private final World world;
		public final ChunkSection[] sections;

		public FixOperation(org.bukkit.Chunk chunk) {
			this.chunk = chunk;
			this.world = chunk.getWorld();
			this.sections = ChunkRef.getSections(NativeUtil.getNative(chunk));
		}

		private int getLightLevel(boolean skyLight, int x, final int y, int z) {
			if (y <= 0 || y >= this.chunk.getWorld().getMaxHeight()) {
				return 0;
			}
			if (x >= 0 && z >= 0 && x < 16 && z < 16) {
				return getLightLevel(this.chunk, skyLight, x, y, z);
			}
			org.bukkit.Chunk chunk = WorldUtil.getChunk(this.world, this.chunk.getX() + (x >> 4), this.chunk.getZ() + (z >> 4));
			if (chunk == null) {
				return 0;
			}
			//x -= (chunk.getX() - this.chunk.getX()) << 4;
			//z -= (chunk.getZ() - this.chunk.getX()) << 4;
			return getLightLevel(chunk, skyLight, x, y, z);
		}

		private static int getLightLevel(org.bukkit.Chunk chunk, boolean skyLight, final int x, final int y, final int z) {
			if (skyLight) {
				return ChunkUtil.getSkyLight(chunk, x, y, z);
			} else {
				return ChunkUtil.getBlockLight(chunk, x, y, z);
			}
		}

		/**
		 * Performs light smoothing to fix dark glitches
		 * 
		 * @param skyLight - True to do skylight, False to do blocklight
		 * @return True if errors were fixed, False if not
		 */
		public boolean smooth(boolean skyLight) {
			int x, y, z, typeid, light, factor;
			int loops = 0;
			boolean haserror = true;
			boolean haderror = false;
			int lasterrx, lasterry, lasterrz;
			lasterrx = lasterry = lasterrz = 0;
			while (haserror) {
				if (loops > 100) {
					lasterrx += this.chunk.getX() << 4;
					lasterrz += this.chunk.getZ() << 4;
					StringBuilder msg = new StringBuilder();
					msg.append("Failed to fix all " + (skyLight ? "Sky" : "Block") + " lighting at [");
					msg.append(lasterrx).append('/').append(lasterry);
					msg.append('/').append(lasterrz).append(']');
					NoLaggLighting.plugin.log(Level.WARNING, msg.toString());
					break;
				}
				haserror = false;
				loops++;
				int inity;
				final int maxY = this.world.getMaxHeight() - 1;
				for (x = 0; x < 16; x++) {
					for (z = 0; z < 16; z++) {
						if (skyLight) {
							inity = Math.min(ChunkUtil.getHeight(this.chunk, x, z), maxY);
						} else {
							inity = maxY;
						}
						for (y = inity; y > 0; --y) {
							if (this.sections[y >> 4] == null) {
								continue;
							}
							typeid = ChunkUtil.getBlockTypeId(chunk, x, y, z);
							if (!MaterialUtil.ISSOLID.get(typeid)) {
								factor = Math.max(1, MaterialUtil.OPACITY.get(typeid));
								light = getLightLevel(this.chunk, skyLight, x, y, z);
								// actual editing here
								int newlight = light + factor;
								// obtain lighting from all sides
								newlight = Math.max(newlight, getLightLevel(skyLight, x - 1, y, z));
								newlight = Math.max(newlight, getLightLevel(skyLight, x + 1, y, z));
								newlight = Math.max(newlight, getLightLevel(skyLight, x, y, z - 1));
								newlight = Math.max(newlight, getLightLevel(skyLight, x, y, z + 1));
								newlight = Math.max(newlight, getLightLevel(skyLight, x, y - 1, z));
								newlight = Math.max(newlight, getLightLevel(skyLight, x, y + 1, z));
								newlight -= factor;
								// pick the highest value
								if (newlight > light) {
									ChunkSection chunksection = this.sections[y >> 4];
									if (chunksection != null) {
										if (skyLight) {
											ChunkSectionRef.setSkyLight(chunksection, x, y, z, newlight);
										} else {
											ChunkSectionRef.setBlockLight(chunksection, x, y, z, newlight);
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
			int slicesLight = ChunkRef.getTopSectionY(NativeUtil.getNative(this.chunk));
			int maxheight = this.world.getMaxHeight() - 1;
			ChunkSection sec;
			// initial calculation of sky light
			for (x = 0; x < 16; x++) {
				for (z = 0; z < 16; z++) {
					int light = 15;
					int darkLight = slicesLight + 15;

					for (y = maxheight; y >= 0; y--) {
						if ((sec = this.sections[y >> 4]) == null) {
							continue;
						}

						if (light <= 0 || --darkLight <= 0 || (light -= MaterialUtil.OPACITY.get(this.chunk, x, y, z)) <= 0) {
							light = 0;
						}
						ChunkSectionRef.setSkyLight(sec, x, y, z, light);
						ChunkSectionRef.setBlockLight(sec, x, y, z, MaterialUtil.EMISSION.get(ChunkSectionRef.getTypeId(sec, x, y, z)));
					}
				}
			}
		}

		public void finish() {
			// send chunk (update)
			CommonUtil.nextTick(new Runnable() {
				@Override
				public void run() {
					boolean found = false;
					for (Player player : CommonUtil.getOnlinePlayers()) {
						if (EntityUtil.isNearChunk(player, chunk.getX(), chunk.getZ(), CommonUtil.VIEW)) {
							EntityUtil.queueChunkSend(player, chunk.getX(), chunk.getZ());
							found = true;
						}
					}
					if (!found) {
						// unload chunk if there were no players nearby
						WorldUtil.setChunkUnloading(world, chunk.getX(), chunk.getZ(), true);
					}
				}
			});
		}
	}
}
