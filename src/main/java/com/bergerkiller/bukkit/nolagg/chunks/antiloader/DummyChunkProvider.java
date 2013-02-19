package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import com.bergerkiller.bukkit.common.bases.ChunkProviderServerRedirect;
import com.bergerkiller.bukkit.common.bases.DummyWorldServer;

public class DummyChunkProvider extends ChunkProviderServerRedirect {

	public DummyChunkProvider(DummyWorldServer world) {
		super(world);
	}

	@Override
	public boolean isSyncLoadSuppressed() {
		return true;
	}
}
