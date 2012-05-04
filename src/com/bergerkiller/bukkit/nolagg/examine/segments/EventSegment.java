package com.bergerkiller.bukkit.nolagg.examine.segments;

import java.util.List;

/**
 * Contains all the information of a single event
 */
public class EventSegment extends SegmentNode {
	
	public EventSegment(String name, int duration, List<DataSegment> events) {
		super(name, duration, events);
	}
	
	@Override
	public String getDescription() {
		StringBuilder builder = new StringBuilder(super.getDescription());
		if (this.isTask()) {
			builder.append('\n').append("Task: ").append(this.getName());
		} else {
			builder.append('\n').append("Event: ").append(this.getName());
		}
		builder.append('\n').append("Plugin count: " + this.getChildren().length);
		return builder.toString();
	}
}
