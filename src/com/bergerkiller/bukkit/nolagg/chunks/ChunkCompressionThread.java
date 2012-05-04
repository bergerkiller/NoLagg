package com.bergerkiller.bukkit.nolagg.chunks;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.zip.Deflater;

import lishid.orebfuscator.obfuscation.Calculations;

import com.bergerkiller.bukkit.common.AsyncTask;

import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkSection;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.NibbleArray;
import net.minecraft.server.Packet51MapChunk;

public class ChunkCompressionThread extends AsyncTask {	
	//create a cyclic player compression queue loop
	private static int queueIndex = 0;
	private static ArrayList<ChunkCompressQueue> compress = new ArrayList<ChunkCompressQueue>();
	private static ChunkCompressQueue nextQueue() {
		synchronized (compress) {
			if (compress.isEmpty()) return null;
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
		
	//thread creation
	private static ChunkCompressionThread[] threads;
	public static void init(int threadcount) {
		if (threadcount <= 0) {
			NoLaggChunks.useBufferedLoading = false;
		}
		if (NoLaggChunks.useBufferedLoading) {
			if (threads != null) {
				if (threads.length > threadcount) {
					//kill some threads
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
					ChunkCompressionThread[] newthreads = new ChunkCompressionThread[threadcount];
					System.arraycopy(threads, 0, newthreads, 0, threads.length);
					for (int i = threads.length; i < newthreads.length; i++) {
						newthreads[i] = new ChunkCompressionThread();
						newthreads[i].start(true);
					}
					threads = newthreads;
				}
			} else {
				threads = new ChunkCompressionThread[threadcount];
				for (; threadcount > 0; --threadcount) {
					threads[threadcount - 1] = new ChunkCompressionThread();
					threads[threadcount - 1].start(true);
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
		if (threads == null) return;
		for (ChunkCompressionThread thread : threads) {
			thread.stop();
		}
		threads = null;
	}
	
	private byte[] rawbuffer = new byte[81920];
	private final byte[] compbuffer = new byte[81920];
    private final Deflater deflater = new Deflater();
    
    public static Packet51MapChunk createPacket(Chunk chunk, byte[] rawData) {
		Packet51MapChunk mapchunk = new Packet51MapChunk();
		mapchunk.a = chunk.x;
		mapchunk.b = chunk.z;
		mapchunk.rawData = rawData;
		fill(chunk, mapchunk);
		return mapchunk;
    }
    
    public static void fill(Chunk chunk, Packet51MapChunk packet) {
		packet.f = true;
        ChunkSection sections[] = chunk.h();
        int j = 0;
        int k = 0;
        int l;
        for(l = 0; l < sections.length; l++)
        {
            if(sections[l] == null || sections[l].a() || (1 << l) == 0) {
                continue;
            }
            packet.c |= 1 << l;
            j++;
            if(sections[l].h() != null) {
                packet.d |= 1 << l;
                k++;
            }
        }

        l = 2048 * (5 * j + k) + 256;
        if (packet.rawData == null || packet.rawData.length < l) {
        	packet.rawData = new byte[l];
        }
        	
        int length = 0;
        for(int j1 = 0; j1 < sections.length; j1++) {
            if(sections[j1] != null && !sections[j1].a())
            {
                byte abyte1[] = sections[j1].g();
                System.arraycopy(abyte1, 0, packet.rawData, length, abyte1.length);
                length += abyte1.length;
            }
        }
        for(int j1 = 0; j1 < sections.length; j1++) {
            if(sections[j1] != null && !sections[j1].a())
            {
                NibbleArray nibblearray = sections[j1].i();
                System.arraycopy(nibblearray.a, 0, packet.rawData, length, nibblearray.a.length);
                length += nibblearray.a.length;
            }
        }
        for(int j1 = 0; j1 < sections.length; j1++) {
            if(sections[j1] != null && !sections[j1].a())
            {
                NibbleArray nibblearray = sections[j1].j();
                System.arraycopy(nibblearray.a, 0, packet.rawData, length, nibblearray.a.length);
                length += nibblearray.a.length;
            }
        }
        for(int j1 = 0; j1 < sections.length; j1++) {
            if(sections[j1] != null && !sections[j1].a())
            {
                NibbleArray nibblearray = sections[j1].k();
                System.arraycopy(nibblearray.a, 0, packet.rawData, length, nibblearray.a.length);
                length += nibblearray.a.length;
            }
        }
        if(k > 0)
        {
            for(int j1 = 0; j1 < sections.length; j1++) {
                if(sections[j1] != null && !sections[j1].a() && sections[j1].h() != null)
                {
                    NibbleArray nibblearray = sections[j1].h();
                    System.arraycopy(nibblearray.a, 0, packet.rawData, length, nibblearray.a.length);
                    length += nibblearray.a.length;
                }
            }
        }
        byte abyte2[] = chunk.l();
        System.arraycopy(abyte2, 0, packet.rawData, length, abyte2.length);
        length += abyte2.length;
    }
    private void deflate(Packet51MapChunk packet) {
        this.deflater.reset();
        this.deflater.setLevel(6);
        this.deflater.setInput(packet.rawData);
        this.deflater.finish();
        packet.size = this.deflater.deflate(this.compbuffer);
        if (packet.size == 0) {
        	packet.size = this.deflater.deflate(this.compbuffer);
        }
        packet.buffer = new byte[packet.size];
        System.arraycopy(this.compbuffer, 0, packet.buffer, 0, packet.size);
        packet.rawData = null; //dereference
    }
                                   
    public static double getBusyPercentage(long timescale) {
    	double per = 0;
    	if (threads != null) {
        	for (ChunkCompressionThread thread : threads) {
        		if (thread == null) continue;
        		per += (double) thread.busyDuration / timescale;
        		thread.busyDuration = 0;
        	}
        	per /= threads.length;
    	}
    	return per;
    }
    
    public long busyDuration = 0;
    private long lasttime;
    private byte[] obfuscationBuffer;
        
    public Packet51MapChunk getCompressedPacket(EntityPlayer player, Chunk chunk) {
    	this.lasttime = System.currentTimeMillis();
    	
		//handle a packet		
		Packet51MapChunk mapchunk = createPacket(chunk, rawbuffer);
		if (mapchunk.rawData.length > rawbuffer.length) {
			rawbuffer = mapchunk.rawData;
		}
		
		//send chunk through possible plugins
		//========================================
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
//		if (NoLaggChunks.isRawCritOrbEnabled) {
//			try {
//				
//				OreObfuscateChunkPacketListener.
//			} catch (Throwable t) {
//				NoLaggChunks.plugin.log(Level.SEVERE, "An error occured in Raw Critics Ore Obfuscator: support for this plugin had to be removed!");
//				t.printStackTrace();
//				NoLaggChunks.isRawCritOrbEnabled = false;
//			}
//		}
		//========================================
		//compress raw data
		this.deflate(mapchunk);
		//actually send this chunk
		this.busyDuration += System.currentTimeMillis() - lasttime;
		return mapchunk;
    }
    
    private static int i = 0;
    private Chunk chunk;
    public final void run() {
		try {
			ChunkCompressQueue queue = nextQueue();
			if (queue == null) {
				sleep(200);
				return;
			}
			if ((chunk = queue.pollChunk()) != null) {
				try {
					queue.enqueue(new ChunkSendCommand(this.getCompressedPacket(queue.nativeOwner(), chunk), chunk));	
				} catch (Throwable t) {
		    		NoLaggChunks.plugin.log(Level.SEVERE, "Failed to compress map chunk [" + chunk.x + ", " + chunk.z + "] for player " + queue.owner().getName());
		    		t.printStackTrace();
		    	}
			} else if (i++ > compress.size()) {
				sleep(100);
			} else {
				return;
			}
			i = 0;
		} catch (Throwable t) {
			t.printStackTrace();
		}
    }

}
