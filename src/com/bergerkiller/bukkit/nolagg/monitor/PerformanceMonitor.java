package com.bergerkiller.bukkit.nolagg.monitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.logging.Level;

import net.minecraft.server.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;

import com.bergerkiller.bukkit.common.StopWatch;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;
import com.bergerkiller.bukkit.nolagg.NoLaggComponents;
import com.bergerkiller.bukkit.nolagg.Permission;
import com.bergerkiller.bukkit.nolagg.chunks.ChunkSendQueue;
import com.bergerkiller.bukkit.nolagg.chunks.DynamicViewDistance;
import com.bergerkiller.bukkit.nolagg.lighting.LightingFixThread;
import com.bergerkiller.bukkit.nolagg.saving.AutoSaveChanger;
import com.bergerkiller.bukkit.nolagg.tnt.TNTHandler;

public class PerformanceMonitor extends Task {

	public PerformanceMonitor() {
		super(NoLagg.plugin);
	}

	public static int monitorInterval = 40;

	public static long prevtime;
	public static long prevusedmem;
	public static long minmem;

	private static long prevlaggingmsg = System.currentTimeMillis();
	public static boolean broadcastLagging;
	public static String broadcastMessage;
	public static long broadcastInterval;
	public static double broadcastThreshold;

	private static final double strength = 0.7;
	public static final ProcessTime entityListGen = new ProcessTime("Entity lstng", ChatColor.GREEN);
	public static final ProcessTime entitySpawnLimit = new ProcessTime("Spwn limtr", ChatColor.DARK_GREEN);
	public static final ProcessTime chunkUpdate = new ProcessTime("Chnk updtr", ChatColor.DARK_RED);
	public static final ProcessTime chunkSend = new ProcessTime("Chnk sending", ChatColor.RED);
	private static final ProcessTime[] times = new ProcessTime[] { entityListGen, entitySpawnLimit, chunkUpdate, chunkSend };

	public static ArrayList<String> recipients = new ArrayList<String>();
	public static HashSet<String> removalReq = new HashSet<String>();
	public static boolean sendConsole = false;
	public static boolean removalCon = false;
	public static boolean sendLog = false;
	private static final Runtime runtime = Runtime.getRuntime();
	private static PerformanceMonitor pm;
	private static BufferedWriter logger;
	private static boolean wroteHeader = false;

	private static File logfile;

	public static void init() {
		pm = new PerformanceMonitor();
		prevtime = System.currentTimeMillis();
		prevusedmem = runtime.maxMemory() - runtime.freeMemory();
		minmem = prevusedmem;
		pm.start(monitorInterval, monitorInterval);
		// set up logger
		logfile = NoLagg.plugin.getDataFolder();
		logfile.mkdirs();
		logfile = new File(logfile + File.separator + "log.txt");
		try {
			logger = new BufferedWriter(new FileWriter(logfile, true));
			log("NoLagg enabled: " + getStamp());
			logger.flush();
		} catch (IOException ex) {
			NoLaggMonitor.plugin.log(Level.SEVERE, "Failed to initialize performance logger file stream:");
			ex.printStackTrace();
		}
	}

	public static void deinit() {
		Task.stop(pm);
		pm = null;

		// set up logger
		if (logger != null) {
			try {
				log("NoLagg disabled: " + getStamp());
				logger.flush();
				logger.close();
			} catch (IOException ex) {
			}
			logger = null;
		}
		recipients.clear();
		removalReq.clear();
	}

	private static String getProgress(int length, ChatColor color) {
		if (length <= 0)
			return "";
		StringBuilder sb = new StringBuilder(length + 1);
		sb.append(color);
		for (int i = 0; i < length; i++) {
			sb.append('|');
		}
		return sb.toString();
	}

	private static String getMemoryProgress(int length, long current, long min, long max) {
		double factor = (double) length / max;
		StringBuilder sb = new StringBuilder(length + 3);
		int used = (int) (factor * min);
		int unused = (int) (factor * (max - current));
		sb.append(getProgress(used, ChatColor.GREEN));
		sb.append(getProgress(length - unused - used, ChatColor.YELLOW));
		sb.append(getProgress(unused, ChatColor.RED));
		return sb.toString();
	}

	private static int mem(double value) {
		return mem((long) value);
	}

	private static int mem(long value) {
		return (int) (value / 1048576);
	}

	public static boolean clearLog() {
		if (logger != null) {
			try {
				logger.close();
			} catch (Exception ex) {
				return false;
			}
		}
		if (logfile.delete()) {
			try {
				logger = new BufferedWriter(new FileWriter(logfile, true));
				wroteHeader = false;
				return true;
			} catch (Exception ex) {
				return false;
			}
		} else {
			return false;
		}
	}

