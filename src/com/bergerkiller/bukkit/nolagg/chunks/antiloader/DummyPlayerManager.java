package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import java.util.Queue;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.reflection.classes.PlayerManagerRef;
import com.bergerkiller.bukkit.common.reflection.classes.WorldServerRef;
import com.bergerkiller.bukkit.common.utils.NativeUtil;

import net.minecraft.server.ChunkCoordIntPair;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.LongHashMap;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WorldServer;

public class DummyPlayerManager extends PlayerManager {
	public static void convert(WorldServer world) {
		if (DummyWorld.INSTANCE != null) {
			WorldServerRef.playerManager.set(world, new DummyPlayerManager(world));
		}
	}

	public static void convert(World world) {
		convert(NativeUtil.getNative(world));
	}

	public static void revert() {
		for (WorldServer world : NativeUtil.getWorlds()) {
			PlayerManager manager = world.getPlayerManager();
			if (manager instanceof DummyPlayerManager) {
				WorldServerRef.playerManager.set(world, ((DummyPlayerManager) manager).base);
			}
		}
	}

	public final PlayerManager base;
	public final WorldServer world;
	private final LongHashMap instances;
	private final Queue<?> dirtyChunkQueue;

	public DummyPlayerManager(WorldServer world) {
		this(world.getPlayerManager(), world);
	}

	public DummyPlayerManager(final PlayerManager base, WorldServer world) {
		super(world, 10);
		this.instances = new DummyInstanceMap(PlayerManagerRef.playerInstances.get(base), this);
		PlayerManagerRef.playerInstances.set(base, this.instances);
		PlayerManagerRef.TEMPLATE.transfer(base, this);
		this.base = base;
		this.world = world;
		this.dirtyChunkQueue = PlayerManagerRef.dirtyBlockChunks.get(base);
	}

	@Override
	public void movePlayer(EntityPlayer arg0) {
		DummyInstancePlayerList.FILTER = true;
		super.movePlayer(arg0);
		DummyInstancePlayerList.FILTER = false;
	}

	@Override
	public void b(EntityPlayer entityplayer) {
		int newCX = (int) entityplayer.locX >> 4;
		int newCZ = (int) entityplayer.locZ >> 4;
		int dx, dz;
		for (dx = -2; dx <= 2; dx++) {
			for (dz = -2; dz <= 2; dz++) {
				entityplayer.world.chunkProvider.getChunkAt(newCX + dx, newCZ + dz);
			}
		}
		super.b(entityplayer);
	}

	public void removeInstance(ChunkCoordIntPair location) {
		long key = (long) location.x + 0x7fffffffL | (long) location.z + 0x7fffffffL << 32;
		Object instance = instances.remove(key);
		if (instance != null) {
			this.dirtyChunkQueue.remove(instance);
		}
	}

	@Override
	public WorldServer a() {
		for (StackTraceElement elem : Thread.currentThread().getStackTrace()) {
			if (elem.getMethodName().equals("<init>")) {
				if (elem.getClassName().equals("net.minecraft.server.PlayerInstance")) {
					return DummyWorld.INSTANCE;
				}
			}
		}
		return super.a();
	}
}
