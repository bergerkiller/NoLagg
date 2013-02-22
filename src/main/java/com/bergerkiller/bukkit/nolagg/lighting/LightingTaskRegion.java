package com.bergerkiller.bukkit.nolagg.lighting;

import java.io.File;
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
		// Is it contained in the cache?
		Object reg = RegionFileCacheRef.FILES.get(file);
		if (reg == null) {
			// Manually load this region file and close it (we don't use it to load chunks)
			reg = RegionFileRef.create(file);
			RegionFileRef.close.invoke(reg);
		}
		// Obtain all generated chunks in this region file
		int dx, dz;
		for (dx = 0; dx < 32; dx++) {
			for (dz = 0; dz < 32; dz++) {
				if (RegionFileRef.exists.invoke(reg, dx, dz)) {
					// Region file exists - add it
					coords.add(new IntVector2(rx + dx, rz + dz));
				}
			}
		}
		reg = null;
		// Schedule
		LightingTask task = new LightingTask(world, coords);
		// Reset
		coords.clear();
		return task;
	}
}
