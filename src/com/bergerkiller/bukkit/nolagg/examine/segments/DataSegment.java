package com.bergerkiller.bukkit.nolagg.examine.segments;

import java.util.Arrays;

public class DataSegment extends Segment {

	private String plugin;
	private String location;
	private boolean task;
	
	public DataSegment(DataSegment segment) {
		super(segment);
		this.plugin = segment.plugin;
		this.location = segment.location;
		this.task = segment.task;
	}
	
	public DataSegment(String name, int duration, SegmentData data, String plugin, String location, boolean task) {
		super(name, duration, Arrays.asList(data));
		this.plugin = plugin;
		this.location = location;
		this.task = task;
	}

	public String getPlugin() {
		return this.plugin;
	}
	
	public String getLocation() {
		return this.location;
	}
	
	public boolean isTask() {
		return this.task;
	}
	
	public boolean isServerOperation() {
		return this.plugin.startsWith("#");
	}
		
	@Override
	public String getDescription() {
		StringBuilder builder = new StringBuilder(super.getDescription());
		if (this.isServerOperation()) {
			builder.append('\n').append("Selected server operation: ").append(this.getName());
			builder.append('\n').append("Server branch: ").append(this.getPlugin());
		} else {
			if (this.isTask()) {
				builder.append('\n').append("Selected task: ").append(this.getName());
			} else {
				builder.append('\n').append("Selected event: ").append(this.getName());
			}
			builder.append('\n').append("Plugin: ").append(this.getPlugin());
			builder.append('\n').append("Location: ").append(this.getLocation());
		}
		return builder.toString();
	}
	
	public DataSegment clone() {
		return new DataSegment(this);
	}
}
