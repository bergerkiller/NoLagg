package com.bergerkiller.bukkit.nolagg.chunks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.ToggledState;
import com.bergerkiller.bukkit.common.collections.InterpolatedMap;
import com.bergerkiller.bukkit.common.collections.StringMap;
import com.bergerkiller.bukkit.common.config.CompressedDataReader;
import com.bergerkiller.bukkit.common.config.CompressedDataWriter;
import com.bergerkiller.bukkit.common.internal.CommonPlugin;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;
import com.bergerkiller.bukkit.nolagg.chunks.antiloader.DummyPlayerManager;

public class DynamicViewDistance {
	private static int viewDistance = CommonUtil.VIEW;
	private static int chunks = 0;
	private static final ToggledState chunksChanged = new ToggledState();
	private static InterpolatedMap nodes = new InterpolatedMap();
	private static Task task;
	private static final StringMap<Integer> viewLimits = new StringMap<Integer>();

	public static int getViewDistance(Player player) {
		if (!NoLaggChunks.useDynamicView) {
			return viewDistance;
		}
		Integer limit = viewLimits.getLower(player.getName());
		return limit == null ? viewDistance : Math.min(viewDistance, limit.intValue());
	}

	public static Integer getViewLimit(Player player) {
		return viewLimits.getLower(player.getName());
	}

	public static void setViewLimit(Player player, int limit) {
		if (limit >= CommonUtil.VIEW) {
			viewLimits.removeLower(player.getName());
		} else {
			viewLimits.putLower(player.getName(), Math.max(limit, 2));
		}
	}

	public static void addChunk() {
		chunks++;
		chunksChanged.set();
	}

	public static void removeChunk() {
		chunks--;
		chunksChanged.set();
	}

	public static void init(List<String> elements) {
		nodes.clear();
		viewDistance = CommonUtil.VIEW;
		chunks = 0;
		Task.stop(task);
		task = null;
		if (!NoLaggChunks.useDynamicView) {
			return;
		}

		// Load per-player limits from disk
		viewLimits.clear();
		new CompressedDataReader(NoLagg.plugin.getDataFile("PlayerViewLimits.dat")) {
			@Override
			public void read(DataInputStream stream) throws IOException {
				int count = stream.readInt();
				for (int i = 0; i < count; i++) {
					viewLimits.putLower(stream.readUTF(), (int) stream.readByte());
				}
			}
		}.read();
		
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
		if (nodes.isEmpty()) {
			return;
		}
		chunks = 0;
		for (World world : WorldUtil.getWorlds()) {
			chunks += WorldUtil.getChunks(world).size();
		}
		chunksChanged.set();

		task = new Task(NoLagg.plugin) {
			public void run() {
				if (chunksChanged.clear()) {
					viewDistance = MathUtil.clamp((int) nodes.get(chunks), 2, CommonUtil.VIEW);
				}
			}
		}.start(15, 40);
		
		CommonPlugin.getInstance().register(new DyanmicViewListener());
	}

	public static void deinit() {
		Task.stop(task);
		task = null;
//		DummyPlayerManager.revert(); Don't revert because it crashes the server :3

		File dataFile = NoLagg.plugin.getDataFile("PlayerViewLimits.dat");
		if (!viewLimits.isEmpty()) {
			new CompressedDataWriter(dataFile) {
				@Override
				public void write(DataOutputStream stream) throws IOException {
					stream.writeInt(viewLimits.size());
					for (Entry<String, Integer> entry : viewLimits.entrySet()) {
						stream.writeUTF(entry.getKey());
						stream.writeByte(entry.getValue());						
					}
				}
			}.write();
		} else if (dataFile.exists()) {
			dataFile.delete();
		}
	}
	
	/**
	 * Prevnet players from typing /reload
	 * It will totally bug out if you use it with dynamicView enabled
	 * @author lenis0012
	 */
	public static class DyanmicViewListener implements Listener {
		
		@EventHandler(priority = EventPriority.HIGHEST)
		public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
			final Player player = event.getPlayer();
			final String message = event.getMessage().toLowerCase();
			if(message.equals("/reload") || message.startsWith("/reload ")) {
				event.setCancelled(true);
				player.sendMessage(ChatColor.RED + "/reload is disabled by NoLagg because dynamicViewDistance is enabled, reloading would crash the server.");
			}
		}
	}
}
