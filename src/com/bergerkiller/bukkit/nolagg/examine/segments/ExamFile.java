package com.bergerkiller.bukkit.nolagg.examine.segments;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

import com.bergerkiller.bukkit.nolagg.examine.reader.ExamReader;

/**
 * Contains the raw information found in a exam file
 * Data will be converted into the required root segment
 */
public class ExamFile extends Segment {
	
	public static List<SegmentData> getEmptySegments(int duration, List<DataSegment> data) {
		ArrayList<SegmentData> rval = new ArrayList<SegmentData>();
		rval.add(new SegmentData("Plugin view", duration));
		rval.add(new SegmentData("Event view", duration));
		SegmentData first = rval.get(0);
		//prepare a monotone graph
		SegmentData[] merged = new SegmentData[data.size()];
		for (int i = 0; i < merged.length; i++) {
			merged[i] = data.get(i).getMergedData();
		}
		first.load(merged);
		return rval;
	}
	
	public ExamFile(String name, int duration, List<DataSegment> data) {
		super(name, duration, getEmptySegments(duration, data));
		this.events = data;
		//generate multi-plugin and multi-event based segments
		this.multiPlugin = MultiPluginSegment.create(duration, data);
		this.multiEvent = MultiEventSegment.create(duration, this.multiPlugin.getPluginCount(), data);
		this.multiPlugin.setParent(this);
		this.multiEvent.setParent(this);
	}

	public Segment getSegment(int index) {
		if (index == 0) {
			return this.multiPlugin;
		} else if (index == 1) {
			return this.multiEvent;
		} else {
			return null;
		}
	}
	
	@Override
	public String getDescription() {
		StringBuilder builder = new StringBuilder(super.getDescription());
		builder.append('\n').append('\n');
		builder.append("Click one of the two view hierarchy modes above.");
		builder.append("\n\nPlugin view: Compare plugins, then \nevents and tasks in these plugins");
		builder.append("\n\nEvent view: Compare events and tasks, then \nlook at the plugins");
		return builder.toString();
	}
	
	public final List<DataSegment> events;
	public final MultiPluginSegment multiPlugin;
	public final MultiEventSegment multiEvent;
	
	public static String getPriority(int slot) {
		switch (slot) {
		case 0 : return "LOWEST";
		case 1 : return "LOW";
		case 3 : return "HIGH";
		case 4 : return "HIGHEST";
		case 5 : return "MONITOR";
		default : return "NORMAL";
		}
	}

	@Override
	public void export(BufferedWriter writer, int indent) throws IOException {
		this.multiPlugin.export(writer, indent);
	}

	public static ExamFile read(File file) {
		ExamFile efile = null;
		try {
			DataInputStream stream = new DataInputStream(new InflaterInputStream(new FileInputStream(file)));
			try {
				try {
					efile = read(stream);
				} catch (ZipException ex) {
					stream.close();
					stream = new DataInputStream(new FileInputStream(file));
					efile = read(stream);
				}
			} catch (Exception ex) {
				ExamReader.msgbox("Failed to load file: \n\n" + ex.toString());
				ex.printStackTrace();
			} finally {
				stream.close();
			}
		} catch (Throwable ex) {
			ExamReader.msgbox("Failed to load file: \n\n" + ex.toString());
			ex.printStackTrace();
		}
		return efile;
	}
	
	public static ExamFile read(DataInputStream stream) throws IOException {
		int listenercount = stream.readInt();
		int duration = stream.readInt();
		List<DataSegment> segments = new ArrayList<DataSegment>();
		for (int i = 0; i < listenercount; i++) {
			if (stream.readBoolean()) {
				String plugin = stream.readUTF();
				String name = stream.readUTF() + "[" + getPriority(stream.readInt()) + "]";
				String loc = stream.readUTF();
				
				SegmentData data = new SegmentData(name, duration);
				data.readLongValues(stream);
				segments.add(new DataSegment(name, duration, data, plugin, loc, false));
			}
		}
		int taskcount = stream.readInt();
		for (int i = 0; i < taskcount; i++) {
			String name = stream.readUTF();
			String plugin = stream.readUTF();
			int loccount = stream.readInt();
			StringBuilder location = new StringBuilder(loccount * 300);
			if (!plugin.startsWith("#")) {
				location.append(name);
			}
			for (int j = 0; j < loccount; j++) {
				location.append('\n').append('\n').append(stream.readUTF());
			}
			String segname;
			if (plugin.startsWith("#")) {
				segname = location.toString().trim();
			} else {
				segname = "Task #" + (i + 1);
			}
			SegmentData data = new SegmentData(name, duration);
			data.readLongValues(stream);
			segments.add(new DataSegment(segname, duration, data, plugin, location.toString(), true));
		}
		return new ExamFile("Results", duration, segments);
	}
	
}
