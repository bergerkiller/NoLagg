package com.bergerkiller.bukkit.nolagg.chunks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.bergerkiller.bukkit.common.MessageBuilder;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.permissions.NoPermissionException;
import com.bergerkiller.bukkit.common.protocol.PacketType;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;
import com.bergerkiller.bukkit.nolagg.Permission;
import com.bergerkiller.bukkit.nolagg.chunks.antiloader.DummyInstanceMap;

/*
 * Important note:
 * The classes in the antiloader package ensure that chunks are NOT loaded by the Player Instance objects
 * Instead the sending queue is used to load the chunks. If any of the many component fail to initialize, this is not active.
 */
public class NoLaggChunks extends NoLaggComponent {
	private static Task chunkUnloadTask;
	public static NoLaggChunks plugin;
	public static boolean isOreObfEnabled = false;
	public static boolean useBufferedLoading = true;
	public static boolean useDynamicView = true;
	public static boolean hasDynamicView = false;

	@Override
	public void updateDependency(Plugin plugin, String pluginName, boolean enabled) {
		if (pluginName.equals("Chunk Manager")) {
			if (enabled) {
				log(Level.WARNING, "Chunk Manager detected, NoLaggChunks has been disabled.");
				log(Level.WARNING, "Either disable Chunk Manager or disable NoLaggChunks.");
				this.disable();
			}
		} else if (pluginName.equals("Orebfuscator")) {
			if (isOreObfEnabled = enabled && useBufferedLoading) {
				log(Level.INFO, "Orebfuscation has been detected and will be used when sending chunks");
				log(Level.INFO, "Note that this may require you to set more threads used for sending!");
			}
		}
		if (enabled && useBufferedLoading) {
			// Check that no plugin is currently listening for chunk packets
			Collection<Plugin> plugins = PacketUtil.getListenerPlugins(PacketType.MAP_CHUNK);
			if (!plugins.isEmpty()) {
				List<String> names = new ArrayList<String>(plugins.size());
				for (Plugin p : plugins) {
					final String name = p.getName();
					if (!name.equals("Orebfuscator")) {
						names.add(name);
					}
				}
				// Disable this entire feature to avoid needless bugs
				if (!names.isEmpty()) {
					log(Level.SEVERE, "Buffered (multi-threaded) packet sending disabled: Plugins are incompatible");
					log(Level.SEVERE, "Incompatible plugin(s): " + StringUtil.combineNames(names));
					log(Level.SEVERE, "Change 'bufferedLoading.enabled' to False in the config.yml of NoLagg to hide this error");
					useBufferedLoading = false;
					ChunkCompressionThread.deinit();
				}
			}
		}
	}

	@Override
	public void onReload(ConfigurationNode config) {
		config.setHeader("minRate", "The minimum chunk sending rate (chunks/tick)");
		config.setHeader("maxRate", "The maximum chunk sending rate (chunks/tick)");

		config.setHeader("bufferedLoader", "If you use a plugin that depends on the net server handler for packets, disable this");
		config.addHeader("bufferedLoader", "For example, Raw Critics' Ore Obfuscation does not function with this enabled.");
		config.addHeader("bufferedLoader", "Orebfuscator is supported and works with this enabled.");

		config.setHeader("bufferedLoader.enabled", "Whether or not to use the buffered packet loader to reduce new memory allocation");
		config.setHeader("bufferedLoader.threadCount", "The amount of threads to use to compress the chunk packets (increase if it can't keep up)");

		config.setHeader("useDynamicView", "Sets whether the dynamic view distance should be enforced");
		config.addHeader("useDynamicView", "If you use maxTPS, set this to false, or it will conflict!");
		config.setHeader("dynamicView", "Sets multiple view distances for different amounts of loaded chunks (chunk_count: view_chunks)");
		config.addHeader("dynamicView", "To disable, remove all chunk: view nodes. The view is smoothed out between nodes");
		config.addHeader("dynamicView", "The dynamic view distance will never be higher than the server view distance!");

		config.setHeader("sendOrder", "Sets in what order chunks are sent to the client");
		config.addHeader("sendOrder", "Available modes: " + StringUtil.combineNames(Arrays.asList(ChunkSendMode.values())));

		ChunkSendQueue.minRate = config.get("minRate", 0.25);
		ChunkSendQueue.maxRate = config.get("maxRate", 1.50);
		useBufferedLoading = config.get("bufferedLoader.enabled", true);
		useDynamicView = config.get("useDynamicView", true);
		ChunkCompressionThread.init(config.get("bufferedLoader.threadCount", 2));

		if (!config.contains("dynamicView")) {
			// Generate default views
			config.set("dynamicView", Arrays.asList("0 = 13", "5000 = 13", "10000 = 13", "60000 = 13"));
		}

		ChunkCoordComparator.init(config.get("sendOrder", ChunkSendMode.SLOPE));
		DynamicViewDistance.init(config.getList("dynamicView", String.class));
	}

	public void onEnable(ConfigurationNode config) {
		plugin = this;
		this.register(NLCListener.class);
		this.onReload(config);
		ChunkSendQueue.init();
		DummyInstanceMap.ENABLED = true;
	}

	public void onDisable(ConfigurationNode config) {
		ChunkSendQueue.deinit();
		ChunkCompressionThread.deinit();
		DynamicViewDistance.deinit();
		DummyInstanceMap.ENABLED = false;
		Task.stop(chunkUnloadTask);
		chunkUnloadTask = null;
	}

	@Override
	public boolean onCommand(CommandSender sender, String[] args) throws NoPermissionException {
		if (args.length == 0)
			return false;
		if (args[0].equalsIgnoreCase("sending")) {
			double avgrate = MathUtil.round(ChunkSendQueue.getAverageRate(), 2);
			double compbus = MathUtil.round(ChunkSendQueue.compressBusyPercentage, 2);
			if (sender instanceof Player) {
				Permission.CHUNKS_SENDING.handle(sender);

				// show sending information of the player
				ChunkSendQueue queue = ChunkSendQueue.bind((Player) sender);
				if (queue != null) {
					MessageBuilder msg = new MessageBuilder();
					msg.green("You receive ").yellow(MathUtil.round(queue.getRate(), 2));
					msg.green(" chunks each tick (");
					msg.yellow(avgrate).yellow(" avg").green(")").newLine();
					msg.green("You have received ");
					int sent = CommonUtil.CHUNKAREA - queue.getPendingSize();
					double per = MathUtil.round((double) sent / (double) CommonUtil.CHUNKAREA * 100.0, 2);
					msg.yellow(sent).white("/").yellow(CommonUtil.CHUNKAREA).green(" chunks (");
					msg.yellow(per, "%").green(")").newLine();
					msg.green("Your packet buffer is ").green(queue.getBufferLoadMsg()).green(" used");
					msg.newLine().green("Chunk compression is ");
					if (compbus > 70) {
						msg.red(compbus, "%");
					} else if (compbus > 40) {
						msg.yellow(compbus, "%");
					} else {
						msg.green(compbus, "%");
					}
					msg.green(" of the time busy");

					msg.send(sender);
				} else {
					sender.sendMessage("An unknown error occured!");
				}
			} else {
				// show average sending info
				sender.sendMessage("Average chunk sending rate: " + avgrate + " chunks each tick");
				sender.sendMessage("Chunk compression is " + compbus + "% of the time busy");
			}
			return true;
		}
		return false;
	}

}
