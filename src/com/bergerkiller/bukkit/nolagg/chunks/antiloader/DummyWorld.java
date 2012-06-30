package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World.Environment;
import org.bukkit.craftbukkit.CraftServer;

import com.bergerkiller.bukkit.nolagg.NoLagg;

import net.minecraft.server.IChunkLoader;
import net.minecraft.server.IChunkProvider;
import net.minecraft.server.IDataManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerFileData;
import net.minecraft.server.WorldData;
import net.minecraft.server.WorldProvider;
import net.minecraft.server.WorldServer;
import net.minecraft.server.WorldSettings;
import net.minecraft.server.WorldType;

@SuppressWarnings("rawtypes")
public class DummyWorld extends WorldServer {
	public static DummyWorld INSTANCE;
	static {
		try {
			INSTANCE = new DummyWorld();
		} catch (Throwable t) {
			NoLagg.plugin.log(Level.SEVERE, "Failed to initialize dummy world for manager replacement:");
			t.printStackTrace();
		}
	}

	private static String getDummyName() {
		String worldname = "dummy";
		while (Bukkit.getServer().getWorld(worldname) != null) {
			worldname += "y";
		}
		return worldname;
	}

	private static IDataManager getDummyDataManager() {
		return new IDataManager() {
			public void e() {}
        	public UUID getUUID() {return null;}
			public void checkSession() {}
			public IChunkLoader createChunkLoader(WorldProvider arg0) {return null;}
			public File getDataFile(String arg0) {return null;}
			public PlayerFileData getPlayerFileData() {return null;}
			public WorldData getWorldData() {return null;}
			public void saveWorldData(WorldData arg0) {}
			public void saveWorldData(WorldData arg0, List arg1) {}
        };
	}
	private static MinecraftServer getMC() {
		return ((CraftServer) Bukkit.getServer()).getServer();
	}
	private static WorldSettings getDummySettings() {
		return new WorldSettings(0, 0, true, false, WorldType.NORMAL);
	}

	public DummyWorld() throws Throwable {
		this(getDummyName());
	}
	public DummyWorld(String worldname) throws Throwable {
		super(getMC(), getDummyDataManager(), worldname, 0, getDummySettings(), Environment.NORMAL, null);
		//dereference this dummy world again...
		Field worlds = CraftServer.class.getDeclaredField("worlds");
		worlds.setAccessible(true);
		((Map) worlds.get(getServer())).remove(worldname.toLowerCase());
		//set some variables to null
		this.chunkProvider = this.chunkProviderServer = new DummyChunkProvider(this);
		this.generator = null;
		this.entityList = null;
		this.tileEntityList = null;
		this.generator = null;
		this.manager = null;
		this.players = null;
		this.tracker = null;
		this.worldMaps = null;
		this.worldProvider = null;
		this.random = null;
	}

	public void c() {};
	public void g() {};
	public IChunkProvider b() {return null;}
}

