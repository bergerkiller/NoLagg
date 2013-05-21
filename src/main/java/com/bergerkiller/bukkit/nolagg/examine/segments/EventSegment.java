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
	public int getPluginCount() {
		return this.getChildren().length;
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
		// List all event execution counts and cancel percentages
		for (Segment segment : this.getSegments()) {
			builder.append('\n').append(((DataSegment) segment).getPlugin());
			builder.append(" ").append(segment.getName());

			int execCount = -1;
			int cancelCount = -1;
			for (String loc : ((DataSegment) segment).getLocation().split("\n")) {
				if (loc.startsWith("Execution count: ")) {
					try {
						execCount = Integer.parseInt(loc.substring(17));
					} catch (NumberFormatException ex) {}
				}
				if (loc.startsWith("Cancelled: ")) {
					try {
						cancelCount = Integer.parseInt(loc.substring(11));
						continue;
					} catch (NumberFormatException ex) {}
				}
				builder.append("\n  ").append(loc);
			}
			if (cancelCount != -1) {
				builder.append("\n  Cancelled: ").append(cancelCount);
				if (execCount != -1) {
					builder.append(" (").append(round(100.0 * (double) cancelCount / (double) execCount, 2)).append("%)");
				}
			}
		}
		return builder.toString();
	}
}