	private static void log(String message) throws IOException {
		logger.write(message);
		logger.newLine();
	}

	public static boolean writeLog(String message) {
		try {
			log(message);
			return true;
		} catch (IOException ex) {
			return false;
		}
	}

	private static String getTime() {
		final SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss");
		return sdf.format(Calendar.getInstance().getTime());
	}

	private static String getStamp() {
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd H:mm:ss");
		return sdf.format(Calendar.getInstance().getTime());
	}

	private static String getColumn(String text) {
		return " " + text + "		|";
	}

	public static class ProcessTime extends StopWatch {
		public ProcessTime(String name, ChatColor color) {
			this.name = name;
			this.color = color;
		}

		public StopWatch stop() {
			return super.stop(strength);
		}

		public StopWatch next() {
			return super.next(strength);
		}

		public String name;
		public ChatColor color;
		public int barlength = 0;

		public String toString() {
			return this.color + "[" + this.name + "]";
		}
	}

	public static String getMemory(boolean player) {
		StringBuilder builder = new StringBuilder();
		if (player) {
			builder.append(ChatColor.YELLOW).append("Memory: ");
			builder.append(getMemoryProgress(50, usedmem, minmem, maxmem));
			builder.append(ChatColor.YELLOW).append(" ").append(mem(minmem)).append("/").append(mem(maxmem)).append(" MB ");
			if (diff > 0) {
				builder.append(ChatColor.RED).append("(+").append(mem(diff / elapsedtimesec)).append(" MB/s)");
			} else {
				builder.append(ChatColor.GREEN).append("(GC)");
			}
		} else {
			builder.append("Memory: ").append(mem(minmem)).append("/").append(mem(maxmem)).append(" MB (+");
			builder.append(mem(usedmem - minmem)).append(" modified)");
			builder.append("(+").append(mem(diff / elapsedtimesec)).append(" MB/s)");
		}
		return builder.toString();
	}

	public static String getTPS(boolean player) {
		StringBuilder builder = new StringBuilder();
		builder.append(ChatColor.YELLOW).append("Ticks per second: ");
		if (player) {
			if (tps >= 17 && tps <= 23) {
				builder.append(ChatColor.GREEN);
			} else if (tps >= 14 && tps <= 26) {
				builder.append(ChatColor.GOLD);
			} else {
				builder.append(ChatColor.RED);
			}
		}
		builder.append(MathUtil.round(tps, 1)).append(" [");
		builder.append(MathUtil.round(tps * 5, 0));
		builder.append("%]");
		return builder.toString();
	}

	public static long maxmem;
	public static long usedmem;
	public static double elapsedtimesec;
	public static long elapsedtime;
	public static long diff;
	public static double tps = 0;

	public static String replaceColors(String line) {
		int index = 0;
		while (true) {
			index = line.indexOf('&', index);
			if (index >= 0 && index < line.length() - 1) {
				char next = line.charAt(index + 1);
				if (next == '0' || next == '1' || next == '2' || next == '3' || next == '4' || next == '5' || next == '6' || next == '7' || next == '8' || next == '9' || next == 'a' || next == 'b'
						|| next == 'c' || next == 'd' || next == 'e' || next == 'f') {
					line = line.substring(0, index) + '§' + line.substring(index + 1);
				}
				index++;
			} else {
				break;
			}
		}
		return line;
	}

