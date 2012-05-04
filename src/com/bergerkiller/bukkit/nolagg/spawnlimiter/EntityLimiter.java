package com.bergerkiller.bukkit.nolagg.spawnlimiter;

import java.util.ArrayList;

public class EntityLimiter {
	
	public EntityLimiter(String name, GroupLimiter... limiters) {
		ArrayList<SpawnLimiter> lim = new ArrayList<SpawnLimiter>();
		for (GroupLimiter group : limiters) {
			for (SpawnLimiter limiter : group.getLimits(name)) {
				if (limiter != null && limiter.limit >= 0) {
					lim.add(limiter);
				}
			}
		}
		this.limits = lim.toArray(new SpawnLimiter[0]);
	}
	public EntityLimiter(SpawnLimiter[]... limiters) {
		ArrayList<SpawnLimiter> lim = new ArrayList<SpawnLimiter>();
		for (SpawnLimiter[] group : limiters) {
			for (SpawnLimiter limiter : group) {
				if (limiter != null && limiter.limit >= 0) {
					lim.add(limiter);
				}
			}
		}
		this.limits = lim.toArray(new SpawnLimiter[0]);
	}

	private final SpawnLimiter[] limits;
	public boolean canSpawn() {
		for (SpawnLimiter limit : limits) {
			if (!limit.canSpawn()) return false;
		}
		return true;
	}
	public void spawn() {
		for (SpawnLimiter limit : limits) limit.spawn();
	}
	public void despawn() {
		for (SpawnLimiter limit : limits) limit.despawn();
	}
	public boolean handleSpawn() {
		if (this.canSpawn()) {
			this.spawn();
			return true;
		} else {
			return false;
		}
	}
}
