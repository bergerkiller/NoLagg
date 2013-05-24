package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.bases.DummyWorldServer;
import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.bases.PlayerChunkMapBase;
import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.reflection.classes.PlayerChunkMapRef;
import com.bergerkiller.bukkit.common.reflection.classes.WorldServerRef;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

public class DummyPlayerManager extends PlayerChunkMapBase {
	private static final DummyWorldServer DUMMYWORLD = DummyWorldServer.newInstance();
	private static final String playerInstanceClassName = CommonUtil.getNMSClass("PlayerChunk").getName();

	public static void convertAll() {
		// Alter player manager to prevent chunk loading outside range
		for (World world : Bukkit.getWorlds()) {
			DummyPlayerManager.convert(world);
		}
	}

	public static void convert(World world) {
		WorldServerRef.playerChunkMap.set(Conversion.toWorldHandle.convert(world), new DummyPlayerManager(world));
	}

	public static void revert() {
		for (World world : WorldUtil.getWorlds()) {
			final Object worldHandle = Conversion.toWorldHandle.convert(world);
			Object playerChunkMap = WorldServerRef.playerChunkMap.get(worldHandle);
			if (playerChunkMap instanceof DummyPlayerManager) {
				WorldServerRef.playerChunkMap.set(worldHandle, ((DummyPlayerManager) playerChunkMap).base);
			}
		}
	}

	public final Object base;
	private final DummyInstanceMap instances;
	private final Queue<?> dirtyChunkQueue;
	public final World world;

	public DummyPlayerManager(World world) {
		this(WorldServerRef.playerChunkMap.get(Conversion.toWorldHandle.convert(world)), world);
	}

	public DummyPlayerManager(final Object base, World world) {
		super(world, 10);
		this.world = world;
		this.instances = new DummyInstanceMap(PlayerChunkMapRef.playerInstances.get(base), this);
		PlayerChunkMapRef.playerInstances.setInternal(base, this.instances);
		PlayerChunkMapRef.TEMPLATE.transfer(base, this);
		this.base = base;
		this.dirtyChunkQueue = PlayerChunkMapRef.dirtyBlockChunks.get(base);
	}

	@Override
	public void movePlayer(Player player) {
		DummyInstancePlayerList.FILTER = true;
		super.movePlayer(player);
		DummyInstancePlayerList.FILTER = false;
	}

	@Override
	public void addChunksToSend(Player player) {
		int newCX = (int) EntityUtil.getLocX(player) >> 4;
		int newCZ = (int) EntityUtil.getLocX(player) >> 4;
		int dx, dz;
		for (dx = -2; dx <= 2; dx++) {
			for (dz = -2; dz <= 2; dz++) {
				player.getWorld().getChunkAt(newCX + dx, newCZ + dz);
			}
		}
		super.addChunksToSend(player);
	}

	public void removeInstance(IntVector2 location) {
		long key = (long) location.x + 0x7fffffffL | (long) location.z + 0x7fffffffL << 32;
		Object instance = instances.remove(key);
		if (instance != null) {
			this.dirtyChunkQueue.remove(instance);
		}
	}

	@Override
	public World getWorld() {
		for (StackTraceElement elem : Thread.currentThread().getStackTrace()) {
			if (elem.getMethodName().equals("<init>")) {
				if (elem.getClassName().equals(playerInstanceClassName)) {
					DUMMYWORLD.DUMMYCPS.setBase(super.getWorld());
					return Conversion.toWorld.convert(DUMMYWORLD);
				}
			}
		}
		return super.getWorld();
	}
}
