package com.bergerkiller.bukkit.nolagg.saving;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.Reference;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import net.minecraft.server.RegionFile;
import net.minecraft.server.RegionFileCache;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.SafeField;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class RegionFileFlusher {

	private static Task flushTask;
	private static SafeField<Map<File, Reference<RegionFile>>> regionFiles;
	private static SafeField<RandomAccessFile> rafField;

	public static void init() {
		if (NoLaggSaving.writeDataEnabled) {
			regionFiles = new SafeField<Map<File, Reference<RegionFile>>>(RegionFileCache.class, "a");
			rafField = new SafeField<RandomAccessFile>(RegionFile.class, "c");
			if (regionFiles.isValid()) {
				if (rafField.isValid()) {
					reload();
				} else {
					NoLaggSaving.plugin.log(Level.SEVERE, "Failed to bind to region file RAF to write out data at a set interval");
				}
			} else {
				NoLaggSaving.plugin.log(Level.SEVERE, "Failed to bind to region file cache to write out data at a set interval");
			}
		}
	}

	public static void reload() {
		Task.stop(flushTask);
		if (NoLaggSaving.writeDataEnabled) {
			if (regionFiles.isValid() && rafField.isValid()) {
				flushTask = new Task(NoLagg.plugin) {
					public void run() {
						//get all the required region files to flush
						final List<Entry<RegionFile, RandomAccessFile>> regions = new ArrayList<Entry<RegionFile, RandomAccessFile>>();
						for (Reference<RegionFile> ref : regionFiles.get(null).values()) {
							RegionFile regionfile = ref.get();
							if (regionfile != null) {
								RandomAccessFile raf = rafField.get(regionfile);
								if (raf != null) {
									regions.add(new SimpleEntry<RegionFile, RandomAccessFile>(regionfile, raf));
								}
							}
						}

						//create an async task to write stuff to file
						new AsyncTask("NoLagg saving data writer") {
							public void run() {
								for (Entry<RegionFile, RandomAccessFile> file : regions) {
									synchronized (file.getKey()) {
										try {
											FileDescriptor fd = file.getValue().getFD();
											if (fd.valid()) {
												fd.sync();
											}
										} catch (IOException e) {
											NoLaggSaving.plugin.log(Level.SEVERE, "Failed to sync region data to file:");
											e.printStackTrace();
										}
									}
								}
							}
						}.start(false);
					}
				}.start(NoLaggSaving.writeDataInterval, NoLaggSaving.writeDataInterval);
			}
		}
	}

	public static void deinit() {
		Task.stop(flushTask);
		flushTask = null;
		regionFiles = null;
		rafField = null;
	}
}
