package com.bergerkiller.bukkit.nolagg.lighting;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.reflection.classes.RegionFileCacheRef;
import com.bergerkiller.bukkit.common.reflection.classes.RegionFileRef;

public class LightingTaskRegion {
	private static final List<IntVector2> coords = new ArrayList<IntVector2>(1024);
	private final World world;
	private final File regionFolder;
	private final String regionFileName;

	public LightingTaskRegion(World world, File regionFolder, String regionFileName) {
		this.world = world;
		this.regionFolder = regionFolder;
		this.regionFileName = regionFileName;
	}

	public LightingTask createTask() {
		// Clear the buffer just in case
		coords.clear();
		// Validate file
		File file = new File(regionFolder, regionFileName);
		if (!file.isFile() || !file.exists()) {
			return null;
		}
		String[] parts = regionFileName.split("\\.");
		if (parts.length != 4 || !parts[0].equals("r") || !parts[3].equals("mca")) {
			return null;
		}
		// Obtain the chunk offset of this region file
		int rx = 0;
		int rz = 0;
		try {
			rx = Integer.parseInt(parts[1]) << 5;
			rz = Integer.parseInt(parts[2]) << 5;
		} catch (Exception ex) {
			return null;
		}
		// Is it contained in the cache? If so, use that to obtain coordinates
		Object reg = RegionFileCacheRef.FILES.get(file);
		int dx, dz;
		if (reg == null) {
			// File size check: Do not bother reading an empty file
			if (file.length() < 4096) {
				return null;
			}
			// Start a new file stream to read the coordinates
			// Creating a new region file is too slow and results in memory leaks
			try {
				DataInputStream stream = new DataInputStream(new FileInputStream(file));
				try {
					for (int coordIndex = 0; coordIndex < 1024; coordIndex++) {
						if (stream.readInt() > 0) {
							// Convert coordinate to dx/dz
							// coordIndex = dx + (dz << 5)
							dx = coordIndex & 31;
							dz = coordIndex >> 5;

							// Add
							coords.add(new IntVector2(rx + dx, rz + dz));
						}
					}
				} finally {
					stream.close();
				}
			} catch (IOException ex) {
				return null;
			}
		} else {
			// Obtain all generated chunks in this region file
			for (dx = 0; dx < 32; dx++) {
				for (dz = 0; dz < 32; dz++) {
					if (RegionFileRef.exists.invoke(reg, dx, dz)) {
						// Region file exists - add it
						coords.add(new IntVector2(rx + dx, rz + dz));
					}
				}
			}
		}
		// Schedule and reset the coordinates buffer for later use
		LightingTask task = new LightingTask(world, coords);
		coords.clear();
		return task;
	}
}
