package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import java.util.List;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.Operation;
import com.bergerkiller.bukkit.common.SafeField;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

import net.minecraft.server.LongHashMap;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WorldServer;

public class DummyManager extends PlayerManager {
	public static SafeField<List<?>> cField = new SafeField<List<?>>(PlayerManager.class, "c");
	public static SafeField<LongHashMap> instanceMap = new SafeField<LongHashMap>(PlayerManager.class, "b");
	public static SafeField<Integer> view = new SafeField<Integer>(PlayerManager.class, "f");
	public static SafeField<Integer> dim = new SafeField<Integer>(PlayerManager.class, "e");

	public static void convert(WorldServer world) {
		if (cField.isValid() && instanceMap.isValid() && view.isValid() && dim.isValid() && DummyWorld.INSTANCE != null) {
			world.manager = new DummyManager(world);
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
				if (world.manager instanceof DummyManager) {
					world.manager = ((DummyManager) world.manager).base;
				}
			}
		};
	}

	public final PlayerManager base;
	public final WorldServer world;

	public DummyManager(WorldServer world) {
		this(world.manager, world);
	}

	public DummyManager(final PlayerManager base, WorldServer world) {
		super(CommonUtil.getMCServer(), dim.get(base), view.get(base));
		instanceMap.set(this, instanceMap.get(base));
		cField.set(this, cField.get(base));
		this.managedPlayers = base.managedPlayers;
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
