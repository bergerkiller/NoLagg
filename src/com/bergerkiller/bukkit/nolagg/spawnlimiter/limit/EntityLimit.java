package com.bergerkiller.bukkit.nolagg.spawnlimiter.limit;

import java.util.ArrayList;


/**
 * Bundles multiple Spawn limits together to form one Entity limit
 */
public class EntityLimit {
	private final String name;
	private final SpawnLimit[] limits;

	public EntityLimit() {
		this.name = "EMPTY";
		this.limits = new SpawnLimit[0];
	}

	public EntityLimit(String name, GroupLimiter... limiters) {
		this.name = name;
		ArrayList<SpawnLimit> lim = new ArrayList<SpawnLimit>();
		for (GroupLimiter group : limiters) {
			for (SpawnLimit limiter : group.getLimits(name)) {
				if (limiter != null && limiter.limit >= 0) {
					lim.add(limiter);
				}
			}
		}
		this.limits = lim.toArray(new SpawnLimit[0]);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Limits of ").append(this.name).append(" [").append(this.limits.length).append("]: ");
		for (SpawnLimit limit : this.limits) {
			builder.append('\n').append("  ").append(limit.toString());
		}
		return builder.toString();
	}

	public boolean canSpawn() {
		for (SpawnLimit limit : limits) {
			if (!limit.canSpawn()) {
				return false;
			}
		}
		return true;
	}

	public void spawn() {
		for (SpawnLimit limit : limits) {
			limit.spawn();
		}
	}

	public void despawn() {
		for (SpawnLimit limit : limits) {
			limit.despawn();
		}
	}
}
