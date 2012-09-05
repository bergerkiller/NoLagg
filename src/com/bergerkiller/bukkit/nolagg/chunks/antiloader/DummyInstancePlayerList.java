package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import java.util.ArrayList;
import java.util.List;

import com.bergerkiller.bukkit.common.SafeField;
import com.bergerkiller.bukkit.nolagg.chunks.ChunkSendQueue;

import net.minecraft.server.ChunkCoordIntPair;
import net.minecraft.server.EntityPlayer;

@SuppressWarnings("rawtypes")
public class DummyInstancePlayerList extends ArrayList {	
	private static SafeField<ChunkCoordIntPair> instanceLocation = null;
	private static SafeField<List> instancePlayers = null;
	private static boolean VALID;

	static {
		try {
			Class<?> playerInstanceClass = Class.forName("net.minecraft.server.PlayerInstance");
			instanceLocation = new SafeField<ChunkCoordIntPair>(playerInstanceClass, "location");
			instancePlayers = new SafeField<List>(playerInstanceClass, "b");
			VALID = instanceLocation.isValid() && instancePlayers.isValid();
		} catch (Throwable t) {
			t.printStackTrace();
			VALID = false;
		}
	}

	@SuppressWarnings("unchecked")
	public static void replace(DummyPlayerManager playerManager, Object playerInstance) {
		if (VALID) {
			DummyInstancePlayerList list = new DummyInstancePlayerList();
			list.playerManager = playerManager;
			list.location = instanceLocation.get(playerInstance);
			list.addAll(instancePlayers.get(playerInstance));
			instancePlayers.set(playerInstance, list);
		}
	}

	private static final long serialVersionUID = -1878411514739243453L;
	public static boolean FILTER = false;
	private DummyPlayerManager playerManager;
	private ChunkCoordIntPair location;

	@Override
	public boolean contains(Object o) {
		if (super.contains(o)) {
			if (!FILTER || ChunkSendQueue.bind((EntityPlayer) o).preUnloadChunk(this.location)) {
				return true;
			}

			// Player still has to receive this chunk
			// Perform custom removal logic, preventing the unload chunk being sent
			// This is to overcome the [0,0] chunk hole problem
			super.remove(o);
			if (super.isEmpty()) {
				// Remove this player instance from the player manager
				this.playerManager.removeInstance(this.location);
				this.playerManager.world.chunkProviderServer.queueUnload(location.x, location.z);
			}
		}
		return false;
	}
}
