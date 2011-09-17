package com.bergerkiller.bukkit.nolagg;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.entity.Item;

public class SpawnLimiter {
	private HashMap<String, SpawnInfo> info = new HashMap<String, SpawnInfo>();
	
	private class SpawnInfo {
		public SpawnInfo(int limit) {
			this.count = 0;
			this.limit = limit;
		}
		public int count;
		public int limit;
	}
	
	private SpawnInfo getInfo(Object object) {
		String name;
		if (object instanceof Item) {
			Material type = ((Item) object).getItemStack().getType();
			name = "item" + type.toString().toLowerCase();
			if (!info.containsKey(name)) {
				name = "item";
			}
		} else {
			name = object.getClass().getSimpleName().toLowerCase();
			if (name.startsWith("craft")) name = name.substring(5);
			if (name.contains("tnt")) name = "tnt";
		}
		return info.get(name);
	}
	
	public boolean canSpawn(Object object) {
		SpawnInfo i = getInfo(object);
		if (i != null) {
			if (i.limit == 0) return false;
			if (i.limit > 0) {
				if (i.count >= i.limit) {
					return false;
				} else {
					i.count++;
				}
			}
		}
		return true;
	}
	
	public void add(Object object) {
		SpawnInfo i = getInfo(object);
		if (i != null) {
			i.count++;
		}
	}
	public void remove(Object object) {
		SpawnInfo i = getInfo(object);
		if (i != null) {
			i.count--;
			if (i.count < 0) {
				i.count = 0;
			}
		}
	}
	
	public void addLimit(String name, int amount) {
		SpawnInfo i = info.get(name);
		if (i == null) {
			info.put(name, new SpawnInfo(amount));
		} else {
			i.limit = amount;
		}
	}
	
	public void deinit() {
		for (SpawnInfo i : info.values()) {
			i.count = 0;
		}
	}
	
}
