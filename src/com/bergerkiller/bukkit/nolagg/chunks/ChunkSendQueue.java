package com.bergerkiller.bukkit.nolagg.chunks;

import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.Set;
import java.util.HashSet;

import org.bukkit.ChatColor;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import org.bukkit.craftbukkit.entity.CraftPlayer;

import com.bergerkiller.bukkit.common.IntRemainder;
import com.bergerkiller.bukkit.common.Operation;
import com.bergerkiller.bukkit.common.SafeField;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkCoordIntPair;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.NetworkManager;
import net.minecraft.server.Packet;
import net.minecraft.server.Packet53BlockChange;
import net.minecraft.server.World;
import net.minecraft.server.WorldServer;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ChunkSendQueue extends LinkedList {
	
	private static SafeField<Integer> queuesizefield;
	private static final long serialVersionUID = 1L;
	private static Task task;
	public static void init() {
		queuesizefield = new SafeField<Integer>(NetworkManager.class, "x");
		if (!queuesizefield.isValid()) {
			NoLaggChunks.plugin.log(Level.SEVERE, "Failed to hook into the player packet queue size field");
			NoLaggChunks.plugin.log(Level.SEVERE, "Distortions in the chunk rate will cause players to get kicked");
			queuesizefield = null;
		}
		prevtime = System.currentTimeMillis();
		task = new Task(NoLagg.plugin) {
			public void run() {
				double newper = ChunkCompressionThread.getBusyPercentage(System.currentTimeMillis() - prevtime);
				compressBusyPercentage = MathUtil.useOld(compressBusyPercentage, newper * 100.0, 0.1);

				prevtime = System.currentTimeMillis();
				try {
					new Operation() {
						public void run() {
							this.doPlayers();
						}
						public void handle(EntityPlayer ep) {
							bind(ep).update();
						}
					};
				} catch (Exception ex) {
					NoLaggChunks.plugin.log(Level.SEVERE, "An error occured while sending chunks:");
					ex.printStackTrace();
				} catch (OutOfMemoryError ex) {
					NoLaggChunks.plugin.log(Level.SEVERE, "We are running out of memory here!");
					NoLaggChunks.plugin.log(Level.SEVERE, "Restart the server and increase the RAM usage available for Bukkit.");
				}
			}
		}.start(1, 1);
	}
	public static void deinit() {
		Task.stop(task);
		task = null;
		//clear bound queues
		new Operation() {
			public void run() {
				this.doPlayers();
			}
			public void handle(EntityPlayer ep) {
				ChunkSendQueue queue = bind(ep);
				if (queue == null) return;
				LinkedList list = new LinkedList(queue.contained);
				ep.chunkCoordIntPairQueue = list;
			}
		};
	}
		
	private void enforceBufferFullSize() {
		NetworkManager nm = this.ep.netServerHandler.networkManager;
		Object g = new SafeField<Object>(NetworkManager.class, "g").get(nm);
		if (g != null && queuesizefield != null) {
			
			List<Packet> low = new SafeField<List<Packet>>(NetworkManager.class, "lowPriorityQueue").get(nm);
			List<Packet> high = new SafeField<List<Packet>>(NetworkManager.class, "highPriorityQueue").get(nm);
			if (low != null && high != null) {
				int queuedsize = 0;
				synchronized (g) {
					for (Packet p : low) queuedsize += p.a() + 1;
					for (Packet p : high) queuedsize += p.a() + 1;	
					queuesizefield.set(nm, queuedsize - 9437184);	
				}
			}
		}
	}
		
	private static long prevtime;
	public static double compressBusyPercentage = 0.0;
	private int maxQueueSize = 300000;
	private int packetBufferQueueSize = 0;
	private int updateQueueSize() {
		if (queuesizefield != null && queuesizefield.isValid()) {
			this.packetBufferQueueSize = (Integer) queuesizefield.get(this.ep.netServerHandler.networkManager);
			this.packetBufferQueueSize += 9437184;
		}
		return this.packetBufferQueueSize;
	}
	private int getMaxQueueSize() {
		return 10485760;
	}
		
	public static ChunkSendQueue bind(Player with) {
		return bind(((CraftPlayer) with).getHandle());
	}
	public static ChunkSendQueue bind(EntityPlayer with) {
		if (!(with.chunkCoordIntPairQueue instanceof ChunkSendQueue)) {
			with.chunkCoordIntPairQueue = new ChunkSendQueue(with);
		}
		return (ChunkSendQueue) with.chunkCoordIntPairQueue;
	}	
	private ChunkSendQueue(final EntityPlayer ep) {
		this.ep = ep;
		this.addAll(ep.chunkCoordIntPairQueue);
		this.world = ep.world;
		this.sendDirection = FaceUtil.yawToFace(this.ep.yaw - 90.0F);
		this.x = (int) (ep.locX + ep.motX * 16) >> 4;
		this.z = (int) (ep.locZ + ep.motZ * 16) >> 4;
		this.chunkQueue = new ChunkCompressQueue(this);
		ChunkCompressionThread.addQueue(this.chunkQueue);
	    this.enforceBufferFullSize();
	}
		
	public static double maxRate = 2;
	public static double minRate = 0.25;
	public static double globalTriggerRate = 1;
			
	public static double getAverageRate() {
		return new Operation() {
			public void run() {
				this.doPlayers();
				super.set(0, this.totalrate / (double) pcount);
			}
			private double totalrate = 0;
			private int pcount = 0;
			public void handle(EntityPlayer ep) {
				this.totalrate += bind(ep).rate.get();
				this.pcount++;
			}
		}.arg(0, Double.class);
	}
	public double getRate() {
		return this.rate.get();
	}
	
	public final EntityPlayer ep;
	public BlockFace sendDirection = BlockFace.NORTH;
	public World world;
	public int x;
	public int z;
	private IntRemainder rate = new IntRemainder(2.0, 1);
	private final IntRemainder triggerRate = new IntRemainder(globalTriggerRate, 1);
	private ChunkCompressQueue chunkQueue;
	private final LinkedList<Packet53BlockChange> toTrigger = new LinkedList<Packet53BlockChange>();
	private final Set<ChunkCoordIntPair> contained = new HashSet<ChunkCoordIntPair>();
	public void removeContained(int x, int z) {
	    this.removeContained(new ChunkCoordIntPair(x, z));
	}
	public void removeContained(ChunkCoordIntPair pair) {
		this.contained.remove(pair);
	}
	
	public void scheduleTrigger(Chunk chunk) {
    	this.scheduleTrigger(new Packet53BlockChange(chunk.x << 4, 0, chunk.x << 4, chunk.world));
	}
	public void scheduleTrigger(Packet53BlockChange packet) {
		this.toTrigger.offer(packet);
	}
	public void updateTriggers() {
		if (this.toTrigger.isEmpty()) return;
		if (this.triggerRate.get() > 0) {
			this.sendTriggers(this.triggerRate.next(), 1);
		} else {
			this.sendTriggers(1, (int) (1 / this.triggerRate.get()));
		}
	}
	private void sendTriggers(int rate, int interval) {
		if (interval == 0) interval = 1;
		if (rate == 0) return;
		if (this.triggerintervalcounter >= interval) {
		    for (int i = 0; i < rate; i++) {
				Packet53BlockChange p = this.toTrigger.poll();
				if (p == null) break;
				PacketUtil.sendPacket(this.ep, p, false);
		    }
			this.triggerintervalcounter = 1;
		} else {
			this.triggerintervalcounter++;
		}
	}
	
	/*
	 * Used to send packets and triggers
	 */
	private int intervalcounter = 200;
	private int triggerintervalcounter = 200;
	private int buffersizeavg = 0;
	private int idleTicks = 0;
	public String getBufferLoadMsg() {
		double per = MathUtil.round(100D * this.buffersizeavg / getMaxQueueSize(), 2);
		if (this.buffersizeavg > 300000) {
			return ChatColor.RED.toString() + per + "%";
		} else if (this.buffersizeavg > 100000) {
			return ChatColor.GOLD.toString() + per + "%";
		} else {
			return ChatColor.GREEN.toString() + per + "%";
		}
	}
		
	private int prevQueueSize = 0;

	public Object[] toArray(Object[] value) {
		synchronized (this) {
			return super.toArray(value);
		}
	}
		
	public void sort() {
	    this.chunkQueue.sort();
	    synchronized (this) {
	    	if (this.isUpdating) {
	    		this.sort(this);
	    	} else {
	    		this.isUpdating = true;
	    		this.sort(this);
	    		this.isUpdating = false;
	    	}
	    }
	}
	public void sort(List elements) {
		if (elements.isEmpty()) return;
		ChunkCoordIntPair middle = new ChunkCoordIntPair(this.x, this.z);
		try {
			Collections.sort(elements, ChunkCoordComparator.get(this.sendDirection, middle));
		} catch (ConcurrentModificationException ex) {
			NoLaggChunks.plugin.log(Level.SEVERE, "Another plugin interfered while sorting a collection!");
		} catch (ArrayIndexOutOfBoundsException ex) {
			NoLaggChunks.plugin.log(Level.SEVERE, "Another plugin interfered while sorting a collection!");
		} catch (Throwable t) {
			NoLaggChunks.plugin.log(Level.SEVERE, "An error occurred while sorting a collection:");
			t.printStackTrace();
		}
	}
	
	private boolean isUpdating = false;
	private void update() {
		this.updateTriggers();
		this.updateQueueSize();
		if (this.buffersizeavg == 0) {
			this.buffersizeavg = this.packetBufferQueueSize;
		} else {
			this.buffersizeavg += 0.3 * (this.packetBufferQueueSize - this.buffersizeavg);
		}
		if (this.idleTicks > 0) {
			this.idleTicks--;
			return;
		}
		
		if (super.size() == 0 && !this.chunkQueue.canSend()) return;
		double newrate = this.rate.get();
		if (this.packetBufferQueueSize > this.maxQueueSize) {
			newrate = minRate;
		} else {
			if (this.prevQueueSize > this.packetBufferQueueSize) {
				newrate += 0.07;
			} else {
				//to force the rate to be optimal
				if (this.packetBufferQueueSize > 80000) {
					newrate -= 0.17;
				} else if (this.packetBufferQueueSize > 20000) {
					newrate -= 0.14;
				} else {
					newrate += 0.06;
				}
			}
			newrate += 0.9 * (this.rate.get() - newrate);
			//set rate bounds
			if (newrate > maxRate) {
				newrate= maxRate;
			} else if (newrate < minRate) {
				newrate = minRate;
			}
		}

		this.rate.set(newrate);
		this.prevQueueSize = this.packetBufferQueueSize;
		//send chunks
		if (newrate >= 1) {
			this.update(1, (int) this.rate.next());
		} else {
			this.update((int) (1 / this.rate.get()), 1);
		}
	}
	private void update(int interval, int rate) {
		if (interval == 0) interval = 1;
		if (rate == 0) return;
		this.isUpdating = true;
		if (this.intervalcounter >= interval)  {
			//sorting
			BlockFace newDirection = FaceUtil.yawToFace(this.ep.yaw - 90.0F);
			int newx = (int) (ep.locX + ep.motX * 16) >> 4;
			int newz = (int) (ep.locZ + ep.motZ * 16) >> 4;
			if (ep.world != this.world || newx != this.x || newz != this.z || this.sendDirection != newDirection) {
				this.sendDirection = newDirection;
				this.x = newx;
				this.z = newz;
				this.world = ep.world;
				this.sort();
			}
			this.sendBatch(rate);
			this.intervalcounter = 1;
		} else {
			this.intervalcounter++;
		}
		this.isUpdating = false;
	}
	
	private ChunkCoordIntPair pollPair() {
		while (!this.isEmpty()) {
			ChunkCoordIntPair pair = (ChunkCoordIntPair) super.poll();
			if (pair == null) return null;
			if (this.isNear(pair, CommonUtil.view)) {
				return pair;
			} else {
				this.contained.remove(pair);
			}
		}
		return null;
	}
	
	private void sendBatch(int count) {				
		//load chunks
		for (int i = 0; i < count; i++) {
			ChunkCoordIntPair pair = this.pollPair();
			if (pair == null) break;
			this.chunkQueue.enqueue(((WorldServer) this.ep.world).chunkProviderServer.getChunkAt(pair.x, pair.z));
		}

		//send chunks
		for (int i = 0; i < count; i++) {
			if (!this.chunkQueue.sendNext()) {
				// Wait a few ticks to make chunks visible
				this.idle(4);
				break;
			}
		}
	}

	public void idle(int ticks) {
		this.idleTicks += ticks;
	}

	public boolean containsAll(Collection coll) {
		synchronized (this) {
			for (Object o : coll) {
				if (!this.contains(o)) return false;
			}
			return true;
		}
	}
	public boolean contains(Object o) {
		synchronized (this) {
			return this.contained.contains(o);
		}
	}
	public boolean addAll(Collection coll) {
		synchronized (this) {
			for (Object o : coll) this.add(o);
			return true;
		}
	}
	public boolean addAll(int index, Collection coll) {
		synchronized (this) {
			for (Object o : coll) this.add(index, o);
			return true;
		}
	}
	public void clear() {
		synchronized (this) {
			super.clear();
		}
		this.chunkQueue.clear();
		this.contained.clear();
	}
	
	/*
	 * Prevent Spout from using this queue...seriously!
	 */
	public boolean isEmpty() {
		return this.isUpdating ? super.isEmpty() : true;
	}
	public int size() {
		return this.isUpdating ? super.size() : 0;
	}
	
	public int getPendingSize() {
		return super.size() + this.chunkQueue.getPendingSize();
	}
	
	/*
	 * get(0) is called in EntityPlayer to get the next chunk to send
	 */
	public Object get(int index) {
		return this.isUpdating ? super.get(index) : null;
	}
	public boolean remove(Object object) {
		ChunkCoordIntPair pair = (ChunkCoordIntPair) object;
		synchronized (this) {
			if (super.remove(object)) {
				this.contained.remove(pair);
				return true;
			}
		}
		if (this.chunkQueue.remove(pair.x, pair.z)) {
			this.contained.remove(pair);
			return true;
		}
		return false;
	}
	
	public boolean isNear(ChunkCoordIntPair coord, final int view) {
		return this.isNear(coord.x, coord.z, view);
	}
	public boolean isNear(final int chunkx, final int chunkz, final int view) {
		return EntityUtil.isNearChunk(this.ep, chunkx, chunkz, view + 1);
	}
		
	private boolean add(ChunkCoordIntPair pair) {
		if (this.isUpdating) return true;
		if (!this.isNear(pair, CommonUtil.view)) return false;
		if (this.contained.add(pair) || this.chunkQueue.remove(pair.x, pair.z)) {
			this.sendDirection = null; //invalidate
			return true;
		}
		return false;
	}
	
	/*
	 * add(o) is called in PlayerInstance to queue a new chunk coordinate
	 */
	public synchronized void add(int index, Object object) {
		if (object == null) return;
		if (this.add((ChunkCoordIntPair) object)) {
			super.add(index, object);
		}
	}
	public synchronized boolean add(Object object) {
		if (object == null) return false;
		if (this.add((ChunkCoordIntPair) object)) {
			return super.add(object);
		} else {
			return false;
		}
	}

}
