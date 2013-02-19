package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import java.util.ArrayList;

import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.reflection.classes.PlayerInstanceRef;
import com.bergerkiller.bukkit.common.utils.ChunkUtil;
import com.bergerkiller.bukkit.nolagg.chunks.ChunkSendQueue;

@SuppressWarnings("rawtypes")
public class DummyInstancePlayerList extends ArrayList {
	@SuppressWarnings("unchecked")
	public static void replace(DummyPlayerManager playerManager, Object playerInstance) {
		DummyInstancePlayerList list = new DummyInstancePlayerList();
		list.playerManager = playerManager;
		list.location = Conversion.toIntVector2.convert(PlayerInstanceRef.location.get(playerInstance));
		list.addAll(PlayerInstanceRef.players.get(playerInstance));
		PlayerInstanceRef.players.set(playerInstance, list);
	}

	private static final long serialVersionUID = -1878411514739243453L;
	public static boolean FILTER = false;
	private DummyPlayerManager playerManager;
	private IntVector2 location;

	@Override
	public boolean contains(Object o) {
		if (super.contains(o)) {
			if (!FILTER || ChunkSendQueue.bind(Conversion.convert(o, Player.class)).preUnloadChunk(this.location)) {
				return true;
			}

			// Player still has to receive this chunk
			// Perform custom removal logic, preventing the unload chunk being
			// sent
			// This is to overcome the [0,0] chunk hole problem
			super.remove(o);
			if (super.isEmpty()) {
				// Remove this player instance from the player manager
				this.playerManager.removeInstance(this.location);
				ChunkUtil.setChunkUnloading(this.playerManager.world, location.x, location.z, true);
			}
		}
		return false;
	}
}
