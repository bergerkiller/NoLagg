package com.bergerkiller.bukkit.nolagg.lighting;

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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class LightingService extends AsyncTask {
	private static AsyncTask fixThread = null;
	private static Task tickTask = null;
	private static final Set<String> recipientsForDone = new HashSet<String>();
	private static final LinkedList<LightingTask> tasks = new LinkedList<LightingTask>();
	private static int taskChunkCount = 0;
	private static LightingTask currentTask;

	/**
	 * Gets whether this service is currently processing something
	 * 
	 * @return True if processing, False if not
	 */
	public static boolean isProcessing() {
		return fixThread != null;
	}

	/**
	 * Starts or stops the processing service.
	 * Stopping the service does not instantly abort, the current task is continued.
	 * 
	 * @param process to abort
	 */
	public static void setProcessing(boolean process) {
		if (process == isProcessing()) {
			return;
		}
		if (process) {
			fixThread = new LightingService().start(true);
			tickTask = new Task(NoLagg.plugin) {
				@Override
				public void run() {
					final LightingTask current = currentTask;
					if (current != null) {
						current.syncTick();
					}
				}
			}.start(1, 1);
		} else {
			// Fix thread is running, abort
			Task.stop(tickTask);
			AsyncTask.stop(fixThread);
			tickTask = null;
			fixThread = null;
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

	public static void scheduleWorld(final World world) {
		schedule(new LightingTaskWorld(world));
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
		schedule(new LightingTaskBatch(world, chunks));
	}

	public static void schedule(LightingTask task) {
		synchronized (tasks) {
			tasks.offer(task);
			taskChunkCount += task.getChunkCount();
		}
		setProcessing(true);
	}

	/**
	 * Checks whether the chunk specified is currently being processed on
	 * 
	 * @param chunk to check
	 * @return True if the chunk is being processed, False if not
	 */
	public static boolean isProcessing(Chunk chunk) {
		final LightingTask current = currentTask;
		if (current == null) {
			return false;
		} else {
			return current.getWorld() == chunk.getWorld() && current.containsChunk(chunk.getX(), chunk.getZ());
		}
	}

	/**
	 * Orders this service to abort all tasks, finishing the current task in an orderly fashion.
	 * This method can only be called from the main Thread.
	 */
	public static void abort() {
		// Clear lighting tasks
		synchronized (tasks) {
			tasks.clear();
			taskChunkCount = 0;
		}
		// Finish the current lighting task if available
		final LightingTask current = currentTask;
		final AsyncTask service = fixThread;
		if (service != null && current != null) {
			setProcessing(false);
			NoLaggLighting.plugin.log(Level.INFO, "Processing lighting in the remaining " + current.getChunkCount() + " chunks...");

			// Sync task no longer executes: make sure that we tick the tasks
			while (service.isRunning()) {
				current.syncTick();
				sleep(20);
			}
		}
	}

	/**
	 * Gets the amount of chunks that are still faulty
	 * 
	 * @return faulty chunk count
	 */
	public static int getChunkFaults() {
		final LightingTask current = currentTask;
		return taskChunkCount + (current == null ? 0 : current.getChunkCount());
	}

	@Override
	public void run() {
		synchronized (tasks) {
			currentTask = tasks.poll();
		}
		if (currentTask == null) {
			// No more tasks, end this thread
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
			setProcessing(false);
			return;
		} else {
			// Subtract task from the task count
			taskChunkCount -= currentTask.getChunkCount();
			// Process the task
			currentTask.process();
			// Protection against 'out of memory' issues
			final Runtime runtime = Runtime.getRuntime();
			if (runtime.freeMemory() >= NoLaggLighting.minFreeMemory) {
				return;
			}
			runtime.gc();
			if (runtime.freeMemory() >= NoLaggLighting.minFreeMemory) {
				return;
			}
			// Save all worlds: memory after garbage collecting is still too high
			NoLaggLighting.plugin.log(Level.WARNING, "Saving all worlds to free some memory...");
			for (World world : WorldUtil.getWorlds()) {
				WorldUtil.saveToDisk(world);
			}
			runtime.gc();
			final long freemb = runtime.freeMemory() / (1024 * 1024);
			NoLaggLighting.plugin.log(Level.WARNING, "All worlds saved. Free memory: " + freemb + "MB. Continueing...");
		}
	}
}
