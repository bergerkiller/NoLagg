package com.bergerkiller.bukkit.nolagg.examine.segments;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Shows a collection of all events
 */
public class MultiEventSegment extends SegmentNode {

	public static MultiEventSegment create(int duration, int plugincount, List<DataSegment> segments) {
		Map<String, List<DataSegment>> eventMap = new LinkedHashMap<String, List<DataSegment>>();
		for (DataSegment seg : segments) {
			// Parse the name (ignore event priority!)
			String name = seg.getName();
			if (!seg.isTask() && name.endsWith("]")) {
				int start = name.lastIndexOf('[');
				if (start != -1) {
					name = name.substring(0, start);
				}
			}

			List<DataSegment> plist = eventMap.get(name);
			if (plist == null) {
				plist = new ArrayList<DataSegment>();
				eventMap.put(name, plist);
			}
			plist.add(seg.clone());
		}

		// sort
		List<EventSegment> events = new ArrayList<EventSegment>();
		for (Entry<String, List<DataSegment>> entry : eventMap.entrySet()) {
			trySort(entry.getValue());
			events.add(new EventSegment(entry.getKey(), duration, entry.getValue()));
		}
		trySort(events);

		return new MultiEventSegment(duration, plugincount, events);
	}

	private int plugincount;

	@Override
	public int getPluginCount() {
		return this.plugincount;
	}

	public MultiEventSegment(int duration, int plugincount, List<? extends Segment> data) {
		super("Events", duration, data);
		this.plugincount = plugincount;
	}

	@Override
	public String getDescription() {
		StringBuilder builder = new StringBuilder(super.getDescription());
		builder.append('\n').append("Plugin count: ").append(this.plugincount);
		return builder.toString();
	}
}
