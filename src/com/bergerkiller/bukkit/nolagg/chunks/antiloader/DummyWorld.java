package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World.Environment;
import org.bukkit.craftbukkit.CraftServer;

import com.bergerkiller.bukkit.common.reflection.SafeField;
import com.bergerkiller.bukkit.common.reflection.classes.WorldServerRef;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

import net.minecraft.server.EnumGamemode;
import net.minecraft.server.IChunkLoader;
import net.minecraft.server.IChunkProvider;
import net.minecraft.server.IDataManager;
import net.minecraft.server.NBTTagCompound;
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
			public UUID getUUID() {
				return null;
			}

			public void checkSession() {
			}

			public IChunkLoader createChunkLoader(WorldProvider arg0) {
				return null;
			}

			public File getDataFile(String arg0) {
				return null;
			}

			public PlayerFileData getPlayerFileData() {
				return null;
			}

			public WorldData getWorldData() {
				return null;
			}

			public void saveWorldData(WorldData arg0) {
			}

			public void a() {
			}

			public String g() {
				return null;
			}

			public void saveWorldData(WorldData arg0, NBTTagCompound arg1) {
			}
		};
	}

	private static WorldSettings getDummySettings() {
		return new WorldSettings(0, EnumGamemode.NONE, true, false, WorldType.NORMAL);
	}

	public DummyWorld() throws Throwable {
		this(getDummyName());
	}

	public DummyWorld(String worldname) throws Throwable {
		super(CommonUtil.getMCServer(), getDummyDataManager(), worldname, 0, getDummySettings(), CommonUtil.getMCServer().methodProfiler, Environment.NORMAL, null);
		// dereference this dummy world again...
		new SafeField<Map>(CraftServer.class, "worlds").get(getServer()).remove(worldname.toLowerCase());
		// set some variables to null
		this.chunkProvider = this.chunkProviderServer = new DummyChunkProvider(this);
		this.generator = null;
		this.entityList = null;
		this.tileEntityList = null;
		this.generator = null;
		WorldServerRef.playerManager.set(this, null);
		this.players = null;
		this.tracker = null;
		this.worldMaps = null;
		this.worldProvider = null;
		this.random = null;
	}

	protected void a(WorldSettings worldsettings) {
	};

	protected void b(WorldSettings worldsettings) {
	};

	public void c() {
	};

	public void g() {
	};

	public void v() {
	};

	public void a() {
	};

	public IChunkProvider b() {
		return null;
	}

	public IChunkProvider h() {
		return null;
	}
}
