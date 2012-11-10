package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import java.util.List;

import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkPosition;
import net.minecraft.server.ChunkProviderServer;
import net.minecraft.server.EnumCreatureType;
import net.minecraft.server.IChunkProvider;
import net.minecraft.server.IProgressUpdate;
import net.minecraft.server.World;
import net.minecraft.server.WorldServer;

public class DummyChunkProvider extends ChunkProviderServer {
	private static final IllegalStateException FAIL = new IllegalStateException("Unsupported method for Dummy chunk provider (oh no!)");

	public DummyChunkProvider(WorldServer world) {
		super(world, null, null);
		this.chunkProvider = null;
	}

	@Override
	public Chunk getChunkAt(int x, int z) {
		return null;
	}

	@Override
	public void a() {
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
