package com.bergerkiller.bukkit.nolagg.examine;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;

import com.bergerkiller.bukkit.common.internal.TimingsListener;

public class NLETimingsListener implements TimingsListener {
	public static final NLETimingsListener INSTANCE = new NLETimingsListener();
	private final TaskMeasurement loadmeas, genmeas, unloadmeas;
	private final PluginLogger logger;

	public NLETimingsListener() {
		logger = NoLaggExamine.logger;
		loadmeas = logger.getServerOperation("Chunk provider", "Chunk load", "Loads chunks from file");
		genmeas = logger.getServerOperation("Chunk provider", "Chunk generate", "Generates the basic terrain");
		unloadmeas = logger.getServerOperation("Chunk provider", "Chunk unload", "Unloads chunks and saves them to file");
	}

	@Override
	public void onNextTicked(Runnable runnable, long executionTime) {
		NoLaggExamine.logger.getNextTickTask(runnable).addDelta(executionTime);
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
		logger.getServerOperation("Chunk populators", classname, loc).addDelta(executionTime);
	}
}
