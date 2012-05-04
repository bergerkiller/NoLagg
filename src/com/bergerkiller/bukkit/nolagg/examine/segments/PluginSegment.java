package com.bergerkiller.bukkit.nolagg.examine.segments;

import java.util.List;

/**
 * Contains all the information of a single plugin
 */
public class PluginSegment extends SegmentNode {
	
	public PluginSegment(String plugin, int duration, List<DataSegment> events) {
		super(plugin, duration, events);
	}
		
	@Override
	public String getDescription() {
		StringBuilder builder = new StringBuilder(super.getDescription());
		if (this.getName().startsWith("#")) {
			builder.append('\n').append("Server operation: ").append(this.getName());
		} else {
			builder.append('\n').append("Plugin: ").append(this.getName());
		}
		return builder.toString();
	}
}
