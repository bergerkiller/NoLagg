package com.bergerkiller.bukkit.nolagg.chunks;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import com.bergerkiller.bukkit.common.ActiveState;
import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.reflection.classes.VectorRef;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.wrappers.LongHashSet;

/**
 * Only contains the empty-faking and double-mapping of contained elements
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class ChunkSendQueueBase extends LinkedList {
	private static final long serialVersionUID = 1L;
	protected final ActiveState<Boolean> updating = new ActiveState<Boolean>(false);
	private final LongHashSet contained = new LongHashSet();

	public abstract int getCenterX();

	public abstract int getCenterZ();

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
				this.contained.remove(pair.x, pair.z);
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
		this.contained.remove(x, z);
	}

	/**
	 * Converts all the contained contents into a linked list
	 * 
	 * @return linked list with the contents
	 */
	public LinkedList toLinkedList() {
		LinkedList value = new LinkedList();
		for (long key : this.contained) {
			value.add(VectorRef.newPair(MathUtil.longHashMsw(key), MathUtil.longHashLsw(key)));
		}
		return value;
	}

	/**
	 * Gets the remaining chunks that need sending
	 * 
	 * @return to send size
	 */
	public int getPendingSize() {
		return super.size();
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
			this.contained.clear();
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
			return this.contained.contains(VectorRef.getPairX(o), VectorRef.getPairZ(o));
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
	 * @param chunkx of the chunk
	 * @param chunkz of the chunk
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
	 * @param coord of the chunk
	 * @param view distance
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

	protected synchronized boolean removePair(IntVector2 pair) {
		if (pair == null) {
			return false;
		}
		return this.contained.remove(MathUtil.longHashToLong(pair.x, pair.z)) && super.remove(Conversion.toChunkCoordIntPairHandle.convert(pair));
	}

	protected final synchronized boolean addPair(IntVector2 pair) {
		return addPair(super.size(), pair);
	}

	protected synchronized boolean addPair(int index, IntVector2 pair) {
		if (pair == null) {
			return false;
		}
		if (this.isNear(pair, CommonUtil.VIEW)) {
			final Object handle = Conversion.toChunkCoordIntPairHandle.convert(pair);
			// Add to sending queue if not contained, or a re-send is requested
			if (this.contained.add(pair.x, pair.z) || !super.contains(handle)) {
				super.add(index, handle);
				return true;
			}
		}
		return false;	
	}

	@Override
	public boolean remove(Object object) {
		return removePair(Conversion.toIntVector2.convert(object));
	}

	/**
	 * Is called in PlayerInstance and PlayerManager to queue a new chunk coordinate
	 */
	@Override
	public synchronized boolean add(Object object) {
		if (this.updating.get()) {
			return super.add(Conversion.toChunkCoordIntPairHandle.convert(object));
		} else {
			return this.addPair(Conversion.toIntVector2.convert(object));
		}
	}

	@Override
	public synchronized void add(int index, Object object) {
		if (this.updating.get()) {
			super.add(index, Conversion.toChunkCoordIntPairHandle.convert(object));
		} else {
			this.addPair(index, Conversion.toIntVector2.convert(object));
		}
	}
}
