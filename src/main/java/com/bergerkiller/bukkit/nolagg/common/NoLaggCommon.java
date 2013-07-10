package com.bergerkiller.bukkit.nolagg.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.timedbukkit.craftbukkit.scheduler.CancelCounterExecutor;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.MessageBuilder;
import com.bergerkiller.bukkit.common.collections.StringMap;
import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.permissions.NoPermissionException;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.common.utils.PlayerUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.EntitySelector;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;
import com.bergerkiller.bukkit.nolagg.NoLaggComponents;
import com.bergerkiller.bukkit.nolagg.NoLaggUtil;
import com.bergerkiller.bukkit.nolagg.Permission;
import com.bergerkiller.bukkit.nolagg.itembuffer.ItemMap;
import com.bergerkiller.bukkit.nolagg.tnt.NoLaggTNT;
import com.bergerkiller.bukkit.nolagg.tnt.TNTHandler;

public class NoLaggCommon extends NoLaggComponent {
	private final Set<String> lastargs = new HashSet<String>();
	private StringMap<List<String>> clearShortcuts = new StringMap<List<String>>();

	@Override
	public void onDisable(ConfigurationNode config) {
		this.clearShortcuts.clear();
	}

	@Override
	public void onEnable(ConfigurationNode config) {
		this.onReload(config);
	}

