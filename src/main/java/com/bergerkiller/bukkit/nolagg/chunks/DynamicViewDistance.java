package com.bergerkiller.bukkit.nolagg.chunks;

import java.util.Iterator;
import java.util.List;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.collections.InterpolatedMap;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;
import com.bergerkiller.bukkit.nolagg.chunks.antiloader.DummyPlayerManager;

public class DynamicViewDistance {
	public static int viewDistance = CommonUtil.VIEW;
	private static int chunks = 0;
	private static boolean chunksChanged = false;
	private static InterpolatedMap nodes = new InterpolatedMap();
	private static Task task;

	public static void addChunk() {
		chunks++;
		chunksChanged = true;
	}

	public static void removeChunk() {
		chunks--;
		chunksChanged = true;
	}

	public static void init(List<String> elements) {
		nodes.clear();
		viewDistance = CommonUtil.VIEW;
		chunks = 0;
		Task.stop(task);
		task = null;
		NoLaggChunks.hasDynamicView = false;
		if (!NoLaggChunks.useDynamicView) {
			return;
		}

		// Convert all worlds to use the new player manager
		DummyPlayerManager.convertAll();

		int lowest = Integer.MAX_VALUE;
		Iterator<String> iter = elements.iterator();
		while (iter.hasNext()) {
			try {
				String[] elem = iter.next().split("=");
				if (elem.length == 2) {
					int chunks = Integer.parseInt(elem[0].trim());
					int view = Integer.parseInt(elem[1].trim());
					if (view >= 2 && chunks >= 0) {
						nodes.put(chunks, view);
						lowest = Math.min(lowest, view);
						continue;
					}
				}
			} catch (Exception ex) {
			}
			iter.remove();
		}
		if (nodes.isEmpty() || lowest >= CommonUtil.VIEW) {
			return;
		}
		NoLaggChunks.hasDynamicView = true;
		chunks = 0;
		for (World world : WorldUtil.getWorlds()) {
			chunks += WorldUtil.getChunks(world).size();
		}
		chunksChanged = true;

		task = new Task(NoLagg.plugin) {
			public void run() {
				if (chunksChanged) {
					chunksChanged = false;
				} else {
					return;
				}
				viewDistance = MathUtil.clamp((int) nodes.get(chunks), 3, CommonUtil.VIEW);

			}
		}.start(15, 40);
	}

	public static void deinit() {
		Task.stop(task);
		task = null;
		DummyPlayerManager.revert();
	}
}
