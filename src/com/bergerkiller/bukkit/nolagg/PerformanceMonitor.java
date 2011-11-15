package com.bergerkiller.bukkit.nolagg;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;

public class PerformanceMonitor implements Runnable {
	
	public static int monitorInterval = 40;
	
	private long prevtime;
	private long prevusedmem;
	private long minmem;
	
	public static ArrayList<String> recipients = new ArrayList<String>();
	public static boolean sendConsole = false;
	public static boolean sendLog = false;
	private static final Runtime runtime = Runtime.getRuntime();
	private static int taskID = -1;
	private static BufferedWriter logger;
	private static boolean wroteHeader = false;
	
	private static File logfile;
	
	public static void init() {
		PerformanceMonitor pm = new PerformanceMonitor();
		pm.prevtime = System.currentTimeMillis();
		pm.prevusedmem = runtime.maxMemory() - runtime.freeMemory();
		pm.minmem = pm.prevusedmem;
		taskID = NoLagg.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(NoLagg.plugin, pm, monitorInterval, monitorInterval);
		//set up logger
		logfile = NoLagg.plugin.getDataFolder();
		logfile.mkdirs();
		logfile = new File(logfile + File.separator + "log.txt");
		try {
			logger = new BufferedWriter(new FileWriter(logfile, true));
			log("NoLagg enabled: " + getStamp());
			logger.flush();
		} catch (IOException ex) {
			NoLagg.log(Level.SEVERE, "Failed to initialize performance logger file stream:");
			ex.printStackTrace();
		}
	}
	public static void deinit() {
		NoLagg.plugin.getServer().getScheduler().cancelTask(taskID);
		//set up logger
		if (logger != null) {
			try {
				log("NoLagg disabled: " + getStamp());
				logger.flush();
				logger.close();
			} catch (IOException ex) {}
			logger = null;
		}
		recipients.clear();
	}
	
	private String getProgress(int length, ChatColor color) {
		if (length <= 0) return "";
		StringBuilder sb = new StringBuilder(length + 1);
		sb.append(color);
		for (int i = 0; i < length; i++) {
			sb.append('|');
		}
		return sb.toString();
	}
	private String getMemoryProgress(int length, long current, long min, long max) {
		double factor = (double) length / max;
		StringBuilder sb = new StringBuilder(length + 3);
		int used = (int) (factor * min);
		int unused = (int) (factor * (max - current));
		sb.append(getProgress(used, ChatColor.GREEN));
		sb.append(getProgress(length - unused - used, ChatColor.YELLOW));
		sb.append(getProgress(unused, ChatColor.RED));
		return sb.toString();
	}
	
