package com.bergerkiller.bukkit.nolagg.examine;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;

import com.bergerkiller.bukkit.common.internal.TimingsListener;

public class NLETimingsListener implements TimingsListener {
	public static final NLETimingsListener INSTANCE = new NLETimingsListener();
	private final TaskMeasurement loadmeas = PluginLogger.getServerOperation("Chunk provider", "Chunk load", "Loads chunks from file");
	private final TaskMeasurement genmeas = PluginLogger.getServerOperation("Chunk provider", "Chunk generate", "Generates the basic terrain");
	private final TaskMeasurement unloadmeas = PluginLogger.getServerOperation("Chunk provider", "Chunk unload", "Unloads chunks and saves them to file");

	@Override
	public void onNextTicked(Runnable runnable, long executionTime) {
		PluginLogger.getNextTickTask(runnable).addDelta(executionTime);
	}

	@Override
	public void onChunkLoad(Chunk chunk, long executionTime) {
		loadmeas.addDelta(executionTime);
	}

	@Override
	public void onChunkGenerate(Chunk chunk, long executionTime) {
		genmeas.addDelta(executionTime);
	}

	@Override
	public void onChunkUnloading(World world, long executionTime) {
		unloadmeas.addDelta(executionTime);
	}

	@Override
	public void onChunkPopulate(Chunk chunk, BlockPopulator populator, long executionTime) {
		final String classname = populator.getClass().getSimpleName();
		final String loc = populator.getClass().getName();
		PluginLogger.getServerOperation("Chunk populators", classname, loc).addDelta(executionTime);
	}
}
