package com.bergerkiller.bukkit.nolagg.examine.reader;

import java.util.ArrayList;
import java.util.List;

public class PluginInfo implements Comparable<PluginInfo> {
	
	public PluginInfo(final int duration, final String plugin) {
		this.duration = duration;
		this.plugin = plugin;
	}
	
	public final String plugin;
	public final List<Segment> segments = new ArrayList<Segment>();
	private Segment combined = null;
	public int taskcount = 0;
	private final int duration;
	public Segment getAll() {
		if (this.combined == null) {
			this.combined = new Segment(plugin, duration, null, plugin).load(segments.toArray(new Segment[0]));
		}
		return this.combined;
	}
	
	public double getTotal() {
		double rval = 0.0;
		for (Segment s : this.segments) {
			rval += s.getTotal();
		}
		return rval;
	}

	@Override
	public int compareTo(PluginInfo info) {
		if (info.getTotal() > this.getTotal()) {
			return -1;
		} else {
			return 1;
		}
	}
	
}
