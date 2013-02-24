package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import com.bergerkiller.bukkit.common.bases.LongHashMapBase;
import com.bergerkiller.bukkit.common.reflection.classes.LongHashMapRef;
import com.bergerkiller.bukkit.common.wrappers.LongHashMap;

public class DummyInstanceMap extends LongHashMapBase {
	public static boolean ENABLED = false;
	private final DummyPlayerManager manager;

	public DummyInstanceMap(LongHashMap<Object> oldMap, DummyPlayerManager playerManager) {
		this.manager = playerManager;
		for (Object value : oldMap.getValues()) {
			DummyInstancePlayerList.replace(this.manager, value);
		}
		LongHashMapRef.entriesField.transfer(oldMap.getHandle(), this);
	}

	@Override
	public void put(long key, Object value) {
		if (ENABLED) {
			DummyInstancePlayerList.replace(this.manager, value);
		}
		super.put(key, value);
	}
}
