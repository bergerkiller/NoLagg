package com.bergerkiller.bukkit.nolagg.chunks.antiloader;

import java.util.ArrayList;

import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.reflection.classes.PlayerChunkRef;
import com.bergerkiller.bukkit.common.utils.ChunkUtil;
import com.bergerkiller.bukkit.common.utils.PlayerUtil;
import com.bergerkiller.bukkit.nolagg.chunks.ChunkSendQueue;

@SuppressWarnings("rawtypes")
public class DummyInstancePlayerList extends ArrayList {
	@SuppressWarnings("unchecked")
	public static void replace(DummyPlayerManager playerManager, Object playerInstance) {
		DummyInstancePlayerList list = new DummyInstancePlayerList();
		list.playerManager = playerManager;
		list.location = Conversion.toIntVector2.convert(PlayerChunkRef.location.get(playerInstance));
		list.addAll(PlayerChunkRef.players.get(playerInstance));
		PlayerChunkRef.players.set(playerInstance, list);
	}

	private static final long serialVersionUID = -1878411514739243453L;
	public static boolean FILTER = false;
	private DummyPlayerManager playerManager;
	private IntVector2 location;

	@Override
	public boolean contains(Object o) {
		if (super.contains(o)) {
			if (!FILTER) {
				return true;
			}
			Player player = Conversion.toPlayer.convert(o);
			if (PlayerUtil.isChunkVisible(player, this.location.x, this.location.z)) {
				// Remove from queue
				ChunkSendQueue.bind(player).removePair(this.location);
				return true;
			}

			// Player still has to receive this chunk
			// Perform custom removal logic, preventing the unload chunk being sent
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
