package com.bergerkiller.bukkit.nolagg.spawnlimiter.limit;

/**
 * Keeps track of the count and limit for a single Spawn limit
 */
public class SpawnLimit {
	public int count = 0;
	public int limit;

	public SpawnLimit() {
		this(-1);
	}

	public SpawnLimit(int limit) {
		this.limit = limit;
	}

	public void clear() {
		this.count = 0;
		this.limit = -1;
	}

	public boolean canSpawn() {
		return this.limit == -1 || this.count < this.limit;
	}

	public void spawn() {
		this.count++;
	}

	public void despawn() {
		this.count--;
		if (this.count < 0)
			this.count = 0;
	}

	@Override
	public String toString() {
		return "[" + this.count + "/" + this.limit + "]";
	}
}
