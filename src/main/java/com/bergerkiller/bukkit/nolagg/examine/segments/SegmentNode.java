package com.bergerkiller.bukkit.nolagg.examine.segments;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public abstract class SegmentNode extends Segment {

	private Segment[] segments;
	private int taskcount;
	private int eventcount;

	public SegmentNode(String name, int duration, List<? extends Segment> data) {
		super(name, duration, getData(data));
		this.segments = data.toArray(new Segment[0]);
		this.taskcount = 0;
		this.eventcount = 0;
		for (Segment seg : this.segments) {
			seg.setParent(this);
			if (seg instanceof SegmentNode) {
				this.taskcount += ((SegmentNode) seg).getTaskCount();
				this.eventcount += ((SegmentNode) seg).getEventCount();
			} else if (seg.isTask()) {
				this.taskcount++;
			} else {
				this.eventcount++;
			}
		}
	}

	public int getTaskCount() {
		return this.taskcount;
	}

	public int getEventCount() {
		return this.eventcount;
	}

	public Segment[] getChildren() {
		return this.segments;
	}

	public abstract int getPluginCount();

	@Override
	public Segment getSegment(int index) {
		if (index < 0 || index >= this.segments.length) {
			return null;
		} else {
			return this.segments[index];
		}
	}

	@Override
	public void export(BufferedWriter writer, int indent) throws IOException {
		super.export(writer, indent);
		int plugins = this.getPluginCount();
		if (plugins != 0) {
			export(writer, indent, "Plugin count: " + plugins);
		}
		export(writer, indent, "Event count: " + this.getEventCount());
		export(writer, indent, "Task count: " + this.getTaskCount());
		export(writer, indent, "=============================");
		for (Segment segment : this.segments) {
			segment.export(writer, indent + 1);
		}
	}

	@Override
	public String getDescription() {
		StringBuilder builder = new StringBuilder(super.getDescription());
		builder.append('\n').append("Event count: ").append(this.getEventCount());
		builder.append('\n').append("Task count: ").append(this.getTaskCount());
		return builder.toString();
	}
}
