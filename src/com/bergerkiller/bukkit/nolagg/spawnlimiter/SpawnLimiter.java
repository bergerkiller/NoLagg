package com.bergerkiller.bukkit.nolagg.spawnlimiter;

public class SpawnLimiter {

	public SpawnLimiter() {
		this(-1);
	}

	public SpawnLimiter(int limit) {
		this.limit = limit;
	}

	public int count = 0;
	public int limit;

	public void reset() {
		this.count = 0;
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
