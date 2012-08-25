package com.bergerkiller.bukkit.nolagg.chunks;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import org.bukkit.craftbukkit.entity.CraftPlayer;

import com.bergerkiller.bukkit.common.IntRemainder;
import com.bergerkiller.bukkit.common.Operation;
import com.bergerkiller.bukkit.common.SafeField;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

import net.minecraft.server.ChunkCoordIntPair;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.INetworkManager;
import net.minecraft.server.NetworkManager;
import net.minecraft.server.Packet;
import net.minecraft.server.World;
import net.minecraft.server.WorldServer;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ChunkSendQueue extends ChunkSendQueueBase {
	private static final long serialVersionUID = 1L;
	public static double maxRate = 2;
	public static double minRate = 0.25;
	public static double compressBusyPercentage = 0.0;
	private static long prevtime;
	private static SafeField<List<?>> chunkQueueField = new SafeField<List<?>>(EntityPlayer.class, "chunkCoordIntPairQueue");
	private static SafeField<Integer> queuesizefield = new SafeField<Integer>(NetworkManager.class, "y");
	private static Task task;

	public static void init() {
		if (!queuesizefield.isValid()) {
			NoLaggChunks.plugin.log(Level.SEVERE, "Failed to hook into the player packet queue size field");
			NoLaggChunks.plugin.log(Level.SEVERE, "Distortions in the chunk rate will cause players to get kicked");
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
							ChunkSendQueue queue = bind(ep);
							queue.setUpdating(true);
							queue.update();
							queue.setUpdating(false);
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
				if (queue != null) {
					chunkQueueField.set(ep, queue.toLinkedList());
				}
			}
		};
	}

	public static ChunkSendQueue bind(Player with) {
		return bind(((CraftPlayer) with).getHandle());
	}

	public static ChunkSendQueue bind(EntityPlayer with) {
		if (!(with.chunkCoordIntPairQueue instanceof ChunkSendQueue)) {
			ChunkSendQueue queue = new ChunkSendQueue(with);
			with.chunkCoordIntPairQueue.clear();
			chunkQueueField.set(with, queue);
		}
		return (ChunkSendQueue) with.chunkCoordIntPairQueue;
	}

	public final EntityPlayer ep;
	private int idleTicks = 0;
	public BlockFace sendDirection = BlockFace.NORTH;
	public World world;
	public int x;
	public int z;
	private IntRemainder rate = new IntRemainder(2.0, 1);
	private int intervalcounter = 200;
	private ChunkCompressQueue chunkQueue;

	/*
	 * Packet queue related variables
	 */
	private int prevQueueSize = 0;
	private int maxQueueSize = 300000;
	private int packetBufferQueueSize = 0;
	private int buffersizeavg = 0;

	private ChunkSendQueue(final EntityPlayer ep) {
		this.ep = ep;
		this.world = ep.world;
		this.sendDirection = FaceUtil.yawToFace(this.ep.yaw - 90.0F);
		this.x = (int) (ep.locX + ep.motX * 16) >> 4;
		this.z = (int) (ep.locZ + ep.motZ * 16) >> 4;
		this.chunkQueue = new ChunkCompressQueue(this);
		this.addAll(ep.chunkCoordIntPairQueue);
		this.add(new ChunkCoordIntPair(MathUtil.locToChunk(ep.locX), MathUtil.locToChunk(ep.locZ)));
		ChunkCompressionThread.addQueue(this.chunkQueue);
	    this.enforceBufferFullSize();
	}

	private void enforceBufferFullSize() {
		INetworkManager nm = this.ep.netServerHandler.networkManager;
		Object lockObject = new SafeField<Object>(NetworkManager.class, "h").get(nm);
		if (lockObject != null && queuesizefield != null) {
			List<Packet> low = new SafeField<List<Packet>>(NetworkManager.class, "lowPriorityQueue").get(nm);
			List<Packet> high = new SafeField<List<Packet>>(NetworkManager.class, "highPriorityQueue").get(nm);
			if (low != null && high != null) {
				int queuedsize = 0;
				synchronized (lockObject) {
					for (Packet p : low) queuedsize += p.a() + 1;
					for (Packet p : high) queuedsize += p.a() + 1;	
					queuesizefield.set(nm, queuedsize - 9437184);	
				}
			}
		}
	}

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

	public void sort() {
	    this.chunkQueue.sort();
	    synchronized (this) {
	    	boolean old = this.setUpdating(true);
	    	this.sort(this);
	    	this.setUpdating(old);
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

	/**
	 * Main update routine - handles the calculation of the rate and interval and updates afterwards
	 */
	private void update() {
		// Update queue size
		if (queuesizefield.isValid()) {
			this.packetBufferQueueSize = (Integer) queuesizefield.get(this.ep.netServerHandler.networkManager);
			this.packetBufferQueueSize += 9437184;
		}
		// Update current buffer size
		if (this.buffersizeavg == 0) {
			this.buffersizeavg = this.packetBufferQueueSize;
		} else {
			this.buffersizeavg += 0.3 * (this.packetBufferQueueSize - this.buffersizeavg);
		}
		// Idling
		if (this.idleTicks > 0) {
			this.idleTicks--;
			return;
		}

		if (this.isEmpty() && !this.chunkQueue.canSend()) return;
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

	/**
	 * Performs sorting and batch sending at the interval and rate settings specified
	 * 
	 * @param interval to send at
	 * @param rate to send at
	 */
	private void update(int interval, int rate) {
		if (interval == 0) interval = 1;
		if (rate == 0) return;
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
	}

	/**
	 * Prepares the given amount of chunks for sending and flushed compressed chunks
	 * 
	 * @param count of chunks to load
	 */
	private void sendBatch(int count) {				
		//load chunks
		for (int i = 0; i < count; i++) {
			ChunkCoordIntPair pair = this.pollNextChunk();
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

	/**
	 * Waits the amount of ticks specified, doing nothing
	 * 
	 * @param ticks to wait
	 */
	public void idle(int ticks) {
		this.idleTicks += ticks;
	}

	private int getMaxQueueSize() {
		return 10485760;
	}

	/**
	 * Gets the remaining chunks that need sending
	 * 
	 * @return to send size
	 */
	public int getPendingSize() {
		return super.size() + this.chunkQueue.getPendingSize();
	}

	@Override
	public boolean remove(ChunkCoordIntPair pair) {
		return super.remove(pair) || this.chunkQueue.remove(pair.x, pair.z);
	}

	@Override
	public boolean isNear(final int chunkx, final int chunkz, final int view) {
		return EntityUtil.isNearChunk(this.ep, chunkx, chunkz, view + 1);
	}

	@Override
	protected boolean add(ChunkCoordIntPair pair) {
		if (super.add(pair)) {
			this.chunkQueue.remove(pair.x, pair.z);
			this.sendDirection = null; //invalidate
			return true;
		} else {
			return false;
		}
	}
}
