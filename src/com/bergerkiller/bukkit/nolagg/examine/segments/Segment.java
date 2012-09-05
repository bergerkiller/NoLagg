package com.bergerkiller.bukkit.nolagg.examine.segments;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Segment implements Comparable<Segment> {
	
	private SegmentData[] data;
	private SegmentData mergedData;
	private int duration;
	private double total;
	private double average;
	private Segment parent;
	private String name;

	public static List<SegmentData> getData(List<? extends Segment> segments) {
		List<SegmentData> data = new ArrayList<SegmentData>(segments.size());
		for (Segment seg : segments) {
			data.add(seg.getMergedData());
		}
		return data;
	}
	
	public Segment(Segment segment) {
		this.name = segment.name;
		this.total = segment.total;
		this.average = segment.average;
		this.parent = segment.parent;
		this.duration = segment.duration;
		this.data = new SegmentData[segment.data.length];
		for (int i = 0; i < this.data.length; i++) {
			this.data[i] = segment.data[i].clone();
		}
		this.mergedData = segment.mergedData.clone();
	}
	
	public Segment(String name, int duration, List<SegmentData> data) {
		this.duration = duration;
		this.data = data.toArray(new SegmentData[0]);
		this.mergedData = new SegmentData(name, duration).load(this.data);
		this.total = this.mergedData.getTotal();
		this.name = name;
		this.average = this.total / (double) this.duration;
	}
	
	public SegmentData getMergedData() {
		return this.mergedData;
	}
	
	public SegmentData[] getData() {
		return this.data;
	}
	
	public void setParent(Segment parent) {
		this.parent = parent;
	}
	
	public Segment getParent() {
		return this.parent;
	}
	
	public String getName() {
		return this.name;
	}
	
	public boolean hasParent() {
		return this.parent != null;
	}
	
	public int getDuration() {
		return this.duration;
	}
	
	public double getTotal() {
		return this.total;
	}
	
	public double getAverage() {
		return this.average;
	}
	
	public boolean isTask() {
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		} else if (o == this) {
			return true;
		} else if (o instanceof Segment) {
			return ((Segment) o).getTotal() == this.getTotal();
		} else {
			return false;
		}
	}

	public static double round(double Rval, int Rpl) {
		double p = Math.pow(10, Rpl);
		return Math.round(Rval * p) / p;
	}

	public void export(BufferedWriter writer, int indent, String text) throws IOException {
		for (String line : text.split("\n", -1)) {
			for (int i = 0; i < indent; i++) {
				writer.write('\t');
			}
			writer.write(line);
			writer.newLine();
		}
	}

	public void export(BufferedWriter writer, int indent) throws IOException {
		if (this instanceof SegmentNode) {
			export(writer, indent, "Name: " + this.getName());
		} else if (this.isTask()) {
			export(writer, indent, "Task name: " + this.getName());
		} else {
			export(writer, indent, "Event name: " + this.getName());
		}
		export(writer, indent, "Time: " + round(this.getTotal(), 3) + " ms in " + this.getDuration() + " ticks" + " (" + round(this.getAverage(), 3) + " ms/tick average)");
	}

	@Override
	public int compareTo(Segment o) {
		return (int) (o.getTotal() - this.getTotal());
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <T extends Comparable> void trySort(List<T> collection) {
		try {
			Collections.sort(collection);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * Gets a child segment, null if not possible
	 * @param index
	 */
	public Segment getSegment(int index) {
		return null;
	}

	public String getDescription() {
		StringBuilder builder = new StringBuilder();
		builder.append("Total duration: ");
		builder.append(round(this.getTotal(), 3)).append(" ms / ");
		builder.append(this.getDuration()).append(" ticks");
		builder.append("\nAverage duration: ");
		builder.append(round(this.getAverage(), 3)).append(" ms/tick");
		return builder.toString();
	}
}