	@Override
	public void onReload(ConfigurationNode config) {
		// clear shortcuts
		this.clearShortcuts.clear();
		config.setHeader("clearShortcuts", "\nDefines all shortcuts for the /lag clear command, more can be added");
		if (!config.contains("clearShortcuts")) {
			ConfigurationNode node = config.getNode("clearShortcuts");
			node.set("enemies", Arrays.asList("monster"));
			node.set("notneutral", Arrays.asList("monster", "item", "tnt", "egg", "arrow"));
		}
		if (!config.contains("clearShortcuts.all")) {
			config.setHeader("clearShortcuts.all", "The entity types removed when using /lag clear all");
			config.set("clearShortcuts.all", Arrays.asList("items", "mobs", "fallingblocks", "tnt", "xporb", "minecart", "boat"));
		}
		if (!config.contains("clearShortcuts.default")) {
			config.setHeader("clearShortcuts.default", "The entity types removed when using /lag clear without arguments");
			config.set("clearShortcuts.default", Arrays.asList("items", "tnt", "xporb"));
		}
		ConfigurationNode shortc = config.getNode("clearShortcuts");
		shortc.setHeader("");
		shortc.addHeader("Several shortcuts you can use for the /nolagg clear(all) command");
		for (String key : shortc.getKeys()) {
			clearShortcuts.putLower(key, shortc.getList(key, String.class));
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, String[] args) throws NoPermissionException {
		if (args.length > 0) {
			boolean all = args[0].equalsIgnoreCase("clearall");
			if (args[0].equalsIgnoreCase("clear") || all) {
				if (sender instanceof Player) {
					Permission.COMMON_CLEAR.handle(sender);
				} else {
					all = true;
				}
				// Get the worlds to work in
				Collection<World> worlds;
				if (all) {
					worlds = Bukkit.getServer().getWorlds();
				} else {
					worlds = Arrays.asList(((Player) sender).getWorld());
				}
				// Read all the requested entity types
				final Set<String> types = new HashSet<String>();
				double radius = Double.MAX_VALUE;
				if (args.length > 1) {
					// Read the types
					List<String> tmpList;
					ArrayList<String> inputTypes = new ArrayList<String>();
					for (int i = 1; i < args.length; i++) {
						final String name = args[i];
						tmpList = clearShortcuts.getLower(name);
						if (tmpList != null) {
							inputTypes.addAll(tmpList);
						} else {
							inputTypes.add(name.toLowerCase(Locale.ENGLISH));
						}
					}
					for (String name : inputTypes) {
						if (ParseUtil.isNumeric(name)) {
							radius = ParseUtil.parseDouble(name, radius);
							continue;
						}
						if (name.equals("minecart")) {
							name = "minecarts";
						} else if (name.equals("item")) {
							name = "items";
						} else if (name.equals("monster")) {
							name = "monsters";
						} else if (name.equals("animal")) {
							name = "animals";
						} else if (name.equals("fallingblock")) {
							name = "fallingblocks";
						}
						if (name.contains("xp") || name.contains("orb")) {
							types.add("experienceorb");
							continue;
						}
						if (name.contains("tnt")) {
							types.add("tnt");
							continue;
						}
						if (name.contains("mob")) {
							types.add("animals");
							types.add("monsters");
							continue;
						}
						types.add(name);
					}
					if (types.remove("last")) {
						sender.sendMessage(ChatColor.GREEN + "The last-used clear arguments are also used");
						types.addAll(lastargs);
					}
				}
				if (types.isEmpty()) {
					// Default types
					types.addAll(clearShortcuts.get("default"));
				}
				lastargs.clear();
				lastargs.addAll(types);

				// Remove from TNT component if enabled
				if (NoLaggComponents.TNT.isEnabled() && (types.contains("all") || types.contains("tnt"))) {
					TNTHandler handler = NoLaggTNT.plugin.getTNTHandler();
					if (all) {
						handler.clear();
					} else {
						for (World world : worlds) {
							handler.clear(world);
						}
					}
				}

				// Prepare an Entity Selector
				final EntitySelector entitySelector;
				if (radius == Double.MAX_VALUE || !(sender instanceof Player)) {
					entitySelector = new TypedEntitySelector(types);
				} else {
					final double searchRadiusSq = radius * radius;
					final Location middle = ((Player) sender).getLocation();
					final Location locBuffer = new Location(null, 0.0, 0.0, 0.0);
					entitySelector = new TypedEntitySelector(types) {
						@Override
						public boolean check(Entity entity) {
							// Perform distance check (we disallow other worlds too)
							if (entity.getWorld() != middle.getWorld()
									|| entity.getLocation(locBuffer).distanceSquared(middle) > searchRadiusSq) {
								return false;
							}
							return super.check(entity);
						}
					};
				}

				// Remove items from the item buffer
				if (NoLaggComponents.ITEMBUFFER.isEnabled()) {
					ItemMap.clear(worlds, entitySelector);
				}

				// Entity removal logic
				int remcount = 0; // The amount of removed entities
				for (World world : worlds) {
					// Use the types set and clear them
					for (Entity e : world.getEntities()) {
						if (entitySelector.check(e)) {
							e.remove();
							remcount++;
						}
					}
				}

				// Final confirmation message
				if (all) {
					sender.sendMessage(ChatColor.YELLOW + "All worlds have been cleared: " + remcount + " entities removed!");
				} else {
					sender.sendMessage(ChatColor.YELLOW + "This world has been cleared: " + remcount + " entities removed!");
				}
			} else if (args[0].equalsIgnoreCase("resend")) {
				Permission.COMMON_RESEND.handle(sender);
				if (sender instanceof Player) {
					Player p = (Player) sender;
					int radius = Bukkit.getServer().getViewDistance();
					if (args.length == 2) {
						try {
							radius = Math.min(radius, Integer.parseInt(args[1]));
						} catch (Exception ex) {
						}
					}
					int cx = p.getLocation().getBlockX() >> 4;
					int cz = p.getLocation().getBlockZ() >> 4;
					for (int a = -radius; a <= radius; a++) {
						for (int b = -radius; b <= radius; b++) {
							for (Player player : WorldUtil.getPlayers(p.getWorld())) {
								if (PlayerUtil.isNearChunk(player, cx + a, cz + b, CommonUtil.VIEW)) {
									PlayerUtil.queueChunkSend(player, cx + a, cz + b);
								}
							}
						}
					}
					// Make sure all entities are re-sent as well
					WorldUtil.getTracker(p.getWorld()).removeViewer(p);
					p.sendMessage(ChatColor.GREEN + "A " + (radius * 2 + 1) + " X " + (radius * 2 + 1) + " chunk area around you is being resent...");
					return true;
				}
			} else if (args[0].equalsIgnoreCase("gc")) {
				Permission.COMMON_GC.handle(sender);
				Runtime.getRuntime().gc();
				sender.sendMessage("Memory garbage collected!");
			} else if (args[0].equalsIgnoreCase("clean")) {
				Permission.COMMON_CLEAN.handle(sender);
				final Runtime runtime = Runtime.getRuntime();
				final long startMemory = runtime.totalMemory() - runtime.freeMemory();
				sender.sendMessage(ChatColor.YELLOW + "Cleaning up server memory...");

				// Register unloader hook to see what plugins are canceling what unloads
				for (RegisteredListener listener : ChunkUnloadEvent.getHandlerList().getRegisteredListeners()) {
					EventExecutor baseExec = NoLaggUtil.exefield.get(listener);
					EventExecutor hookExec = new CancelCounterExecutor(baseExec, listener.getPlugin());
					NoLaggUtil.exefield.set(listener, hookExec);
				}

				// Obtain a list of all loaded chunks
				Collection<Chunk> chunks = new ArrayList<Chunk>();
				for (World world : WorldUtil.getWorlds()) {
					chunks.addAll(WorldUtil.getChunks(world));
				}

				// Try to unload all of them
				int playerInUseCount = 0;
				int spawnAreaCount = 0;
				int unloadCount = 0;
				for (Chunk chunk : chunks) {
					World w = chunk.getWorld();
					if (w.getKeepSpawnInMemory()) {
						Location spawn = w.getSpawnLocation();
						int spawnX = spawn.getBlockX() >> 4;
						int spawnZ = spawn.getBlockZ() >> 4;
						if (Math.abs(spawnX - chunk.getX()) <= 12 && Math.abs(spawnZ - chunk.getZ()) <= 12) {
							spawnAreaCount++;
							continue;
						}
					}
					if (w.isChunkInUse(chunk.getX(), chunk.getZ())) {
						playerInUseCount++;
						continue;
					}
					// Fire the event (because Bukkit doesn't!)
					ChunkUnloadEvent event = new ChunkUnloadEvent(chunk);
					if (!CommonUtil.callEvent(event).isCancelled() && chunk.unload(true)) {
						unloadCount++;
					}
				}

				// Gather the plugin-specific unloading data and unhook the cancel counter hooks
				HashMap<Plugin, PluginStatistic> pluginStatistics = new HashMap<Plugin, PluginStatistic>();
				for (RegisteredListener listener : ChunkUnloadEvent.getHandlerList().getRegisteredListeners()) {
					// Obtain and validate the registered hooks
					CancelCounterExecutor hookExec = CommonUtil.tryCast(NoLaggUtil.exefield.get(listener), CancelCounterExecutor.class);
					if (hookExec == null) {
						continue;
					}
					// Unhook
					EventExecutor baseExec = hookExec.getProxyBase();
					NoLaggUtil.exefield.set(listener, baseExec);
					// Gather data
					PluginStatistic stat = pluginStatistics.get(hookExec.owner);
					if (stat == null) {
						stat = new PluginStatistic(hookExec.owner);
						pluginStatistics.put(hookExec.owner, stat);
					}
					stat.count += hookExec.cancelCount;
				}
				List<PluginStatistic> statistics = new ArrayList<PluginStatistic>(pluginStatistics.values());
				Collections.sort(statistics);
	
				// Display the statistics information to the user
				final int maxPluginStats = 3;
				final int statCount = Math.min(statistics.size(), maxPluginStats);
				MessageBuilder message = new MessageBuilder();
				message.green("A total of ").yellow(unloadCount).green(" out of ").yellow(chunks.size()).green(" could be unloaded").newLine();
				message.green("Reasons for chunks not to be unloaded are:").newLine();
				message.green("- ").yellow(playerInUseCount).green(" because players are near").newLine();
				message.green("- ").yellow(spawnAreaCount).green(" are part of world spawn area").newLine();
				for (int i = 0; i < statCount; i++) {
					PluginStatistic stat = statistics.get(i);
					if (stat.count == 0) {
						continue;
					}
					message.green("- ").yellow(stat.count).green(" are cancelled by ").yellow(stat.plugin.getName()).newLine();
				}
				int remCount = 0;
				for (int i = statCount - 1; i < statistics.size(); i++) {
					remCount += statistics.get(i).count;
				}
				if (remCount > 0) {
					message.green("- ").yellow(remCount).green(" are cancelled by other plugins").newLine();
				}
				message.newLine();
				message.green("Now saving worlds and garbage collecting memory...");
				message.send(sender);
				if (sender instanceof Player) {
					message.log(Level.INFO);
				}
				// Start a new thread to save the worlds on, and to execute GC
				final String rec = sender instanceof Player ? sender.getName() : null;
				new AsyncTask() {
					public void run() {
						for (World world : Bukkit.getWorlds()) {
							WorldUtil.saveToDisk(world);
						}
						runtime.gc();

						// Prepare the message
						MessageBuilder message = new MessageBuilder();
						long memChange = (startMemory - (runtime.totalMemory() - runtime.freeMemory())) >> 20;
						if (memChange >= 0) {
							message.green("A total of ").yellow(memChange, " MB").green(" was freed");
						} else {
							message.red("Memory increased by ").yellow(memChange, " MB");
						}

						// Send the completion message
						if (rec != null) {
							Player p = Bukkit.getPlayer(rec);
							if (p != null) {
								message.send(p);
							}
						}
						message.log(Level.INFO);
					}
				}.start(false);
			} else {
				return false;
			}
			return true;
		}
		return false;
	}

	private static class PluginStatistic implements Comparable<PluginStatistic> {
		public final Plugin plugin;
		public int count = 0;

		public PluginStatistic(Plugin plugin) {
			this.plugin = plugin;
		}

		/**
		 * Note: equals only checks the count since sorting causes errors otherwise
		 */
		@Override
		public boolean equals(Object o) {
			return o instanceof PluginStatistic && ((PluginStatistic) o).count == this.count;
		}

		@Override
		public int compareTo(PluginStatistic o) {
			return o.count - this.count;
		}
	}

	private static class TypedEntitySelector implements EntitySelector {
		private final Set<String> types;
		private final boolean monsters;
		private final boolean animals;
		private final boolean items;
		private final boolean fallingblocks;
		private final boolean minecarts;
		private final boolean tnt;

		public TypedEntitySelector(Set<String> types) {
			this.types = types;
			this.monsters = types.contains("monsters");
			this.animals = types.contains("animals");
			this.items = types.contains("items");
			this.fallingblocks = types.contains("fallingblocks");
			this.minecarts = types.contains("minecarts");
			this.tnt = types.contains("tnt");
		}

		@Override
		public boolean check(Entity entity) {
			if (entity instanceof Player) {
				return false;
			}
			if (monsters && EntityUtil.isMonster(entity)) {
				return true;
			} else if (animals && EntityUtil.isAnimal(entity)) {
				return true;
			} else if (items && entity instanceof Item) {
				return true;
			} else if (fallingblocks && entity instanceof FallingBlock) {
				return true;
			} else if (minecarts && entity instanceof Minecart) {
				return true;
			} else if (tnt && entity instanceof TNTPrimed) {
				return true;
			} else if (types.contains(EntityUtil.getName(entity))) {
				return true;
			} else {
				return false;
			}
		}
	}
}
