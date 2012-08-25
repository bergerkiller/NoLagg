package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkProviderServer;
import net.minecraft.server.WorldServer;

public class DummyChunkProvider extends ChunkProviderServer {

	public DummyChunkProvider(WorldServer world) {
		super(world, null, null);
		this.chunks = null;
	}

	public Chunk getChunkAt(final int x, final int z) {
		return null;
	}
}
