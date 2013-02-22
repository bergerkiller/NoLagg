package com.bergerkiller.bukkit.nolagg.lighting;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

public class LightingService extends AsyncTask {
	private static AsyncTask fixThread = null;
	private static final LinkedList<LightingTask> tasks = new LinkedList<LightingTask>();
	private static final LinkedList<LightingTaskRegion> regions = new LinkedList<LightingTaskRegion>();
	private static final Set<String> recipientsForDone = new HashSet<String>();
	private static int taskChunkCount = 0;
	private static LightingTask currentTask;

	public static void scheduleWorld(final World world) {
		// Obtain the folder where regions of the world are stored
		File regionFolderTmp = WorldUtil.getWorldFolder(world);
		// Special dim folder for nether and the_end
		if (world.getEnvironment() == Environment.NETHER) {
			regionFolderTmp = new File(regionFolderTmp, "DIM-1");
		} else if (world.getEnvironment() == Environment.THE_END) {
			regionFolderTmp = new File(regionFolderTmp, "DIM1");
		}
		// Final region folder appending
		final File regionFolder = new File(regionFolderTmp, "region");
		if (regionFolder.exists()) {
			synchronized (regions) {
				for (String regionFileName : regionFolder.list()) {
					regions.add(new LightingTaskRegion(world, regionFolder, regionFileName));
					taskChunkCount += 1024;
				}
			}
		}
		// Start the fixing thread
		if (fixThread == null) {
			fixThread = new LightingService().start(true);
		}
	}

	/**
	 * Adds a player who will be notified of the lighting operations being completed
	 * 
	 * @param player to add, null for console
	 */
	public static void addRecipient(CommandSender sender) {
		synchronized (recipientsForDone) {
			recipientsForDone.add((sender instanceof Player) ? sender.getName() : null);
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
		return fixThread != null;
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
		// Clear regions
		synchronized (regions) {
			regions.clear();
		}
		// Clear lighting tasks
		synchronized (tasks) {
			tasks.clear();
			taskChunkCount = 0;
		}
		// Finish the current lighting task if available
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
		// No task, maybe a region?
		if (currentTask == null) {
			LightingTaskRegion reg;
			synchronized (regions) {
				reg = regions.poll();
			}
			if (reg != null) {
				currentTask = reg.createTask();
				taskChunkCount -= 1024;
			}
		} else {
			taskChunkCount -= currentTask.getChunkCount();
		}
		if (currentTask == null) {
			// Messages
			final String message = ChatColor.GREEN + "All lighting operations are completed.";
			synchronized (recipientsForDone) {
				for (String player : recipientsForDone) {
					CommandSender recip = player == null ? Bukkit.getConsoleSender() : Bukkit.getPlayer(player);
					if (recip != null) {
						recip.sendMessage(message);
					}
				}
				recipientsForDone.clear();
			}
			// Stop task and abort
			this.stop();
			fixThread = null;
			return;
		}
		// Operate on the task
		// Load
		currentTask.startLoading();
		currentTask.waitForCompletion();
		// Fix
		currentTask.fix();
		// Apply
		currentTask.startApplying();
		currentTask.waitForCompletion();
	}
}
