package com.bergerkiller.bukkit.nolagg.chunks;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.zip.Deflater;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.reflection.classes.Packet51MapChunkRef;
import com.lishid.orebfuscator.obfuscation.Calculations;

import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkSection;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.Packet51MapChunk;

public class ChunkCompressionThread extends AsyncTask {
	// create a cyclic player compression queue loop
	private static int queueIndex = 0;
	private static int idleCounter = 0;
	private static ChunkCompressionThread[] threads;
	private static ArrayList<ChunkCompressQueue> compress = new ArrayList<ChunkCompressQueue>();
	/**
	 * Maximum size a chunk packet can contain. This consists of:<br>
	 * - 16 slices, each containing 5 data sections of 2048 bytes in length<br>
	 * - Biome data, one byte for each column, 16 x 16
	 */
	private static final int MAX_CHUNK_DATA_LENGTH = (16 * 5 * 2048) + (16 * 16);

	private static ChunkCompressQueue nextQueue() {
		synchronized (compress) {
			if (compress.isEmpty())
				return null;
			if (queueIndex > compress.size() - 1) {
				queueIndex = 0;
			}
			ChunkCompressQueue queue = compress.get(queueIndex);
			if (queue.isAlive()) {
				queueIndex++;
				return queue;
			} else {
				compress.remove(queueIndex);
				return nextQueue();
			}
		}
	}

	public static void addQueue(ChunkCompressQueue queue) {
		synchronized (compress) {
			compress.add(queue);
		}
	}

	public static void init(int threadcount) {
		if (threadcount <= 0) {
			NoLaggChunks.useBufferedLoading = false;
		}
		if (NoLaggChunks.useBufferedLoading) {
			if (threads != null) {
				if (threads.length > threadcount) {
					// kill some threads
					ChunkCompressionThread[] newthreads = new ChunkCompressionThread[threadcount];
					for (int i = 0; i < threads.length; i++) {
						if (i < newthreads.length) {
							newthreads[i] = threads[i];
						} else {
							threads[i].stop();
						}
					}
					threads = newthreads;
				} else if (threads.length < threadcount) {
					// Append new threads
					ChunkCompressionThread[] newthreads = new ChunkCompressionThread[threadcount];
					System.arraycopy(threads, 0, newthreads, 0, threads.length);
					for (int i = threads.length; i < newthreads.length; i++) {
						newthreads[i] = new ChunkCompressionThread();
						newthreads[i].start(true);
					}
					threads = newthreads;
				}
			} else {
				// Create all new threads
				threads = new ChunkCompressionThread[threadcount];
				for (int i = 0; i < threadcount; i++) {
					threads[i] = new ChunkCompressionThread();
					threads[i].start(true);
				}
			}
		} else if (threads != null) {
			for (ChunkCompressionThread thread : threads) {
				thread.stop();
			}
			threads = null;
		}
	}

	public static void deinit() {
		if (threads != null) {
			for (ChunkCompressionThread thread : threads) {
				thread.stop();
			}
			threads = null;
		}
	}

	private int rawLength = 0;
	private final byte[] rawbuffer = new byte[MAX_CHUNK_DATA_LENGTH];
	private final byte[] compbuffer = new byte[81920];
	private final Deflater deflater = new Deflater();

	private void rawAppend(byte[] data) {
		System.arraycopy(data, 0, this.rawbuffer, this.rawLength, data.length);
		this.rawLength += data.length;
	}

