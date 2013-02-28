package com.bergerkiller.bukkit.nolagg.lighting;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.World.Environment;

import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.reflection.classes.RegionFileCacheRef;
import com.bergerkiller.bukkit.common.reflection.classes.RegionFileRef;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.wrappers.LongHashSet;

public class LightingTaskWorld implements LightingTask {
	private static final int ASSUMED_CHUNKS_PER_REGION = 34 * 34;
	private final World world;
	private final File regionFolder;
	private final List<WorldRegion> regions;
	private final LongHashSet chunks = new LongHashSet();
	private int chunkCount;

	public LightingTaskWorld(World world) {
		this.world = world;

		// Obtain the folder where regions of the world are stored
		File regionFolderTmp = WorldUtil.getWorldFolder(world);
		// Special dim folder for nether and the_end
		if (world.getEnvironment() == Environment.NETHER) {
			regionFolderTmp = new File(regionFolderTmp, "DIM-1");
		} else if (world.getEnvironment() == Environment.THE_END) {
			regionFolderTmp = new File(regionFolderTmp, "DIM1");
		}
		// Final region folder appending
		this.regionFolder = new File(regionFolderTmp, "region");

		// Obtain the region file names
		String[] regionFileNames = this.regionFolder.list();
		this.regions = new ArrayList<WorldRegion>(regionFileNames.length);
		for (String regionFileName : regionFileNames) {
			File file = new File(regionFolder, regionFileName);
			if (!file.isFile() || !file.exists() || file.length() < 4096) {
				continue;
			}
			String[] parts = regionFileName.split("\\.");
			if (parts.length != 4 || !parts[0].equals("r") || !parts[3].equals("mca")) {
				continue;
			}
			// Obtain the chunk offset of this region file
			try {
				int rx = Integer.parseInt(parts[1]) << 5;
				int rz = Integer.parseInt(parts[2]) << 5;
				this.regions.add(new WorldRegion(file, rx, rz));
			} catch (Exception ex) {
			}
		}
		this.chunkCount = this.regions.size() * ASSUMED_CHUNKS_PER_REGION;
	}

	@Override
	public World getWorld() {
		return world;
	}

	@Override
	public boolean containsChunk(int chunkX, int chunkZ) {
		// This task always contains all chunks
		return true;
	}

	@Override
	public int getChunkCount() {
		return chunkCount;
	}

	@Override
	public void syncTick() {
		// Nothing happens here...
	}

	@Override
	public void process() {
		// Start loading all regions and all chunks contained in these regions
		int dx, dz;
		Object regionFile;
		int regionChunkCount;
		for (WorldRegion region : this.regions) {
			regionChunkCount = 0;
			// Is it contained in the cache? If so, use that to obtain coordinates
			if ((regionFile = RegionFileCacheRef.FILES.get(region.file)) == null) {
				// Start a new file stream to read the coordinates
				// Creating a new region file is too slow and results in memory leaks
				try {
					DataInputStream stream = new DataInputStream(new FileInputStream(region.file));
					try {
						for (int coordIndex = 0; coordIndex < 1024; coordIndex++) {
							if (stream.readInt() > 0) {
								// Convert coordinate to dx/dz
								// coordIndex = dx + (dz << 5)
								dx = coordIndex & 31;
								dz = coordIndex >> 5;

								// Add
								chunks.add(region.rx + dx, region.rz + dz);
								regionChunkCount++;
							}
						}
					} finally {
						stream.close();
					}
				} catch (IOException ex) {
				}
			} else {
				// Obtain all generated chunks in this region file
				for (dx = 0; dx < 32; dx++) {
					for (dz = 0; dz < 32; dz++) {
						if (RegionFileRef.exists.invoke(regionFile, dx, dz)) {
							// Region file exists - add it
							chunks.add(region.rx + dx, region.rz + dz);
							regionChunkCount++;
						}
					}
				}
			}
			// Update chunk count to subtract the missing chunks
			chunkCount -= ASSUMED_CHUNKS_PER_REGION - regionChunkCount;
		}

		// We now know of all the regions to be processed, convert all of them into tasks
		// Use a slightly larger area to avoid cross-region errors
		final List<IntVector2> buffer = new ArrayList<IntVector2>(1100);
		for (WorldRegion region : regions) {
			// Put the coordinates that are available
			for (dx = -1; dx < 33; dx++) {
				for (dz = -1; dz < 33; dz++) {
					if (chunks.contains(region.rx + dx, region.rz + dz)) {
						buffer.add(new IntVector2(region.rx + dx, region.rz + dz));
					}
				}
			}
			// Reduce count, schedule and clear the buffer
			this.chunkCount -= buffer.size();
			LightingService.schedule(world, buffer);
			buffer.clear();
		}
	}

	private static class WorldRegion {
		public final File file;
		public final int rx, rz;

		public WorldRegion(File file, int rx, int rz) {
			this.file = file;
			this.rx = rx;
			this.rz = rz;
		}
	}
}
