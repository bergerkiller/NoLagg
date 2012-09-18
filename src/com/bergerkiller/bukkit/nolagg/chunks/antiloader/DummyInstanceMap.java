package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import com.bergerkiller.bukkit.common.reflection.classes.LongHashMapRef;

import net.minecraft.server.LongHashMap;

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
	public void put(long arg0, Object arg1) {
		if (ENABLED) {
			DummyInstancePlayerList.replace(this.manager, arg1);
		}
		super.put(arg0, arg1);
	}
}
