package com.bergerkiller.bukkit.nolaggchunks;

import java.util.LinkedList;
import java.util.Queue;

public class Compression {
	
	private static Queue<BufferedChunk> toCompress = new LinkedList<BufferedChunk>();
	
	public static void deinit() {
		toCompress.clear();
		toCompress = null;
	}
	
	public static void schedule(BufferedChunk bc) {
		if (toCompress == null) return;
		synchronized (toCompress) {
			if (toCompress.size() > 50) return; //prevent endlessly increasing queue it can't handle
			toCompress.add(bc);
		}
	}
	
	public static boolean execute() {
		if (toCompress == null) return false;
		synchronized (toCompress) {
			while (true) {
				BufferedChunk bc = toCompress.poll();
				if (bc == null) return false;
				if (bc.needsCompression()) {
					bc.compress();
					return true;
				}
			}
		}
	}
		
}
