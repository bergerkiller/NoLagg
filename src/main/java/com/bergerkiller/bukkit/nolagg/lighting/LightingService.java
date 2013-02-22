package com.bergerkiller.bukkit.nolagg.lighting;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.World.Environment;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.reflection.classes.RegionFileCacheRef;
import com.bergerkiller.bukkit.common.reflection.classes.RegionFileRef;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

public class LightingService extends AsyncTask {
	private static AsyncTask fixThread = null;
	private static final LinkedList<LightingTask> tasks = new LinkedList<LightingTask>();
	private static int taskChunkCount = 0;
	private static LightingTask currentTask;

	public static void scheduleWorld(World world) {
		// Obtain the folder where regions of the world are stored
		File regionFolder = WorldUtil.getWorldFolder(world);
		// Special dim folder for nether and the_end
		if (world.getEnvironment() == Environment.NETHER) {
			regionFolder = new File(regionFolder, "DIM-1");
		} else if (world.getEnvironment() == Environment.THE_END) {
			regionFolder = new File(regionFolder, "DIM1");
		}
		// Final region folder appending
		regionFolder = new File(regionFolder, "region");
		if (regionFolder.exists()) {
			List<IntVector2> coords = new ArrayList<IntVector2>(1024);
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
				Object reg = RegionFileCacheRef.FILES.get(file);
				if (reg == null) {
					// Manually load this region file and close it (we don't use it to load chunks)
					reg = RegionFileRef.create(file);
					RegionFileRef.close.invoke(reg);
				}
				// Obtain all generated chunks in this region file
				for (dx = 0; dx < 32; dx++) {
					for (dz = 0; dz < 32; dz++) {
						if (RegionFileRef.exists.invoke(reg, dx, dz)) {
							// Region file exists - add it
							coords.add(new IntVector2(rx + dx, rz + dz));
						}
					}
				}
				// Schedule
				schedule(world, coords);
				// Reset
				coords.clear();
			}
		}
	}

	/**
	 * Schedules a square chunk area for lighting fixing
	 * 
	 * @param world the chunks are in
	 * @param middleX
	 * @param middleZ
	 * @param radius
	 */
	public static void scheduleArea(World world, int middleX, int middleZ, int radius) {
		List<IntVector2> chunks = new ArrayList<IntVector2>();
		for (int a = -radius; a <= radius; a++) {
			for (int b = -radius; b <= radius; b++) {
				chunks.add(new IntVector2(middleX + a, middleZ + b));
			}
		}
		schedule(world, chunks);
	}

	public static void schedule(World world, List<IntVector2> chunks) {
		schedule(new LightingTask(world, chunks));
	}

	public static void schedule(LightingTask task) {
		synchronized (tasks) {
			tasks.offer(task);
			taskChunkCount += task.getChunkCount();
		}
		if (fixThread == null) {
			fixThread = new LightingService().start(true);
		}
	}

	/**
	 * Gets whether this service si currently processing something
	 * 
	 * @return True if processing, False if not
	 */
	public static boolean isProcessing() {
		return currentTask != null;
	}

	/**
	 * Checks whether the chunk specified is being processed on
	 * 
	 * @param chunk to check
	 * @return True if the chunk is being processed, False if not
	 */
	public static boolean isProcessing(Chunk chunk) {
		final LightingTask current = currentTask;
		if (current == null) {
			return false;
		} else {
			return current.world == chunk.getWorld() && current.containsChunk(chunk.getX(), chunk.getZ());
		}
	}

	/**
	 * Orders this service to abort all tasks, finishing the current task in an
	 * orderly fashion.
	 */
	public static void abort() {
		synchronized (tasks) {
			tasks.clear();
			taskChunkCount = 0;
		}
		final LightingTask current = currentTask;
		if (fixThread != null && current != null) {
			NoLaggLighting.plugin.log(Level.INFO, "Processing lighting in the remaining " + current.getFaults() + " chunks...");
			fixThread.stop();
			// Sync tasks no longer execute: make sure that we tick them
			while (fixThread.isRunning()) {
				current.tickTask();
				sleep(20);
			}
			fixThread = null;
		}
	}

	/**
	 * Gets the amount of chunks that are still faulty
	 * 
	 * @return faulty chunk count
	 */
	public static int getChunkFaults() {
		final LightingTask current = currentTask;
		return taskChunkCount + (current == null ? 0 : current.getFaults());
	}

	@Override
	public void run() {
		synchronized (tasks) {
			currentTask = tasks.poll();
		}
		if (currentTask == null) {
			this.stop();
			fixThread = null;
			return;
		}
		taskChunkCount -= currentTask.getChunkCount();
		// Operate on the task
		// Load
		currentTask.startLoading();
		currentTask.waitForCompletion();
		// Fix
		currentTask.fix();
		// Apply
		currentTask.startApplying();
		currentTask.waitForCompletion();
		currentTask = null;
	}
}
