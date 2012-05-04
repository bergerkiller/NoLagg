package com.bergerkiller.bukkit.nolagg.examine.reader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class ExamFile {
	
	
	public int duration;
	public int taskcount;
	
	public final List<Segment> segments = new ArrayList<Segment>();
	public Segment addSegment(final String name, final String location, final String plugin, boolean isTask) {
		Segment seg = new Segment(name, this.duration, location, plugin);
		this.segments.add(seg);
		PluginInfo info = getInfo(plugin);
		if (isTask) info.taskcount++;
		info.segments.add(seg);
		return seg;
	}
	
	public PluginInfo getInfo(String pluginname) {
		PluginInfo info = pluginSegments.get(pluginname);
		if (info == null) {
			info = new PluginInfo(this.duration, pluginname);
			pluginSegments.put(pluginname, info);
		}
		return info;
	}
	
	public int getPluginCount() {
		return pluginSegments.size();
	}
	
	public final LinkedHashMap<String, PluginInfo> pluginSegments = new LinkedHashMap<String, PluginInfo>();
	public void sort() {
		//sort segments
		Collections.sort(this.segments);
		
		//generate plugin info
		for (PluginInfo i : pluginSegments.values()) {
			i.segments.clear();
		}
		for (Segment seg : this.segments) {
			getInfo(seg.plugin).segments.add(seg);
		}
		
		//sort plugins
		SortedSet<PluginInfo> sortedEntries = new TreeSet<PluginInfo>();
		sortedEntries.addAll(this.pluginSegments.values());
		this.pluginSegments.clear();
		for (PluginInfo info : sortedEntries) {
			Collections.sort(info.segments);
			Collections.reverse(info.segments);
			this.pluginSegments.put(info.plugin, info);
		}
	}

}
