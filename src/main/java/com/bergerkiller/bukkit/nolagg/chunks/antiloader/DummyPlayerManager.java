package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.World;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.bases.DummyWorldServer;
import com.bergerkiller.bukkit.common.bases.PlayerChunkMapBase;
import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.reflection.classes.PlayerManagerRef;
import com.bergerkiller.bukkit.common.reflection.classes.WorldServerRef;
import com.bergerkiller.bukkit.common.utils.NativeUtil;

import net.minecraft.server.v1_4_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_4_R1.EntityPlayer;
import net.minecraft.server.v1_4_R1.LongHashMap;
import net.minecraft.server.v1_4_R1.PlayerChunkMap;
import net.minecraft.server.v1_4_R1.WorldServer;

public class DummyPlayerManager extends PlayerChunkMapBase {
	public static final DummyWorldServer DUMMYWORLD;
	public static final DummyChunkProvider DUMMYCPS;
	static {
		DUMMYWORLD = new DummyWorldServer();
		DUMMYCPS = new DummyChunkProvider(DUMMYWORLD);
		WorldServerRef.chunkProviderServer.set(DUMMYWORLD, DUMMYCPS);
	}

	public static void convertAll() {
		// Alter player manager to prevent chunk loading outside range
		for (World world : Bukkit.getWorlds()) {
			DummyPlayerManager.convert(world);
		}
	}

	public static void convert(World world) {
		WorldServerRef.playerManager.set(Conversion.toWorldHandle.convert(world), new DummyPlayerManager(world));
	}

	public static void revert() {
		for (WorldServer world : NativeUtil.getWorlds()) {
			PlayerChunkMap manager = world.getPlayerChunkMap();
			if (manager instanceof DummyPlayerManager) {
				WorldServerRef.playerManager.set(world, ((DummyPlayerManager) manager).base);
			}
		}
	}

	public final Object base;
	private final LongHashMap instances;
	private final Queue<?> dirtyChunkQueue;
	public final World world;

	public DummyPlayerManager(World world) {
		this(WorldServerRef.playerManager.get(Conversion.toWorldHandle.convert(world)), world);
	}

	public DummyPlayerManager(final Object base, World world) {
		super(world, 10);
		this.world = world;
		this.instances = new DummyInstanceMap(PlayerManagerRef.playerInstances.get(base), this);
		PlayerManagerRef.playerInstances.set(base, this.instances);
		PlayerManagerRef.TEMPLATE.transfer(base, this);
		this.base = base;
		this.dirtyChunkQueue = PlayerManagerRef.dirtyBlockChunks.get(base);
	}

	@Override
	public void movePlayer(EntityPlayer arg0) {
		DummyInstancePlayerList.FILTER = true;
		super.movePlayer(arg0);
		DummyInstancePlayerList.FILTER = false;
	}

	@Override
	public void addChunksToSend(EntityPlayer entityplayer) {
		int newCX = (int) entityplayer.locX >> 4;
		int newCZ = (int) entityplayer.locZ >> 4;
		int dx, dz;
		for (dx = -2; dx <= 2; dx++) {
			for (dz = -2; dz <= 2; dz++) {
				entityplayer.world.chunkProvider.getChunkAt(newCX + dx, newCZ + dz);
			}
		}
		super.addChunksToSend(entityplayer);
	}

	public void removeInstance(ChunkCoordIntPair location) {
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
				if (elem.getClassName().equals(Common.NMS_ROOT + ".PlayerInstance")) {
					DUMMYCPS.setBase(super.getWorld());
					return DUMMYWORLD.getWorld();
				}
			}
		}
		return super.getWorld();
	}
}
