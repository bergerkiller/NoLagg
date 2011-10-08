package com.bergerkiller.bukkit.nolagg;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Item;

public class SpawnLimiter {	
	private HashMap<String, SpawnInfo> info = new HashMap<String, SpawnInfo>();
	private SpawnInfo animal;
	private SpawnInfo monster;
	private SpawnInfo mob;
	private SpawnInfo item;
		
	public SpawnLimiter() {}
	
	private class SpawnInfo {
		public SpawnInfo(int limit) {
			this.count = 0;
			this.limit = limit;
		}
		public int count;
		public int limit;
	}
		
	private SpawnInfo[] getInfo(Object object) {
		String name = object.getClass().getSimpleName().toLowerCase();
		if (name.startsWith("craft")) name = name.substring(5);
		if (name.contains("tnt")) name = "tnt";
		//Animal, monster or mob?
		if (isAnimal(name)) {
			return new SpawnInfo[] {mob, animal, info.get(name)};
		} else if (isMonster(name)) {
			return new SpawnInfo[] {mob, monster, info.get(name)};
		} else if (object instanceof Item) {
			Material type = ((Item) object).getItemStack().getType();
			return new SpawnInfo[] {item, info.get("item" + type.toString().toLowerCase())};
		} else {
			return new SpawnInfo[] {info.get(name)};
		}
	}
	
	private static boolean isAnimal(String name) {
		return in(name, "cow", "pig", "sheep", "chicken", "wolf", "squid");
	}
	private static boolean isMonster(String name) {
		return in(name, "creeper", "skeleton", "zombie", "slime", "skeleton", "pigzombie", "spider", "giant", "ghast", "enderman", "cavespider");
	}
	private static boolean in(String item, String... items) {
		for (String i : items) {
			if (item.equalsIgnoreCase(i)) return true;
		}
		return false;
	}
	
	public boolean hasLimit(String name) {
		if (name.equalsIgnoreCase("animal")) {
			return this.animal != null;
		} else if (name.equalsIgnoreCase("monster")) {
			return this.monster != null;
		} else if (name.equalsIgnoreCase("mob")) {
		    return this.mob != null;
		} else if (name.equalsIgnoreCase("item")) {
			return this.item != null;
		} else {
			return info.containsKey(name);
		}
	}

	public boolean canSpawn(Object object) {
		for (SpawnInfo i : getInfo(object)) {
			if (i != null) {
				if (i.limit == 0) {
					return false;
				} else if (i.limit > 0) {
					if (i.count >= i.limit) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	public void addSpawn(Object object) {
		addSpawn(object, 1);
	}
	public void addSpawn(Object object, int count) {
		for (SpawnInfo i : getInfo(object)) {
			if (i != null && i.limit > 0) {
				i.count += count;
			}
		}
	}
	public void removeSpawn(Object object) {
		removeSpawn(object, 1);
	}
	public void removeSpawn(Object object, int count) {
		addSpawn(object, -count);
	}

	public void setLimit(String name, int limit) {
		if (limit < 0) return;
		if (name.equalsIgnoreCase("animal")) {
			animal = new SpawnInfo(limit);
		} else if (name.equalsIgnoreCase("monster")) {
			monster = new SpawnInfo(limit);
		} else if (name.equalsIgnoreCase("mob")) {
			mob = new SpawnInfo(limit);
		} else if (name.equalsIgnoreCase("item")) {
			item = new SpawnInfo(limit);
		} else {
			SpawnInfo i = info.get(name);
			if (i == null) {
				info.put(name, new SpawnInfo(limit));
			} else {
				i.limit = limit;
			}
		}
	}
	
	public void reset() {
		if (animal != null) animal.count = 0;
		if (monster != null) monster.count = 0;
		if (mob != null) mob.count = 0;
		if (item != null) item.count = 0;
		for (SpawnInfo i : info.values()) {
			i.count = 0;
		}
	}

	public SpawnLimiter clone() {
		SpawnLimiter l = new SpawnLimiter();
		for (Map.Entry<String, SpawnInfo> i : info.entrySet()) {
			l.info.put(i.getKey(), new SpawnInfo(i.getValue().limit));
		}
		if (animal != null) l.animal = new SpawnInfo(animal.limit);
		if (monster != null) l.monster = new SpawnInfo(monster.limit);
		if (mob != null) l.mob = new SpawnInfo(mob.limit);
		return l;
	}
}