	@Override
	public void run() {
		long time = System.currentTimeMillis();
		elapsedtime = time - prevtime;
		elapsedtimesec = (double) elapsedtime / 1000;
		maxmem = runtime.maxMemory();
		usedmem = runtime.totalMemory() - runtime.freeMemory();
		diff = usedmem - prevusedmem;
		if (diff < 0) {
			minmem = usedmem;
		}
		tps = monitorInterval / elapsedtimesec;
		if (tps < broadcastThreshold && broadcastLagging) {
			if (System.currentTimeMillis() > (prevlaggingmsg + broadcastInterval)) {
				prevlaggingmsg = System.currentTimeMillis();
				String msg = replaceColors(broadcastMessage);
				for (Player player : Bukkit.getServer().getOnlinePlayers()) {
					if (Permission.MONITOR_NOTIFYLAGGING.has(player)) {
						player.sendMessage(msg);
					}
				}
			}
		}
		double chunksendbusy = 0.0;
		if (NoLaggComponents.CHUNKS.isEnabled()) {
			chunksendbusy = MathUtil.round(ChunkSendQueue.compressBusyPercentage, 2);
		}
		if (sendLog || sendConsole || recipients.size() > 0) {
			int mobcount = 0;
			int entitycount = 0;
			int itemcount = 0;
			int tntcount = 0;
			int bufftnt = NoLaggComponents.TNT.isEnabled() ? TNTHandler.getBufferCount() : 0;
			int playercount = 0;
			for (World w : Bukkit.getServer().getWorlds()) {
				for (Entity e : w.getEntities()) {
					if (e.isDead())
						continue;
					if (e instanceof Player) {
						playercount++;
					} else if (EntityUtil.isMob(e)) {
						mobcount++;
					} else if (e instanceof Item) {
						itemcount++;
					} else if (e instanceof TNTPrimed) {
						tntcount++;
					}
					entitycount++;
				}
			}
			int lighting;
			if (NoLaggComponents.LIGHTING.isEnabled()) {
				lighting = LightingFixThread.getPendingSize();
			} else {
				lighting = 0;
			}

			int totalchunkcount = 0;
			int totaluchunkcount = 0;

			for (WorldServer world : WorldUtil.getWorlds()) {
				int count = world.chunkProviderServer.getLoadedChunks();
				totalchunkcount += count;
				totaluchunkcount += count;
				if (world.keepSpawnInMemory) {
					totaluchunkcount -= 144;
				}
			}

			double totaltime = 0;
			String maxelement = null;
			{
				double max = Double.MIN_VALUE;
				for (ProcessTime ptime : times) {
					double dur = ptime.get(monitorInterval);
					totaltime += dur;
					if (dur > max) {
						max = dur;
						maxelement = ptime.name;
					}
				}
			}
			// ================
			if (sendLog && logger != null) {
				try {
					if (!wroteHeader) {
						wroteHeader = true;
						String columns = "";
						columns += "| Time		| Tick rate		| Total Memory	| Static Memory	| Dynamic Memory	";
						columns += "| Memory Write Rate	| Total Chunks	| Unloadable Chunks	";
						columns += "| Chunks Loaded	| Chunks generated	| Chunks Unloaded	| Lighting Fixes	";
						columns += "| Chunk Packets	| Entities		| Mobs		| Items		| TNT		| Players		| Update		| Update type	|";
						log(columns);
					}
					String msg = "";
					msg += "|" + getColumn(getTime());
					msg += getColumn(String.valueOf(MathUtil.round(tps, 1)));
					msg += getColumn(mem(maxmem) + " MB");
					msg += getColumn(mem(minmem) + " MB");
					msg += getColumn(mem(usedmem - minmem) + " MB");
					if (diff <= 0) {
						msg += getColumn("GC");
					} else {
						msg += getColumn(mem(diff / elapsedtimesec) + " MB/s");
					}
					msg += getColumn(String.valueOf(totalchunkcount));
					msg += getColumn(String.valueOf(totaluchunkcount));
					msg += getColumn(String.valueOf(NLMListener.loadedChunks));
					msg += getColumn(String.valueOf(NLMListener.generatedChunks));
					msg += getColumn(String.valueOf(NLMListener.unloadedChunks));
					msg += getColumn(String.valueOf(lighting));
					msg += getColumn(String.valueOf(bufftnt));
					msg += getColumn(String.valueOf(entitycount));
					msg += getColumn(String.valueOf(mobcount));
					msg += getColumn(String.valueOf(itemcount));
					msg += getColumn(String.valueOf(tntcount));
					msg += getColumn(String.valueOf(playercount));
					msg += getColumn(MathUtil.round(totaltime, 1) + " ms");
					msg += getColumn(maxelement);
					log(msg);
					logger.flush();
				} catch (IOException ex) {
					NoLaggMonitor.plugin.log(Level.SEVERE, "Logging disabled:");
					ex.printStackTrace();
					try {
						logger.close();
					} catch (IOException e) {
					}
					logger = null;
				}
			}

			if (sendConsole) {
				CommandSender s = Bukkit.getServer().getConsoleSender();
				// Line
				s.sendMessage("-");
				String mem;
				// update times
				mem = "Update: " + MathUtil.round(totaltime, 1) + " ms (" + maxelement + " took longest)";
				s.sendMessage(mem);
				// memory
				s.sendMessage(getMemory(false));
				// chunks
				mem = "Chunks: ";
				mem += totalchunkcount + " [" + totaluchunkcount + " Unloadable]";
				mem += " [+" + (NLMListener.loadedChunks + NLMListener.generatedChunks) + "]";
				mem += " [-" + NLMListener.unloadedChunks + "]";
				s.sendMessage(mem);
				// Entities
				mem = "Entities: " + entitycount + " [" + mobcount + " mobs]";
				mem += " [" + itemcount + " items] [" + tntcount + " mobile TNT]";
				s.sendMessage(mem);
				// Compression thread busy
				mem = "Chunk packet sending thread: " + chunksendbusy + "% busy";
				s.sendMessage(mem);
				// Tick times
				s.sendMessage(getTPS(false));

				if (removalCon) {
					removalCon = false;
					sendConsole = false;
				}
			}
			if (recipients.size() > 0) {
				ArrayList<String> messages = new ArrayList<String>(6);
				// Line
				messages.add("");

				String mem;
				// Global update times
				if (totaltime > 0) {
					mem = "";
					for (ProcessTime ptim : times)
						mem += ptim.toString();
					messages.add(mem);

					final int length = 50;
					// get bar lengths
					int totallength = 0;
					for (ProcessTime ptime : times) {
						totallength += (ptime.barlength = (int) (length * ptime.get(monitorInterval) / totaltime));
					}
					int i = 0;
					while (totallength > length) {
						times[i].barlength--;
						totallength--;
						i++;
						if (i > times.length - 1)
							i = 0;
					}
					while (totallength < length) {
						times[i].barlength++;
						totallength++;
						i++;
						if (i > times.length - 1)
							i = 0;
					}
					// get message

					mem = ChatColor.YELLOW + "Update: ";
					for (ProcessTime ptime : times) {
						mem += getProgress(ptime.barlength, ptime.color);
					}
					mem += ChatColor.YELLOW + " (" + MathUtil.round(totaltime, 1) + " MS/tick)";
					if (NoLaggComponents.SAVING.isEnabled()) {
						if (AutoSaveChanger.isSaving()) {
							mem += ChatColor.RED + " (SAVING " + AutoSaveChanger.SAVE_PERCENTAGE + "%)";
						}
					}
					messages.add(mem);
				}

				// Memory
				messages.add(getMemory(true));
				// Tick times
				messages.add(getTPS(true));
				// Chunks
				mem = ChatColor.YELLOW + "Chunks: " + ChatColor.GOLD;
				mem += totalchunkcount + " [" + totaluchunkcount + " U]";
				mem += ChatColor.GREEN + " [+" + NLMListener.loadedChunks + "]";
				mem += ChatColor.YELLOW + " [+" + NLMListener.generatedChunks + "]";
				mem += ChatColor.RED + " [-" + NLMListener.unloadedChunks + "]";
				mem += ChatColor.GOLD + " [" + lighting + " lighting]";
				messages.add(mem);
				// Buffering
				// entities
				mem = ChatColor.YELLOW + "Entities: " + ChatColor.GOLD;
				mem += entitycount + " " + ChatColor.DARK_GREEN + "[" + mobcount + " mobs]" + ChatColor.YELLOW + " [" + itemcount + " items] ";
				mem += ChatColor.GREEN + "[" + tntcount + " TNT] " + ChatColor.AQUA + "[" + playercount + " players]";
				messages.add(mem);
				// Compression thread busy
				mem = ChatColor.YELLOW + "Packet compression busy: ";
				if (chunksendbusy > 60) {
					mem += ChatColor.RED;
				} else if (chunksendbusy > 30) {
					mem += ChatColor.GOLD;
				} else {
					mem += ChatColor.GREEN;
				}
				mem += chunksendbusy + "% busy";
				messages.add(mem);

				// average send rate
				double avgrate = 0;
				if (NoLaggComponents.CHUNKS.isEnabled()) {
					avgrate = ChunkSendQueue.getAverageRate();
				}

				int i = 0;
				while (i < recipients.size()) {
					String name = recipients.get(i);
					Player p = Bukkit.getServer().getPlayer(name);
					if (p != null) {
						for (String msg : messages) {
							p.sendMessage(msg);
						}
						// send individual sending rate
						if (NoLaggComponents.CHUNKS.isEnabled()) {
							ChunkSendQueue queue = ChunkSendQueue.bind(p);
							int tosend = CommonUtil.chunkArea - queue.getPendingSize();
							String msg = ChatColor.YELLOW + "Chunk sending: " + ChatColor.GREEN + (tosend * 100 / CommonUtil.chunkArea) + "%";
							msg += ChatColor.YELLOW + " at " + ChatColor.GREEN + MathUtil.round(queue.getRate(), 2) + " chunks/tick (" + MathUtil.round(avgrate, 2) + " avg)";
							p.sendMessage(msg);
							msg = ChatColor.YELLOW + "Packet buffer size: " + queue.getBufferLoadMsg();
							p.sendMessage(msg);
							msg = ChatColor.YELLOW + "Dynamic view distance: " + ChatColor.GREEN + DynamicViewDistance.viewDistance + ChatColor.YELLOW + " chunks";
							p.sendMessage(msg);
						}
						if (removalReq.remove(name)) {
							recipients.remove(i);
						} else {
							i++;
						}
					} else {
						removalReq.remove(name);
						recipients.remove(i);
					}

				}
			}
		}

		// reset processing times calculations
		for (ProcessTime ptime : times) {
			ptime.clear();
		}

		NLMListener.reset();
		prevtime = time;
		prevusedmem = usedmem;
	}

}