	public Packet51MapChunk createPacket(Chunk chunk) {
		// Version which uses the Chunkmap buffer to create the packet
		Packet51MapChunk mapchunk = new Packet51MapChunk();
		Packet51MapChunkRef.x.set(mapchunk, chunk.x);
		Packet51MapChunkRef.z.set(mapchunk, chunk.z);
		Packet51MapChunkRef.hasBiomeData.set(mapchunk, true); // yes, has biome data

		// =====================================
		// =========== Fill with data ==========
		// =====================================
		int chunkDataBitMap = 0;
		int chunkBiomeBitMap = 0;
		int i;

		// Calculate the available chunk sections and bitmap
		ChunkSection sections[] = chunk.i();
		boolean sectionsEmpty[] = new boolean[sections.length];
		for (i = 0; i < sections.length; i++) {
			sectionsEmpty[i] = sections[i] == null || sections[i].a();
			if (!sectionsEmpty[i]) {
				chunkDataBitMap |= 1 << i;
				if (sections[i].i() != null) {
					chunkBiomeBitMap |= 1 << i;
				}
			}
		}

		// Check if it is an "EmptyChunk"
		// This type of packet needs separate handling
		if (chunkDataBitMap == 0 && chunkBiomeBitMap == 0) {
			// Fill with 0 data, 5 main sections of 2048 each
			this.rawLength = (5 * 2048);
			chunkDataBitMap = 1;
			java.util.Arrays.fill(this.rawbuffer, 0, this.rawLength, (byte) 0);
		} else {
			// Fill with chunk section data
			// There are 5 parts, each 2048 bytes of length
			this.rawLength = 0;
			for (i = 0; i < sections.length; i++) {
				if (!sectionsEmpty[i]) {
					rawAppend(sections[i].g());
				}
			}
			for (i = 0; i < sections.length; i++) {
				if (!sectionsEmpty[i]) {
					rawAppend(sections[i].j().a);
				}
			}
			for (i = 0; i < sections.length; i++) {
				if (!sectionsEmpty[i]) {
					rawAppend(sections[i].k().a);
				}
			}
			for (i = 0; i < sections.length; i++) {
				if (!sectionsEmpty[i]) {
					rawAppend(sections[i].l().a);
				}
			}
			for (i = 0; i < sections.length; i++) {
				if (!sectionsEmpty[i] && sections[i].i() != null) {
					rawAppend(sections[i].i().a);
				}
			}
		}
		// Biome information
		rawAppend(chunk.m());
		// =====================================

		// Set data in packet
		Packet51MapChunkRef.chunkDataBitMap.set(mapchunk, chunkDataBitMap);
		Packet51MapChunkRef.chunkBiomeBitMap.set(mapchunk, chunkBiomeBitMap);
		Packet51MapChunkRef.size.set(mapchunk, this.rawLength);
		Packet51MapChunkRef.inflatedBuffer.set(mapchunk, this.rawbuffer);
		return mapchunk;
	}

	private void deflate(Packet51MapChunk packet) {
		int size = Packet51MapChunkRef.size.get(packet);
		// Set input information
		this.deflater.reset();
		this.deflater.setLevel(6);
		this.deflater.setInput(Packet51MapChunkRef.inflatedBuffer.get(packet), 0, size);
		this.deflater.finish();
		// Start deflating
		size = this.deflater.deflate(this.compbuffer);
		if (size == 0) {
			size = this.deflater.deflate(this.compbuffer);
		}
		byte[] buffer = new byte[size];
		System.arraycopy(this.compbuffer, 0, buffer, 0, size);
		// Write to packet
		Packet51MapChunkRef.size.set(packet, size);
		Packet51MapChunkRef.buffer.set(packet, buffer);
		Packet51MapChunkRef.inflatedBuffer.set(packet, null); // dereference
	}

	public static double getBusyPercentage(long timescale) {
		double per = 0;
		if (threads != null && threads.length > 0) {
			for (ChunkCompressionThread thread : threads) {
				if (thread != null) {
					per += (double) thread.busyDuration / timescale;
					thread.busyDuration = 0;
				}
			}
			per /= threads.length;
		}
		return per;
	}

	public long busyDuration = 0;
	private long lasttime;
	private byte[] obfuscationBuffer;

	/**
	 * Obtains the compressed chunk data packet for the given chunk
	 * 
	 * @param player to get the compressed packet for
	 * @param chunk to get the data for
	 * @return compressed packet of which the raw data can no longer be used
	 */
	public Packet51MapChunk getCompressedPacket(EntityPlayer player, Chunk chunk) {
		this.lasttime = System.currentTimeMillis();
		Packet51MapChunk mapchunk = createPacket(chunk);

		// send chunk through possible plugins
		// ========================================
		if (NoLaggChunks.isOreObfEnabled) {
			if (this.obfuscationBuffer == null) {
				this.obfuscationBuffer = new byte[65536];
			}
			try {
				Calculations.Obfuscate(mapchunk, player.netServerHandler.getPlayer(), false, this.obfuscationBuffer);
			} catch (Throwable t) {
				NoLaggChunks.plugin.log(Level.SEVERE, "An error occured in Orebfuscator: support for this plugin had to be removed!");
				t.printStackTrace();
				NoLaggChunks.isOreObfEnabled = false;
				this.obfuscationBuffer = null;
			}
		}
		// ========================================

		// compression
		this.deflate(mapchunk);
		// end of processing, return produced packet
		this.busyDuration += System.currentTimeMillis() - lasttime;
		return mapchunk;
	}

	@Override
	public void run() {
		try {
			ChunkCompressQueue queue = nextQueue();
			if (queue == null) {
				sleep(200);
				return;
			}
			Chunk chunk = queue.pollChunk();
			if (chunk != null) {
				try {
					queue.enqueue(new ChunkSendCommand(this.getCompressedPacket(queue.nativeOwner(), chunk), chunk));
				} catch (Throwable t) {
					NoLaggChunks.plugin.log(Level.SEVERE, "Failed to compress map chunk [" + chunk.x + ", " + chunk.z + "] for player " + queue.owner().getName());
					t.printStackTrace();
				}
			} else if (idleCounter++ > compress.size()) {
				sleep(100);
			} else {
				return;
			}
			idleCounter = 0;
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
