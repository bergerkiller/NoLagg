package com.bergerkiller.bukkit.nolaggchunks;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.WorldListener;

public class NLWorldListener extends WorldListener {

	@Override
	public void onChunkPopulate(ChunkPopulateEvent event) {
		for (Player p : event.getWorld().getPlayers()) {
			PlayerChunkBuffer pcb = PlayerChunkLoader.getBuffer(p);
			BufferedChunk bc = pcb.get(event.getChunk().getX(), event.getChunk().getZ());
			if (bc == null) continue;
			Task t = new Task(NoLaggChunks.plugin, bc, event.getWorld()) {
				public void run() {
					BufferedChunk bc = (BufferedChunk) getArg(0);
					World w = (World) getArg(1);
					bc.clear();
					bc.queueChunk(w);
				}
			};
			t.startDelayed(10, true);
		}
	}
	
}
