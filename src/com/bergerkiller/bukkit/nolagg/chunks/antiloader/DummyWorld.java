package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import java.io.File;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World.Environment;

import com.bergerkiller.bukkit.common.reflection.classes.WorldServerRef;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
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

public class DummyWorld extends WorldServer {
	private static boolean enabled = false;
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
			public void checkAccess() {
				if (enabled) {
					throw new IllegalStateException("NoLagg chunks dummy world has been accessed!");
				}
			}

			public UUID getUUID() {
				checkAccess();
				return UUID.randomUUID();
			}

			public void checkSession() {
				checkAccess();
			}

			public IChunkLoader createChunkLoader(WorldProvider arg0) {
				checkAccess();
				return null;
			}

			public File getDataFile(String arg0) {
				checkAccess();
				return null;
			}

			public PlayerFileData getPlayerFileData() {
				checkAccess();
				return null;
			}

			public WorldData getWorldData() {
				checkAccess();
				return null;
			}

			public void saveWorldData(WorldData arg0) {
				checkAccess();
			}

			public void a() {
				checkAccess();
			}

			public String g() {
				checkAccess();
				return null;
			}

			public void saveWorldData(WorldData arg0, NBTTagCompound arg1) {
				checkAccess();
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
		enabled = true;
		// dereference this dummy world again...
		WorldUtil.removeWorld(this.getWorld());
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