    private static double round(double Rval, int Rpl) {
    	double p = Math.pow(10, Rpl);
    	return Math.round(Rval * p) / p;
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

	@Override
	public void run() {	
		long time = System.currentTimeMillis();
		double elapsedtimesec = (double) (time - prevtime) / 1000;
		long totalmem = runtime.totalMemory();
		long usedmem = totalmem - runtime.freeMemory();
		long diff = usedmem - prevusedmem;
		if (diff < 0) {
			if (usedmem > minmem) {
				if (mem(usedmem) + 100 > mem(runtime.maxMemory())) {
					NoLagg.log(Level.SEVERE, "Memory usage is exceeding the maximum, a server restart may be required!");
					Bukkit.getServer().broadcastMessage(ChatColor.DARK_RED + "[NoLagg] Memory usage is exceeding the maximum, a server restart may be required!");
				}
			}
			minmem = usedmem;
		}
		if (sendLog || sendConsole || recipients.size() > 0) {
			int mobcount = 0;
			int entitycount = 0;
			int itemcount = 0;
			int tntcount = 0;
			int playercount = 0;
			for (World w : Bukkit.getServer().getWorlds()) {
				for (Entity e : w.getEntities()) {
					if (e instanceof LivingEntity) {
						if (e instanceof Player) {
							playercount++;
						} else {
							mobcount++;
						}
					} else if (e instanceof Item) {
						itemcount++;
					} else if (e instanceof TNTPrimed) {
						tntcount++;
					}
					entitycount++;
				}
			}
			double tps = monitorInterval / elapsedtimesec;
			int hiddenItems = ItemHandler.getHiddenCount();
			int savesize = AsyncSaving.getSize();
			if (sendLog && logger != null) {
				try {
					if (!wroteHeader) {
						wroteHeader = true;
						String columns = "";
						columns += "| Time		| Tick rate		| Total Memory	| Static Memory	| Dynamic Memory	";
						columns += "| Memory Write Rate	| Total Chunks	| Buffered Chunks	";
						columns += "| Chunks Loaded	| Chunks Unloaded	| Chunks To Save	| Buffered TNT	| Buffered Items	"; 
						columns += "| Entities		| Mobs		| Items		| TNT		| Players		|";
						log(columns);
					}
					String msg = "";
					msg += "|" + getColumn(getTime());
					msg += getColumn(String.valueOf(round(tps, 1)));
					msg += getColumn(mem(totalmem) + " MB");
					msg += getColumn(mem(minmem) + " MB");
					msg += getColumn(mem(usedmem - minmem) + " MB");
					if (diff <= 0) {
						msg += getColumn("GC");
					} else {
						msg += getColumn(mem(diff / elapsedtimesec) + " MB/s");
					}
					msg += getColumn(String.valueOf(ChunkHandler.getTotalCount()));
					msg += getColumn(String.valueOf(ChunkHandler.getBufferCount()));
					msg += getColumn(String.valueOf(ChunkHandler.getLoadCount()));
					msg += getColumn(String.valueOf(ChunkHandler.getUnloadCount()));
					msg += getColumn(String.valueOf(savesize));
					msg += getColumn(String.valueOf(TnTHandler.getBufferCount()));
					msg += getColumn(String.valueOf(ItemHandler.getHiddenCount()));
					msg += getColumn(String.valueOf(entitycount));
					msg += getColumn(String.valueOf(mobcount));
					msg += getColumn(String.valueOf(itemcount));
					msg += getColumn(String.valueOf(tntcount));
					msg += getColumn(String.valueOf(playercount));
					log(msg);
					logger.flush();
				} catch (IOException ex) {
					NoLagg.log(Level.SEVERE, "Logging disabled:");
					ex.printStackTrace();
					try {
						logger.close();
					} catch (IOException e) {}
					logger = null;
				}
			}
			
			if (sendConsole) {
				CommandSender s = Bukkit.getServer().getConsoleSender();
				//Line
				s.sendMessage("-");
				//memory
				String mem = "Memory: " + mem(minmem) + "/" + mem(totalmem) + " MB (+" + mem(usedmem - minmem) + " modified)";
				mem += "(+" + mem(diff / elapsedtimesec) + " MB/s)";
				s.sendMessage(mem);
				//chunks
				mem = "Chunks: ";
				mem += ChunkHandler.getTotalCount() + " [" + ChunkHandler.getBufferCount() + " Buffered]";
				mem += " [+" + ChunkHandler.getLoadCount() + "]";
				mem += " [-" + ChunkHandler.getUnloadCount() + "]";
				mem += " [" + savesize + " left to save]";
				s.sendMessage(mem);
				//Entities
				mem = "Entities: " + entitycount + " [" + mobcount + " mobs]";
				mem += " [" + itemcount + " items] [" + tntcount + " mobile TNT]";
				s.sendMessage(mem);
				//Buffering
				mem = "Buffers: [" + TnTHandler.getBufferCount() + " TNT] [" + hiddenItems + " items]";
				s.sendMessage(mem);
				//Tick times
				mem = "Ticks per second: " + round(tps, 1) + " [" + round(tps * 5, 0) + "%]";
				s.sendMessage(mem);
			}
			if (recipients.size() > 0) {
				ArrayList<String> messages = new ArrayList<String>(6);
				//Line
				messages.add("");
				//Memory
				String mem = ChatColor.YELLOW + "Memory: ";
				mem += getMemoryProgress(50, usedmem,  minmem, totalmem);
				mem += ChatColor.YELLOW + " " + mem(minmem) + "/" + mem(totalmem) + " MB ";
				if (diff > 0) {
					mem += ChatColor.RED + "(+" + mem(diff / elapsedtimesec) + " MB/s)";
				} else {
					mem += ChatColor.GREEN + "(GC)";
				}
				messages.add(mem);
				//Chunks
				mem = ChatColor.YELLOW + "Chunks: ";
				mem += ChunkHandler.getTotalCount() + " [" + ChunkHandler.getBufferCount() + " Buffered]";
				mem += ChatColor.GREEN + " [+" + ChunkHandler.getLoadCount() + "]";
				mem += ChatColor.RED + " [-" + ChunkHandler.getUnloadCount() + "]";
				mem += ChatColor.YELLOW + " [" + savesize + " left to save]";
				messages.add(mem);
				//Entities
				mem = ChatColor.YELLOW + "Entities: ";
				mem += entitycount + " [" + mobcount + " mobs] [" + itemcount + " items] [" + tntcount + " TNT] [" + playercount + " players]";
				messages.add(mem);
				//Buffering
				mem = ChatColor.YELLOW + "Buffers: [" + TnTHandler.getBufferCount() + " TNT] [" + hiddenItems + " items]";
				messages.add(mem);
				//Tick times
				mem = ChatColor.YELLOW + "Ticks per second: " + round(tps, 1) + " [" + round(tps * 5, 0) + "%]";
				messages.add(mem);
				
				int i = 0;
				while (i < recipients.size()) {
					String name = recipients.get(i);
					Player p = Bukkit.getServer().getPlayer(name);
					if (p != null) {
						for (String msg : messages) {
							p.sendMessage(msg);
						}
						i++;
					} else {
						recipients.remove(i);
					}
				}
			}
		}

		ChunkHandler.reset();
		prevtime = time;
		this.prevusedmem = usedmem;
	}

}
