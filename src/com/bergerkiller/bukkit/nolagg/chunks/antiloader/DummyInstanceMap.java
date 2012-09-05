package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import com.bergerkiller.bukkit.common.SafeField;

import net.minecraft.server.LongHashMap;

public class DummyInstanceMap extends LongHashMap {
	public static boolean ENABLED = false;
	private static final SafeField<Object> entries = new SafeField<Object>(LongHashMap.class, "entries");
	private final DummyPlayerManager manager;

	public DummyInstanceMap(LongHashMap oldMap, DummyPlayerManager playerManager) {
		this.manager = playerManager;
		Object[] entryArray = (Object[]) entries.get(oldMap);
		for (Object o : entryArray) {
			if (o != null) {
				DummyInstancePlayerList.replace(this.manager, o);
			}
		}
		entries.set(this, entryArray);
	}

	@Override
	public void put(long arg0, Object arg1) {
		if (ENABLED) {
			DummyInstancePlayerList.replace(this.manager, arg1);
		}
		super.put(arg0, arg1);
	}
}
