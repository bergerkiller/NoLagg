package com.bergerkiller.bukkit.nolagg.saving;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.v1_4_R1.RegionFileCache;
import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.reflection.classes.RegionFileCacheRef;
import com.bergerkiller.bukkit.common.reflection.classes.RegionFileRef;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class RegionFileFlusher {
	private static Task flushTask;

	public static void reload() {
		Task.stop(flushTask);
		if (NoLaggSaving.writeDataEnabled) {
			flushTask = new Task(NoLagg.plugin) {
				public void run() {
					// get all the required region files to flush
					final List<Object> regions = new ArrayList<Object>();
					for (Object regionfile : RegionFileCacheRef.FILES.values()) {
						if (regionfile != null) {
							regions.add(regionfile);
						}
					}

					// create an async task to write stuff to file
					new AsyncTask("NoLagg saving data writer") {
						public void run() {
							for (Object region : regions) {
								synchronized (region) {
									RandomAccessFile raf = RegionFileRef.stream.get(region);
									if (raf == null) {
										continue;
									}
									File source = RegionFileRef.file.get(region);
									if (source == null) {
										continue;
									}

									/* Old method:
									FileDescriptor fd = raf.getFD();
									if (fd.valid()) {
										fd.sync();
									}
									*/

									try {
										RandomAccessFile old;
										synchronized (RegionFileCache.class) {
											// Replace the old instance with a new instance
											old = RegionFileRef.stream.get(region);
											RegionFileRef.stream.set(region, new RandomAccessFile(source, "rw"));
										}
										// Flush the old instance to disk
										old.close();
									} catch (IOException ex) {
										// Was already closed
									}
								}
							}
						}
					}.start(false);
				}
			}.start(NoLaggSaving.writeDataInterval, NoLaggSaving.writeDataInterval);
		}
	}

	public static void deinit() {
		Task.stop(flushTask);
		flushTask = null;
	}
}
