package com.bergerkiller.bukkit.nolagg.examine.segments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Shows a collection of all plugins
 */
public class MultiPluginSegment extends SegmentNode {
	
	public static MultiPluginSegment create(int duration, List<DataSegment> segments) {
		Map<String, List<DataSegment>> pluginsegments = new LinkedHashMap<String, List<DataSegment>>();
		for (DataSegment seg : segments) {
			List<DataSegment> plist = pluginsegments.get(seg.getPlugin());
			if (plist == null) {
				plist = new ArrayList<DataSegment>();
				pluginsegments.put(seg.getPlugin(), plist);
			}
			plist.add(seg.clone());
		}
		
		//sort
		List<PluginSegment> plugins = new ArrayList<PluginSegment>();
		for (Entry<String, List<DataSegment>> entry : pluginsegments.entrySet()) {
			Collections.sort(entry.getValue());
			plugins.add(new PluginSegment(entry.getKey(), duration, entry.getValue()));
		}
		Collections.sort(plugins);

		return new MultiPluginSegment(duration, plugins);
	}

	@Override
	public int getPluginCount() {
		return this.getChildren().length;
	}

	public MultiPluginSegment(int duration, List<PluginSegment> plugins) {
		super("Plugins", duration, plugins);
	}

	@Override
	public String getDescription() {
		StringBuilder builder = new StringBuilder(super.getDescription());
		builder.append('\n').append("Plugin count: ").append(this.getPluginCount());
		return builder.toString();
	}
}
