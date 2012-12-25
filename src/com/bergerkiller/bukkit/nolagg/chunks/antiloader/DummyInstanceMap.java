package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import com.bergerkiller.bukkit.common.reflection.classes.LongHashMapRef;

import net.minecraft.server.v1_4_6.LongHashMap;

public class DummyInstanceMap extends LongHashMap {
	public static boolean ENABLED = false;
	private final DummyPlayerManager manager;

	public DummyInstanceMap(LongHashMap oldMap, DummyPlayerManager playerManager) {
		this.manager = playerManager;
		for (Object value : LongHashMapRef.getValues(oldMap)) {
			DummyInstancePlayerList.replace(this.manager, value);
		}
		LongHashMapRef.setEntries(this, LongHashMapRef.getEntries(oldMap));
	}

	@Override
	public void put(long key, Object value) {
		if (ENABLED) {
			DummyInstancePlayerList.replace(this.manager, value);
		}
		super.put(key, value);
	}
}
