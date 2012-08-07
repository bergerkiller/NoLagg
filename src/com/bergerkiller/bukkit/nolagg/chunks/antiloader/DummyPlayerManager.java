package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import java.util.List;

import org.bukkit.World;

import com.bergerkiller.bukkit.common.Operation;
import com.bergerkiller.bukkit.common.SafeField;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.LongHashMap;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WorldServer;

public class DummyPlayerManager extends PlayerManager {
	public static SafeField<LongHashMap> instanceMap = new SafeField<LongHashMap>(PlayerManager.class, "c");
	public static SafeField<List<?>> cField = new SafeField<List<?>>(PlayerManager.class, "d");
	public static SafeField<Integer> view = new SafeField<Integer>(PlayerManager.class, "e");
	public static SafeField<List<?>> managedPlayers = new SafeField<List<?>>(PlayerManager.class, "managedPlayers");
	public static SafeField<PlayerManager> worldManager = new SafeField<PlayerManager>(WorldServer.class, "manager");

	public static void convert(WorldServer world) {
		if (cField.isValid() && instanceMap.isValid() && view.isValid() && worldManager.isValid() && managedPlayers.isValid() && DummyWorld.INSTANCE != null) {
			worldManager.set(world, new DummyPlayerManager(world));
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
				if (manager instanceof DummyPlayerManager) {
					worldManager.set(world, ((DummyPlayerManager) manager).base);
				}
			}
		};
	}

	public final PlayerManager base;
	public final WorldServer world;

	public DummyPlayerManager(WorldServer world) {
		this(world.getPlayerManager(), world);
	}

	public DummyPlayerManager(final PlayerManager base, WorldServer world) {
		super(world, view.get(base));
		instanceMap.set(this, instanceMap.get(base));
		cField.set(this, cField.get(base));
		managedPlayers.set(this, managedPlayers.get(base));
		this.base = base;
		this.world = world;
	}

	@Override
    public void movePlayer(EntityPlayer entityplayer) {
		// Changed chunks? load a 5x5 area
        if (MathUtil.distanceSquared(entityplayer.d, entityplayer.e, entityplayer.locX, entityplayer.locZ) >= 64.0D) {
            int newCX = (int) entityplayer.locX >> 4;
            int newCZ = (int) entityplayer.locZ >> 4;
            int dx, dz;
            for (dx = -2; dx <= 2; dx++) {
            	for (dz = -2; dz <= 2; dz++) {
            		entityplayer.world.getChunkAt(newCX + dx, newCZ + dz);
            	}
            }
        }
		super.movePlayer(entityplayer);		
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
