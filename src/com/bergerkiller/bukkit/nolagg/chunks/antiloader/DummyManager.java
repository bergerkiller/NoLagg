package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import java.util.List;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.Operation;
import com.bergerkiller.bukkit.common.SafeField;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

import net.minecraft.server.LongHashMap;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WorldServer;

public class DummyManager extends PlayerManager {
	public static SafeField<LongHashMap> instanceMap = new SafeField<LongHashMap>(PlayerManager.class, "c");
	public static SafeField<List<?>> cField = new SafeField<List<?>>(PlayerManager.class, "d");
	public static SafeField<Integer> view = new SafeField<Integer>(PlayerManager.class, "e");
	public static SafeField<List<?>> managedPlayers = new SafeField<List<?>>(PlayerManager.class, "managedPlayers");
	public static SafeField<PlayerManager> worldManager = new SafeField<PlayerManager>(WorldServer.class, "manager");

	public static void convert(WorldServer world) {
		if (cField.isValid() && instanceMap.isValid() && view.isValid() && worldManager.isValid() && managedPlayers.isValid() && DummyWorld.INSTANCE != null) {
			worldManager.set(world, new DummyManager(world));
		}
	}

	public static void convert(World world) {
		convert(WorldUtil.getNative(world));
	}

	public static void revert() {
		new Operation() {
			public void run() {
				this.doWorlds();
			}
			public void handle(WorldServer world) {
				PlayerManager manager = world.getPlayerManager();
				if (manager instanceof DummyManager) {
					worldManager.set(world, ((DummyManager) manager).base);
				}
			}
		};
	}

	public final PlayerManager base;
	public final WorldServer world;

	public DummyManager(WorldServer world) {
		this(world.getPlayerManager(), world);
	}

	public DummyManager(final PlayerManager base, WorldServer world) {
		super(world, view.get(base));
		instanceMap.set(this, instanceMap.get(base));
		cField.set(this, cField.get(base));
		managedPlayers.set(this, managedPlayers.get(base));
		this.base = base;
		this.world = world;
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
