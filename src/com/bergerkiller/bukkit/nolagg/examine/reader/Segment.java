package com.bergerkiller.bukkit.nolagg.examine.reader;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

public class Segment implements Comparable<Segment> {
	
	public Segment(final String name, final int duration, final String location, final String plugin) {
		this.times = new double[duration];
		this.name = name;
		this.location = location;
		this.plugin = plugin;
	}
	
	public final double[] times;
	public final String name;
	public String location;
	public final String plugin;
	
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
	
	public void clear() {
		Arrays.fill(this.times, 0.0);
	}
	public Segment load(Segment[] children) {
		this.clear();
		for (Segment seg : children) {
			for (int i = 0; i < this.times.length; i++) {
				this.times[i] += seg.times[i];
			}
		}
		return this;
	}

	@Override
	public int compareTo(Segment o) {
		return (int) (o.getTotal() - this.getTotal());
	}
	
}
