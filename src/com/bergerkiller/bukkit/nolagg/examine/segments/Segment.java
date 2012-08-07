package com.bergerkiller.bukkit.nolagg.examine.segments;

import java.util.ArrayList;
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

	@Override
	public int compareTo(Segment o) {
		return (int) (o.getTotal() - this.getTotal());
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
		builder.append(Math.round(this.getTotal() * 1000.0) / 1000.0).append(" ms / ");
		builder.append(this.getDuration()).append(" ticks");
		builder.append("\nAverage duration: ");
		builder.append(Math.round(this.getAverage() * 1000.0) / 1000.0).append(" ms/tick");
		return builder.toString();
	}
}
