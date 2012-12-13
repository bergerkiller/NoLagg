package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import java.util.List;

import org.bukkit.craftbukkit.v1_4_5.chunkio.ChunkIOExecutor;

import com.bergerkiller.bukkit.common.reflection.classes.ChunkProviderServerRef;

import net.minecraft.server.v1_4_5.Chunk;
import net.minecraft.server.v1_4_5.ChunkPosition;
import net.minecraft.server.v1_4_5.ChunkProviderServer;
import net.minecraft.server.v1_4_5.ChunkRegionLoader;
import net.minecraft.server.v1_4_5.EnumCreatureType;
import net.minecraft.server.v1_4_5.IChunkLoader;
import net.minecraft.server.v1_4_5.IChunkProvider;
import net.minecraft.server.v1_4_5.IProgressUpdate;
import net.minecraft.server.v1_4_5.World;
import net.minecraft.server.v1_4_5.WorldServer;

public class DummyChunkProvider extends ChunkProviderServer {
	private static final IllegalStateException FAIL = new IllegalStateException("Unsupported method for Dummy chunk provider (oh no!)");

	public DummyChunkProvider(WorldServer world) {
		super(world, null, null);
		this.chunkProvider = null;
	}

	@Override
	public Chunk getChunkAt(int x, int z) {
		throw FAIL;
	}

	@Override
	public boolean canSave() {
		throw FAIL;
	}

	@Override
	public ChunkPosition findNearestMapFeature(World world, String s, int i, int j, int k) {
		throw FAIL;
	}

	@Override
	public void getChunkAt(IChunkProvider arg0, int arg1, int arg2) {
		throw FAIL;
	}

	@Override
	public int getLoadedChunks() {
		throw FAIL;
	}

	@Override
	public List<?> getMobsFor(EnumCreatureType enumcreaturetype, int i, int j, int k) {
		throw FAIL;
	}

	@Override
	public String getName() {
		throw FAIL;
	}

	@Override
	public Chunk getOrCreateChunk(int arg0, int arg1) {
		throw FAIL;
	}

	@Override
	public Chunk getChunkAt(int x, int z, Runnable task) {
		ChunkProviderServer cps = ((WorldServer) this.world).chunkProviderServer;
		if (cps.isChunkLoaded(x, z)) {
			return null; // Ignore, is already loaded
		}
		IChunkLoader l = ChunkProviderServerRef.chunkLoader.get(cps);
		if (l instanceof ChunkRegionLoader) {
			ChunkRegionLoader loader = (ChunkRegionLoader) l;
			if (loader.chunkExists(cps.world, x, z)) {
				// Load the chunk async
	            ChunkIOExecutor.queueChunkLoad(cps.world, loader, cps, x, z, task);
			}
		}
		// Ignore attempt to generate the chunk
		return null;
	}

	@Override
	public boolean isChunkLoaded(int i, int j) {
		return false;
	}

	@Override
	public Chunk loadChunk(int arg0, int arg1) {
		return null;
	}

	@Override
	public void queueUnload(int arg0, int arg1) {
		throw FAIL;
	}

	@Override
	public void saveChunk(Chunk arg0) {
		throw FAIL;
	}

	@Override
	public void saveChunkNOP(Chunk arg0) {
		throw FAIL;
	}

	@Override
	public boolean saveChunks(boolean arg0, IProgressUpdate arg1) {
		throw FAIL;
	}

	@Override
	public boolean unloadChunks() {
		throw FAIL;
	}
}
