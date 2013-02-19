package com.bergerkiller.bukkit.nolagg.itembuffer;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;

public class NoLaggItemBuffer extends NoLaggComponent {
	public static NoLaggItemBuffer plugin;
	private static List<Integer> items = new ArrayList<Integer>();
	private static boolean enabled = false;
	public static int maxItemsPerChunk = 80;

	@Override
	public void onEnable(ConfigurationNode config) {
		plugin = this;
		this.register(NLIListener.class);
		this.onReload(config);
		ItemMap.init();
		enabled = true;
	}

	@Override
	public void onReload(ConfigurationNode config) {
		config.setHeader("maxItemsPerChunk", "The maximum amount of items allowed per chunk");
		maxItemsPerChunk = config.get("maxItemsPerChunk", maxItemsPerChunk);
	}

	@Override
	public void onDisable(ConfigurationNode config) {
		ItemMap.deinit();
		enabled = true;
	}

	public static Item dropIgnoredItem(Location loc, ItemStack type) {
		Item item = loc.getWorld().dropItem(loc, type);
		if(enabled) {
			items.add(item.getEntityId());
		}
		return item;
	}

	public static Item dropIgnoredItemRandomly(Location loc, ItemStack type) {
		Item item = loc.getWorld().dropItemNaturally(loc, type);
		if (enabled) {
			items.add(item.getEntityId());
		}
		return item;
	}

	public static boolean shouldIgnore(Entity entity) {
		return EntityUtil.isIgnored(entity) || items.contains(entity.getEntityId());
	}

	public static void remove(Entity entity) {
		items.remove(entity.getEntityId());
	}
}
