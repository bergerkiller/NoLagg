package com.bergerkiller.bukkit.nolagg.chunks;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import com.bergerkiller.bukkit.common.IntRemainder;
import com.bergerkiller.bukkit.common.ToggledState;
import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.protocol.PacketFields;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.reflection.classes.EntityPlayerRef;
import com.bergerkiller.bukkit.common.reflection.classes.EntityRef;
import com.bergerkiller.bukkit.common.reflection.classes.NetworkManagerRef;
import com.bergerkiller.bukkit.common.reflection.classes.PlayerConnectionRef;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.PlayerUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ChunkSendQueue extends ChunkSendQueueBase {
	private static final long serialVersionUID = 1L;
	public static double maxRate = 2;
	public static double minRate = 0.25;
	public static double compressBusyPercentage = 0.0;
	private static long prevtime;
	private static Task task;

	private static class ChunkLoadingTask extends Task {
		public ChunkLoadingTask() {
			super(NoLagg.plugin);
		}

		@Override
		public void run() {
			try {
				double newper = ChunkCompressionThread.getBusyPercentage(System.currentTimeMillis() - prevtime);
				compressBusyPercentage = MathUtil.useOld(compressBusyPercentage, newper * 100.0, 0.1);
				prevtime = System.currentTimeMillis();
				for (Player player : CommonUtil.getOnlinePlayers()) {
					ChunkSendQueue queue = bind(player);
					queue.updating.next(true);
					queue.update();
					queue.updating.reset(false);
				}
			} catch (Exception ex) {
				NoLaggChunks.plugin.log(Level.SEVERE, "An error occured while sending chunks:");
				ex.printStackTrace();
			} catch (OutOfMemoryError ex) {
				NoLaggChunks.plugin.log(Level.SEVERE, "We are running out of memory here!");
				NoLaggChunks.plugin.log(Level.SEVERE, "Restart the server and increase the RAM usage available for Bukkit.");
			}
		}
	}

	public static void init() {
		if (!NetworkManagerRef.queueSize.isValid()) {
			NoLaggChunks.plugin.log(Level.SEVERE, "Failed to hook into the player packet queue size field");
			NoLaggChunks.plugin.log(Level.SEVERE, "Distortions in the chunk rate will cause players to get kicked");
		}
		prevtime = System.currentTimeMillis();
		task = new ChunkLoadingTask().start(1, 1);
	}

	public static void deinit() {
		Task.stop(task);
		task = null;
		// clear bound queues
		for (Player player : CommonUtil.getOnlinePlayers()) {
			ChunkSendQueue queue = bind(player);
			if (queue != null) {
				EntityPlayerRef.chunkQueue.set(Conversion.toEntityHandle.convert(player), queue.toLinkedList());
			}
		}
	}

	public static ChunkSendQueue bind(Player with) {
		Object ep = Conversion.toEntityHandle.convert(with);
		List<?> currqueue = EntityPlayerRef.chunkQueue.get(ep);
		if (currqueue instanceof ChunkSendQueue) {
			return (ChunkSendQueue) currqueue;
		} else {
			ChunkSendQueue queue = new ChunkSendQueue(with);
			currqueue.clear();
			EntityPlayerRef.chunkQueue.set(ep, queue);
			return queue;
		}
	}

	public final Player player;
	private int idleTicks = 0;
	public BlockFace sendDirection = BlockFace.NORTH;
	public World world;
	public int x;
	public int z;
	private IntRemainder rate = new IntRemainder(2.0, 1);
	private int intervalcounter = 200;
	private ChunkCompressQueue chunkQueue;
	private final ToggledState isSentChunksVerified = new ToggledState();

	/*
	 * Packet queue related variables
	 */
	private int prevQueueSize = 0;
	private int maxQueueSize = 300000;
	private int packetBufferQueueSize = 0;
	private int buffersizeavg = 0;

	private ChunkSendQueue(final Player player) {
		this.player = player;
		this.world = player.getWorld();
		this.sendDirection = null; // Force a sorting operation the next tick
		Object playerHandle = Conversion.toEntityHandle.convert(player);
		this.x = (int) (EntityRef.locX.get(playerHandle) + EntityRef.motX.get(playerHandle) * 16) >> 4;
		this.z = (int) (EntityRef.locZ.get(playerHandle) + EntityRef.motZ.get(playerHandle) * 16) >> 4;
		this.chunkQueue = new ChunkCompressQueue(this);
		this.addAll(EntityPlayerRef.chunkQueue.get(playerHandle));
		this.add(new IntVector2(EntityRef.chunkX.get(playerHandle), EntityRef.chunkZ.get(playerHandle)));
		ChunkCompressionThread.addQueue(this.chunkQueue);
		this.enforceBufferFullSize();
	}

	private void enforceBufferFullSize() {
		final Object playerHandle = Conversion.toEntityHandle.convert(player);
		final Object playerConnection = EntityPlayerRef.playerConnection.get(playerHandle);
		final Object nm = PlayerConnectionRef.networkManager.get(playerConnection);
		// We can only work on Network manager implementations, INetworkManager implementations are unknown to us
		if (!NetworkManagerRef.TEMPLATE.isInstance(nm)) {
			return;
		}
		Object lockObject = NetworkManagerRef.lockObject.get(nm);
		if (lockObject != null) {
			List<Object> low = NetworkManagerRef.lowPriorityQueue.get(nm);
			List<Object> high = NetworkManagerRef.highPriorityQueue.get(nm);
			if (low != null && high != null) {
				int queuedsize = 0;
				synchronized (lockObject) {
					for (Object p : low) {
						queuedsize += PacketFields.DEFAULT.getPacketSize(p) + 1;
					}
					for (Object p : high) {
						queuedsize += PacketFields.DEFAULT.getPacketSize(p) + 1;
					}
					NetworkManagerRef.queueSize.set(nm, queuedsize - 9437184);
				}
			}
		}
	}

	@Override
	public int getCenterX() {
		return this.x;
	}

	@Override
	public int getCenterZ() {
		return this.z;
	}

	public static double getAverageRate() {
		double totalrate = 0;
		int pcount = 0;
		for (Player player : CommonUtil.getOnlinePlayers()) {
			totalrate += bind(player).rate.get();
			pcount++;
		}
		return totalrate / (double) pcount;
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

	/**
	 * Sorts the contents of this queue to send in direction of the player<br>
	 * Also cleans up some of the other internal collections (to handle chunk
	 * change movement)
	 */
	public void sort() {
		this.chunkQueue.sort();
		synchronized (this) {
			this.updating.next(true);
			this.sort(this);
			this.updating.previous();
		}
	}

	public void sort(List elements) {
		if (elements.isEmpty()) {
			return;
		}
		IntVector2 middle = new IntVector2(this.x, this.z);
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

	public void verifySentChunks() {
		if (isSentChunksVerified.set()) {
			// Verify all chunks - add those that haven't been sent yet
			final int view = DynamicViewDistance.viewDistance;
			int cx, cz;
			int x = this.getCenterX();
			int z = this.getCenterZ();
			IntVector2 pair;
			for (cx = x - view; cx <= x + view; cx++) {
				for (cz = z - view; cz <= z + view; cz++) {
					pair = new IntVector2(cx, cz);
					if (!PlayerUtil.isChunkVisible(player, pair.x, pair.z)) {
						this.add(pair);
					}
				}
			}
		}
	}

	/**
	 * Main update routine - handles the calculation of the rate and interval
	 * and updates afterwards
	 */
	private void update() {
		// Update queue size
		if (NetworkManagerRef.queueSize.isValid()) {
			final Object playerHandle = Conversion.toEntityHandle.convert(this.player);
			final Object playerConnection = EntityPlayerRef.playerConnection.get(playerHandle);
			final Object networkManager = PlayerConnectionRef.networkManager.get(playerConnection);
			if (NetworkManagerRef.TEMPLATE.isInstance(networkManager)) {
				this.packetBufferQueueSize = NetworkManagerRef.queueSize.get(networkManager) + 9437184;
			} else {
				// We can not properly find out what the size queued is
				// This section occurs in Spigot, and therefore needs to be handled
				// As fallback we can only assume that there is no buffer related issue...
				this.packetBufferQueueSize = 0;
				//TODO: Find out a way to add support for Spigot so the queued size can be read.
			}
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

		if (this.isEmpty() && !this.chunkQueue.canSend()) {
			// Queue some remaining chunks?
			this.verifySentChunks();
			if (this.isEmpty()) {
				return;
			}
		}
		double newrate = this.rate.get();
		if (this.packetBufferQueueSize > this.maxQueueSize) {
			newrate = minRate;
		} else {
			if (this.prevQueueSize > this.packetBufferQueueSize) {
				newrate += 0.07;
			} else {
				// to force the rate to be optimal
				if (this.packetBufferQueueSize > 80000) {
					newrate -= 0.17;
				} else if (this.packetBufferQueueSize > 20000) {
					newrate -= 0.14;
				} else {
					newrate += 0.06;
				}
			}
			newrate += 0.9 * (this.rate.get() - newrate);
			// set rate bounds
			if (newrate > maxRate) {
				newrate = maxRate;
			} else if (newrate < minRate) {
				newrate = minRate;
			}
		}

		this.rate.set(newrate);
		this.prevQueueSize = this.packetBufferQueueSize;
		// send chunks
		if (newrate >= 1) {
			this.update(1, (int) this.rate.next());
		} else {
			this.update((int) (1 / this.rate.get()), 1);
		}
	}

	/**
	 * Performs sorting and batch sending at the interval and rate settings
	 * specified
	 * 
	 * @param interval to send at
	 * @param rate to send at
	 */
	private void update(int interval, int rate) {
		if (interval == 0) {
			interval = 1;
		}
		if (rate == 0) {
			return;
		}
		if (this.intervalcounter >= interval) {
			updatePosition(player.getLocation());
			this.sendBatch(rate);
			this.intervalcounter = 1;
		} else {
			this.intervalcounter++;
		}
	}

	/**
	 * Updates the position of this queue for the player
	 * 
	 * @param position to set to
	 */
	public void updatePosition(Location position) {
		updatePosition(position.getWorld(), position.getX(), position.getZ(), position.getYaw());
	}

	/**
	 * Updates the position of this queue for the player
	 * 
	 * @param world to set to
	 * @param locX to set to
	 * @param locZ to set to
	 * @param yaw to set to
	 */
	public void updatePosition(World world, double locX, double locZ, float yaw) {
		BlockFace newDirection = FaceUtil.yawToFace(yaw - 90.0F);
		int newx = MathUtil.toChunk(locX);
		int newz = MathUtil.toChunk(locZ);
		if (world != this.world || newx != this.x || newz != this.z || this.sendDirection != newDirection) {
			this.sendDirection = newDirection;
			this.x = newx;
			this.z = newz;
			this.world = world;
			this.sort();
		}
	}

	/**
	 * Prepares the given amount of chunks for sending and flushed compressed
	 * chunks
	 * 
	 * @param count of chunks to load
	 */
	private void sendBatch(int count) {
		// load chunks
		final World world = this.player.getWorld();
		for (int i = 0; i < count; i++) {
			final IntVector2 pair = this.pollNextChunk();
			if (pair == null) {
				break;
			}
			WorldUtil.getChunkAsync(this.player.getWorld(), pair.x, pair.z, new Runnable() {
				public void run() {
					// Check whether we didn't just change worlds
					if (ChunkSendQueue.this.player.getWorld() != world) {
						return;
					}
					// Load the chunk
					ChunkSendQueue.this.chunkQueue.enqueue(WorldUtil.getChunk(world, pair.x, pair.z));
				}
			});
		}

		// send chunks
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

	@Override
	public int getPendingSize() {
		return super.getPendingSize() + this.chunkQueue.getPendingSize();
	}

	@Override
	public boolean removePair(IntVector2 pair) {
		return super.removePair(pair) || this.chunkQueue.remove(pair.x, pair.z);
	}

	@Override
	public boolean isNear(final int chunkx, final int chunkz, final int view) {
		return EntityUtil.isNearChunk(this.player, chunkx, chunkz, view + 1);
	}

	@Override
	protected boolean addPair(int index, IntVector2 pair) {
		if (super.addPair(index, pair)) {
			this.chunkQueue.remove(pair.x, pair.z);
			this.sendDirection = null; // invalidate
			this.isSentChunksVerified.clear(); // invalidate
			return true;
		} else {
			return false;
		}
	}
}
