package com.bergerkiller.bukkit.nolagg.itemstacker;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

import com.bergerkiller.bukkit.common.WorldProperty;
import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;

public class NoLaggItemStacker extends NoLaggComponent {
	public static NoLaggItemStacker plugin;
	public static int stackThreshold = 2;
	public static WorldProperty<Double> stackRadius = new WorldProperty<Double>(2.0);
	public static Set<Material> ignoredTypes = new HashSet<Material>();
	public static boolean stackOrbs = true;
	public static int interval;

	@Override
	public void onReload(ConfigurationNode config) {
		config.setHeader("radius", "The block radius to look for other items when stacking");
		config.setHeader("threshold", "The amount of (physical) items needed to form one stack");
		config.setHeader("interval", "The interval in ticks at which item stacking is performed (1 tick = 1/20 sec)");

		// Radius
		ConfigurationNode radius = config.getNode("radius");
		radius.setHeader("The block radius to look for other items when stacking");
		radius.addHeader("You can set it for multiple worlds");
		stackRadius = new WorldProperty<Double>(1.0);
		stackRadius.load(radius);

		stackThreshold = config.get("threshold", 2);
		interval = config.get("interval", 20);

		// ignored types
		ignoredTypes.clear();
		if (!config.contains("ignoredItemTypes")) {
			config.set("ignoredItemTypes", Arrays.asList("DIAMOND_PICKAXE", "WOODEN_HOE"));
		}
		config.setHeader("ignoredItemTypes", "");
		config.addHeader("ignoredItemTypes", "The item types (materials) to ignore during item stacking, buffering and spawn limits");
		config.addHeader("ignoredItemTypes", "Use 'orb' to ignore experience orbs");
		stackOrbs = true;
		for (String type : config.getList("ignoredItemTypes", String.class)) {
			if (type.equalsIgnoreCase("experienceorb") || type.equalsIgnoreCase("xporb") || type.equalsIgnoreCase("orb")) {
				stackOrbs = false;
				continue;
			}
			Material mat = ParseUtil.parseMaterial(type, null);
			if (mat != null) {
				ignoredTypes.add(mat);
			} else {
				log(Level.WARNING, "Unknown item type found in configuration: " + type);
			}
		}
	}

	public static boolean isIgnoredItem(net.minecraft.server.Entity itementity) {
		return isIgnoredItem(itementity.getBukkitEntity());
	}

	public static boolean isIgnoredItem(Entity itementity) {
		if (EntityUtil.isIgnored(itementity))
			return true;
		Item item = (Item) itementity;
		return ignoredTypes.contains(item.getItemStack().getType());
	}

	@Override
	public void onEnable(ConfigurationNode config) {
		plugin = this;
		this.register(NLISListener.class);
		this.onReload(config);
		StackFormer.init();
	}

	@Override
	public void onDisable(ConfigurationNode config) {
		StackFormer.deinit();
	}
}
