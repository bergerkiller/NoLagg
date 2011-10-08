package com.bergerkiller.bukkit.nolaggchunks;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

@SuppressWarnings("unchecked")
public class PlayerDefault<T> {
	public PlayerDefault(T value) {
		this.def = value;
	}
	
	private HashMap<String, T> defaults = new HashMap<String, T>();
	private T def;
	
	public T get(Player player) {
		if (player == null) return this.get();
		return this.get(player.getName());
	}
	public T get(String playername) {
		if (playername == null) return this.get();
		T found = this.defaults.get(playername.toLowerCase());
		if (found == null) {
			return this.def;
		} else {
			return found;
		}
	}
	public T get() {
		return this.def;
	}
	public void set(Player player, Object value) {
		if (player == null) {
			this.set(value);
		} else {
			this.set(player.getName(), value);
		}
		
	}
	public void set(String playername, Object value) {
		if (playername == null) {
			this.set(value);
		} else {
			this.defaults.put(playername.toLowerCase(), (T) value);
		}
		
	}
	public void set(Object value) {
		this.def = (T) value;
	}
	
	public static int parseInt(Configuration config, String key, int def) {
		def = config.getInt(key, def);
		config.setProperty(key, def);
		return def;
	}
	public static double parseDouble(Configuration config, String key, double def) {
		def = config.getDouble(key, def);
		config.setProperty(key, def);
		return def;
	}
	
	public void parse(String playername, Configuration config, String key) {
		if (this.def instanceof Integer) {
			this.set(playername, parseInt(config, key, (Integer) this.get(playername)));
		} else if (this.def instanceof Double) {
			this.set(playername, parseDouble(config, key, (Double) this.get(playername)));
		}
	}
	public void parse(Configuration config, String key) {
		this.parse(null, config, key);
	}
	
}
