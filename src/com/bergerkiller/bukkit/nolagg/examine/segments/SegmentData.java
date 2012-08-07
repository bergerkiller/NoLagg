package com.bergerkiller.bukkit.nolagg.examine.segments;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Contains the data to display in the graph
 */
public class SegmentData implements Comparable<SegmentData> {

	public SegmentData(final String name, final int duration) {
		this.times = new double[duration];
		this.name = name;
	}

	private final double[] times;
	private final String name;
	
	public String getName() {
		return this.name;
	}
	
	public void readLongValues(DataInputStream stream) throws IOException {
		for (int i = 0; i < this.times.length; i++) {
			this.times[i] = (double) stream.readLong() / 10E6;
		}
	}
	
	public double getTotal() {
		double rval = 0.0;
		for (double v : this.times) {
			rval += v;
		}
		return rval;
	}
	
	public double[] getTimes() {
		return this.times;
	}
	
	public void clear() {
		Arrays.fill(this.times, 0.0);
	}
	public SegmentData load(SegmentData[] children) {
		this.clear();
		for (SegmentData seg : children) {
			for (int i = 0; i < this.times.length; i++) {
				this.times[i] += seg.times[i];
			}
		}
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		} else if (o == this) {
			return true;
		} else if (o instanceof SegmentData) {
			return ((SegmentData) o).getTotal() == this.getTotal();
		} else {
			return false;
		}
	}

	@Override
	public int compareTo(SegmentData o) {
		return (int) (o.getTotal() - this.getTotal());
	}
	
	public SegmentData clone() {
		SegmentData rval = new SegmentData(this.getName(), this.times.length);
		for (int i = 0; i < rval.times.length; i++) {
			rval.times[i] = this.times[i];
		}
		return rval;
	}
}
