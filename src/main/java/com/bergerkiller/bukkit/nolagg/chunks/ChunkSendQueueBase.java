package com.bergerkiller.bukkit.nolagg.chunks;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import com.bergerkiller.bukkit.common.ActiveState;
import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.utils.CommonUtil;

/**
 * Only contains the empty-faking and double-mapping of contained elements
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class ChunkSendQueueBase extends LinkedList {
	private static final long serialVersionUID = 1L;
	protected final ActiveState<Boolean> updating = new ActiveState<Boolean>(false);
	private final Set<IntVector2> contained = new HashSet<IntVector2>();
	protected final Set<IntVector2> sentChunks = new HashSet<IntVector2>();
	private boolean isSentChunksVerified = true;

	/**
	 * Sorts the contents of this queue to send in direction of the player<br>
	 * Also cleans up some of the other internal collections (to handle chunk
	 * change movement)
	 */
	public void sort() {
		for (Iterator<IntVector2> iter = sentChunks.iterator(); iter.hasNext();) {
			if (!this.isNear(iter.next(), CommonUtil.VIEW)) {
				iter.remove();
			}
		}
	}

	/**
	 * Notifies the queue that all chunks previously sent have been unloaded by the client
	 */
	public void setOldUnloaded() {
		this.sentChunks.clear();
	}

	/**
	 * Performs a pre-unload operation on this queue
	 * 
	 * @param chunkCoord of the chunk to unload
	 * @return True if an unload packet is required, False if not
	 */
	public boolean preUnloadChunk(IntVector2 chunkCoord) {
		this.remove(chunkCoord);
		return this.sentChunks.remove(chunkCoord);
	}

	public abstract int getCenterX();

	public abstract int getCenterZ();

	public void verifySentChunks() {
		if (!this.isSentChunksVerified) {
			this.isSentChunksVerified = true;
			// Verify all chunks - add those that haven't been sent yet
			final int view = DynamicViewDistance.viewDistance;
			int cx, cz;
			int x = this.getCenterX();
			int z = this.getCenterZ();
			IntVector2 pair;
			for (cx = x - view; cx <= x + view; cx++) {
				for (cz = z - view; cz <= z + view; cz++) {
					pair = new IntVector2(cx, cz);
					if (!this.sentChunks.contains(pair)) {
						this.add(pair);
					}
				}
			}
		}
	}

	protected boolean remove(IntVector2 pair) {
		synchronized (this) {
			return this.contained.remove(pair) && super.remove(pair);
		}
	}

	protected boolean add(IntVector2 pair) {
		if (this.isNear(pair, CommonUtil.VIEW)) {
			synchronized (this) {
				// Add to sending queue if not contained, or a re-send is
				// requested
				if (this.contained.add(pair) || !super.contains(pair)) {
					this.isSentChunksVerified = false;
					return super.add(pair);
				}
			}
		}
		return false;	
	}

	/**
	 * Polls the next chunk coordinate for the chunk that can be loaded and sent
	 * 
	 * @return next Chunk coordinate
	 */
	protected synchronized IntVector2 pollNextChunk() {
		Iterator<Object> iter = super.iterator();
		while (iter.hasNext()) {
			IntVector2 pair = Conversion.toIntVector2.convert(iter.next());
			if (isNearDynamic(pair.x, pair.z)) {
				iter.remove();
				return pair;
			} else if (!this.isNear(pair, CommonUtil.VIEW)) {
				iter.remove();
				this.contained.remove(pair);
			}
		}
		return null;
	}

	/**
	 * Removes a chunk coordinate from the contained set
	 * 
	 * @param x- coordinate of the chunk
	 * @param z- coordinate of the chunk
	 */
	public void removeContained(int x, int z) {
		this.contained.remove(new IntVector2(x, z));
	}

	/**
	 * Converts all the contained contents into a linked list
	 * 
	 * @return linked list with the contents
	 */
	public LinkedList toLinkedList() {
		return new LinkedList(this.contained);
	}

	/*
	 * Prevent Spout and CB from using this queue...seriously!
	 */
	@Override
	public boolean isEmpty() {
		return this.updating.get() ? super.isEmpty() : true;
	}

	@Override
	public int size() {
		return this.updating.get() ? super.size() : 0;
	}

	@Override
	public Object[] toArray() {
		this.updating.next(true);
		try {
			synchronized (this) {
				return super.toArray();
			}
		} finally {
			this.updating.previous();
		}
	}

	@Override
	public Object[] toArray(Object[] value) {
		this.updating.next(true);
		try {
			synchronized (this) {
				return super.toArray(value);
			}
		} finally {
			this.updating.previous();
		}
	}

	@Override
	public void clear() {
		if (this.updating.get()) {
			super.clear();
			return;
		}
		synchronized (this) {
			this.contained.removeAll(this);
			super.clear();
		}
	}

	@Override
	public boolean containsAll(Collection coll) {
		synchronized (this) {
			for (Object o : coll) {
				if (!this.contains(o))
					return false;
			}
			return true;
		}
	}

	@Override
	public boolean contains(Object o) {
		synchronized (this) {
			return this.contained.contains(o);
		}
	}

	@Override
	public boolean addAll(Collection coll) {
		synchronized (this) {
			for (Object o : coll)
				this.add(o);
			return true;
		}
	}

	@Override
	public boolean addAll(int index, Collection coll) {
		synchronized (this) {
			for (Object o : coll)
				this.add(index, o);
			return true;
		}
	}

	/**
	 * Checks if the chunk is near this queue when using the dynamic view
	 * distance
	 * 
	 * @param chunkx
	 *            of the chunk
	 * @param chunkz
	 *            of the chunk
	 * @return True is it is near, False if not
	 */
	public boolean isNearDynamic(final int chunkx, final int chunkz) {
		if (NoLaggChunks.hasDynamicView) {
			return this.isNear(chunkx, chunkz, DynamicViewDistance.viewDistance - 1);
		} else {
			return true;
		}
	}

	/**
	 * Checks if the chunk is near this queue and can be contained
	 * 
	 * @param coord
	 *            of the chunk
	 * @param view
	 *            distance
	 * @return True is it is near, False if not
	 */
	public boolean isNear(IntVector2 coord, final int view) {
		return this.isNear(coord.x, coord.z, view);
	}

	/**
	 * Checks if the chunk is near this queue and can be contained
	 * 
	 * @param chunkx of the chunk
	 * @param chunkz of the chunk
	 * @param view distance
	 * @return True is it is near, False if not
	 */
	public abstract boolean isNear(final int chunkx, final int chunkz, final int view);

	@Override
	public boolean remove(Object object) {
		IntVector2 value = Conversion.toIntVector2.convert(object);
		if (value == null) {
			return false;
		}
		return remove(value);
	}

	/**
	 * Is called in PlayerInstance and PlayerManager to queue a new chunk
	 * coordinate
	 */
	@Override
	public synchronized boolean add(Object object) {
		if (this.updating.get()) {
			return super.add(object);
		}
		IntVector2 value = Conversion.toIntVector2.convert(object);
		if (value == null) {
			return false;
		}
		return this.add(value);
	}

	@Override
	public synchronized void add(int index, Object object) {
		if (this.updating.get()) {
			super.add(index, object);
			return;
		}
		IntVector2 value = Conversion.toIntVector2.convert(object);
		if (value == null) {
			return;
		}
		if (this.add(value)) {
			super.add(index, value);
		}
	}
}
